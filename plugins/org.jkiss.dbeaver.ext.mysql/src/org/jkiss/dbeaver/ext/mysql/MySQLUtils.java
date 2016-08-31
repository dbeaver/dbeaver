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

package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQL utils
 */
public class MySQLUtils {

    private static final Log log = Log.getLog(MySQLUtils.class);

    private static Map<String, Integer> typeMap = new HashMap<>();
    public static final String COLUMN_POSTFIX_PRIV = "_priv";

    static {
        typeMap.put("bit", java.sql.Types.BIT);
        typeMap.put("bool", java.sql.Types.BOOLEAN);
        typeMap.put("boolean", java.sql.Types.BOOLEAN);
        typeMap.put("tinyint", java.sql.Types.TINYINT);
        typeMap.put("smallint", java.sql.Types.SMALLINT);
        typeMap.put("mediumint", java.sql.Types.INTEGER);
        typeMap.put("int", java.sql.Types.INTEGER);
        typeMap.put("integer", java.sql.Types.INTEGER);
        typeMap.put("int24", java.sql.Types.INTEGER);
        typeMap.put("bigint", java.sql.Types.BIGINT);
        typeMap.put("real", java.sql.Types.DOUBLE);
        typeMap.put("float", java.sql.Types.FLOAT);
        typeMap.put("decimal", java.sql.Types.DECIMAL);
        typeMap.put("dec", java.sql.Types.DECIMAL);
        typeMap.put("numeric", java.sql.Types.DECIMAL);
        typeMap.put("double", java.sql.Types.DOUBLE);
        typeMap.put("double precision", java.sql.Types.DOUBLE);
        typeMap.put("char", java.sql.Types.CHAR);
        typeMap.put("varchar", java.sql.Types.VARCHAR);
        typeMap.put("date", java.sql.Types.DATE);
        typeMap.put("time", java.sql.Types.TIME);
        typeMap.put("year", java.sql.Types.DATE);
        typeMap.put("timestamp", java.sql.Types.TIMESTAMP);
        typeMap.put("datetime", java.sql.Types.TIMESTAMP);

        typeMap.put("tinyblob", java.sql.Types.BINARY);
        typeMap.put("blob", java.sql.Types.LONGVARBINARY);
        typeMap.put("mediumblob", java.sql.Types.LONGVARBINARY);
        typeMap.put("longblob", java.sql.Types.LONGVARBINARY);

        typeMap.put("tinytext", java.sql.Types.VARCHAR);
        typeMap.put("text", java.sql.Types.VARCHAR);
        typeMap.put("mediumtext", java.sql.Types.VARCHAR);
        typeMap.put("longtext", java.sql.Types.VARCHAR);

        typeMap.put(MySQLConstants.TYPE_NAME_ENUM, java.sql.Types.CHAR);
        typeMap.put(MySQLConstants.TYPE_NAME_SET, java.sql.Types.CHAR);
        typeMap.put("geometry", java.sql.Types.BINARY);
        typeMap.put("binary", java.sql.Types.BINARY);
        typeMap.put("varbinary", java.sql.Types.VARBINARY);
    }

    public static int typeNameToValueType(String typeName)
    {
        Integer valueType = typeMap.get(typeName.toLowerCase(Locale.ENGLISH));
        return valueType == null ? java.sql.Types.OTHER : valueType;
    }

    public static List<String> collectPrivilegeNames(ResultSet resultSet)
    {
        // Now collect all privileges columns
        try {
            List<String> privs = new ArrayList<>();
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                String colName = rsMetaData.getColumnName(i + 1);
                if (colName.toLowerCase(Locale.ENGLISH).endsWith(COLUMN_POSTFIX_PRIV)) {
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
        Map<String, Boolean> privs = new TreeMap<>();
        for (String privName : privNames) {
            privs.put(privName, "Y".equals(JDBCUtils.safeGetString(resultSet, privName + COLUMN_POSTFIX_PRIV)));
        }
        return privs;
    }


    public static String getMySQLConsoleBinaryName()
    {
        return RuntimeUtils.getNativeBinaryName("mysql");
    }

    public static File getHomeBinary(DBPClientHome home, String binName) throws IOException
    {
        binName = RuntimeUtils.getNativeBinaryName(binName);
        File dumpBinary = new File(home.getHomePath(), "bin/" + binName);
        if (!dumpBinary.exists()) {
            dumpBinary = new File(home.getHomePath(), binName);
            if (!dumpBinary.exists()) {
                throw new IOException("Utility '" + binName + "' not found in MySQL home '" + home.getDisplayName() + "'");
            }
        }
        return dumpBinary;
    }

    public static String determineCurrentDatabase(JDBCSession session) throws DBCException {
        // Get active schema
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()")) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(1);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

}
