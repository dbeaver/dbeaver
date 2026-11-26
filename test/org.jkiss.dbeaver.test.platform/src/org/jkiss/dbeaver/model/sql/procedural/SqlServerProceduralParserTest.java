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
package org.jkiss.dbeaver.model.sql.procedural;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDialectMssql;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlServerProceduralParserTest {
    @Test
    void incompleteInsertSelectKeepsItsFromClauseInsideTrigger() {
        String insert = "INSERT INTO dbo.order_audit (order_id) SELECT i. FROM INSERTED AS i";
        String trigger = "CREATE OR ALTER TRIGGER dbo.orders_ai ON dbo.orders " +
            "AFTER INSERT AS BEGIN " + insert + "; END";

        assertInsertSource(parse(insert), "SELECT i. FROM INSERTED AS i", 0);
        assertInsertSource(parse(trigger), "SELECT i. FROM INSERTED AS i", 1);
    }

    @Test
    void unknownStatementCanStillContainDots() {
        String trigger = "CREATE TRIGGER tr ON tab AFTER INSERT AS BEGIN " +
            "PRINT dbo.message; SELECT i. FROM INSERTED AS i; END";
        SQLStandardParser.SqlQueryContext query = parse(trigger);
        var unknown = Trees.findAllRuleNodes(query, SQLStandardParser.RULE_proceduralStatementTokens);
        Assertions.assertEquals(1, unknown.size());
        Assertions.assertEquals("PRINTdbo.message", unknown.iterator().next().getText());
        var select = Trees.findAllRuleNodes(query, SQLStandardParser.RULE_queryExpression);
        Assertions.assertEquals(1, select.size());
        Assertions.assertEquals("SELECTi.FROMINSERTEDASi", select.iterator().next().getText());
    }

    private static void assertInsertSource(SQLStandardParser.SqlQueryContext query, String expected, int proceduralStatements) {
        Assertions.assertEquals(proceduralStatements,
            Trees.findAllRuleNodes(query, SQLStandardParser.RULE_proceduralStatement).size());
        var insert = Trees.findAllRuleNodes(query, SQLStandardParser.RULE_insertStatement);
        Assertions.assertEquals(1, insert.size());
        var sources = Trees.findAllRuleNodes(insert.iterator().next(), SQLStandardParser.RULE_queryExpression);
        Assertions.assertEquals(1, sources.size());
        Assertions.assertEquals(expected.replace(" ", ""), sources.iterator().next().getText());
        Assertions.assertTrue(Trees.findAllRuleNodes(query, SQLStandardParser.RULE_proceduralStatementTokens).isEmpty());
    }

    private static SQLStandardParser.SqlQueryContext parse(String sql) {
        var dialect = new SQLServerDialectMssql();
        var preferences = new SimplePreferenceStore() {
            @Override
            public void save() {
                // In-memory test preferences.
            }
        };
        var syntax = new SQLSyntaxManager(preferences);
        syntax.init(dialect, preferences);
        var parameters = LSMAnalyzerParameters.forDialect(dialect, syntax);
        var parser = new SQLStandardParser(new CommonTokenStream(
            new SQLStandardLexer(CharStreams.fromString(sql), parameters)), parameters);
        var query = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), sql);
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), sql);
        return query;
    }
}
