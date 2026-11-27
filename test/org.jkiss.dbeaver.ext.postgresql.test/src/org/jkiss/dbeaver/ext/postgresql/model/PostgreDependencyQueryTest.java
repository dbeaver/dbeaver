/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Runs against a disposable schema in a PostgreSQL-compatible test database.
 * DBEAVER_PG_TEST_URL, DBEAVER_PG_TEST_USER and DBEAVER_PG_TEST_PASSWORD select the database.
 * All fixture DDL is rolled back, including on failure.
 */
@EnabledIfEnvironmentVariable(named = "DBEAVER_PG_TEST_URL", matches = ".+")
public class PostgreDependencyQueryTest {
    private Connection connection;
    private final List<Long> objectIds = new ArrayList<>();

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
            System.getenv("DBEAVER_PG_TEST_URL"),
            System.getenv("DBEAVER_PG_TEST_USER"),
            System.getenv("DBEAVER_PG_TEST_PASSWORD"));
        connection.setAutoCommit(false);
        String schema = "dependency_test_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        execute("CREATE TABLE " + schema + ".parent (id serial PRIMARY KEY, amount integer DEFAULT 42 CHECK (amount >= 0), payload text)");
        execute("CREATE INDEX parent_payload ON " + schema + ".parent (payload)");
        execute("CREATE TABLE " + schema + ".child (parent_id integer REFERENCES " + schema + ".parent(id))");
        execute("CREATE VIEW " + schema + ".parent_view AS SELECT id, amount FROM " + schema + ".parent");
        execute("CREATE FUNCTION " + schema + ".parent_rows() RETURNS SETOF " + schema + ".parent LANGUAGE SQL AS 'SELECT * FROM "
            + schema + ".parent'");
        String version = connection.getMetaData().getDatabaseProductVersion();
        if (!version.contains("Greenplum") && !version.contains("Cloudberry") && !version.contains("Greengage")) {
            execute("CREATE FUNCTION " + schema + ".parent_trigger() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RETURN NEW; END;'");
            execute("CREATE TRIGGER parent_insert BEFORE INSERT ON " + schema + ".parent FOR EACH ROW EXECUTE PROCEDURE "
                + schema + ".parent_trigger()");
        }
        try (var statement = connection.prepareStatement(
            "SELECT oid FROM pg_class WHERE relnamespace=(SELECT oid FROM pg_namespace WHERE nspname=?) "
                + "UNION ALL SELECT oid FROM pg_proc WHERE pronamespace=(SELECT oid FROM pg_namespace WHERE nspname=?)")) {
            statement.setString(1, schema);
            statement.setString(2, schema);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    objectIds.add(rows.getLong(1));
                }
            }
        }
        assertFalse(objectIds.isEmpty());
        objectIds.add(0L);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
        objectIds.clear();
    }

    @Test
    public void dependentsMatchOriginalQuery() throws IOException, SQLException {
        compareQueries(true);
    }

    @Test
    public void dependenciesMatchOriginalQuery() throws IOException, SQLException {
        compareQueries(false);
    }

    private void compareQueries(boolean dependents) throws IOException, SQLException {
        String original;
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("PostgreDependencyQueryTest.sql"))) {
            original = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                .replace("{queryObjectId}", dependents ? "objid" : "refobjid")
                .replace("{conditionObjectId}", dependents ? "refobjid" : "objid");
        }
        for (long objectId : objectIds) {
            // The oracle needs a small search space too; the query under test must work with Greenplum's default of 20.
            execute("SET LOCAL join_collapse_limit=8");
            Set<List<String>> expected = readRows(original, objectId);
            execute("SET LOCAL join_collapse_limit=20");
            Set<List<String>> actual = readRows(PostgreDependency.getDependenciesQuery(dependents), objectId);
            assertEquals(expected, actual, "Dependencies differ for OID " + objectId + ", dependents=" + dependents);
        }
    }

    @NotNull
    private Set<List<String>> readRows(@NotNull String query, long objectId) throws SQLException {
        try (var statement = connection.prepareStatement(query)) {
            // Also catches a removed optimization barrier on Greenplum, where planning the old query takes seconds.
            statement.setQueryTimeout(3);
            statement.setLong(1, objectId);
            try (var rows = statement.executeQuery()) {
                Set<List<String>> result = new HashSet<>();
                int columnCount = rows.getMetaData().getColumnCount();
                while (rows.next()) {
                    List<String> row = new ArrayList<>(columnCount);
                    for (int column = 1; column <= columnCount; column++) {
                        row.add(rows.getString(column));
                    }
                    result.add(row);
                }
                return result;
            }
        }
    }

    private void execute(@NotNull String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
