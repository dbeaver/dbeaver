/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Oracle utils
 */
public class OracleUtils {

    static final Log log = LogFactory.getLog(OracleUtils.class);

    private static Map<String, Integer> typeMap = new HashMap<String, Integer>();
    public static final String COLUMN_POSTFIX_PRIV = "_priv";

    static {
        typeMap.put("CHAR", java.sql.Types.CHAR);
        typeMap.put("VARCHAR", java.sql.Types.VARCHAR);
        typeMap.put("VARCHAR2", java.sql.Types.VARCHAR);

        typeMap.put("NCHAR", java.sql.Types.NCHAR);
        typeMap.put("NVARCHAR", java.sql.Types.NVARCHAR);
        typeMap.put("NVARCHAR2", java.sql.Types.NVARCHAR);

        typeMap.put("LONG", java.sql.Types.LONGVARBINARY);

        typeMap.put("NUMBER", java.sql.Types.NUMERIC);
        typeMap.put("BINARY_FLOAT", java.sql.Types.FLOAT);
        typeMap.put("BINARY_DOUBLE", java.sql.Types.DOUBLE);

        typeMap.put("DATE", java.sql.Types.DATE);
        typeMap.put("TIMESTAMP", java.sql.Types.TIMESTAMP);
        typeMap.put("TIMESTAMP WITH TIME ZONE", java.sql.Types.TIMESTAMP);
        typeMap.put("TIMESTAMP WITH LOCAL TIME ZONE", java.sql.Types.TIMESTAMP);

        typeMap.put("BLOB", java.sql.Types.BLOB);
        typeMap.put("CLOB", java.sql.Types.CLOB);
        typeMap.put("NCLOB", java.sql.Types.NCLOB);
        typeMap.put("BFILE", java.sql.Types.DATALINK);

        typeMap.put("ROWID", java.sql.Types.ROWID);
        typeMap.put("UROWID", java.sql.Types.ROWID);
    }

    public static int typeNameToValueType(String typeName)
    {
        Integer valueType = typeMap.get(typeName.toUpperCase());
        return valueType == null ? java.sql.Types.OTHER : valueType;
    }

    public static List<String> collectPrivilegeNames(ResultSet resultSet)
    {
        // Now collect all privileges columns
        try {
            List<String> privs = new ArrayList<String>();
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                String colName = rsMetaData.getColumnName(i + 1);
                if (colName.toLowerCase().endsWith(COLUMN_POSTFIX_PRIV)) {
                    privs.add(colName.substring(0, colName.length() - COLUMN_POSTFIX_PRIV.length()));
                }
            }
            return privs;
        } catch (SQLException e) {
            log.debug(e);
            return Collections.emptyList();
        }
    }

    public static Map<String, Boolean> collectPrivileges(List<String> privNames, ResultSet resultSet)
    {
        // Now collect all privileges columns
        Map<String, Boolean> privs = new TreeMap<String, Boolean>();
        for (String privName : privNames) {
            privs.put(privName, "Y".equals(JDBCUtils.safeGetString(resultSet, privName + COLUMN_POSTFIX_PRIV)));
        }
        return privs;
    }

}
