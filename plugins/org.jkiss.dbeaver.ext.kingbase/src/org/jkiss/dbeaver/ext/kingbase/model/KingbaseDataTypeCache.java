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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
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

/**
 * KingbaseDataTypeCache
 */
public class KingbaseDataTypeCache extends JDBCObjectCache<KingbaseSchema, KingbaseDataType>
{
    private static final Log log = Log.getLog(KingbaseDataTypeCache.class);

    private final LongKeyMap<KingbaseDataType> dataTypeMap = new LongKeyMap<>();

    KingbaseDataTypeCache() {
        setListOrderComparator(DBUtils.nameComparator());
        setCaseSensitive(false);
    }

    @Override
    protected String getCacheName() {
        return "Data type cache";
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, KingbaseSchema schema) throws DBException {
        super.loadObjects(monitor, schema);
        mapAliases(schema);

    }

    void loadDefaultTypes(KingbaseSchema schema) {

        List<KingbaseDataType> types = new ArrayList<>();
        for (Field oidField : KingbaseOid.class.getDeclaredFields()) {
            if (!Modifier.isPublic(oidField.getModifiers()) || !Modifier.isStatic(oidField.getModifiers())) {
                continue;
            }
            try {
                Object typeId = oidField.get(null);
                String fieldName = oidField.getName().toLowerCase(Locale.ENGLISH);
                if (fieldName.endsWith("_array")) {
                    fieldName = fieldName.substring(0, fieldName.length() - 6) + "_";
                    continue;
                } else {
                    KingbaseDataType type = new KingbaseDataType(schema, CommonUtils.toInt(typeId), fieldName);
                    types.add(type);
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        setCache(types);
        mapAliases(schema);
    }

    void mapAliases(KingbaseSchema schema) {
        if (schema.isCatalogSchema() || schema.isSysCatalogSchema()) {
            KingbaseServerExtension serverType = schema.getDataSource().getServerType();
            mapDataTypeAliases(serverType.getDataTypeAliases(), false);
            if (serverType.supportSerialTypes()) {
                mapDataTypeAliases(KingbaseConstants.SERIAL_TYPES, true);
            }
        }
    }

    private void mapDataTypeAliases(Map<String, String> aliases, boolean isSerialType) {
        for (Map.Entry<String,String> aliasMapping : aliases.entrySet()) {
            String value = aliasMapping.getValue();
            KingbaseDataType realType = getCachedObject(value);
            if (realType != null) {
                KingbaseDataType serialType = new KingbaseDataType(realType, aliasMapping.getKey());
                int typeId = -1;
                if (isSerialType) {
                    switch (value) {
                        case KingbaseConstants.TYPE_INT4:
                            typeId = KingbaseOid.SERIAL;
                            break;
                        case KingbaseConstants.TYPE_INT2:
                            typeId = KingbaseOid.SMALLSERIAL;
                            break;
                        case KingbaseConstants.TYPE_INT8:
                            typeId = KingbaseOid.BIGSERIAL;
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
    public void removeObject(@NotNull KingbaseDataType object, boolean resetFullCache) {
        super.removeObject(object, resetFullCache);
        dataTypeMap.remove(object.getObjectId());
    }

    @Override
    public void cacheObject(@NotNull KingbaseDataType object) {
        if (getCachedObject(object.getName()) != null) {

        } else {
            super.cacheObject(object);
            if (!object.isAlias() || object.isExtraDataType()) {
                dataTypeMap.put(object.getObjectId(), object);
            }
        }
    }

    @Override
    public void setCache(@NotNull List<KingbaseDataType> kingbaseDataTypes) {
        super.setCache(kingbaseDataTypes);
        for (KingbaseDataType dt : kingbaseDataTypes) {
            if (!dt.isAlias()) {
                dataTypeMap.put(dt.getObjectId(), dt);
            }
        }
    }

    static String getBaseTypeNameClause(@NotNull KingbaseDataSource dataSource) {
        
        return "format_type(nullif(t.typbasetype, 0), t.typtypmod) as base_type_name";
        
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseSchema owner) throws SQLException {
        KingbaseDataSource dataSource = owner.getDataSource();
        boolean readAllTypes = dataSource.supportReadingAllDataTypes();
        boolean supportsSysTypColumn = owner.getDatabase().supportsSysTypCategoryColumn(session);
        StringBuilder sql = new StringBuilder(256);
        sql.append("SELECT t.oid,t.*,c.relkind,").append(getBaseTypeNameClause(dataSource)).append(", d.description" +
            "\nFROM sys_catalog.sys_type t");
        if (!readAllTypes && supportsSysTypColumn) {
            sql.append("\nLEFT OUTER JOIN sys_catalog.sys_type et ON et.oid=t.typelem ");
        }
        sql.append("\nLEFT OUTER JOIN sys_catalog.sys_class c ON c.oid=t.typrelid" +
            "\nLEFT OUTER JOIN sys_catalog.sys_description d ON t.oid=d.objoid" +
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
    protected KingbaseDataType fetchObject(@NotNull JDBCSession session, @NotNull KingbaseSchema owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return KingbaseDataType.readDataType(session, owner.getDatabase(), dbResult, true);
    }

    @Override
    protected void invalidateObjects(DBRProgressMonitor monitor, KingbaseSchema schema, Iterator<KingbaseDataType> objectIter) {
        while (objectIter.hasNext()) {
            KingbaseDataType dt = objectIter.next();
            dt.resolveValueTypeFromBaseType(monitor);
        }
    }

    public KingbaseDataType getDataType(long oid) {
        return dataTypeMap.get(oid);
    }

    @NotNull
    static KingbaseDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull KingbaseDatabase database, long oid) throws SQLException, DBException {
        try (JDBCSession session = database.getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, "Resolve data type by OID")) {
            try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.oid,t.*,c.relkind," + getBaseTypeNameClause(database.getDataSource()) + " FROM sys_catalog.sys_type t" +
                    "\nLEFT OUTER JOIN sys_class c ON c.oid=t.typrelid" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_description d ON t.oid=d.objoid" +
                    "\nWHERE t.oid=? ")) {
                dbStat.setLong(1, oid);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long schemaOid = JDBCUtils.safeGetLong(dbResult, "typnamespace");
                        KingbaseSchema schema = database.getSchema(monitor, schemaOid);
                        if (schema == null) {
                            throw new DBException("Schema " + schemaOid + " not found for data type " + oid);
                        }
                        KingbaseDataType dataType = KingbaseDataType.readDataType(session, database, dbResult, false);
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
    static KingbaseDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull KingbaseDatabase database, String name) throws SQLException, DBException {
        try (JDBCSession session = database.getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, "Resolve data type by name")) {
            try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.oid,t.*," + getBaseTypeNameClause(database.getDataSource()) + " FROM sys_catalog.sys_type t" +
                    "\nLEFT OUTER JOIN sys_class c ON c.oid=t.typrelid" +
                    "\nLEFT OUTER JOIN sys_catalog.sys_description d ON t.oid=d.objoid" +
                    "\nWHERE t.typname=? ")) {
                dbStat.setString(1, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long schemaOid = JDBCUtils.safeGetLong(dbResult, "typnamespace");
                        KingbaseSchema schema = database.getSchema(monitor, schemaOid);
                        if (schema == null) {
                            throw new DBException("Schema " + schemaOid + " not found for data type " + name);
                        }
                        KingbaseDataType dataType = KingbaseDataType.readDataType(session, database, dbResult, false);
                        if (dataType != null) {
                            return dataType;
                        }
                    }
                    throw new DBException("Data type " + name + " not found in database " + database.getName());
                }
            }
        }
    }

}
