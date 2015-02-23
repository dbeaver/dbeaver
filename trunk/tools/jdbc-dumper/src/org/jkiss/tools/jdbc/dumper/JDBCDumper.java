/*
 * Copyright (C) 2010-2015 Serge Rieder
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
            dumpResultSet("Catalogs", "", metaData.getCatalogs(), null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            dumpResultSet("Schemas", "", metaData.getSchemas(), null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            dumpResultSet("Tables", "", metaData.getTables(null, null, null, null), null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void dumpMetaDataTree(final DatabaseMetaData metaData) throws SQLException
    {
        try {
            List<String> catalogs = new ArrayList<String>();
            try {
                catalogs = dumpResultSetAndReturn("Catalogs", metaData.getCatalogs(), "TABLE_CAT");
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (catalogs.isEmpty()) {
                catalogs.add("%");
            }
            for (String catalog : catalogs) {
                List<String> schemas = new ArrayList<String>();
                try {
                    schemas = dumpResultSetAndReturn(catalog + " Schemas", metaData.getSchemas(catalog, "%"), "TABLE_SCHEM");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (schemas.isEmpty() && catalogs.size() == 1) {
                    catalog = "%";
                    try {
                        schemas = dumpResultSetAndReturn("All Schemas", metaData.getSchemas("%", "%"), "TABLE_SCHEM");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                if (schemas.isEmpty() && catalogs.size() == 1) {
                    try {
                        schemas = dumpResultSetAndReturn(catalog + " Schemas", metaData.getSchemas(), "TABLE_SCHEM");
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                if (schemas.isEmpty()) {
                    schemas.add("%");
                }
                for (final String schema : schemas) {
                    dumpResultSet("Tables of " + catalog + "." + schema, "", metaData.getTables(catalog, schema, "%", null),
                        new NestedFetcher() {
                            @Override
                            public void readNestedInfo(ResultSet resultSet) throws SQLException {
                                String tableName = resultSet.getString("TABLE_NAME");
                                String catName = resultSet.getString("TABLE_CAT");
                                String schemaName = resultSet.getString("TABLE_SCHEM");
                                System.out.println("\tColumns:");
                                try {
                                    dumpResultSet(null, "\t\t", metaData.getColumns(catName, schemaName, tableName, "%"), null);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                                System.out.println("\tPrimary Keys:");
                                try {
                                    dumpResultSet(null, "\t\t", metaData.getPrimaryKeys(catName, schema, tableName), null);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                                System.out.println("\tIndexes:");
                                try {
                                    dumpResultSet(null, "\t\t", metaData.getIndexInfo(catName, schemaName, tableName, false, false), null);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                                System.out.println("\tImported Keys:");
                                try {
                                    dumpResultSet(null, "\t\t", metaData.getImportedKeys(catName, schemaName, tableName), null);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                                System.out.println("\tExported Keys:");
                                try {
                                    dumpResultSet(null, "\t\t", metaData.getExportedKeys(catName, schemaName, tableName), null);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void dumpResultSet(String name, String prefix, ResultSet dbResult, NestedFetcher nestedFetcher) throws SQLException
    {
        if (name != null) {
            System.out.println(prefix + "Dump of [" + name + "]");
            System.out.println(prefix + "======================================================");
        }

        ResultSetMetaData rsMeta = dbResult.getMetaData();
        int columnCount = rsMeta.getColumnCount();
        System.out.print(prefix + "# ");
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(rsMeta.getColumnName(i) + "\t");
        }
        System.out.println();
		int index = 0;
        while (dbResult.next()) {
			index++;
            System.out.print(prefix + index + ".");
            for (int i = 1; i <= columnCount; i++) {
				Object objValue = dbResult.getObject(i);
				String value = objValue == null ? "NULL" : objValue.toString();
				String colName = rsMeta.getColumnName(i);
                System.out.print(value);
				if (value.length() < colName.length()) {
					for (int k = 0; k < colName.length() - value.length(); k++) {
						System.out.print(' ');
					}
				}
				System.out.print('\t');
            }
            System.out.println();
            if (nestedFetcher != null) {
                nestedFetcher.readNestedInfo(dbResult);
            }
        }

        if (name != null) {
            System.out.println(prefix + "======================================================");
        }
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
            } catch (Throwable e) {
                // no such column
            }
        }

        System.out.println("======================================================");
        return result;
    }

    public interface NestedFetcher {
        void readNestedInfo(ResultSet resultSet) throws SQLException;
    }

}
