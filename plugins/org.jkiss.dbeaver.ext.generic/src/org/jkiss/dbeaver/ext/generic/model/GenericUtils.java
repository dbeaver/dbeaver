/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaColumn;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

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

}
