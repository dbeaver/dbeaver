/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * PostgreDataTypeCache
 */
public class PostgreDataTypeCache extends JDBCObjectCache<PostgreSchema, PostgreDataType>
{
    private LongKeyMap<PostgreDataType> dataTypeMap = new LongKeyMap<>();

    PostgreDataTypeCache() {
        setListOrderComparator(DBUtils.nameComparator());
    }

    @Override
    protected String getCacheName() {
        return "Data type cache";
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, PostgreSchema schema) throws DBException {
        super.loadObjects(monitor, schema);

        // Cache aliases
        if (schema.isCatalogSchema()) {
            mapDataTypeAliases(schema.getDataSource().getServerType().getDataTypeAliases());
            mapDataTypeAliases(PostgreConstants.SERIAL_TYPES);
        }
    }

    private void mapDataTypeAliases(Map<String, String> aliases) {
        // Add serial data types
        for (Map.Entry<String,String> aliasMapping : aliases.entrySet()) {
            PostgreDataType realType = getCachedObject(aliasMapping.getValue());
            if (realType != null) {
                PostgreDataType serialType = new PostgreDataType(realType, aliasMapping.getKey());
                cacheObject(serialType);
            }
        }
    }


    @Override
    public void clearCache() {
        super.clearCache();
        dataTypeMap.clear();
    }

    @Override
    public void removeObject(@NotNull PostgreDataType object, boolean resetFullCache) {
        super.removeObject(object, resetFullCache);
        dataTypeMap.remove(object.getObjectId());
    }

    @Override
    public void cacheObject(@NotNull PostgreDataType object) {
        if (getCachedObject(object.getName()) != null) {

        } else {
            super.cacheObject(object);
            if (!object.isAlias()) {
                dataTypeMap.put(object.getObjectId(), object);
            }
        }
    }

    @Override
    public void setCache(List<PostgreDataType> postgreDataTypes) {
        super.setCache(postgreDataTypes);
        for (PostgreDataType dt : postgreDataTypes) {
            if (!dt.isAlias()) {
                dataTypeMap.put(dt.getObjectId(), dt);
            }
        }
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner) throws SQLException
    {
        // Initially cache only base types (everything but composite and arrays)
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.oid,t.*,c.relkind");
        if (owner.getDataSource().isServerVersionAtLeast(7, 3)) {
            sql.append(", format_type(nullif(t.typbasetype, 0), t.typtypmod) base_type_name");
        }
        sql.append("\n" +
            "FROM pg_catalog.pg_type t" +
            "\nLEFT OUTER JOIN pg_class c ON c.oid=t.typrelid" +
            "\nWHERE typnamespace=? ");
        if (PostgreUtils.supportsTypeCategory(session.getDataSource())) {
            //sql.append("AND t.typcategory <> 'A'");
        }
        sql.append("\nORDER by t.oid");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
        dbStat.setLong(1, owner.getObjectId());
        return dbStat;
    }

    @Override
    protected PostgreDataType fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return PostgreDataType.readDataType(session, owner, dbResult, true);
    }

    @Override
    protected void invalidateObjects(DBRProgressMonitor monitor, PostgreSchema postgreSchema, Iterator<PostgreDataType> objectIter) {
        // Resolve value type IDs (#3731)
        while (objectIter.hasNext()) {
            PostgreDataType dt = objectIter.next();
            dt.resolveValueTypeFromBaseType(monitor);
        }
    }

    public PostgreDataType getDataType(long oid) {
        return dataTypeMap.get(oid);
    }

    @NotNull
    static PostgreDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, long oid) throws SQLException, DBException {
        // Initially cache only base types (everything but composite and arrays)
        try (JDBCSession session = database.getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, "Resolve data type by OID")) {
            try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.oid,t.*,c.relkind FROM pg_catalog.pg_type t" +
                    "\nLEFT OUTER JOIN pg_class c ON c.oid=t.typrelid" +
                    "\nWHERE t.oid=? ")) {
                dbStat.setLong(1, oid);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long schemaOid = JDBCUtils.safeGetLong(dbResult, "typnamespace");
                        PostgreSchema schema = database.getSchema(monitor, schemaOid);
                        if (schema == null) {
                            throw new DBException("Schema " + schemaOid + " not found for data type " + oid);
                        }
                        PostgreDataType dataType = PostgreDataType.readDataType(session, schema, dbResult, false);
                        if (dataType != null) {
                            return dataType;
                        }
                    }
                    throw new DBException("Data type " + oid + " not found in database " + database.getName());
                }
            }
        }
    }

    @NotNull
    static PostgreDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, String name) throws SQLException, DBException {
        // Initially cache only base types (everything but composite and arrays)
        try (JDBCSession session = database.getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, "Resolve data type by name")) {
            try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.oid,t.* FROM pg_catalog.pg_type t" +
                    "\nLEFT OUTER JOIN pg_class c ON c.oid=t.typrelid" +
                    "\nWHERE t.typname=? ")) {
                dbStat.setString(1, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long schemaOid = JDBCUtils.safeGetLong(dbResult, "typnamespace");
                        PostgreSchema schema = database.getSchema(monitor, schemaOid);
                        if (schema == null) {
                            throw new DBException("Schema " + schemaOid + " not found for data type " + name);
                        }
                        return PostgreDataType.readDataType(session, schema, dbResult, false);
                    } else {
                        throw new DBException("Data type " + name + " not found in database " + database.getName());
                    }
                }
            }
            //dbStat;
        }
    }

}
