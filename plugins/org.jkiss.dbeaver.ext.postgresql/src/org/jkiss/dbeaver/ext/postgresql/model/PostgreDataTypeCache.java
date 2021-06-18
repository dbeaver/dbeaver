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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;

/**
 * PostgreDataTypeCache
 */
public class PostgreDataTypeCache extends JDBCObjectCache<PostgreSchema, PostgreDataType>
{
    private static final Log log = Log.getLog(PostgreDataTypeCache.class);

    private final LongKeyMap<PostgreDataType> dataTypeMap = new LongKeyMap<>();

    PostgreDataTypeCache() {
        setListOrderComparator(DBUtils.nameComparator());
        setCaseSensitive(false);
    }

    @Override
    protected String getCacheName() {
        return "Data type cache";
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, PostgreSchema schema) throws DBException {
        super.loadObjects(monitor, schema);
        mapAliases(schema);

    }

    void loadDefaultTypes(PostgreSchema schema) {

        List<PostgreDataType> types = new ArrayList<>();
        for (Field oidField : PostgreOid.class.getDeclaredFields()) {
            if (!Modifier.isPublic(oidField.getModifiers()) || !Modifier.isStatic(oidField.getModifiers())) {
                continue;
            }
            try {
                Object typeId = oidField.get(null);
                String fieldName = oidField.getName().toLowerCase(Locale.ENGLISH);
                if (fieldName.endsWith("_array")) {
                    fieldName = fieldName.substring(0, fieldName.length() - 6) + "_";
                    //PostgreDataType type = new PostgreDataType(schema, CommonUtils.toInt(typeId), fieldName);
                    //types.add(type);
                    // Ignore array types
                    continue;
                } else {
                    PostgreDataType type = new PostgreDataType(schema, CommonUtils.toInt(typeId), fieldName);
                    types.add(type);
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        setCache(types);
        // Cache aliases
        mapAliases(schema);
    }

    void mapAliases(PostgreSchema schema) {
        // Cache aliases
        if (schema.isCatalogSchema()) {
            PostgreServerExtension serverType = schema.getDataSource().getServerType();
            mapDataTypeAliases(serverType.getDataTypeAliases(), false);
            if (serverType.supportSerialTypes()) {
                mapDataTypeAliases(PostgreConstants.SERIAL_TYPES, true);
            }
        }
    }

    private void mapDataTypeAliases(Map<String, String> aliases, boolean isSerialType) {
        // Add serial data types
        for (Map.Entry<String,String> aliasMapping : aliases.entrySet()) {
            String value = aliasMapping.getValue();
            PostgreDataType realType = getCachedObject(value);
            if (realType != null) {
                PostgreDataType serialType = new PostgreDataType(realType, aliasMapping.getKey());
                int typeId = -1;
                if (isSerialType) {
                    switch (value) {
                        case PostgreConstants.TYPE_INT4:
                            typeId = PostgreOid.SERIAL;
                            break;
                        case PostgreConstants.TYPE_INT2:
                            typeId = PostgreOid.SMALLSERIAL;
                            break;
                        case PostgreConstants.TYPE_INT8:
                            typeId = PostgreOid.BIGSERIAL;
                            break;
                    }
                    serialType.setTypeId(typeId);
                    serialType.setExtraDataType(true);
                }
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
            if (!object.isAlias() || object.isExtraDataType()) {
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

    static String getBaseTypeNameClause(@NotNull PostgreDataSource dataSource) {
        if (dataSource.isServerVersionAtLeast(7, 3)) {
            return "format_type(nullif(t.typbasetype, 0), t.typtypmod) as base_type_name";
        } else {
            return "NULL as base_type_name";
        }
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner) throws SQLException {
        // Initially cache only base types (everything but composite and some arrays)
        PostgreDataSource dataSource = owner.getDataSource();
        boolean readAllTypes = dataSource.supportReadingAllDataTypes();
        boolean supportsSysTypColumn = owner.getDatabase().supportsSysTypCategoryColumn(session);
        StringBuilder sql = new StringBuilder(256);
        sql.append("SELECT t.oid,t.*,c.relkind,").append(getBaseTypeNameClause(dataSource)).append(", d.description" +
            "\nFROM pg_catalog.pg_type t");
        if (!readAllTypes && supportsSysTypColumn) {
            sql.append("\nLEFT OUTER JOIN pg_catalog.pg_type et ON et.oid=t.typelem ");
        }
        sql.append("\nLEFT OUTER JOIN pg_catalog.pg_class c ON c.oid=t.typrelid" +
            "\nLEFT OUTER JOIN pg_catalog.pg_description d ON t.oid=d.objoid" +
            "\nWHERE t.typname IS NOT NULL");
        if (!readAllTypes) {
            sql.append("\nAND (c.relkind IS NULL OR c.relkind = 'c')");
            if (supportsSysTypColumn) {
                sql.append(" AND (et.typcategory IS NULL OR et.typcategory <> 'C')");
            }
        }
        sql.append("\nAND t.typnamespace=? ");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
        dbStat.setLong(1, owner.getObjectId());
        return dbStat;
    }

    @Override
    protected PostgreDataType fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return PostgreDataType.readDataType(session, owner.getDatabase(), dbResult, true);
    }

    @Override
    protected void invalidateObjects(DBRProgressMonitor monitor, PostgreSchema schema, Iterator<PostgreDataType> objectIter) {
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
                "SELECT t.oid,t.*,c.relkind," + getBaseTypeNameClause(database.getDataSource()) + " FROM pg_catalog.pg_type t" +
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
                        PostgreDataType dataType = PostgreDataType.readDataType(session, database, dbResult, false);
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
                "SELECT t.oid,t.*," + getBaseTypeNameClause(database.getDataSource()) + " FROM pg_catalog.pg_type t" +
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
                        PostgreDataType dataType = PostgreDataType.readDataType(session, database, dbResult, false);
                        if (dataType != null) {
                            return dataType;
                        }
                    }
                    throw new DBException("Data type " + name + " not found in database " + database.getName());
                }
            }
            //dbStat;
        }
    }

}
