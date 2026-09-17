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
package org.jkiss.dbeaver.model.lsm.test;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLObjectOperation;
import org.jkiss.dbeaver.model.sql.semantics.SQLObjectOperationRecognizer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SQLObjectOperationRecognizerTest extends DBeaverUnitTest {
    @Test
    void recognizesObjectOperations() {
        assertOperation("CREATE TABLE cat.sch.tab (id INT)", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.TABLE, "cat", "sch", "tab");
        assertOperation("ALTER TABLE cat.sch.tab ADD value INT", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.TABLE, "cat", "sch", "tab");
        assertOperation("ALTER TABLE ONLY cat.sch.tab ADD value INT", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.TABLE, "cat", "sch", "tab");
        assertOperation("DROP TABLE IF EXISTS cat.sch.tab", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.TABLE, "cat", "sch", "tab");

        assertOperation("CREATE VIEW sch.view_name AS SELECT 1", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.VIEW, "sch", "view_name");
        assertOperation("CREATE OR REPLACE MATERIALIZED VIEW sch.view_name AS SELECT 1", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.VIEW, "sch", "view_name");
        assertOperation("ALTER VIEW sch.view_name AS SELECT 2", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.VIEW, "sch", "view_name");
        assertOperation("DROP MATERIALIZED VIEW sch.view_name", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.VIEW, "sch", "view_name");

        assertOperation("CREATE INDEX idx ON cat.sch.tab(id)", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.INDEX, "cat", "sch", "idx");
        assertOperation("DROP INDEX cat.sch.idx", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.INDEX, "cat", "sch", "idx");
        assertOperation("DROP INDEX idx ON cat.sch.tab", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.INDEX, "cat", "sch", "idx");

        assertOperation("CREATE FUNCTION sch.func() RETURNS INT RETURN 1", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.FUNCTION, "sch", "func");
        assertOperation("DROP FUNCTION sch.func", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.FUNCTION, "sch", "func");
        assertOperation("ALTER FUNCTION sch.func() RENAME TO renamed", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.FUNCTION, "sch", "func");
        assertOperation("CREATE PROCEDURE sch.proc() AS 'SELECT 1'", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.PROCEDURE, "sch", "proc");
        assertOperation("DROP PROCEDURE sch.proc", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.PROCEDURE, "sch", "proc");
        assertOperation("DROP PROCEDURE sch.proc(INT) CASCADE", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.PROCEDURE, "sch", "proc");
        assertOperation("ALTER PROCEDURE sch.proc() OWNER TO admin", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.PROCEDURE, "sch", "proc");

        assertOperation("CREATE SEQUENCE sch.seq", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SEQUENCE, "sch", "seq");
        assertOperation("ALTER SEQUENCE sch.seq RESTART WITH 2", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.SEQUENCE, "sch", "seq");
        assertOperation("DROP SEQUENCE sch.seq", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.SEQUENCE, "sch", "seq");
        assertOperation("CREATE SYNONYM sch.syn FOR sch.tab", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SYNONYM, "sch", "syn");
        assertOperation("DROP SYNONYM sch.syn", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.SYNONYM, "sch", "syn");
        assertOperation("ALTER SYNONYM sch.syn COMPILE", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.SYNONYM, "sch", "syn");
        assertOperation("ALTER INDEX sch.idx REBUILD", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.INDEX, "sch", "idx");
        assertOperation("RENAME TABLE sch.old_name TO sch.new_name", SQLObjectOperation.Operation.RENAME, SQLObjectOperation.ObjectKind.TABLE, "sch", "old_name");

        assertOperation("CREATE OR REPLACE SCHEMA root.child", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA, "root", "child");
        assertOperation("DROP SCHEMA IF EXISTS `root`.`child`", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.SCHEMA, "`root`", "`child`");
        assertOperation("ALTER SCHEMA root.old_name RENAME TO root.new_name", SQLObjectOperation.Operation.RENAME, SQLObjectOperation.ObjectKind.SCHEMA, "root", "old_name");
        assertOperation("ALTER SCHEMA root.child OWNER TO admin", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.SCHEMA, "root", "child");

        assertOperation("CREATE DATABASE db;", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.DATABASE, "db");
        assertOperation("DROP DATABASE IF EXISTS db", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.DATABASE, "db");
        assertOperation("ALTER /* kind */ DATABASE [old database] RENAME TO [new database]", SQLObjectOperation.Operation.RENAME, SQLObjectOperation.ObjectKind.DATABASE, "[old database]");
        assertOperation("ALTER DATABASE db SET OWNER admin", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.DATABASE, "db");

        assertOperation("CREATE OR REPLACE CATALOG \"my catalog\"", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.CATALOG, "\"my catalog\"");
        assertOperation("DROP CATALOG IF EXISTS catalog_name", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.CATALOG, "catalog_name");
        assertOperation("ALTER CATALOG old_name RENAME TO new_name", SQLObjectOperation.Operation.RENAME, SQLObjectOperation.ObjectKind.CATALOG, "old_name");
        assertOperation("ALTER CATALOG main SET OWNER admin", SQLObjectOperation.Operation.ALTER, SQLObjectOperation.ObjectKind.CATALOG, "main");

        assertOperation("DROP ROLE role_name", SQLObjectOperation.Operation.DROP, SQLObjectOperation.ObjectKind.OTHER, "role_name");
        assertOperation("CREATE SCHEMA AUTHORIZATION user_name", SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA);
    }

    @Test
    void ignoresNonDdl() {
        Assertions.assertNull(parse("SELECT * FROM example"));
    }

    @Test
    void ignoresMalformedAndMultipleQueries() {
        Assertions.assertNull(parse("CREATE DATABASE"));
        Assertions.assertNull(parse("CREATE DATABASE first; DROP DATABASE second"));
    }

    private static void assertOperation(
        @NotNull String sql,
        @NotNull SQLObjectOperation.Operation operation,
        @NotNull SQLObjectOperation.ObjectKind objectKind,
        @NotNull String... nameParts
    ) {
        Assertions.assertEquals(new SQLObjectOperation(operation, objectKind, List.of(nameParts)), parse(sql), sql);
    }

    private static SQLObjectOperation parse(@NotNull String sql) {
        return SQLObjectOperationRecognizer.recognize(BasicSQLDialect.INSTANCE, sql);
    }
}
