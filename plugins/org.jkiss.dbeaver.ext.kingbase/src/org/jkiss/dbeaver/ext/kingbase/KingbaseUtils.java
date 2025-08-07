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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.sql.SQLException;

/**
 * KingbaseUtils
 */
public class KingbaseUtils {

    private static final Log log = Log.getLog(KingbaseUtils.class);

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
            final String[] strings = vector.split(PostgreConstants.DEFAULT_ARRAY_DELIMITER);
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
            final String[] strings = vector.split(PostgreConstants.DEFAULT_ARRAY_DELIMITER);
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

    public static String getQueryForSystemColumnChecking(@NotNull String tableName, @NotNull String columnName) {
        if (tableName.contains("pg_")) {
            tableName.replaceAll("pg_", "sys_");
        }
        return "SELECT " + columnName + " FROM sys_catalog." + tableName + " WHERE 1<>1 LIMIT 1";
    }

    public static boolean isMetaObjectExists(@NotNull JDBCSession session, @NotNull String tableName, @NotNull String columnName) {
        try {
            JDBCUtils.queryString(session, getQueryForSystemColumnChecking(tableName, columnName));
            return true;
        } catch (SQLException e) {
            log.debug("Error reading system information from the " + tableName + " table: " + e.getMessage());
        }
        return false;
    }
   
}
