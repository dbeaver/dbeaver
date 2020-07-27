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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreViewManager;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerTypeRegistry;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
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
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.*;

/**
 * postgresql utils
 */
public class PostgreUtils {

    private static final Log log = Log.getLog(PostgreUtils.class);

    private static final int UNKNOWN_LENGTH = -1;

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
        return object != null && object.getClass().getName().equals(PostgreConstants.PG_OBJECT_CLASS);
    }

    public static Object extractPGObjectValue(Object pgObject) {
        if (pgObject == null) {
            return null;
        }
        if (!pgObject.getClass().getName().equals(PostgreConstants.PG_OBJECT_CLASS)) {
            return pgObject;
        }
        try {
            return pgObject.getClass().getMethod("getValue").invoke(pgObject);
        } catch (Exception e) {
            log.debug("Can't extract value from PgObject", e);
        }
        return null;
    }

    public static boolean supportsTypeCategory(JDBCDataSource dataSource) {
        return dataSource.isServerVersionAtLeast(8, 4);
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends PostgreObject> OBJECT getObjectById(
            @NotNull DBRProgressMonitor monitor,
            @NotNull AbstractObjectCache<OWNER, OBJECT> cache,
            @NotNull OWNER owner,
            long objectId)
            throws DBException {
        for (OBJECT object : cache.getAllObjects(monitor, owner)) {
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
                ids[i] = Long.parseLong(strings[i]);
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
        } else {
            throw new IllegalArgumentException("Unsupported vector type: " + pgVector.getClass().getName());
        }
    }

    public static int[] getIntVector(Object pgObject) {
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
            final int[] ids = new int[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ids[i] = Integer.parseInt(strings[i]);
            }
            return ids;
        } else if (pgVector instanceof int[]) {
            return (int[]) pgVector;
        } else if (pgVector instanceof Integer[]) {
            Integer[] objVector = (Integer[]) pgVector;
            int[] result = new int[objVector.length];
            for (int i = 0; i < objVector.length; i++) {
                result[i] = objVector[i];
            }
            return result;
        } else if (pgVector instanceof Number) {
            return new int[]{((Number) pgVector).intValue()};
        } else if (pgVector instanceof java.sql.Array) {
            try {
                Object array = ((java.sql.Array) pgVector).getArray();
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

    public static int getScale(long oid, int typmod) {
        //oid = convertArrayToBaseOid(oid);
        switch ((int) oid) {
            case PostgreOid.FLOAT4:
                return 8;
            case PostgreOid.FLOAT8:
                return 17;
            case PostgreOid.NUMERIC:
                if (typmod == -1)
                    return 0;
                return (typmod - 4) & 0xFFFF;
            case PostgreOid.TIME:
            case PostgreOid.TIMETZ:
            case PostgreOid.TIMESTAMP:
            case PostgreOid.TIMESTAMPTZ:
                if (typmod == -1)
                    return 6;
                return typmod;
            case PostgreOid.INTERVAL:
                if (typmod == -1)
                    return 6;
                return typmod & 0xFFFF;
            default:
                return 0;
        }
    }

    public static PostgreDataType findDataType(DBCSession session, PostgreDataSource dataSource, DBSTypedObject type) throws DBCException {
        if (type instanceof PostgreDataType) {
            return (PostgreDataType) type;
        } else if (type instanceof PostgreAttribute) {
            return ((PostgreAttribute) type).getDataType();
        } else {
            if (type instanceof JDBCColumnMetaData) {
                try {
                    DBCEntityMetaData entityMetaData = ((DBCAttributeMetaData) type).getEntityMetaData();
                    if (entityMetaData != null) {
                        DBSEntity docEntity = DBUtils.getEntityFromMetaData(session.getProgressMonitor(), session.getExecutionContext(), entityMetaData);
                        if (docEntity != null) {
                            DBSEntityAttribute attribute = docEntity.getAttribute(session.getProgressMonitor(), ((DBCAttributeMetaData) type).getName());
                            if (attribute instanceof DBSTypedObjectEx) {
                                DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
                                if (dataType instanceof PostgreDataType) {
                                    return (PostgreDataType) dataType;
                                }
                            }
                        }
                    } else {
                        String databaseName = ((JDBCColumnMetaData) type).getCatalogName();
                        PostgreDatabase database = dataSource.getDatabase(databaseName);
                        if (database != null) {
                            PostgreDataType dataType = database.getDataType(session.getProgressMonitor(), type.getTypeName());
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
            return dataSource.getLocalDataType(typeName);
        }
    }


    public static void setArrayParameter(JDBCPreparedStatement dbStat, int index, List<? extends PostgreObject> objectList) throws SQLException {
        for (int i = 0; i < objectList.size(); i++) {
            dbStat.setLong(index + i, objectList.get(i).getObjectId());
        }
    }

    public static String getViewDDL(DBRProgressMonitor monitor, PostgreViewBase view, String definition) throws DBException {
        // In some cases view definition already has view header (e.g. Redshift + with no schema binding)
        if (definition.toLowerCase(Locale.ENGLISH).startsWith("create ")) {
            return definition;
        }
        StringBuilder sql = new StringBuilder(view instanceof PostgreView ? "CREATE OR REPLACE " : "CREATE ");
        sql.append(view.getViewType()).append(" ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL));

        final DBERegistry editorsRegistry = view.getDataSource().getContainer().getPlatform().getEditorsRegistry();
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

    public static List<PostgrePrivilege> extractPermissionsFromACL(DBRProgressMonitor monitor, @NotNull PostgrePrivilegeOwner owner, @Nullable Object acl) throws DBException {
        if (!(acl instanceof java.sql.Array)) {
            if (acl == null) {
                // Special case. Means ALL permissions are granted to table owner
                PostgreRole objectOwner = owner.getOwner(monitor);
                String granteeName = objectOwner == null ? null : objectOwner.getName();

                List<PostgrePrivilegeGrant> privileges = new ArrayList<>();
                privileges.add(
                        new PostgrePrivilegeGrant(
                                granteeName,
                                granteeName,
                                owner.getDatabase().getName(),
                                owner.getSchema().getName(),
                                owner.getName(),
                                PostgrePrivilegeType.ALL,
                                false,
                                false));
                PostgreObjectPrivilege permission = new PostgreObjectPrivilege(owner, objectOwner == null ? null : objectOwner.getName(), privileges);
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
        List<PostgrePrivilege> permissions = new ArrayList<>();
        int itemCount = Array.getLength(itemArray);
        for (int i = 0; i < itemCount; i++) {
            Object aclItem = Array.get(itemArray, i);
            String aclValue = CommonUtils.toString(extractPGObjectValue(aclItem));
            if (CommonUtils.isEmpty(aclValue)) {
                continue;
            }
            int divPos = aclValue.indexOf('=');
            if (divPos == -1) {
                log.warn("Bad ACL item: " + aclValue);
                continue;
            }
            String grantee = aclValue.substring(0, divPos);
            if (grantee.isEmpty()) {
                grantee = "public";
            }
            String permString = aclValue.substring(divPos + 1);
            int divPos2 = permString.indexOf('/');
            if (divPos2 == -1) {
                log.warn("Bad permissions string: " + permString);
                continue;
            }
            String privString = permString.substring(0, divPos2);
            String grantor = permString.substring(divPos2 + 1);

            List<PostgrePrivilegeGrant> privileges = new ArrayList<>();
            for (int k = 0; k < privString.length(); k++) {
                char pCode = privString.charAt(k);
                boolean withGrantOption = false;
                if (k < privString.length() - 1 && privString.charAt(k + 1) == '*') {
                    withGrantOption = true;
                    k++;
                }
                privileges.add(new PostgrePrivilegeGrant(
                        grantor, grantee,
                        owner.getDatabase().getName(),
                        owner.getSchema().getName(),
                        owner.getName(),
                        PostgrePrivilegeType.getByCode(pCode),
                        withGrantOption,
                        false
                ));
            }
            permissions.add(new PostgreObjectPrivilege(owner, grantee, privileges));
        }

        return permissions;
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

    public static String getObjectUniqueName(PostgrePrivilegeOwner object) {
        if (object instanceof PostgreProcedure) {
            return ((PostgreProcedure) object).getFullQualifiedSignature();
        } else {
            return DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
        }
    }

    public static void getObjectGrantPermissionActions(DBRProgressMonitor monitor, PostgrePrivilegeOwner object, List<DBEPersistAction> actions, Map<String, Object> options) throws DBException {
        if (object.isPersisted() && CommonUtils.getOption(options, PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS)) {
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(object, true);
            actions.add(new SQLDatabasePersistActionComment(object.getDataSource(), "Permissions"));

            // Owner
            PostgreRole owner = object.getOwner(monitor);
            if (owner != null) {
                String alterScript = object.generateChangeOwnerQuery(DBUtils.getQuotedIdentifier(owner));
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
                                new PostgreCommandGrantPrivilege(permission.getOwner(), true, permission, new PostgrePrivilegeType[]{PostgrePrivilegeType.ALL})
                                        .getPersistActions(monitor, executionContext, options));
                    } else {
                        PostgreCommandGrantPrivilege grant = new PostgreCommandGrantPrivilege(permission.getOwner(), true, permission, permission.getPrivileges());
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
        return name.replace("$user", database.getMetaContext().getActiveUser());
    }

}
