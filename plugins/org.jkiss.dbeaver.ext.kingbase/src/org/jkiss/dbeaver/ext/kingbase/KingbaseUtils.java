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

package org.jkiss.dbeaver.ext.kingbase;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.kingbase.edit.KingbaseCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.kingbase.edit.KingbaseViewManager;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseAttribute;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDefaultPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseObject;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseObjectPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseOid;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeGrant;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeOwner;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRole;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRoleReference;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSequence;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseView;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseViewBase;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerKingbaseSQL;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerType;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerTypeRegistry;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

/**
 * KingbaseUtils
 */
public class KingbaseUtils {

    private static final Log log = Log.getLog(KingbaseUtils.class);

    private static final int UNKNOWN_LENGTH = -1;

    private static final Pattern ROLE_TYPE_PATTERN = Pattern.compile("^\\w+\\s+");

    public static String getObjectComment(DBRProgressMonitor monitor, GenericStructContainer container, String schema, String object)
            throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load Kingbase description")) {
            return JDBCUtils.queryString(
                    session,
                    "select description from sys_catalog.sys_description\n" +
                            "join sys_catalog.sys_class on sys_description.objoid = sys_class.oid\n" +
                            "join sys_catalog.sys_namespace on sys_class.relnamespace = sys_namespace.oid\n" +
                            "where sys_class.relname = ? and sys_namespace.nspname=?", object, schema);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN:
                return "bool";
            case NUMERIC:
                return "int";
            case STRING:
                return "varchar";
            case DATETIME:
                return "timestamp";
            case BINARY:
                return "bytea";
            case CONTENT:
                return "bytea";
            case ROWID:
                return "oid";
            default:
                return "varchar";
        }
    }

    public static <T extends KingbaseAttribute> T getAttributeByNum(Collection<T> attrs, int attNum) {
        for (T attr : attrs) {
            if (attr.getOrdinalPosition() == attNum) {
                return attr;
            }
        }
        return null;
    }

    public static boolean isKBObject(Object object) {
        if (object == null) {
            return false;
        }
        String className = object.getClass().getName();
        return className.equals(KingbaseConstants.KB_OBJECT_CLASS);
    }

    public static Object extractKBObjectValue(Object kbObject) {
        if (kbObject == null) {
            return null;
        }
        if (!isKBObject(kbObject)) {
            return kbObject;
        }
        try {
            return kbObject.getClass().getMethod("getValue").invoke(kbObject);
        } catch (Exception e) {
            log.debug("Can't extract value from " + kbObject.getClass().getName(), e);
        }
        return null;
    }

    public static boolean supportsTypeCategory(JDBCDataSource dataSource) {
        return true;
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends KingbaseObject> OBJECT getObjectById(
            @Nullable DBRProgressMonitor monitor,
            @NotNull AbstractObjectCache<OWNER, OBJECT> cache,
            @NotNull OWNER owner,
            long objectId)
            throws DBException {
        Collection<OBJECT> objects;
        if (monitor == null) {
            objects = cache.getCachedObjects();
        } else {
            objects = cache.getAllObjects(monitor, owner);
        }
        for (OBJECT object : objects) {
            if (object.getObjectId() == objectId) {
                return object;
            }
        }
        return null;
    }

    public static long[] getIdVector(Object kbObject) {
        Object kbVector = extractKBObjectValue(kbObject);
        if (kbVector == null) {
            return null;
        }
        if (kbVector instanceof String) {
            final String vector = (String) kbVector;
            if (vector.isEmpty()) {
                return null;
            }
            final String[] strings = vector.split(KingbaseConstants.DEFAULT_ARRAY_DELIMITER);
            final long[] ids = new long[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ids[i] = CommonUtils.toLong(strings[i]);
            }
            return ids;
        } else if (kbVector instanceof long[]) {
            return (long[]) kbVector;
        } else if (kbVector instanceof Long[]) {
            Long[] objVector = (Long[]) kbVector;
            long[] result = new long[objVector.length];
            for (int i = 0; i < objVector.length; i++) {
                result[i] = objVector[i];
            }
            return result;
        } else if (kbVector instanceof Number) {
            return new long[]{((Number) kbVector).longValue()};
        } else if (kbVector instanceof java.sql.Array) {
            try {
                Object array = ((java.sql.Array) kbVector).getArray();
                if (array == null) {
                    return null;
                }
                int length = Array.getLength(array);
                long[] result = new long[length];
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(array, i);
                    if (item instanceof Number) {
                        result[i] = ((Number) item).longValue();
                    } else if (item != null) {
                        throw new IllegalArgumentException("Bad array item type: " + item.getClass().getName());
                    }
                }
                return result;
            } catch (SQLException e) {
                throw new IllegalArgumentException("Error reading array value: " + kbVector);
            }
        } else {
            throw new IllegalArgumentException("Unsupported vector type: " + kbVector.getClass().getName());
        }
    }

    public static int[] getIntVector(Object kbObject) {
        Object kbVector = extractKBObjectValue(kbObject);
        if (kbVector == null) {
            return null;
        }
        if (kbVector instanceof String vector) {
            if (vector.isEmpty()) {
                return null;
            }
            final String[] strings = vector.split(KingbaseConstants.DEFAULT_ARRAY_DELIMITER);
            final int[] ids = new int[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ids[i] = CommonUtils.toInt(strings[i]);
            }
            return ids;
        } else if (kbVector instanceof int[] intVector) {
            return intVector;
        } else if (kbVector instanceof Integer[] objVector) {
            int[] result = new int[objVector.length];
            for (int i = 0; i < objVector.length; i++) {
                result[i] = objVector[i];
            }
            return result;
        } else if (kbVector instanceof Number number) {
            return new int[]{number.intValue()};
        } else if (kbVector instanceof java.sql.Array kbArray) {
            try {
                Object array = kbArray.getArray();
                if (array == null) {
                    return null;
                }
                int length = Array.getLength(array);
                int[] result = new int[length];
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(array, i);
                    if (item instanceof Number) {
                        result[i] = ((Number) item).intValue();
                    } else if (item != null) {
                        throw new IllegalArgumentException("Bad array item type: " + item.getClass().getName());
                    }
                }
                return result;
            } catch (SQLException e) {
                throw new IllegalArgumentException("Error reading array value: " + kbVector);
            }
        } else {
            throw new IllegalArgumentException("Unsupported vector type: " + kbVector.getClass().getName());
        }
    }

    public static int getAttributePrecision(long typeOid, int typeMod) {
        //typeOid = convertArrayToBaseOid(typeOid);
        switch ((int) typeOid) {
            case KingbaseOid.INT2:
                return 5;

            case KingbaseOid.OID:
            case KingbaseOid.INT4:
                return 10;

            case KingbaseOid.INT8:
                return 19;

            case KingbaseOid.FLOAT4:
                return 8;

            case KingbaseOid.FLOAT8:
                return 17;

            case KingbaseOid.NUMERIC:
                if (typeMod == -1)
                    return 0;
                return ((typeMod - 4) & 0xFFFF0000) >> 16;

            case KingbaseOid.CHAR:
            case KingbaseOid.BOOL:
                return 1;

            case KingbaseOid.BPCHAR:
            case KingbaseOid.VARCHAR:
                if (typeMod == -1)
                    return UNKNOWN_LENGTH;
                return typeMod - 4;

            case KingbaseOid.DATE:
            case KingbaseOid.TIME:
            case KingbaseOid.TIMETZ:
            case KingbaseOid.INTERVAL:
            case KingbaseOid.TIMESTAMP:
            case KingbaseOid.TIMESTAMPTZ:
                return getDisplaySize(typeOid, typeMod);

            case KingbaseOid.BIT:
                return typeMod;

            case KingbaseOid.VARBIT:
                if (typeMod == -1)
                    return UNKNOWN_LENGTH;
                return typeMod;

            case KingbaseOid.TEXT:
            case KingbaseOid.BYTEA:
            default:
                return UNKNOWN_LENGTH;
        }
    }

    public static int getDisplaySize(long oid, int typmod) {
        switch ((int) oid) {
            case KingbaseOid.INT2:
                return 6; // -32768 to +32767
            case KingbaseOid.INT4:
                return 11; // -2147483648 to +2147483647
            case KingbaseOid.OID:
                return 10; // 0 to 4294967295
            case KingbaseOid.INT8:
                return 20; // -9223372036854775808 to +9223372036854775807
            case KingbaseOid.FLOAT4:
                return 15; // sign + 9 digits + decimal point + e + sign + 2 digits
            case KingbaseOid.FLOAT8:
                return 25; // sign + 18 digits + decimal point + e + sign + 3 digits
            case KingbaseOid.CHAR:
                return 1;
            case KingbaseOid.BOOL:
                return 1;
            case KingbaseOid.DATE:
                return 13; // "4713-01-01 BC" to  "01/01/4713 BC" - "31/12/32767"
            case KingbaseOid.TIME:
            case KingbaseOid.TIMETZ:
            case KingbaseOid.TIMESTAMP:
            case KingbaseOid.TIMESTAMPTZ:
                // Calculate the number of decimal digits + the decimal point.
                int secondSize;
                switch (typmod) {
                    case -1:
                        secondSize = 6 + 1;
                        break;
                    case 0:
                        secondSize = 0;
                        break;
                    case 1:
                        // Bizarrely SELECT '0:0:0.1'::time(1); returns 2 digits.
                        secondSize = 2 + 1;
                        break;
                    default:
                        secondSize = typmod + 1;
                        break;
                }

                switch ((int) oid) {
                    case KingbaseOid.TIME:
                        return 8 + secondSize;
                    case KingbaseOid.TIMETZ:
                        return 8 + secondSize + 6;
                    case KingbaseOid.TIMESTAMP:
                        return 13 + 1 + 8 + secondSize;
                    case KingbaseOid.TIMESTAMPTZ:
                        return 13 + 1 + 8 + secondSize + 6;
                }
            case KingbaseOid.INTERVAL:
                return 49; // SELECT LENGTH('-123456789 years 11 months 33 days 23 hours 10.123456 seconds'::interval);
            case KingbaseOid.VARCHAR:
            case KingbaseOid.BPCHAR:
                if (typmod == -1)
                    return UNKNOWN_LENGTH;
                return typmod - 4;
            case KingbaseOid.NUMERIC:
                if (typmod == -1)
                    return 131089; // SELECT LENGTH(pow(10::numeric,131071)); 131071 = 2^17-1
                int precision = (typmod - 4 >> 16) & 0xffff;
                int scale = (typmod - 4) & 0xffff;
                // sign + digits + decimal point (only if we have nonzero scale)
                return 1 + precision + (scale != 0 ? 1 : 0);
            case KingbaseOid.BIT:
                return typmod;
            case KingbaseOid.VARBIT:
                if (typmod == -1)
                    return UNKNOWN_LENGTH;
                return typmod;
            case KingbaseOid.TEXT:
            case KingbaseOid.BYTEA:
                return UNKNOWN_LENGTH;
            default:
                return UNKNOWN_LENGTH;
        }
    }

    public static KingbaseDataType findDataType(DBCSession session, KingbaseDataSource dataSource, DBSTypedObject type) throws DBCException {
        if (type instanceof KingbaseDataType) {
            return (KingbaseDataType) type;
        } else if (type instanceof KingbaseAttribute) {
            return ((KingbaseAttribute) type).getDataType();
        } else {
            DBRProgressMonitor monitor = session.getProgressMonitor();
            if (type instanceof JDBCColumnMetaData) {
                try {
                    DBCEntityMetaData entityMetaData = ((DBCAttributeMetaData) type).getEntityMetaData();
                    if (entityMetaData != null) {
                        DBSEntity docEntity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), entityMetaData);
                        if (docEntity != null) {
                            DBSEntityAttribute attribute = docEntity.getAttribute(monitor, ((DBCAttributeMetaData) type).getName());
                            if (attribute instanceof DBSTypedObjectEx) {
                                DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
                                if (dataType instanceof KingbaseDataType) {
                                    return (KingbaseDataType) dataType;
                                }
                            }
                        }
                    }
                    {
                        String databaseName = ((JDBCColumnMetaData) type).getCatalogName();
                        KingbaseDatabase database = dataSource.getDatabase(databaseName);
                        if (database != null) {
                            String typeName = type.getTypeName();
                            if (KingbaseUtils.isCompositeTypeName(typeName)) {
                                // Type name in JDBCColumnMetaData can be fully qualified and quoted. Let's fix it for the better search in the getDataType() method
                                String[] identifiers = SQLUtils.splitFullIdentifier(typeName, ".", dataSource.getSQLDialect().getIdentifierQuoteStrings(), false);
                                if (!ArrayUtils.isEmpty(identifiers)) {
                                    typeName = identifiers[identifiers.length - 1];
                                    if (identifiers.length == 2) {
                                        // Most likely, in the identifiers array we have the name of the scheme and the name of the data type in this case
                                        // Try to find data type in the schema data type cache
                                        String schemaName = identifiers[0];
                                        KingbaseSchema schema = database.getSchema(monitor, schemaName);
                                        if (schema != null) {
                                            KingbaseDataType dataType = schema.getDataTypeCache().getObject(monitor, schema, typeName);
                                            if (dataType != null) {
                                                return dataType;
                                            }
                                        }
                                    }
                                }
                            }
                            KingbaseDataType dataType = database.getDataType(monitor, typeName);
                            if (dataType != null) {
                                return dataType;
                            }
                        }
                    }
                } catch (DBException e) {
                    throw new DBCException("Error extracting column " + type + " data type", e);
                }
            }

            String typeName = type.getTypeName();
            DBSInstance ownerInstance = session.getExecutionContext().getOwnerInstance();
            if (ownerInstance instanceof KingbaseDatabase) {
                KingbaseDataType localDataType = ((KingbaseDatabase) ownerInstance).getDataType(monitor, typeName);
                if (localDataType != null) {
                    return localDataType;
                }
            }
            return dataSource.getLocalDataType(typeName);
        }
    }

    @Nullable
    public static KingbaseDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull KingbaseSchema schema, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(monitor, schema.getDataSource(), schema.getDatabase(), schema, fullTypeName);
    }

    @Nullable
    public static KingbaseDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull KingbaseDatabase database, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(monitor, database.getDataSource(), database, database.getMetaContext().getDefaultSchema(), fullTypeName);
    }

    @Nullable
    public static KingbaseDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull KingbaseDataSource dataSource, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(
            monitor, dataSource, dataSource.getDefaultInstance(),
            dataSource.getDefaultInstance().getMetaContext().getDefaultSchema(), fullTypeName
        );
    }

    @Nullable
    private static KingbaseDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull KingbaseDataSource dataSource, @NotNull KingbaseDatabase database,
        @NotNull KingbaseSchema schema, @NotNull String fullTypeName
    ) throws DBException {
        final String identifier = DBUtils.getTypeModifiers(fullTypeName).getFirst();
        String[] parts = splitTypeNameIdentifier(dataSource, fullTypeName);

        // Try to get cashed data type from specified schema
        KingbaseDataType dataType = schema.getDataTypeCache().getObject(monitor, schema, identifier);
        if (dataType != null) {
            return dataType;
        }
        // Try to resolve local data type in specified database
        dataType = database.getLocalDataType(identifier);
        if (dataType != null) {
            return dataType;
        } else if (parts.length > 1) {
            // Search data type in schema from fullTypeName part
            KingbaseSchema resolvedSchema = database.getSchema(monitor, parts[0]);
            if (resolvedSchema != null) {
                String schemaTypeName;
                if (parts.length == 2) {
                    schemaTypeName = parts[1];
                } else {
                    schemaTypeName = DBUtils.getFullyQualifiedName(dataSource, Arrays.copyOfRange(parts, 1, parts.length));
                }

                dataType = resolvedSchema.getDataTypeCache().getObject(monitor, resolvedSchema, schemaTypeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }

        // Try to resolve local data type in specified data source
        dataType = dataSource.getLocalDataType(identifier);
        if (dataType != null) {
            return dataType;
        } else if (parts.length > 1) {
            // Search data type in database from fullTypeName part
            KingbaseDatabase resolvedDatabase = dataSource.getDatabase(parts[0]);
            if (resolvedDatabase != null) {
                String dbTypeName;
                if (parts.length == 2) {
                    dbTypeName = parts[1];
                } else {
                    dbTypeName = DBUtils.getFullyQualifiedName(dataSource, Arrays.copyOfRange(parts, 1, parts.length));
                }
                // Try to resolve local data type in database from fullTypeName part
                dataType = resolvedDatabase.getLocalDataType(dbTypeName);
                if (dataType != null) {
                    return dataType;
                } else if (parts.length > 2) {
                    // Search data type in database and schema from fullTypeName part
                    KingbaseSchema resolvedSchema = resolvedDatabase.getSchema(monitor, parts[1]);
                    if (resolvedSchema != null) {
                        String dbSchemaTypeName;
                        if (parts.length == 3) {
                            dbSchemaTypeName = parts[2];
                        } else {
                            dbSchemaTypeName = DBUtils.getFullyQualifiedName(dataSource, Arrays.copyOfRange(parts, 2, parts.length));
                        }
                        dataType = resolvedSchema.getDataTypeCache().getObject(monitor, resolvedSchema, dbSchemaTypeName);
                        if (dataType != null) {
                            return dataType;
                        }
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    private static String[] splitTypeNameIdentifier(
        @NotNull KingbaseDataSource dataSource, @NotNull String fullTypeName
    ) throws DBException {
        final Pair<String, String[]> typeNameInfo = DBUtils.getTypeModifiers(fullTypeName);
        final String identifier = typeNameInfo.getFirst();

        String[] parts;
        if (KingbaseUtils.isCompositeTypeName(identifier)) {
            parts = SQLUtils.splitFullIdentifier(identifier, ".", dataSource.getSQLDialect().getIdentifierQuoteStrings(), false);
        } else {
            parts = new String[]{identifier};
        }

        return parts;
    }
    
    private static boolean isCompositeTypeName(@NotNull String typeName) {
        return typeName.startsWith("\"") || typeName.contains(".");
    }

    public static void setArrayParameter(JDBCPreparedStatement dbStat, int index, List<? extends KingbaseObject> objectList) throws SQLException {
        for (int i = 0; i < objectList.size(); i++) {
            dbStat.setLong(index + i, objectList.get(i).getObjectId());
        }
    }

    public static String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull KingbaseViewBase view,
        @NotNull String definition,
        @NotNull Map<String, Object> options
    ) throws DBException {
        // In some cases view definition already has view header (e.g. Redshift + with no schema binding)
        if (definition.toLowerCase(Locale.ENGLISH).startsWith("create ")) {
            return definition;
        }
        StringBuilder sql = new StringBuilder(view instanceof KingbaseView ? "CREATE OR REPLACE " : "CREATE ");
        sql.append(view.getTableTypeName()).append(" ").append(DBUtils.getEntityScriptName(view, options));

        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        final KingbaseViewManager entityEditor = editorsRegistry.getObjectManager(view.getClass(), KingbaseViewManager.class);
        if (entityEditor != null) {
            entityEditor.appendViewDeclarationPrefix(monitor, sql, view);
        }
        definition = definition.trim();
        while (definition.endsWith(";")) {
            definition = definition.substring(0, definition.length() - 1);
        }
        sql.append("\nAS ").append(definition);
        if (entityEditor != null) {
            entityEditor.appendViewDeclarationPostfix(monitor, sql, view);
        }
        view.appendTableModifiers(monitor, sql);
        sql.append(";");
        return sql.toString();
    }

    public static KingbaseServerType getServerType(DBPDriver driver) {
        String serverTypeName = CommonUtils.toString(driver.getDriverParameter(KingbaseConstants.PROP_SERVER_TYPE));
        if (CommonUtils.isEmpty(serverTypeName)) {
            serverTypeName = KingbaseServerKingbaseSQL.TYPE_ID;
        }
        KingbaseServerType serverType = KingbaseServerTypeRegistry.getInstance().getServerType(serverTypeName);
        if (serverType == null) {
            throw new IllegalStateException("Kingbase server type '" + serverTypeName + "' not found");
        }
        return serverType;
    }

    public static Set<KingbaseRoleReference> extractGranteesFromACL(@NotNull KingbaseDatabase database, @NotNull String[] acl) {
        final Set<KingbaseRoleReference> grantees = new HashSet<>();
        for (String aclValue : acl) {
            if (CommonUtils.isEmpty(aclValue)) {
                continue;
            }
            int divPos = aclValue.indexOf('=');
            if (divPos == -1) {
                log.warn("Bad ACL item: " + aclValue);
                continue;
            }
            KingbaseRoleReference grantee = extractGranteeName(database, aclValue, divPos);
            grantees.add(grantee);
        }
        return grantees;
    }

    // FIXME consider user/group/role name like "test test", "test=test", "test,test", "test\"test" and user name like "group" or "role"
    public static List<KingbasePrivilege> extractPermissionsFromACL(
        @NotNull KingbasePrivilegeOwner owner,
        @NotNull String[] acl,
        boolean isDefault
    ) {
        List<KingbasePrivilege> permissions = new ArrayList<>();
        for (String aclValue : acl) {
            if (CommonUtils.isEmpty(aclValue)) {
                continue;
            }
            int divPos = aclValue.indexOf('=');
            if (divPos == -1) {
                log.warn("Bad ACL item: " + aclValue);
                continue;
            }
            KingbaseRoleReference grantee = extractGranteeName(owner.getDatabase(), aclValue, divPos);
            String permString = aclValue.substring(divPos + 1);
            int divPos2 = permString.indexOf('/');
            if (divPos2 == -1) {
                log.warn("Bad permissions string: " + permString);
                continue;
            }
            String privString = permString.substring(0, divPos2);
            String grantor = permString.substring(divPos2 + 1);

            List<KingbasePrivilegeGrant> privileges = new ArrayList<>();
            for (int k = 0; k < privString.length(); k++) {
                char pCode = privString.charAt(k);
                boolean withGrantOption = false;
                if (k < privString.length() - 1 && privString.charAt(k + 1) == '*') {
                    withGrantOption = true;
                    k++;
                }
                privileges.add(new KingbasePrivilegeGrant(
                    new KingbaseRoleReference(owner.getDatabase(), grantor, null),
                    grantee,
                    owner.getDatabase().getName(),
                    owner.getSchema().getName(),
                    owner.getName(),
                    KingbasePrivilegeType.getByCode(pCode),
                    withGrantOption,
                    false
                ));
            }
            if (isDefault) {
                permissions.add(new KingbaseDefaultPrivilege(owner, grantee, privileges));
            } else {
                permissions.add(new KingbaseObjectPrivilege(owner, grantee, privileges));
            }
        }
        return permissions;
    }

    @NotNull
    private static KingbaseRoleReference extractGranteeName(@NotNull KingbaseDatabase database, @NotNull String aclValue, int divPos) {
        String grantee = aclValue.substring(0, divPos).trim();
        String granteeType = null;
        if (grantee.isEmpty()) {
            grantee = "public";
        } else {
            Matcher m = ROLE_TYPE_PATTERN.matcher(grantee);
            if (m.find()) {
                int prefixEnd = m.end();
                if (prefixEnd < grantee.length()) {
                    granteeType = grantee.substring(0, prefixEnd).trim();
                    grantee = grantee.substring(prefixEnd).trim();
                }
            }
            grantee = DBUtils.getUnQuotedIdentifier(database.getDataSource(), grantee);
        }
        return new KingbaseRoleReference(database, grantee, granteeType);
    }

    public static List<KingbasePrivilege> extractPermissionsFromACL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull KingbasePrivilegeOwner owner,
        @Nullable Object acl,
        boolean isDefault
    ) throws DBException {
        if (!(acl instanceof java.sql.Array)) {
            if (acl == null) {
                // Special case. Means ALL permissions are granted to table owner
                KingbaseRole objectOwner = owner.getOwner(monitor);
                KingbaseRoleReference granteeReference = objectOwner == null ? null : objectOwner.getRoleReference();

                List<KingbasePrivilegeGrant> privileges = new ArrayList<>();
                privileges.add(
                        new KingbasePrivilegeGrant(
                                granteeReference,
                                granteeReference,
                                owner.getDatabase().getName(),
                                owner.getSchema().getName(),
                                owner.getName(),
                                KingbasePrivilegeType.ALL,
                                false,
                                false));
                KingbaseObjectPrivilege permission = new KingbaseObjectPrivilege(owner, granteeReference, privileges);
                return Collections.singletonList(permission);
            }
            return Collections.emptyList();
        }
        Object itemArray;
        try {
            itemArray = ((java.sql.Array) acl).getArray();
        } catch (SQLException e) {
            log.error(e);
            return Collections.emptyList();
        }
        int aclValuesCount = Array.getLength(itemArray);
        String[] aclValues = new String[aclValuesCount];
        for (int i = 0; i < aclValuesCount; i++) {
            Object aclItem = Array.get(itemArray, i);
            String aclValue = CommonUtils.toString(extractKBObjectValue(aclItem));
            // Quoted role names are stored with escaped quotes. We don't need quotes here (#13477)
            aclValue = aclValue.replace("\\\"", "\"");
            aclValues[i] = aclValue;
        }
        return extractPermissionsFromACL(owner, aclValues, isDefault);
    }

    public static String getOptionsString(String[] options) {
        StringBuilder opt = new StringBuilder();
        opt.append("(");
        if (!ArrayUtils.isEmpty(options)) {
            for (int i = 0; i < options.length; i++) {
                String option = options[i];
                if (i > 0) opt.append(", ");
                int divPos = option.indexOf('=');
                if (divPos < 0) {
                    opt.append(option);
                } else {
                    opt.append(option.substring(0, divPos)).append(" '").append(option.substring(divPos + 1)).append("'");
                }
            }
        }
        opt.append(")");
        return opt.toString();
    }

    public static String getObjectTypeName(KingbasePrivilegeOwner object) {
        if (object instanceof KingbaseSequence) {
            return "SEQUENCE";
        } else if (object instanceof KingbaseProcedure) {
            return ((KingbaseProcedure) object).getProcedureTypeName();
        } else if (object instanceof KingbaseSchema) {
            return "SCHEMA";
        } else if (object instanceof KingbaseDatabase) {
            return "DATABASE";
        } else {
            return "TABLE";
        }
    }

    public static String getObjectUniqueName(KingbasePrivilegeOwner object, Map<String, Object> options) {
        if (object instanceof KingbaseProcedure) {
            return ((KingbaseProcedure) object).getFullQualifiedSignature();
        } else {
            return DBUtils.getEntityScriptName(object, options);
        }
    }

    public static void getObjectGrantPermissionActions(DBRProgressMonitor monitor, KingbasePrivilegeOwner object, List<DBEPersistAction> actions, Map<String, Object> options) throws DBException {
        if (object.isPersisted() && CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(object, true);
            if (object.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO)) {
                actions.add(new SQLDatabasePersistActionComment(object.getDataSource(), "Permissions"));
            }

            // Owner
            KingbaseRole owner = object.getOwner(monitor);
            if (owner != null) {
                String alterScript = object.generateChangeOwnerQuery(DBUtils.getQuotedIdentifier(owner), options);
                if (!CommonUtils.isEmpty(alterScript)) {
                    actions.add(new SQLDatabasePersistAction("Owner change", alterScript));
                }
            }

            // Permissions
            Collection<KingbasePrivilege> permissions = object.getPrivileges(monitor, true);
            if (!CommonUtils.isEmpty(permissions)) {

                for (KingbasePrivilege permission : permissions) {
                    if (permission.hasAllPrivileges(object)) {
                        Collections.addAll(actions,
                                new KingbaseCommandGrantPrivilege(permission.getOwner(), true, object, permission, new KingbasePrivilegeType[]{KingbasePrivilegeType.ALL})
                                        .getPersistActions(monitor, executionContext, options));
                    } else {
                        KingbaseCommandGrantPrivilege grant = new KingbaseCommandGrantPrivilege(permission.getOwner(), true, object, permission, permission.getPrivileges());
                        Collections.addAll(actions, grant.getPersistActions(monitor, executionContext, options));
                    }
                }
            }
        }
    }

    public static boolean isGISDataType(String typeName) {
        return KingbaseConstants.TYPE_GEOMETRY.equals(typeName) ||
                KingbaseConstants.TYPE_GEOGRAPHY.equals(typeName);
    }

    public static String getRealSchemaName(KingbaseDatabase database, String name) {
        return name.replace(KingbaseConstants.USER_VARIABLE, database.getMetaContext().getActiveUser());
    }

    /**
     * Usually, we can check the info about system columns (whether existing or not, depending on the server version) in the documentation.
     * But sometimes, this approach is not working.
     * In this case, we can directly check the existing system column on the database.
     * If the column doesn't exist, then there will be an exception
     *
     * @param tableName name of the system table
     * @param columnName name of the system column. Use "*" param, if you need to check access to the full table/view.
     * @return query for the system column checking
     */
    @NotNull
    public static String getQueryForSystemColumnChecking(@NotNull String tableName, @NotNull String columnName) {
        return "SELECT " + columnName + " FROM sys_catalog." + tableName + " WHERE 1<>1 LIMIT 1";
    }

    /**
     * Returns state of the meta object existence from the system catalogs.
     *
     * @param session to execute a query
     * @param tableName name of the required table
     * @param columnName name of the required column or symbol *
     * @return state of the meta object existence in the system data
     */
    public static boolean isMetaObjectExists(@NotNull JDBCSession session, @NotNull String tableName, @NotNull String columnName) {
        try {
            JDBCUtils.queryString(session, getQueryForSystemColumnChecking(tableName, columnName));
            return true;
        } catch (SQLException e) {
            log.debug("Error reading system information from the " + tableName + " table: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves delimiter used for separating array elements of the given type.
     *
     * @param type type to get array delimiter for
     * @return a type-specific array delimiter, or {@code ","} if the given type is not a kingbase data type.
     */
    @NotNull
    public static String getArrayDelimiter(@NotNull DBSTypedObject type) {
        if (type instanceof KingbaseDataType) {
            return ((KingbaseDataType) type).getArrayDelimiter();
        } else {
            return ",";
        }
    }

    /**
     * Attempts to retrieve an array using {@link ResultSet#getArray(String)}, and if it can't
     * be done due to an exception, falls back to manually parsing the string representation
     * of an array retrieved using {@link ResultSet#getString(String)}.
     *
     * @param dbResult   a result set to retrieve data from
     * @param columnName a name of a column to retrieve data from
     * @param converter  a function that takes string representation of an element and returns {@code T}
     * @param generator  a function that takes a length and creates array of {@code T}
     * @return array elements
     * @see KingbaseValueParser#parsePrimitiveArray(String, Function, IntFunction)
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T[] safeGetArray(
        @NotNull ResultSet dbResult,
        @NotNull String columnName,
        @NotNull Function<String, T> converter,
        @NotNull IntFunction<T[]> generator
    ) {
        Exception exception = null;

        try {
            final java.sql.Array value = dbResult.getArray(columnName);
            return value != null ? (T[]) value.getArray() : null;
        } catch (SQLFeatureNotSupportedException ignored) {
            // Some drivers (ODBC) might not have an implementation for that API, just ignore and try with a string
        } catch (Exception e) {
            exception = e;
        }

        try {
            final String value = dbResult.getString(columnName);
            return value != null ? KingbaseValueParser.parsePrimitiveArray(value, converter, generator) : null;
        } catch (Exception e) {
            if (exception == null) {
                exception = e;
            }
        }

        log.debug("Can't get column '" + columnName + "': " + exception.getMessage());
        return null;
    }

    /**
     * Attempts to retrieve an array of strings from the result set under the given {@code columnName}.
     *
     * @see #safeGetArray(ResultSet, String, Function, IntFunction)
     */
    @Nullable
    public static String[] safeGetStringArray(@NotNull ResultSet dbResult, @NotNull String columnName) {
        return safeGetArray(dbResult, columnName, Function.identity(), String[]::new);
    }

    /**
     * Attempts to retrieve an array of shorts from the result set under the given {@code columnName}.
     *
     * @see #safeGetArray(ResultSet, String, Function, IntFunction)
     */
    @Nullable
    public static Number[] safeGetNumberArray(@NotNull ResultSet dbResult, @NotNull String columnName) {
        return safeGetArray(dbResult, columnName, KingbaseUtils::parseNumber, Number[]::new);
    }

    /**
     * Attempts to retrieve an array of booleans from the result set under the given {@code columnName}.
     *
     * @see #safeGetArray(ResultSet, String, Function, IntFunction)
     */
    @Nullable
    public static Boolean[] safeGetBooleanArray(@NotNull ResultSet dbResult, @NotNull String columnName) {
        return safeGetArray(dbResult, columnName, Boolean::valueOf, Boolean[]::new);
    }

    @NotNull
    private static Number parseNumber(@NotNull String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return Double.parseDouble(str);
        }
    }
}
