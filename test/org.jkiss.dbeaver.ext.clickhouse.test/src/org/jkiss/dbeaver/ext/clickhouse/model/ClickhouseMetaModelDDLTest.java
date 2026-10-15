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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClickhouseMetaModelDDLTest extends DBeaverUnitTest {

    @Test
    public void normalizesTableColumnDeclaration() {
        final String ddl = "CREATE TABLE db.t (`a` Int32, `b` String) ENGINE = MergeTree ORDER BY a";
        Assertions.assertEquals(
            "CREATE TABLE db.t (\n`a` Int32,\n `b` String\n) ENGINE = MergeTree ORDER BY a",
            ClickhouseMetaModel.normalizeDDL(ddl));
    }

    @Test
    public void keepsViewDefinitionWithoutParenthesesIntact() {
        // SHOW CREATE TABLE on a plain view returns a statement with no column declaration block.
        final String ddl = "CREATE VIEW db.v AS SELECT id FROM db.t";
        Assertions.assertEquals(ddl, ClickhouseMetaModel.normalizeDDL(ddl));
    }

    @Test
    public void keepsViewDefinitionWithParenthesesIntact() {
        // Parentheses belonging to the view query must not be mistaken for a column declaration.
        final String ddl = "CREATE VIEW db.v AS SELECT count(id), toInt32(x) FROM db.t";
        Assertions.assertEquals(ddl, ClickhouseMetaModel.normalizeDDL(ddl));
    }

    @Test
    public void keepsAlreadyFormattedDeclarationIntact() {
        // ") ENGINE" does not match when the statement is already multi-line formatted.
        final String ddl = "CREATE TABLE db.t\n(\n    `a` Int32\n)\nENGINE = MergeTree";
        Assertions.assertEquals(ddl, ClickhouseMetaModel.normalizeDDL(ddl));
    }
}
