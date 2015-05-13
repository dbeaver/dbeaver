/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import java.sql.*;

public class MySQLErrorsTest {

    public static void main(String[] args) throws Exception
    {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/sa", "root", "1978");
        {
            System.out.println("SHOW VARIABLES");
            PreparedStatement stat = con.prepareStatement("SHOW VARIABLES LIKE '%char%'");
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1) + "=" + rs.getString(2));
            }
        }
        try {
            System.out.println("SELECT * from tablica");
            PreparedStatement stat = con.prepareStatement("SELECT * from tablica");
            stat.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        try {
            System.out.println("SELECT * from �������");
            PreparedStatement stat = con.prepareStatement("SELECT * from �������");
            stat.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
