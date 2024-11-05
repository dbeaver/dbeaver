/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
