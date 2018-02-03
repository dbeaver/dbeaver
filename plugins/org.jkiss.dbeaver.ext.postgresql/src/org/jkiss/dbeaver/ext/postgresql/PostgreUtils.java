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

package org.jkiss.dbeaver.ext.postgresql;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

/**
 * postgresql utils
 */
public class PostgreUtils {

    private static final Log log = Log.getLog(PostgreUtils.class);
    
    private static final int UNKNOWN_LENGTH = -1;

    public static String getObjectComment(DBRProgressMonitor monitor, DBPDataSource dataSource, String schema, String object)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load PostgreSQL description")) {
            return JDBCUtils.queryString(
                session,
                "select description from pg_description\n" +
                "join pg_class on pg_description.objoid = pg_class.oid\n" +
                "join pg_namespace on pg_class.relnamespace = pg_namespace.oid\n" +
                "where pg_class.relname = ? and pg_namespace.nspname=?", object, schema);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN: return "bool";
            case NUMERIC: return "int";
            case STRING: return "varchar";
            case DATETIME: return "timestamp";
            case BINARY: return "bytea";
            case CONTENT: return "bytea";
            case ROWID: return "oid";
            default: return "varchar";
        }
    }

    private static Method getValueMethod;

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

    public static <T> T extractPGObjectValue(Object pgObject) {
        if (pgObject == null) {
            return null;
        }
        if (getValueMethod == null) {
            try {
                getValueMethod = pgObject.getClass().getMethod("getValue");
            } catch (NoSuchMethodException e) {
                log.debug(e);
            }
        }
        if (getValueMethod != null) {
            try {
                return (T)getValueMethod.invoke(pgObject);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        return null;
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends PostgreObject> OBJECT getObjectById(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AbstractObjectCache<OWNER,OBJECT> cache,
        @NotNull OWNER owner,
        long objectId)
        throws DBException
    {
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
            final String[] strings = vector.split(" ");
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
            final String[] strings = vector.split(" ");
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
                return ((typeMod-4) & 0xFFFF0000) >> 16;

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
        switch((int)oid) {
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
                switch(typmod) {
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

                switch((int)oid) {
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
                int precision = (typmod-4 >> 16) & 0xffff;
                int scale = (typmod-4) & 0xffff;
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
        switch((int)oid) {
            case PostgreOid.FLOAT4:
                return 8;
            case PostgreOid.FLOAT8:
                return 17;
            case PostgreOid.NUMERIC:
                if (typmod == -1)
                    return 0;
                return (typmod-4) & 0xFFFF;
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

    public static PostgreDataType findDataType(PostgreDataSource dataSource, DBSTypedObject type) {
        if (type instanceof PostgreAttribute) {
            return  ((PostgreAttribute) type).getDataType();
        } else {
            String typeName = type.getTypeName();
            return dataSource.getLocalDataType(typeName);
        }
    }
    
    public static Object convertStringToValue(DBSTypedObject itemType, String string, boolean unescape) {
        switch (itemType.getTypeID()) {
            case Types.BOOLEAN: return Boolean.valueOf(string); 
            case Types.TINYINT: return Byte.parseByte(string); 
            case Types.SMALLINT: return Short.parseShort(string); 
            case Types.INTEGER: return Integer.parseInt(string); 
            case Types.BIGINT: return Long.parseLong(string); 
            case Types.FLOAT: return Float.parseFloat(string); 
            case Types.REAL:
            case Types.DOUBLE: return Double.parseDouble(string); 
            default:
                return string; 
        }
    }

    public static String[] parseObjectString(String string) throws DBCException {
        if (string.isEmpty()) {
            return new String[0];
        }
        try {
            return new CSVReader(new StringReader(string)).readNext();
        } catch (IOException e) {
            throw new DBCException("Error parsing PGObject", e);
        }
    }

    public static String generateObjectString(Object[] values) throws DBCException {
        String[] line = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            line[i] = value == null ? "NULL" : value.toString();
        }
        StringWriter out = new StringWriter();
        final CSVWriter writer = new CSVWriter(out);
        writer.writeNext(line);
        try {
            writer.flush();
        } catch (IOException e) {
            log.warn(e);
        }
        return "(" + out.toString().trim() + ")";
    }


    public static void setArrayParameter(JDBCPreparedStatement dbStat, int index, List<? extends PostgreObject> objectList) throws SQLException {
        for (int i = 0; i < objectList.size(); i++) {
            dbStat.setLong(index + i, objectList.get(i).getObjectId());
        }
    }

    public static String getViewDDL(PostgreViewBase view, String definition) {
        String createSQL = (view instanceof PostgreView ? "CREATE OR REPLACE " : "CREATE ");
        return createSQL + view.getViewType() + " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\n" + definition;
    }

}
