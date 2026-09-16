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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLQueryDangerousDetectionTest extends DBeaverUnitTest {

    @Test
    public void noDropStatementShouldReturnNoneTypeDrop() {
        // given
        var query = new SQLQuery(null, "SELECT * FROM table WHERE id = ?");
        // then
        Assertions.assertFalse(query.isDropDangerous());
    }

    @Test
    public void noDangerousUpdateShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1 WHERE id = 1");
        // then
        Assertions.assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void noDangerousDeleteShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table SET a = 1 WHERE id = 1");
        // then
        Assertions.assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousUpdateShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1");
        // then
        Assertions.assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousDeleteShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table");
        // then
        Assertions.assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dropTableStatementShouldReturnTableDropType() {
        // given
        var query = new SQLQuery(null, "DROP table users");
        // then
        Assertions.assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropSchemaStatementShouldReturnDropType() {
        // given
        var query = new SQLQuery(null, "DROP schema users");
        // then
        Assertions.assertTrue(query.isDropDangerous());
    }

    @Test
    public void allSelectStatementsShouldHaveSelectType() {
        for (String queryText : List.of(
            "SELECT * FROM test",
            "SELECT id FROM test UNION SELECT id FROM other_test",
            "(SELECT * FROM test)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertEquals(SQLQueryType.SELECT, query.getType(), queryText);
        }
    }

    @Test
    public void schemaChangingStatementsShouldHaveDdlType() {
        for (String queryText : List.of(
            "CREATE TABLE test (id INT)",
            "CREATE VIEW test_view AS SELECT 1",
            "CREATE INDEX test_index ON test (id)",
            "CREATE SCHEMA test_schema",
            "CREATE SEQUENCE test_sequence",
            "CREATE FUNCTION test_function() RETURNS INT RETURN 1",
            "CREATE PROCEDURE test_procedure() AS 'SELECT 1'",
            "ALTER TABLE test ADD name VARCHAR(10)",
            "ALTER VIEW test_view AS SELECT 2",
            "ALTER SEQUENCE test_sequence RESTART WITH 2",
            "DROP TABLE test"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
        }
    }

    @Test
    public void qualifiedDdlShouldExposeItsQualifiedObjectName() {
        for (String queryText : List.of(
            "CREATE TABLE test_catalog.test_schema.test (id INT)",
            "CREATE VIEW test_catalog.test_schema.test_view AS SELECT 1",
            "CREATE SEQUENCE test_catalog.test_schema.test_sequence",
            "CREATE SYNONYM test_catalog.test_schema.test_synonym FOR test",
            "CREATE FUNCTION test_catalog.test_schema.test_function() RETURNS INT RETURN 1",
            "CREATE PROCEDURE test_catalog.test_schema.test_procedure() AS 'SELECT 1'",
            "ALTER VIEW test_catalog.test_schema.test_view AS SELECT 2",
            "ALTER SEQUENCE test_catalog.test_schema.test_sequence RESTART WITH 2"
        )) {
            var query = new SQLQuery(null, queryText);
            var change = query.getDdlChange();

            Assertions.assertNotNull(change, queryText);
            Assertions.assertEquals("test_catalog", change.qualifiedNameParts().get(0), queryText);
            Assertions.assertEquals("test_schema", change.qualifiedNameParts().get(1), queryText);
        }
    }

    @Test
    public void deeplyQualifiedFunctionShouldPreserveItsFullName() {
        var query = new SQLQuery(null, "CREATE FUNCTION server.test_catalog.test_schema.test_function() " +
            "RETURNS INT RETURN 1");
        var change = query.getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.ObjectKind.FUNCTION, change.objectKind());
        Assertions.assertEquals(
            List.of("server", "test_catalog", "test_schema", "test_function"),
            change.qualifiedNameParts()
        );
        Assertions.assertNull(query.getEntityMetadata(false));
    }

    @Test
    public void createSchemaShouldExposeItsDdlChange() {
        var query = new SQLQuery(null, "CREATE SCHEMA test_schema");
        var change = query.getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.Operation.CREATE, change.operation());
        Assertions.assertEquals(SQLDdlChange.ObjectKind.SCHEMA, change.objectKind());
        Assertions.assertEquals(List.of("test_schema"), change.qualifiedNameParts());
        Assertions.assertNull(query.getEntityMetadata(false));
    }

    @Test
    public void containerDdlShouldExposeItsSemanticChange() {
        for (String queryText : List.of(
            "CREATE SCHEMA test_schema",
            "DROP SCHEMA test_schema",
            "ALTER SCHEMA test_schema RENAME TO renamed_schema"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertNotNull(query.getDdlChange(), queryText);
            Assertions.assertEquals(SQLDdlChange.ObjectKind.SCHEMA, query.getDdlChange().objectKind(), queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
        }

        for (String queryText : List.of(
            "CREATE DATABASE test_database",
            "DROP DATABASE IF EXISTS test_database",
            "DROP CATALOG test_catalog",
            "ALTER DATABASE test_database RENAME TO renamed_database"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertNotNull(query.getDdlChange(), queryText);
            Assertions.assertTrue(
                query.getDdlChange().objectKind() == SQLDdlChange.ObjectKind.DATABASE ||
                    query.getDdlChange().objectKind() == SQLDdlChange.ObjectKind.CATALOG,
                queryText
            );
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
        }
    }

    @Test
    public void commentedContainerDdlShouldExposeItsSemanticChange() {
        for (String queryText : List.of(
            "CREATE /* action */ SCHEMA /* object */ IF /* condition */ NOT /* existence */ EXISTS project.dataset",
            "ALTER SCHEMA project /* separator */ . /* separator */ dataset /* target */ RENAME TO renamed_dataset"
        )) {
            var query = new SQLQuery(null, queryText);

            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
            Assertions.assertNotNull(query.getDdlChange(), queryText);
            Assertions.assertEquals(List.of("project", "dataset"), query.getDdlChange().qualifiedNameParts(), queryText);
        }
    }

    @Test
    public void dialectLineCommentsShouldNotHideContainerDdl() {
        var dialect = new BasicSQLDialect() {
            @NotNull
            @Override
            public String[] getSingleLineComments() {
                return new String[] {"--", "//", "#"};
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);

        for (String queryText : List.of(
            "// comment\nCREATE DATABASE test_database",
            "CREATE # comment\nDATABASE test_database"
        )) {
            var query = new SQLQuery(dataSource, queryText);

            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertEquals(SQLDdlChange.ObjectKind.DATABASE, query.getDdlChange().objectKind(), queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
        }
    }

    @Test
    public void nullDialectLineCommentsShouldStillParseContainerDdl() {
        var dialect = new BasicSQLDialect() {
            @Nullable
            @Override
            public String[] getSingleLineComments() {
                return null;
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);

        var change = new SQLQuery(dataSource, "CREATE DATABASE test_database").getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.ObjectKind.DATABASE, change.objectKind());
    }

    @Test
    public void createOrReplaceContainerShouldExposeItsSemanticChange() {
        for (String queryText : List.of(
            "CREATE OR REPLACE DATABASE test_database",
            "CREATE OR REPLACE CATALOG test_catalog"
        )) {
            var query = new SQLQuery(null, queryText);

            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertEquals(SQLDdlChange.Operation.CREATE, query.getDdlChange().operation(), queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
        }
    }

    @Test
    public void qualifiedSchemaRenameShouldExposeItsParentCatalog() {
        var query = new SQLQuery(
            null,
            "/* leading comment */ ALTER SCHEMA \"test.catalog\".\"old.schema\" RENAME TO \"new.schema\""
        );
        var change = query.getDdlChange();

        Assertions.assertEquals(SQLQueryType.DDL, query.getType());
        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.Operation.RENAME, change.operation());
        Assertions.assertEquals(List.of("\"test.catalog\"", "\"old.schema\""), change.qualifiedNameParts());
    }

    @Test
    public void escapedIdentifierQuotesShouldBePreservedDuringContainerParsing() {
        var dialect = new BasicSQLDialect() {
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return new String[][] {{"\"", "\""}, {"`", "`"}, {"[", "]"}};
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        var expectedNames = List.of(
            new String[] {"ALTER SCHEMA \"cat\"\"alog\".\"sche\"\"ma\" RENAME TO renamed", "cat\"alog", "sche\"ma"},
            new String[] {"ALTER SCHEMA `cat``alog`.`sche``ma` RENAME TO renamed", "cat`alog", "sche`ma"},
            new String[] {"ALTER SCHEMA [cat]]alog].[sche]]ma] RENAME TO renamed", "cat]alog", "sche]ma"},
            new String[] {"ALTER SCHEMA \"cat``alog]]\".\"sche``ma]]\" RENAME TO renamed", "cat``alog]]", "sche``ma]]"},
            new String[] {"ALTER SCHEMA `cat\"\"alog]]`.`sche\"\"ma]]` RENAME TO renamed", "cat\"\"alog]]", "sche\"\"ma]]"},
            new String[] {"ALTER SCHEMA [cat\"\"alog``].[sche\"\"ma``] RENAME TO renamed", "cat\"\"alog``", "sche\"\"ma``"}
        );

        for (String[] expected : expectedNames) {
            var change = new SQLQuery(dataSource, expected[0]).getDdlChange();

            Assertions.assertNotNull(change, expected[0]);
            Assertions.assertEquals(
                List.of(expected[1], expected[2]),
                change.qualifiedNameParts().stream().map(name -> dialect.getUnquotedIdentifier(name, true)).toList(),
                expected[0]
            );
        }
    }

    @Test
    public void nonRenameContainerAlterShouldExposeAlterOperation() {
        var schemaQuery = new SQLQuery(null, "ALTER SCHEMA test_schema OWNER TO test_user");
        Assertions.assertEquals(SQLQueryType.DDL, schemaQuery.getType());
        Assertions.assertEquals(SQLDdlChange.Operation.ALTER, schemaQuery.getDdlChange().operation());
        Assertions.assertEquals(SQLDdlChange.ObjectKind.SCHEMA, schemaQuery.getDdlChange().objectKind());
        Assertions.assertTrue(schemaQuery.isMutatingStatement());
        Assertions.assertTrue(schemaQuery.isModifying());

        for (String queryText : List.of(
            "ALTER DATABASE test_database SET TABLESPACE test_tablespace",
            "ALTER CATALOG test_catalog OWNER TO test_user"
        )) {
            var query = new SQLQuery(null, queryText);

            Assertions.assertEquals(SQLQueryType.DDL, query.getType(), queryText);
            Assertions.assertEquals(SQLDdlChange.Operation.ALTER, query.getDdlChange().operation(), queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
            Assertions.assertTrue(query.isModifying(), queryText);
        }
    }

    @Test
    public void containerParserShouldUseDialectIdentifierRules() {
        var dialect = new BasicSQLDialect() {
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return new String[][] {{"<", ">"}};
            }

            @Override
            public char getStructSeparator() {
                return ':';
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);

        var change = new SQLQuery(dataSource, "CREATE SCHEMA <catalog>:<schema>").getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.ObjectKind.SCHEMA, change.objectKind());
        Assertions.assertEquals(List.of("<catalog>", "<schema>"), change.qualifiedNameParts());
    }

    @Test
    public void dialectDdlChangeShouldBeCachedWithParsedQuery() {
        var invocationCount = new AtomicInteger();
        var dialect = new BasicSQLDialect() {
            @Nullable
            @Override
            public SQLDdlChange parseDdlChange(@NotNull String sql) {
                invocationCount.incrementAndGet();
                return new SQLDdlChange(
                    SQLDdlChange.Operation.ALTER,
                    SQLDdlChange.ObjectKind.OTHER,
                    List.of("vendor_object")
                );
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        var query = new SQLQuery(dataSource, "VENDOR DDL vendor_object");

        Assertions.assertEquals(SQLQueryType.DDL, query.getType());
        Assertions.assertNotNull(query.getDdlChange());
        Assertions.assertTrue(query.isMutatingStatement());
        Assertions.assertEquals(1, invocationCount.get());

        query.setText("VENDOR DDL another_object");
        Assertions.assertNotNull(query.getDdlChange());
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void semanticDdlShouldOverrideDialectFallback() {
        var dialect = new BasicSQLDialect() {
            @NotNull
            @Override
            public SQLDdlChange parseDdlChange(@NotNull String sql) {
                return new SQLDdlChange(
                    SQLDdlChange.Operation.ALTER,
                    SQLDdlChange.ObjectKind.OTHER,
                    List.of("fallback")
                );
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);

        var change = new SQLQuery(dataSource, "CREATE TABLE test_schema.test_table (id INT)").getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.Operation.CREATE, change.operation());
        Assertions.assertEquals(SQLDdlChange.ObjectKind.TABLE, change.objectKind());
        Assertions.assertEquals(List.of("test_schema", "test_table"), change.qualifiedNameParts());
    }

    @Test
    public void qualifiedIndexShouldPreserveItsTableContainer() {
        var change = new SQLQuery(null, "CREATE INDEX test_index ON test_catalog.test_schema.test_table (id)")
            .getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.ObjectKind.INDEX, change.objectKind());
        Assertions.assertEquals(List.of("test_catalog", "test_schema", "test_index"), change.qualifiedNameParts());
    }

    @Test
    public void schemaAuthorizationShouldNotTreatAuthorizationAsObjectName() {
        var change = new SQLQuery(null, "CREATE SCHEMA AUTHORIZATION test_user").getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(SQLDdlChange.ObjectKind.SCHEMA, change.objectKind());
        Assertions.assertTrue(change.qualifiedNameParts().isEmpty());
    }

    @Test
    public void nullDialectQuotesShouldPreserveSeparatorsInsideQuotedFunctionName() {
        var dialect = new BasicSQLDialect() {
            @Nullable
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return null;
            }
        };
        var dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        var query = new SQLQuery(dataSource, "CREATE FUNCTION \"test.catalog\".test_schema.test_function() " +
            "RETURNS INT RETURN 1");
        var change = query.getDdlChange();

        Assertions.assertNotNull(change);
        Assertions.assertEquals(
            List.of("\"test.catalog\"", "test_schema", "test_function"),
            change.qualifiedNameParts()
        );
    }

    @Test
    public void readOnlySelectStatementsShouldNotBeMutating() {
        for (String queryText : List.of(
            "SELECT * FROM test",
            "SELECT id FROM test UNION SELECT id FROM other_test",
            "WITH selected AS (SELECT * FROM test) SELECT * FROM selected",
            "(SELECT * FROM test)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertFalse(query.isMutatingStatement(), queryText);
            Assertions.assertFalse(query.isModifying(), queryText);
        }
    }

    @Test
    public void modifyingSelectStatementsShouldBeMutating() {
        for (String queryText : List.of(
            "SELECT * INTO copy FROM test",
            "SELECT * FROM (SELECT * INTO copy FROM test) nested",
            "WITH changed AS (DELETE FROM test RETURNING *) SELECT * FROM changed",
            "WITH changed AS (INSERT INTO test VALUES (1) RETURNING *) SELECT * FROM changed",
            "WITH changed AS (UPDATE test SET id = 1 RETURNING *) SELECT * FROM changed",
            "WITH safe AS (SELECT 1), changed AS (DELETE FROM test RETURNING *) SELECT * FROM safe",
            "SELECT 1 ORDER BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)",
            "SELECT 1 UNION SELECT 2 " +
                "ORDER BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)",
            "SELECT 1 GROUP BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
            Assertions.assertTrue(query.isModifying(), queryText);
        }

        var lockingQuery = new SQLQuery(null, "SELECT * FROM test FOR UPDATE");
        Assertions.assertFalse(lockingQuery.isMutatingStatement());
        Assertions.assertTrue(lockingQuery.isModifying());
    }

}
