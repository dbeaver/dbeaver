/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.*;

public class    MySQLErrorsTest {

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
