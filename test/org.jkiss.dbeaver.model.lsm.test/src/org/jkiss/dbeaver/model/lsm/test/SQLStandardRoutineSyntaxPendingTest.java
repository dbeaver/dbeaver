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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * SQL body forms to enable when they retain their structure instead of falling back to raw DDL/body tokens.
 * Inputs are individual top-level statements; SQLScriptParser is responsible for separating scripts.
 */
@Disabled("The remaining SQL routine and trigger body forms need structured grammar rules (dbeaver#35607)")
public class SQLStandardRoutineSyntaxPendingTest extends DBeaverUnitTest {

    @Test
    void parsesSingleQuotedSqlFunctionBody() {
        assertStructured("CREATE FUNCTION f() RETURNS int LANGUAGE sql AS 'SELECT 1'", "directSqlDataStatement");
    }

    @Test
    void parsesSingleQuotedSqlProcedureBlock() {
        assertStructured("CREATE PROCEDURE p() LANGUAGE sql AS 'BEGIN SELECT 1; END;'", "proceduralBlock", "directSqlDataStatement");
    }

    @Test
    void parsesSqlQueryFunctionBody() {
        assertStructured("CREATE TABLE FUNCTION dataset.f(x INT64) AS (SELECT x)", "queryExpression");
    }

    @Test
    void parsesSqlExpressionFunctionBody() {
        assertStructured("CREATE FUNCTION f(x INT64) AS (x + 1)", "valueExpression");
        assertStructured("CREATE FUNCTION f(x INT) RETURNS INT RETURN x + 1", "valueExpression");
    }

    @Test
    void parsesUnwrappedMultiStatementProcedureBody() {
        assertStructured("CREATE OR ALTER PROC dbo.p AS DECLARE @n int; SELECT @n = 1; SELECT @n", "directSqlDataStatement");
    }

    @Test
    void parsesTriggerWhenPredicateAndInlineSql() {
        assertStructured(
            "CREATE TRIGGER audit_t AFTER UPDATE ON t FOR EACH ROW " +
                "WHEN (NEW.id > 0) BEGIN INSERT INTO logs VALUES (NEW.id); END",
            "triggerWhenClause", "searchCondition", "directSqlDataStatement"
        );
    }

    @Test
    void parsesDeclaredExceptionHandler() {
        assertStructured(
            "CREATE PROCEDURE p() BEGIN DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET failed = 1; " +
                "INSERT INTO t VALUES (1); END",
            "proceduralHandler", "directSqlDataStatement"
        );
    }

    @Test
    void parsesLabeledBlock() {
        assertStructured(
            "CREATE PROCEDURE p() outer_label: BEGIN SELECT 1; END outer_label",
            "proceduralLabeledBlock", "directSqlDataStatement"
        );
    }

    @Test
    void parsesIfElseWithUnlabeledBlocks() {
        assertStructured(
            "CREATE OR ALTER PROCEDURE p AS IF @x = 1 BEGIN SELECT 1; END ELSE BEGIN SELECT 2; END",
            "proceduralIf", "proceduralBlock", "directSqlDataStatement"
        );
    }

    @Test
    void parsesWhileWithUnlabeledBlock() {
        assertStructured(
            "CREATE OR ALTER PROCEDURE p AS WHILE @x < 2 BEGIN SELECT @x; END",
            "proceduralLoop", "proceduralBlock", "directSqlDataStatement"
        );
    }

    @Test
    void parsesCursorLoopRecordScope() {
        assertStructured(
            "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
                "BEGIN FOR row_value IN c LOOP SELECT row_value.id; END LOOP; END",
            "proceduralCursorForLoop", "directSqlDataStatement"
        );
    }

    @Test
    void parsesPositionedUpdateAgainstCursor() {
        assertStructured(
            "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
                "BEGIN UPDATE tab SET id = 1 WHERE CURRENT OF c; END",
            "cursorCurrentOf", "directSqlDataStatement"
        );
    }

    private static void assertStructured(@NotNull String sql, @NotNull String... expectedRules) {
        SQLStandardLexer lexer = new SQLStandardLexer(CharStreams.fromString(sql));
        SQLStandardParser parser = new SQLStandardParser(new CommonTokenStream(lexer));
        // sqlQuery does not consume a top-level delimiter. The supplied input deliberately has none.
        ParseTree tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), sql);
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), sql);
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_sqlQuery).size(), sql);
        Assertions.assertTrue(
            !Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createNamedObjectStatement).isEmpty() ||
                !Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createTriggerStatement).isEmpty(),
            sql
        );
        for (String expectedRule : expectedRules) {
            int ruleId = Arrays.asList(parser.getRuleNames()).indexOf(expectedRule);
            Assertions.assertTrue(ruleId >= 0, () -> "Missing grammar rule " + expectedRule);
            Assertions.assertFalse(Trees.findAllRuleNodes(tree, ruleId).isEmpty(), () -> "Missing " + expectedRule + " in " + sql);
        }
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineBodyTokens).isEmpty(), sql);
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralStatementTokens).isEmpty(), sql);
    }
}
