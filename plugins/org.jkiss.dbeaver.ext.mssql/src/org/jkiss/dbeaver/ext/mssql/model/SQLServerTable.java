/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCheckConstraintContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServerTable
 */
public class SQLServerTable extends SQLServerTableBase
        implements DBPObjectStatistics, DBSCheckConstraintContainer, DBPReferentialIntegrityController, DBSEntityConstrainable {
    private static final Log log = Log.getLog(SQLServerTable.class);

    private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE ? NOCHECK CONSTRAINT ALL";
    private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL";

    private final CheckConstraintCache checkConstraintCache = new CheckConstraintCache();

    private transient volatile List<SQLServerTableForeignKey> references;

    private transient volatile long totalBytes = -1;
    private transient volatile long usedBytes = -1;

    public SQLServerTable(SQLServerSchema schema) {
        super(schema);
    }

    public SQLServerTable(@NotNull SQLServerSchema catalog, @NotNull ResultSet dbResult, @NotNull String name) {
        super(catalog, dbResult, name);
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, expensive = true, order = 30)
    @Override
    public Long getRowCount(DBRProgressMonitor monitor) throws DBCException {
        readTableStats(monitor);
        return super.getRowCount(monitor);
    }

    @Property(viewable = true, category = DBConstants.CAT_STATISTICS, order = 31)
    public long getTotalBytes(DBRProgressMonitor monitor) throws DBCException {
        readTableStats(monitor);
        return totalBytes;
    }

    @Property(viewable = true, category = DBConstants.CAT_STATISTICS, order = 32)
    public long getUsedBytes(DBRProgressMonitor monitor) throws DBCException {
        readTableStats(monitor);
        return usedBytes;
    }

    @Nullable
    @Override
    @Association
    public Collection<SQLServerTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getUniqueConstraintCache().getObjects(monitor, getSchema(), this);
    }

    @Nullable
    @Association
    public synchronized Collection<SQLServerTableCheckConstraint> getCheckConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return checkConstraintCache.getAllObjects(monitor, this);
    }

    public CheckConstraintCache getCheckConstraintCache() {
        return checkConstraintCache;
    }

    @Override
    @Association
    public List<SQLServerTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (references != null || monitor == null) {
            return references;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this,  "Read table references")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.schema_id as schema_id,t.name as table_name,fk.name as key_name\n" +
                    "FROM " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " t, " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "foreign_keys") + " fk, " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " tr\n" +
                    "WHERE t.object_id = fk.parent_object_id AND tr.object_id=fk.referenced_object_id AND fk.referenced_object_id=?\n" +
                    "ORDER BY 1,2,3")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<SQLServerTableForeignKey> result = new ArrayList<>();
                    while (dbResult.next()) {
                        long schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                        String tableName = JDBCUtils.safeGetString(dbResult, "table_name");
                        String fkName = JDBCUtils.safeGetString(dbResult, "key_name");

                        SQLServerSchema schema = getDatabase().getSchema(monitor, schemaId);
                        if (schema != null) {
                            SQLServerTableBase table = schema.getTable(monitor, tableName);
                            if (table != null) {
                                DBSEntityAssociation object = DBUtils.findObject(table.getAssociations(monitor), fkName);
                                if (object instanceof SQLServerTableForeignKey) {
                                    result.add((SQLServerTableForeignKey) object);
                                }
                            }
                        }
                    }
                    this.references = result;
                    return result;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @Override
    public Collection<SQLServerTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchema().getForeignKeyCache().getObjects(monitor, getSchema(), this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return OPTION_DDL_ONLY_FOREIGN_KEYS.equals(option)
            || OPTION_DDL_SKIP_FOREIGN_KEYS.equals(option)
            || OPTION_INCLUDE_NESTED_OBJECTS.equals(option);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        references = null;
        totalBytes = -1;
        usedBytes = -1;
        getSchema().resetTableStatistics();

        getContainer().getIndexCache().clearObjectCache(this);
        getContainer().getUniqueConstraintCache().clearObjectCache(this);
        getContainer().getForeignKeyCache().clearObjectCache(this);

        return super.refreshObject(monitor);
    }

    @Override
    boolean supportsTriggers() {
        return true;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        // Nope
    }

    @Override
    public boolean hasStatistics() {
        return totalBytes != -1;
    }

    @Override
    public long getStatObjectSize() {
        return totalBytes;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    private void readTableStats(DBRProgressMonitor monitor) throws DBCException {
        if (hasStatistics()) {
            return;
        }
        if (SQLServerUtils.isDriverBabelfish(getDataSource().getContainer().getDriver())) {
            setDefaultTableStats();
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table statistics")) {
            try (JDBCPreparedStatement dbStat = SQLServerUtils.prepareTableStatisticLoadStatement(
                session,
                getDataSource(),
                getDatabase(),
                getSchema().getObjectId(),
                this,
                true)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchTableStats(dbResult);
                    } else {
                        setDefaultTableStats();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading table statistics", e);
        }
    }

    void fetchTableStats(JDBCResultSet dbResult) throws SQLException {
        rowCount = dbResult.getLong("rows");
        totalBytes = dbResult.getLong("totalSize") * 1024;
        usedBytes = dbResult.getLong("usedSize") * 1024;
    }

    void setDefaultTableStats() {
        totalBytes = 0;
        usedBytes = 0;
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) {
        return true;
    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        String sql = getChangeReferentialIntegrityStatement(monitor, enable);
        sql = sql.replace("?", getFullyQualifiedName(DBPEvaluationContext.DDL));
        try {
            JDBCUtils.executeInMetaSession(monitor, this, "Changing referential integrity", sql);
        } catch (SQLException e) {
            throw new DBException("Unable to change referential integrity", e);
        }
    }

    @NotNull
    @Override
    public String getChangeReferentialIntegrityStatement(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        if (enable) {
            return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
    }

    @NotNull
    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        return List.of(
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, SQLServerTableUniqueKey.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, SQLServerTableUniqueKey.class)
            // DBSEntityConstraintInfo.of(DBSEntityConstraintType.CHECK, SQLServerTableCheckConstraint.class) only as a separate key class
        );
    }

    /**
     * Constraint cache implementation
     */
    static class CheckConstraintCache extends JDBCObjectCache<SQLServerTable, SQLServerTableCheckConstraint> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerTable table) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT c.* FROM " + SQLServerUtils.getSystemTableName(table.getDatabase(), "check_constraints") + " c WHERE c.parent_object_id=?");
            dbStat.setLong(1, table.getObjectId());
            return dbStat;
        }

        @Override
        protected SQLServerTableCheckConstraint fetchObject(@NotNull JDBCSession session, @NotNull SQLServerTable table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerTableCheckConstraint(table, resultSet);
        }
    }
}
