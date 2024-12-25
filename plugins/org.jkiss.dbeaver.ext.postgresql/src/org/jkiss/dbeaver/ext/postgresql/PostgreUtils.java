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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreViewManager;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerTypeRegistry;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * postgresql utils
 */
public class PostgreUtils {

    private static final Log log = Log.getLog(PostgreUtils.class);

    private static final int UNKNOWN_LENGTH = -1;

    private static final Pattern ROLE_TYPE_PATTERN = Pattern.compile("^\\w+\\s+");

    public static String getObjectComment(DBRProgressMonitor monitor, GenericStructContainer container, String schema, String object)
            throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load PostgreSQL description")) {
            return JDBCUtils.queryString(
                    session,
                    "select description from pg_catalog.pg_description\n" +
                            "join pg_catalog.pg_class on pg_description.objoid = pg_class.oid\n" +
                            "join pg_catalog.pg_namespace on pg_class.relnamespace = pg_namespace.oid\n" +
                            "where pg_class.relname = ? and pg_namespace.nspname=?", object, schema);
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

    public static <T extends PostgreAttribute> T getAttributeByNum(Collection<T> attrs, int attNum) {
        for (T attr : attrs) {
            if (attr.getOrdinalPosition() == attNum) {
                return attr;
            }
        }
        return null;
    }

    public static boolean isPGObject(Object object) {
        if (object == null) {
            return false;
        }
        String className = object.getClass().getName();
        return className.equals(PostgreConstants.PG_OBJECT_CLASS) ||
            className.equals(PostgreConstants.RS_OBJECT_CLASS) ||
            className.equals(PostgreConstants.EDB_OBJECT_CLASS);
    }

    public static Object extractPGObjectValue(Object pgObject) {
        if (pgObject == null) {
            return null;
        }
        if (!isPGObject(pgObject)) {
            return pgObject;
        }
        try {
            return pgObject.getClass().getMethod("getValue").invoke(pgObject);
        } catch (Exception e) {
            log.debug("Can't extract value from " + pgObject.getClass().getName(), e);
        }
        return null;
    }

    public static boolean supportsTypeCategory(JDBCDataSource dataSource) {
        return dataSource.isServerVersionAtLeast(8, 4);
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends PostgreObject> OBJECT getObjectById(
            @Nullable DBRProgressMonitor monitor,
            @NotNull AbstractObjectCache<OWNER, OBJECT> cache,
            @NotNull OWNER owner,
            long objectId)
            throws DBException {
        Collection<OBJECT> objects;
        if (monitor == null) {
            // The monitor is null. Let's find our object in the cached objects list.
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

    public static long[] getIdVector(Object pgObject) {
        Object pgVector = extractPGObjectValue(pgObject);
        if (pgVector == null) {
            return null;
        }
        if (pgVector instanceof String) {
            final String vector = (String) pgVector;
            if (vector.isEmpty()) {
                return null;
            }
            final String[] strings = vector.split(PostgreConstants.DEFAULT_ARRAY_DELIMITER);
            final long[] ids = new long[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ids[i] = CommonUtils.toLong(strings[i]);
            }
            return ids;
        } else if (pgVector instanceof long[]) {
            return (long[]) pgVector;
        } else if (pgVector instanceof Long[]) {
            Long[] objVector = (Long[]) pgVector;
            long[] result = new long[objVector.length];
            for (int i = 0; i < objVector.length; i++) {
                result[i] = objVector[i];
            }
            return result;
        } else if (pgVector instanceof Number) {
            return new long[]{((Number) pgVector).longValue()};
        } else if (pgVector instanceof java.sql.Array) {
            try {
                Object array = ((java.sql.Array) pgVector).getArray();
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
                throw new IllegalArgumentException("Error reading array value: " + pgVector);
            }
        } else {
            throw new IllegalArgumentException("Unsupported vector type: " + pgVector.getClass().getName());
        }
    }

    public static int[] getIntVector(Object pgObject) {
        Object pgVector = extractPGObjectValue(pgObject);
        if (pgVector == null) {
            return null;
        }
        if (pgVector instanceof String vector) {
            if (vector.isEmpty()) {
                return null;
            }
            final String[] strings = vector.split(PostgreConstants.DEFAULT_ARRAY_DELIMITER);
            final int[] ids = new int[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ids[i] = CommonUtils.toInt(strings[i]);
            }
            return ids;
        } else if (pgVector instanceof int[] intVector) {
            return intVector;
        } else if (pgVector instanceof Integer[] objVector) {
            int[] result = new int[objVector.length];
            for (int i = 0; i < objVector.length; i++) {
                result[i] = objVector[i];
            }
            return result;
        } else if (pgVector instanceof Number number) {
            return new int[]{number.intValue()};
        } else if (pgVector instanceof java.sql.Array pgArray) {
            try {
                Object array = pgArray.getArray();
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
                throw new IllegalArgumentException("Error reading array value: " + pgVector);
            }
        } else {
            throw new IllegalArgumentException("Unsupported vector type: " + pgVector.getClass().getName());
        }
    }

    public static int getAttributePrecision(long typeOid, int typeMod) {
        //typeOid = convertArrayToBaseOid(typeOid);
        switch ((int) typeOid) {
            case PostgreOid.INT2:
                return 5;

            case PostgreOid.OID:
            case PostgreOid.INT4:
                return 10;

            case PostgreOid.INT8:
                return 19;

            case PostgreOid.FLOAT4:
                // For float4 and float8, we can normally only get 6 and 15
                // significant digits out, but extra_float_digits may raise
                // that number by up to two digits.
                return 8;

            case PostgreOid.FLOAT8:
                return 17;

            case PostgreOid.NUMERIC:
                if (typeMod == -1)
                    return 0;
                return ((typeMod - 4) & 0xFFFF0000) >> 16;

            case PostgreOid.CHAR:
            case PostgreOid.BOOL:
                return 1;

            case PostgreOid.BPCHAR:
            case PostgreOid.VARCHAR:
                if (typeMod == -1)
                    return UNKNOWN_LENGTH;
                return typeMod - 4;

            // datetime types get the
            // "length in characters of the String representation"
            case PostgreOid.DATE:
            case PostgreOid.TIME:
            case PostgreOid.TIMETZ:
            case PostgreOid.INTERVAL:
            case PostgreOid.TIMESTAMP:
            case PostgreOid.TIMESTAMPTZ:
                return getDisplaySize(typeOid, typeMod);

            case PostgreOid.BIT:
                return typeMod;

            case PostgreOid.VARBIT:
                if (typeMod == -1)
                    return UNKNOWN_LENGTH;
                return typeMod;

            case PostgreOid.TEXT:
            case PostgreOid.BYTEA:
            default:
                return UNKNOWN_LENGTH;
        }
    }

    public static int getDisplaySize(long oid, int typmod) {
        //oid = convertArrayToBaseOid(oid);
        switch ((int) oid) {
            case PostgreOid.INT2:
                return 6; // -32768 to +32767
            case PostgreOid.INT4:
                return 11; // -2147483648 to +2147483647
            case PostgreOid.OID:
                return 10; // 0 to 4294967295
            case PostgreOid.INT8:
                return 20; // -9223372036854775808 to +9223372036854775807
            case PostgreOid.FLOAT4:
                // varies based upon the extra_float_digits GUC.
                // These values are for the longest possible length.
                return 15; // sign + 9 digits + decimal point + e + sign + 2 digits
            case PostgreOid.FLOAT8:
                return 25; // sign + 18 digits + decimal point + e + sign + 3 digits
            case PostgreOid.CHAR:
                return 1;
            case PostgreOid.BOOL:
                return 1;
            case PostgreOid.DATE:
                return 13; // "4713-01-01 BC" to  "01/01/4713 BC" - "31/12/32767"
            case PostgreOid.TIME:
            case PostgreOid.TIMETZ:
            case PostgreOid.TIMESTAMP:
            case PostgreOid.TIMESTAMPTZ:
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

                // We assume the worst case scenario for all of these.
                // time = '00:00:00' = 8
                // date = '5874897-12-31' = 13 (although at large values second precision is lost)
                // date = '294276-11-20' = 12 --enable-integer-datetimes
                // zone = '+11:30' = 6;

                switch ((int) oid) {
                    case PostgreOid.TIME:
                        return 8 + secondSize;
                    case PostgreOid.TIMETZ:
                        return 8 + secondSize + 6;
                    case PostgreOid.TIMESTAMP:
                        return 13 + 1 + 8 + secondSize;
                    case PostgreOid.TIMESTAMPTZ:
                        return 13 + 1 + 8 + secondSize + 6;
                }
            case PostgreOid.INTERVAL:
                return 49; // SELECT LENGTH('-123456789 years 11 months 33 days 23 hours 10.123456 seconds'::interval);
            case PostgreOid.VARCHAR:
            case PostgreOid.BPCHAR:
                if (typmod == -1)
                    return UNKNOWN_LENGTH;
                return typmod - 4;
            case PostgreOid.NUMERIC:
                if (typmod == -1)
                    return 131089; // SELECT LENGTH(pow(10::numeric,131071)); 131071 = 2^17-1
                int precision = (typmod - 4 >> 16) & 0xffff;
                int scale = (typmod - 4) & 0xffff;
                // sign + digits + decimal point (only if we have nonzero scale)
                return 1 + precision + (scale != 0 ? 1 : 0);
            case PostgreOid.BIT:
                return typmod;
            case PostgreOid.VARBIT:
                if (typmod == -1)
                    return UNKNOWN_LENGTH;
                return typmod;
            case PostgreOid.TEXT:
            case PostgreOid.BYTEA:
                return UNKNOWN_LENGTH;
            default:
                return UNKNOWN_LENGTH;
        }
    }

    public static PostgreDataType findDataType(DBCSession session, PostgreDataSource dataSource, DBSTypedObject type) throws DBCException {
        if (type instanceof PostgreDataType) {
            return (PostgreDataType) type;
        } else if (type instanceof PostgreAttribute) {
            return ((PostgreAttribute) type).getDataType();
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
                                if (dataType instanceof PostgreDataType) {
                                    return (PostgreDataType) dataType;
                                }
                            }
                        }
                    }
                    {
                        String databaseName = ((JDBCColumnMetaData) type).getCatalogName();
                        PostgreDatabase database = dataSource.getDatabase(databaseName);
                        if (database != null) {
                            String typeName = type.getTypeName();
                            if (PostgreUtils.isCompositeTypeName(typeName)) {
                                // Type name in JDBCColumnMetaData can be fully qualified and quoted. Let's fix it for the better search in the getDataType() method
                                String[] identifiers = SQLUtils.splitFullIdentifier(typeName, ".", dataSource.getSQLDialect().getIdentifierQuoteStrings(), false);
                                if (!ArrayUtils.isEmpty(identifiers)) {
                                    typeName = identifiers[identifiers.length - 1];
                                    if (identifiers.length == 2) {
                                        // Most likely, in the identifiers array we have the name of the scheme and the name of the data type in this case
                                        // Try to find data type in the schema data type cache
                                        // Do not forget to turn on the PG connection setting "Read all data types" to have arrays, tables, etc. types in the data type cache
                                        String schemaName = identifiers[0];
                                        PostgreSchema schema = database.getSchema(monitor, schemaName);
                                        if (schema != null) {
                                            PostgreDataType dataType = schema.getDataTypeCache().getObject(monitor, schema, typeName);
                                            if (dataType != null) {
                                                return dataType;
                                            }
                                        }
                                    }
                                }
                            }
                            PostgreDataType dataType = database.getDataType(monitor, typeName);
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
            if (ownerInstance instanceof PostgreDatabase) {
                PostgreDataType localDataType = ((PostgreDatabase) ownerInstance).getDataType(monitor, typeName);
                if (localDataType != null) {
                    return localDataType;
                }
            }
            return dataSource.getLocalDataType(typeName);
        }
    }

    @Nullable
    public static PostgreDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull PostgreSchema schema, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(monitor, schema.getDataSource(), schema.getDatabase(), schema, fullTypeName);
    }

    @Nullable
    public static PostgreDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(monitor, database.getDataSource(), database, database.getMetaContext().getDefaultSchema(), fullTypeName);
    }

    @Nullable
    public static PostgreDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull PostgreDataSource dataSource, @NotNull String fullTypeName
    ) throws DBException {
        return resolveTypeFullName(
            monitor, dataSource, dataSource.getDefaultInstance(),
            dataSource.getDefaultInstance().getMetaContext().getDefaultSchema(), fullTypeName
        );
    }

    @Nullable
    private static PostgreDataType resolveTypeFullName(
        @NotNull DBRProgressMonitor monitor, @NotNull PostgreDataSource dataSource, @NotNull PostgreDatabase database,
        @NotNull PostgreSchema schema, @NotNull String fullTypeName
    ) throws DBException {
        final String identifier = DBUtils.getTypeModifiers(fullTypeName).getFirst();
        String[] parts = splitTypeNameIdentifier(dataSource, fullTypeName);

        // Try to get cashed data type from specified schema
        PostgreDataType dataType = schema.getDataTypeCache().getObject(monitor, schema, identifier);
        if (dataType != null) {
            return dataType;
        }
        // Try to resolve local data type in specified database
        dataType = database.getLocalDataType(identifier);
        if (dataType != null) {
            return dataType;
        } else if (parts.length > 1) {
            // Search data type in schema from fullTypeName part
            PostgreSchema resolvedSchema = database.getSchema(monitor, parts[0]);
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
            PostgreDatabase resolvedDatabase = dataSource.getDatabase(parts[0]);
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
                    PostgreSchema resolvedSchema = resolvedDatabase.getSchema(monitor, parts[1]);
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
        @NotNull PostgreDataSource dataSource, @NotNull String fullTypeName
    ) throws DBException {
        final Pair<String, String[]> typeNameInfo = DBUtils.getTypeModifiers(fullTypeName);
        final String identifier = typeNameInfo.getFirst();

        String[] parts;
        if (PostgreUtils.isCompositeTypeName(identifier)) {
            parts = SQLUtils.splitFullIdentifier(identifier, ".", dataSource.getSQLDialect().getIdentifierQuoteStrings(), false);
        } else {
            parts = new String[]{identifier};
        }

        return parts;
    }
    
    private static boolean isCompositeTypeName(@NotNull String typeName) {
        return typeName.startsWith("\"") || typeName.contains(".");
    }

    public static void setArrayParameter(JDBCPreparedStatement dbStat, int index, List<? extends PostgreObject> objectList) throws SQLException {
        for (int i = 0; i < objectList.size(); i++) {
            dbStat.setLong(index + i, objectList.get(i).getObjectId());
        }
    }

    public static String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreViewBase view,
        @NotNull String definition,
        @NotNull Map<String, Object> options
    ) throws DBException {
        // In some cases view definition already has view header (e.g. Redshift + with no schema binding)
        if (definition.toLowerCase(Locale.ENGLISH).startsWith("create ")) {
            return definition;
        }
        StringBuilder sql = new StringBuilder(view instanceof PostgreView ? "CREATE OR REPLACE " : "CREATE ");
        sql.append(view.getTableTypeName()).append(" ").append(DBUtils.getEntityScriptName(view, options));

        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        final PostgreViewManager entityEditor = editorsRegistry.getObjectManager(view.getClass(), PostgreViewManager.class);
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

    public static PostgreServerType getServerType(DBPDriver driver) {
        String serverTypeName = CommonUtils.toString(driver.getDriverParameter(PostgreConstants.PROP_SERVER_TYPE));
        if (CommonUtils.isEmpty(serverTypeName)) {
            serverTypeName = PostgreServerPostgreSQL.TYPE_ID;
        }
        PostgreServerType serverType = PostgreServerTypeRegistry.getInstance().getServerType(serverTypeName);
        if (serverType == null) {
            throw new IllegalStateException("PostgreSQL server type '" + serverTypeName + "' not found");
        }
        return serverType;
    }

    public static Set<PostgreRoleReference> extractGranteesFromACL(@NotNull PostgreDatabase database, @NotNull String[] acl) {
        final Set<PostgreRoleReference> grantees = new HashSet<>();
        for (String aclValue : acl) {
            if (CommonUtils.isEmpty(aclValue)) {
                continue;
            }
            int divPos = aclValue.indexOf('=');
            if (divPos == -1) {
                log.warn("Bad ACL item: " + aclValue);
                continue;
            }
            PostgreRoleReference grantee = extractGranteeName(database, aclValue, divPos);
            grantees.add(grantee);
        }
        return grantees;
    }

    // FIXME consider user/group/role name like "test test", "test=test", "test,test", "test\"test" and user name like "group" or "role"
    public static List<PostgrePrivilege> extractPermissionsFromACL(
        @NotNull PostgrePrivilegeOwner owner,
        @NotNull String[] acl,
        boolean isDefault
    ) {
        List<PostgrePrivilege> permissions = new ArrayList<>();
        for (String aclValue : acl) {
            if (CommonUtils.isEmpty(aclValue)) {
                continue;
            }
            int divPos = aclValue.indexOf('=');
            if (divPos == -1) {
                log.warn("Bad ACL item: " + aclValue);
                continue;
            }
            PostgreRoleReference grantee = extractGranteeName(owner.getDatabase(), aclValue, divPos);
            String permString = aclValue.substring(divPos + 1);
            int divPos2 = permString.indexOf('/');
            if (divPos2 == -1) {
                log.warn("Bad permissions string: " + permString);
                continue;
            }
            String privString = permString.substring(0, divPos2);
            String grantorName = permString.substring(divPos2 + 1);
            PostgreRoleReference grantor = new PostgreRoleReference(owner.getDatabase(), grantorName, null);
            List<PostgrePrivilegeGrant> privileges = new ArrayList<>();
            for (int k = 0; k < privString.length(); k++) {
                char pCode = privString.charAt(k);
                boolean withGrantOption = false;
                if (k < privString.length() - 1 && privString.charAt(k + 1) == '*') {
                    withGrantOption = true;
                    k++;
                }
                privileges.add(new PostgrePrivilegeGrant(
                    grantor,
                    grantee,
                    owner.getDatabase().getName(),
                    owner.getSchema().getName(),
                    owner.getName(),
                    PostgrePrivilegeType.getByCode(pCode),
                    withGrantOption,
                    false
                ));
            }
            if (isDefault) {
                permissions.add(new PostgreDefaultPrivilege(owner, grantee, grantor, privileges));
            } else {
                permissions.add(new PostgreObjectPrivilege(owner, grantee, privileges));
            }
        }
        return permissions;
    }

    @NotNull
    private static PostgreRoleReference extractGranteeName(@NotNull PostgreDatabase database, @NotNull String aclValue, int divPos) {
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
        return new PostgreRoleReference(database, grantee, granteeType);
    }

    public static List<PostgrePrivilege> extractPermissionsFromACL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgrePrivilegeOwner owner,
        @Nullable Object acl,
        boolean isDefault
    ) throws DBException {
        if (!(acl instanceof java.sql.Array)) {
            if (acl == null) {
                // Special case. Means ALL permissions are granted to table owner
                PostgreRole objectOwner = owner.getOwner(monitor);
                PostgreRoleReference granteeReference = objectOwner == null ? null : objectOwner.getRoleReference();

                List<PostgrePrivilegeGrant> privileges = new ArrayList<>();
                privileges.add(
                        new PostgrePrivilegeGrant(
                                granteeReference,
                                granteeReference,
                                owner.getDatabase().getName(),
                                owner.getSchema().getName(),
                                owner.getName(),
                                PostgrePrivilegeType.ALL,
                                false,
                                false));
                PostgreObjectPrivilege permission = new PostgreObjectPrivilege(owner, granteeReference, privileges);
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
            String aclValue = CommonUtils.toString(extractPGObjectValue(aclItem));
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

    public static String getObjectTypeName(PostgrePrivilegeOwner object) {
        if (object instanceof PostgreSequence) {
            return "SEQUENCE";
        } else if (object instanceof PostgreProcedure) {
            return ((PostgreProcedure) object).getProcedureTypeName();
        } else if (object instanceof PostgreSchema) {
            return "SCHEMA";
        } else if (object instanceof PostgreDatabase) {
            return "DATABASE";
        } else {
            return "TABLE";
        }
    }

    public static String getObjectUniqueName(PostgrePrivilegeOwner object, Map<String, Object> options) {
        if (object instanceof PostgreProcedure) {
            return ((PostgreProcedure) object).getFullQualifiedSignature();
        } else {
            return DBUtils.getEntityScriptName(object, options);
        }
    }

    public static void getObjectGrantPermissionActions(DBRProgressMonitor monitor, PostgrePrivilegeOwner object, List<DBEPersistAction> actions, Map<String, Object> options) throws DBException {
        if (object.isPersisted() && CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(object, true);
            if (object.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO)) {
                actions.add(new SQLDatabasePersistActionComment(object.getDataSource(), "Permissions"));
            }

            // Owner
            PostgreRole owner = object.getOwner(monitor);
            if (owner != null) {
                String alterScript = object.generateChangeOwnerQuery(DBUtils.getQuotedIdentifier(owner), options);
                if (!CommonUtils.isEmpty(alterScript)) {
                    actions.add(new SQLDatabasePersistAction("Owner change", alterScript));
                }
            }

            // Permissions
            Collection<PostgrePrivilege> permissions = object.getPrivileges(monitor, true);
            if (!CommonUtils.isEmpty(permissions)) {

                for (PostgrePrivilege permission : permissions) {
                    if (permission.hasAllPrivileges(object)) {
                        Collections.addAll(actions,
                                new PostgreCommandGrantPrivilege(permission.getOwner(), true, object, permission, new PostgrePrivilegeType[]{PostgrePrivilegeType.ALL})
                                        .getPersistActions(monitor, executionContext, options));
                    } else {
                        PostgreCommandGrantPrivilege grant = new PostgreCommandGrantPrivilege(permission.getOwner(), true, object, permission, permission.getPrivileges());
                        Collections.addAll(actions, grant.getPersistActions(monitor, executionContext, options));
                    }
                }
            }
        }
    }

    public static boolean isGISDataType(String typeName) {
        return PostgreConstants.TYPE_GEOMETRY.equals(typeName) ||
                PostgreConstants.TYPE_GEOGRAPHY.equals(typeName);
    }

    public static String getRealSchemaName(PostgreDatabase database, String name) {
        return name.replace(PostgreConstants.USER_VARIABLE, database.getMetaContext().getActiveUser());
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
        return "SELECT " + columnName + " FROM pg_catalog." + tableName + " WHERE 1<>1 LIMIT 1";
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
     * @return a type-specific array delimiter, or {@code ","} if the given type is not a postgres data type.
     */
    @NotNull
    public static String getArrayDelimiter(@NotNull DBSTypedObject type) {
        if (type instanceof PostgreDataType) {
            return ((PostgreDataType) type).getArrayDelimiter();
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
     * @see PostgreValueParser#parsePrimitiveArray(String, Function, IntFunction)
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
        } catch (SQLFeatureNotSupportedException | UnsupportedOperationException | IncompatibleClassChangeError ignored) {
            // Some drivers (ODBC) might not have an implementation for that API, just ignore and try with a string
        } catch (Exception e) {
            exception = e;
        }

        try {
            final String value = dbResult.getString(columnName);
            return value != null ? PostgreValueParser.parsePrimitiveArray(value, converter, generator) : null;
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
        return safeGetArray(dbResult, columnName, PostgreUtils::parseNumber, Number[]::new);
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

    @Nullable
    public static String getDatabaseNameFromConfiguration(DBPConnectionConfiguration configuration) {
        String activeDatabaseName = null;
        if (configuration.getConfigurationType() == DBPDriverConfigurationType.MANUAL) {
            activeDatabaseName = configuration.getBootstrap().getDefaultCatalogName();
            if (CommonUtils.isEmpty(activeDatabaseName)) {
                activeDatabaseName = configuration.getDatabaseName();
            }
        } else {
            String url = configuration.getUrl();
            int divPos = url.lastIndexOf('/');
            if (divPos > 0) {
                int lastPos = getLastNonDatabaseCharPos(divPos, url);
                activeDatabaseName = url.substring(divPos + 1, lastPos);
            }
        }
        return activeDatabaseName;
    }


    @NotNull
    public static String updateDatabaseNameInURL(String url, String dbName) {
        int divPos = url.lastIndexOf('/');
        if (divPos > 0) {
            int lastPos = getLastNonDatabaseCharPos(divPos, url);
            return url.substring(0, divPos + 1) + dbName + url.substring(lastPos);
        } else {
            return url + "/" + dbName;
        }
    }

    private static int getLastNonDatabaseCharPos(int divPos, String url) {
        int lastPos = -1;
        for (int i = divPos + 1; i < url.length(); i++) {
            char c = url.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$' && c != '.') {
                lastPos = i;
                break;
            }
        }
        if (lastPos < 0) lastPos = url.length();
        return lastPos;
    }

}
