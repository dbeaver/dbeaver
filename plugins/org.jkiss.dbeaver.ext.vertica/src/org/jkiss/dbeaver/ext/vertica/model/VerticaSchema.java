/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * VerticaSchema
 */
public class VerticaSchema extends GenericSchema implements DBPSystemObject
{
    private static final Log log = Log.getLog(VerticaSchema.class);

    private static final String SYSTEM_SCHEMAS[] = {
        "v_catalog",
        "v_internal",
        "v_monitor",
        "v_txtindex",
    };

    final ProjectionCache projectionCache = new ProjectionCache();
    final UDFCache udfCache = new UDFCache();

    public VerticaSchema(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        List<DBSObject> children = new ArrayList<>(getTables(monitor));
        children.addAll(getProjections(monitor));
        return children;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        DBSObject child = getTable(monitor, childName);
        if (child == null) {
            child = getProjection(monitor, childName);
        }
        return child;
    }

    @Association
    public Collection<GenericTable> getFlexTables(DBRProgressMonitor monitor) throws DBException {
        Collection<GenericTable> tables = getTables(monitor);
        if (tables != null) {
            List<GenericTable> filtered = new ArrayList<>();
            for (GenericTable table : tables) {
                if (table instanceof VerticaTable && ((VerticaTable) table).isFlexTable()) {
                    filtered.add(table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Association
    public Collection<VerticaProjection> getProjections(DBRProgressMonitor monitor) throws DBException {
        return projectionCache.getAllObjects(monitor, this);
    }

    @Association
    public VerticaProjection getProjection(DBRProgressMonitor monitor, String name) throws DBException {
        return projectionCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<VerticaUDF> getUserDefinedFunctions(DBRProgressMonitor monitor) throws DBException {
        return udfCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean isSystem() {
        return ArrayUtils.contains(SYSTEM_SCHEMAS, getName());
    }

    public class ProjectionCache extends JDBCStructLookupCache<VerticaSchema, VerticaProjection, VerticaProjectionColumn> {

        ProjectionCache()
        {
            super("projection_name");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull VerticaSchema schema, @Nullable VerticaProjection object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.*,c.comment FROM v_catalog.projections p\n" +
                    "LEFT OUTER JOIN v_catalog.comments c ON c.object_type = 'PROJECTION' AND c.object_schema = p.projection_schema AND c.object_name = p.projection_name\n" +
                    "WHERE p.projection_schema=?" +
                    (object == null && objectName == null ? "" : " AND projection_name=?")
            );
            dbStat.setString(1, schema.getName());
            if (object != null || objectName != null) dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected VerticaProjection fetchObject(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new VerticaProjection(VerticaSchema.this, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @Nullable VerticaProjection forTable)
            throws SQLException
        {
            String sql = ("SELECT pc.*,c.comment FROM v_catalog.projection_columns pc\n" +
                "LEFT OUTER JOIN v_catalog.comments c ON c.object_id = pc.column_id\n" +
                "WHERE pc.projection_id=?\n" +
                "ORDER BY pc.column_position");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            dbStat.setLong(1, forTable.getObjectId());
            return dbStat;
        }

        @Override
        protected VerticaProjectionColumn fetchChild(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @NotNull VerticaProjection table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new VerticaProjectionColumn(table, dbResult);
        }

    }

    public class UDFCache extends JDBCObjectLookupCache<VerticaSchema, VerticaUDF> {

        UDFCache()
        {
            super();
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull VerticaSchema schema, @Nullable VerticaUDF object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM v_catalog.user_functions WHERE schema_schema=?" +
                    (object == null && objectName == null ? "" : " AND function_name=?")
            );
            dbStat.setString(1, schema.getName());
            if (object != null || objectName != null) dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected VerticaUDF fetchObject(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new VerticaUDF(VerticaSchema.this, dbResult);
        }

    }

}
