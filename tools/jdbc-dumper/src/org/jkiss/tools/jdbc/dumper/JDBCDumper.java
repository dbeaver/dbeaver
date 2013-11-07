/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.tools.jdbc.dumper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dumps JDBC metadata
 */
public class JDBCDumper
{
    public static void main(String [] args)
        throws Exception
    {
        if (args.length < 3) {
            System.out.println("Usage: JDBCDumper action driver-class jdbc-url [user] [password]");
            System.exit(1);
        }
        String action = args[0];
        String className = args[1];
        String url = args[2];
        String user = args.length >= 4 ? args[3] : null;
        String password = args.length >= 5 ? args[4] : null;

        Class.forName(className);

        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            if ("plain".equals(action)) {
                dumpMetaDataPlain(connection.getMetaData());
            } else if ("tree".equals(action)) {
                dumpMetaDataTree(connection.getMetaData());
            }
        }
        finally {
            connection.close();
        }
    }

    private static void dumpMetaDataPlain(DatabaseMetaData metaData) throws SQLException
    {
        try {
            dumpResultSet("Catalogs", metaData.getCatalogs());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            dumpResultSet("Schemas", metaData.getSchemas());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            dumpResultSet("Tables", metaData.getTables(null, null, null, null));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void dumpMetaDataTree(DatabaseMetaData metaData) throws SQLException
    {
        try {
            List<String> catalogs = dumpResultSetAndReturn("Catalogs", metaData.getCatalogs(), "TABLE_CAT");
            if (catalogs.isEmpty()) {
                catalogs.add("%");
            }
            for (String catalog : catalogs) {
                List<String> schemas = dumpResultSetAndReturn(catalog + " Schemas", metaData.getSchemas(catalog, "%"), "TABLE_SCHEM");
                if (schemas.isEmpty() && catalogs.size() == 1) {
                    catalog = "%";
                    schemas = dumpResultSetAndReturn("All Schemas", metaData.getSchemas("%", "%"), "TABLE_SCHEM");
                    if (schemas.isEmpty()) {
                        schemas.add("%");
                    }
                }
                for (String schema : schemas) {
                    dumpResultSet("Tables of " + catalog + "." + schema, metaData.getTables(catalog, schema, "%", null));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void dumpResultSet(String name, ResultSet dbResult) throws SQLException
    {
        System.out.println("Dump of [" + name + "]");
        System.out.println("======================================================");

        ResultSetMetaData rsMeta = dbResult.getMetaData();
        int columnCount = rsMeta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(rsMeta.getColumnName(i) + " " + rsMeta.getColumnTypeName(i) + "[" + rsMeta.getColumnDisplaySize(i) + "]" + "(" + rsMeta.getColumnType(i) + ")\t");
        }
        System.out.println();
        while (dbResult.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(dbResult.getObject(i) + "\t");
            }
            System.out.println();
        }

        System.out.println("======================================================");
    }

    private static List<String> dumpResultSetAndReturn(String name, ResultSet dbResult, String columnName) throws SQLException
    {
        System.out.println("Dump of [" + name + "]");
        System.out.println("======================================================");

        ResultSetMetaData rsMeta = dbResult.getMetaData();
        int columnCount = rsMeta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(rsMeta.getColumnName(i) + " " + rsMeta.getColumnTypeName(i) + "[" + rsMeta.getColumnDisplaySize(i) + "]" + "(" + rsMeta.getColumnType(i) + ")\t");
        }
        System.out.println();
        List<String> result = new ArrayList<String>();
        while (dbResult.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(dbResult.getObject(i) + "\t");
            }
            System.out.println();
            try {
                result.add(dbResult.getString(columnName));
            } catch (SQLException e) {
                // no such column
            }
        }

        System.out.println("======================================================");
        return result;
    }

}
