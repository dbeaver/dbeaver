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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;

/**
 * Dumps JDBC metadata
 */
public class JDBCDumper
{
    public static void main(String [] args)
        throws Exception
    {
        if (args.length < 4) {
            System.out.println("Usage: JDBCDumper driver-class jdbc-url user password");
            System.exit(1);
        }
        String className = args[0];
        String url = args[1];
        String user = args[2];
        String password = args[3];

        Class.forName(className);

        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            dumpMetaData(connection.getMetaData());
        }
        finally {
            connection.close();
        }
    }

    private static void dumpMetaData(DatabaseMetaData metaData)
    {

    }
}
