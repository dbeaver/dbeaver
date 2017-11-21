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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;

/**
 * VerticaSchema
 */
public class VerticaSchema extends GenericSchema
{
    private static final Log log = Log.getLog(VerticaSchema.class);

    ProjectionCache projectionCache = new ProjectionCache();

    public VerticaSchema(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Association
    public Collection<VerticaProjection> getProjections(DBRProgressMonitor monitor) throws DBException {
        return projectionCache.getAllObjects(monitor, this);
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
                "SELECT * FROM v_catalog.projections WHERE projections_schema=?" +
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
            return new VerticaProjection(VerticaSchema.this, dbResult.getString("projection_name"));
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @Nullable VerticaProjection forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT c.relname,a.*,pg_catalog.pg_get_expr(ad.adbin, ad.adrelid, true) as def_value,dsc.description" +
                "\nFROM pg_catalog.pg_attribute a" +
                "\nINNER JOIN pg_catalog.pg_class c ON (a.attrelid=c.oid)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_attrdef ad ON (a.attrelid=ad.adrelid AND a.attnum = ad.adnum)" +
                "\nLEFT OUTER JOIN pg_catalog.pg_description dsc ON (c.oid=dsc.objoid AND a.attnum = dsc.objsubid)" +
                "\nWHERE NOT a.attisdropped");
            if (forTable != null) {
                sql.append(" AND c.oid=?");
            } else {
                sql.append(" AND c.relnamespace=? AND c.relkind not in ('i','c')");
            }
            sql.append(" ORDER BY a.attnum");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setString(1, forTable.getName());
            } else {
                dbStat.setString(1, VerticaSchema.this.getName());
            }
            return dbStat;
        }

        @Override
        protected VerticaProjectionColumn fetchChild(@NotNull JDBCSession session, @NotNull VerticaSchema owner, @NotNull VerticaProjection table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new VerticaProjectionColumn(table, true);
        }

    }

}
