/*
 * Copyright (C) 2010-2012 Serge Rieder
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

/**
 * Dumps JDBC metadata
 */
public class JDBCDumper
{
    public static void main(String [] args)
        throws Exception
    {
        if (args.length < 3) {
            System.out.println("Usage: JDBCDumper driver-class jdbc-url user [password]");
            System.exit(1);
        }
        String className = args[0];
        String url = args[1];
        String user = args[2];
        String password = args.length >= 4 ? args[3] : null;

        Class.forName(className);

        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            dumpMetaData(connection.getMetaData());
        }
        finally {
            connection.close();
        }
    }

    private static void dumpMetaData(DatabaseMetaData metaData) throws SQLException
    {
        dumpResultSet("Catalogs", metaData.getCatalogs());
        dumpResultSet("Schemas", metaData.getSchemas());
        dumpResultSet("Tables", metaData.getTables(null, null, null, null));
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
}
