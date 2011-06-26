/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
