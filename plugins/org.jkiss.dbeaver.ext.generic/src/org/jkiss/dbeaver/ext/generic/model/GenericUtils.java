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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaColumn;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 * Generic utils
 */
public class GenericUtils {

    public static Object getColumn(GenericDataSource dataSource, String objectType, String columnId)
    {
        GenericMetaObject object = dataSource.getMetaObject(objectType);
        if (object == null) {
            return columnId;
        }
        GenericMetaColumn column = object.getColumn(columnId);
        if (column == null || !column.isSupported()) {
            return columnId;
        }
        return column.getColumnIdentifier();
    }

    public static Object getColumn(GenericMetaObject object, String columnId)
    {
        GenericMetaColumn column = object == null ? null : object.getColumn(columnId);
        if (column == null || !column.isSupported()) {
            return columnId;
        }
        return column.getColumnIdentifier();
    }


    public static String safeGetString(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetString(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetString(dbResult, column.toString());
        }
    }

    public static String safeGetStringTrimmed(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetStringTrimmed(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetStringTrimmed(dbResult, column.toString());
        }
    }

    public static int safeGetInt(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetInt(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetInt(dbResult, column.toString());
        }
    }

    public static Integer safeGetInteger(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetInteger(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetInteger(dbResult, column.toString());
        }
    }

    public static long safeGetLong(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetLong(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetLong(dbResult, column.toString());
        }
    }

    public static double safeGetDouble(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetDouble(dbResult, ((Number)column).intValue());
        } else {
            return JDBCUtils.safeGetDouble(dbResult, column.toString());
        }
    }

    public static BigDecimal safeGetBigDecimal(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetBigDecimal(dbResult, ((Number)column).intValue());
        } else {
            return JDBCUtils.safeGetBigDecimal(dbResult, column.toString());
        }
    }

    public static boolean safeGetBoolean(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetBoolean(dbResult, ((Number)column).intValue());
        } else {
            return JDBCUtils.safeGetBoolean(dbResult, column.toString());
        }
    }

    public static Object safeGetObject(GenericMetaObject object, ResultSet dbResult, String columnId)
    {
        Object column = getColumn(object, columnId);
        if (column instanceof Number) {
            return JDBCUtils.safeGetObject(dbResult, ((Number) column).intValue());
        } else {
            return JDBCUtils.safeGetObject(dbResult, column.toString());
        }
    }

    public static boolean isLegacySQLDialect(DBSObject owner) {
        SQLDialect dialect = SQLUtils.getDialectFromObject(owner);
        return dialect instanceof GenericSQLDialect && ((GenericSQLDialect)dialect).isLegacySQLDialect();
    }

    public static String normalizeProcedureName(String procedureName) {
        int divPos = procedureName.lastIndexOf(';');
        if (divPos != -1) {
            // [JDBC: SQL Server native driver]
            procedureName = procedureName.substring(0, divPos);
        }
        return procedureName;

    }

    public static boolean canAlterTable(@NotNull DBSObject object) {
        // Either object is not yet persisted (so no alter is required) or database supports table altering
        return !object.isPersisted() || object.getDataSource().getSQLDialect().supportsAlterTableStatement();
    }
}
