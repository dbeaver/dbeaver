package org.jkiss.dbeaver.ext.test.tools;

import java.sql.*;
import java.util.Properties;

public class HugeTableGenerator {

    public static void main(String[] args) throws SQLException {

        final String url = "jdbc:oracle:thin:@127.0.0.1:1521:ORCL";
        final Properties props = new Properties();
        props.setProperty("user", "sys as sysdba");
        props.setProperty("password", "cityman");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(true);

            try (PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE BigTable (table_key integer, some_string varchar(64), create_time timestamp, primary key(table_key))"))
            {
                stmt.execute();
            }
            // 10kk records
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO BigTable(table_key, some_string, create_time) values(?,?,?)")) {
                for (int i = 0; i < 10000000; i++) {
                    stmt.setInt(1, i);
                    stmt.setString(2, "Row " + i);
                    stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    stmt.execute();
                    if (i % 100000 == 0) {
                        conn.commit();
                        System.out.println(i + " records");
                    }
                }
                conn.commit();
            }
        }
    }

}
