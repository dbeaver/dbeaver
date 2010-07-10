/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import java.util.Map;
import java.util.HashMap;

/**
 * MySQL utils
 */
public class MySQLUtils {
    private static Map<String, Integer> typeMap = new HashMap<String, Integer>();

    static {
        typeMap.put("BIT", java.sql.Types.BIT);
        typeMap.put("TINYINT", java.sql.Types.TINYINT);
        typeMap.put("SMALLINT", java.sql.Types.SMALLINT);
        typeMap.put("MEDIUMINT", java.sql.Types.INTEGER);
        typeMap.put("INT", java.sql.Types.INTEGER);
        typeMap.put("INTEGER", java.sql.Types.INTEGER);
        typeMap.put("INT24", java.sql.Types.INTEGER);
        typeMap.put("BIGINT", java.sql.Types.BIGINT);
        typeMap.put("REAL", java.sql.Types.DOUBLE);
        typeMap.put("FLOAT", java.sql.Types.FLOAT);
        typeMap.put("DECIMAL", java.sql.Types.DECIMAL);
        typeMap.put("NUMERIC", java.sql.Types.DECIMAL);
        typeMap.put("DOUBLE", java.sql.Types.DOUBLE);
        typeMap.put("CHAR", java.sql.Types.CHAR);
        typeMap.put("VARCHAR", java.sql.Types.VARCHAR);
        typeMap.put("DATE", java.sql.Types.DATE);
        typeMap.put("TIME", java.sql.Types.TIME);
        typeMap.put("YEAR", java.sql.Types.DATE);
        typeMap.put("TIMESTAMP", java.sql.Types.TIMESTAMP);
        typeMap.put("DATETIME", java.sql.Types.TIMESTAMP);
        typeMap.put("TINYBLOB", java.sql.Types.BINARY);
        typeMap.put("BLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("MEDIUMBLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("LONGBLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("TINYTEXT", java.sql.Types.VARCHAR);
        typeMap.put("TEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put("MEDIUMTEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put("LONGTEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put(MySQLConstants.TYPE_NAME_ENUM, java.sql.Types.CHAR);
        typeMap.put(MySQLConstants.TYPE_NAME_SET, java.sql.Types.CHAR);
        typeMap.put("GEOMETRY", java.sql.Types.BINARY);
        typeMap.put("BINARY", java.sql.Types.BINARY);
        typeMap.put("VARBINARY", java.sql.Types.VARBINARY);
    }

    public static int typeNameToValueType(String typeName)
    {
        Integer valueType = typeMap.get(typeName.toUpperCase());
        return valueType == null ? java.sql.Types.OTHER : valueType;
    }

}
