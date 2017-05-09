package org.jkiss.dbeaver.ext.test.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class HugeSchemaGenerator {

    public static void main(String[] args) throws SQLException {

        final String url = "jdbc:postgresql://localhost/postgres";
        final Properties props = new Properties();
        props.setProperty("user", "");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(true);

            try (PreparedStatement stmt = conn.prepareStatement(
                        "CREATE SCHEMA HUGE_SCHEMA"))
            {
                stmt.execute();
            }

            for (int i = 0; i < 10000; i++) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE HUGE_SCHEMA.TEST_TABLE" + i + "(ID INTEGER NOT NULL, VAL VARCHAR(64))")) {
                    stmt.execute();
                    if (i % 100 == 0) {
                        System.out.println(i + " tables");
                    }
                }
            }
        }
    }

}
