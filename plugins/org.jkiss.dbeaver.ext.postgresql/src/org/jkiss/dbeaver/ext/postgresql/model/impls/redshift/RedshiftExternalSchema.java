/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * RedshiftExternalSchema
 */
public class RedshiftExternalSchema extends PostgreSchema {
    private static final Log log = Log.getLog(RedshiftExternalSchema.class);

    private String esOptions;
    private final ExternalTableCache externalTableCache = new ExternalTableCache();

    public RedshiftExternalSchema(PostgreDatabase database, String name, String esOptions, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
        this.esOptions = esOptions;
    }

    public RedshiftExternalSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    public ExternalTableCache getExternalTableCache() {
        return externalTableCache;
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public boolean isStatisticsCollected() {
        return true;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        // Not supported
    }

/*
    protected void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "esoid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "esowner");
        this.esOptions = JDBCUtils.safeGetString(dbResult, "esoptions");
        this.persisted = true;
    }
*/

    @Property(viewable = true, editable = false, updatable = false, length = PropertyLength.MULTILINE, order = 50)
    public String getExternalOptions() {
        return esOptions;
    }

    @Override
    public PostgreTableBase getTable(DBRProgressMonitor monitor, long tableId) throws DBException {
        return null;
    }

    @Association
    public Collection<RedshiftExternalTable> getExternalTables(DBRProgressMonitor monitor) throws DBException {
        return externalTableCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<? extends PostgreTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return getExternalTables(monitor);
    }

    @Override
    public Collection<RedshiftExternalTable> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getExternalTables(monitor);
    }

    @Override
    public RedshiftExternalTable getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return externalTableCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return RedshiftExternalTable.class;
    }

    public class ExternalTableCache extends JDBCStructLookupCache<RedshiftExternalSchema, RedshiftExternalTable, RedshiftExternalTableColumn> {

        protected ExternalTableCache() {
            super(JDBCConstants.TABLE_NAME);
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull RedshiftExternalSchema postgreSchema, @Nullable RedshiftExternalTable object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM pg_catalog.svv_external_tables WHERE schemaname=?" +
                    (object == null && objectName == null ? "" : " AND tablename=?"));
            dbStat.setString(1, postgreSchema.getName());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected RedshiftExternalTable fetchObject(@NotNull JDBCSession session, @NotNull RedshiftExternalSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new RedshiftExternalTable(owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull RedshiftExternalSchema owner, @Nullable RedshiftExternalTable forTable)
            throws SQLException {
            return session.getMetaData().getColumns(
                null,
                owner.getName(),
                forTable == null ? null : forTable.getName(),
                null).getSourceStatement();
        }

        @Override
        protected RedshiftExternalTableColumn fetchChild(@NotNull JDBCSession session, @NotNull RedshiftExternalSchema owner, @NotNull RedshiftExternalTable table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TYPE_NAME);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.COLUMN_SIZE);
            boolean isNotNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
            Integer scale = null;
            try {
                scale = JDBCUtils.safeGetInteger(dbResult, JDBCConstants.DECIMAL_DIGITS);
            } catch (Throwable e) {
                log.warn("Error getting column scale", e);
            }
            Integer precision = null;
            if (valueType == Types.NUMERIC || valueType == Types.DECIMAL) {
                precision = (int) columnSize;
            }
            String defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
            int ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            boolean autoGenerated = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.IS_GENERATEDCOLUMN));

            return new RedshiftExternalTableColumn(
                table,
                true,
                columnName,
                typeName, valueType, ordinalPos,
                columnSize,
                scale, precision, isNotNull, autoGenerated, defaultValue
            );
        }

    }

}

