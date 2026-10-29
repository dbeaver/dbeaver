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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SQLStandardGrammarAcceptanceTest extends DBeaverUnitTest {
    private static final List<GrammarCase> CASES = List.of(
        new GrammarCase("dbeaver#37939 GENERATED identity with UNIQUE", """
            CREATE TABLE IF NOT EXISTS requirements (
              id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              name varchar(255) NOT NULL,
              unique (id, name)
            );
            """),
        new GrammarCase("dbeaver#36357 INNER JOIN LATERAL", """
            select *
            from
              pg_class
              inner join lateral (
                select * from pg_attribute where attrelid = pg_class.oid
              ) t on true
            """),
        new GrammarCase("dbeaver#36541 LEFT JOIN LATERAL", """
            SELECT * FROM prt1 t1 LEFT JOIN LATERAL
              (SELECT t2.a AS t2a, t3.a AS t3a, least(t1.a,t2.a,t3.b) FROM prt1 t2 JOIN prt2 t3 ON (t2.a = t3.b)) ss
              ON t1.a = ss.t2a WHERE t1.b = 0 ORDER BY t1.a;
            """),
        new GrammarCase("dbeaver#37431 STRAIGHT_JOIN", """
            SELECT
                mps.id AS mpSupplyID,
                mpsp.id AS mpSupplyProductID,
                mpsp.mpProductCardID,
                mps.mpWarehouseID,
                mpst.id AS mpStockID,
                mpstd.id AS mpStockDateID,
                mpstd.mpQuantitySupply,
                GROUP_CONCAT(mpsp.id) AS mpSupplyProductIDs,
                SUM(mpsp.productNumApi) AS productNumApi
                FROM
                sMarketplace_supply AS mps
                STRAIGHT_JOIN sPlace AS pl ON (pl.mpSupplyID=mps.id)
                STRAIGHT_JOIN sSerial AS s ON (s.placeID=pl.placeID)
                STRAIGHT_JOIN sMarketplace_supply_product AS mpsp ON (mpsp.mpSupplyID=mps.id)
                STRAIGHT_JOIN sMarketplace_stock AS mpst ON (mpst.mpWarehouseID=mps.mpWarehouseID AND mpst.mpProductCardID=mpsp.mpProductCardID)
                STRAIGHT_JOIN sMarketplace_stock_date AS mpstd ON (mpstd.mpStockID=mpst.id AND mpstd.mpStockDate=mps.mpSupplyDateFact)
                WHERE 1=1
                #AND mpsp.mpProductCardID=85

                GROUP BY mpstd.id
                HAVING mpQuantitySupply!=productNumApi
            """),
        new GrammarCase("dbeaver#35593 nested Array datatype", """
            CREATE TABLE test_arrays
            (
                id UInt32,
                int_array Array(Int32),
                string_array Array(String),
                float_array Array(Float64),
                nested_int_array Array(Array(Int32)),
                nested_string_array Array(Array(String)),
                boolean_array Array(Bool)
            ) ENGINE = MergeTree()
            ORDER BY id;
            """),
        new GrammarCase("dbeaver#35125 / dbeaver#37894 recursive CTE with VALUES", """
            with recursive all_data as (
                select *
                from (values(1, 1, 10),(1, 2, 3),(1, 3, 7),(1, 4, 1),(1, 5, 2),(2, 1, 6),(2, 2, 4),(2, 3, 5),(2, 4, 8),(2, 5, 1)) as s(gr, id, quantity)
            ),
            ct AS (
                select *, c.quantity AS sm
                from all_data AS c
                WHERE id = 1
                UNION ALL
                select c2.*, c.sm + c2.quantity AS sm
                from ct AS c
                JOIN all_data AS c2 ON c2.id = c.id + 1 AND c.gr = c2.gr
            )
            SELECT *
            FROM ct
            ORDER BY gr, id
            ;
            """),
        new GrammarCase("dbeaver#34924 UPDATE FROM aliases", """
            update tab1 t
            set name1 = t2.name1
            from tab2 t2
            where t.id = t2.id
            """),
        new GrammarCase("dbeaver#22336 GROUP_CONCAT ORDER BY SEPARATOR", """
            select
            u.username
            ,u.lov_user_status
            ,GROUP_CONCAT(m.name ORDER BY m.name SEPARATOR ', ') modules
            ,u.created
            ,u.updated
            ,u.lov_user_type
            ,u.last_login_date
            ,u.lov_user_status
            ,u.blocked
            from users u
            left join users_modules um on um.user_id = u.id
            left join modules m on um.module_id = m.id
            where
            u.lov_user_status = 'Active'
            Group by u.id
            ;
            """),
        new GrammarCase("dbeaver#22168 ROW_NUMBER OVER", """
            SELECT *, ROW_NUMBER()
            OVER (ORDER BY price) AS row_num
            FROM Persons;
            """),
        new GrammarCase("dbeaver#22773 ORDER BY i.*", "select * from account_invoice i order by i.*;"),
        new GrammarCase("dbeaver#20996 NOTNULL", "select * from mytable where 1 notnull;"),
        new GrammarCase("dbeaver#29316 DROP TABLE cache", "DROP TABLE cache;"),
        new GrammarCase("dbeaver#35607 ALTER FUNCTION", "ALTER FUNCTION s.f(integer) OWNER TO admin;"),
        new GrammarCase("dbeaver#35607 ALTER FUNCTION without signature", "ALTER FUNCTION myschema.myfunction OWNER TO myowner;"),
        new GrammarCase("dbeaver#41493 function argument is not a column", """
            CREATE OR REPLACE FUNCTION test_func(IN x int4) RETURNS int4 LANGUAGE sql AS $$ SELECT x; $$;
            """),
        new GrammarCase("dbeaver#35607 CREATE FUNCTION with tagged SQL body", """
            CREATE OR REPLACE FUNCTION s.f(in x integer) RETURNS integer LANGUAGE plpgsql AS $fn$
            BEGIN
              RETURN x + 1;
            END;
            $fn$;
            """),
        new GrammarCase("dbeaver#35607 CREATE PROCEDURE with SQL body", """
            CREATE PROCEDURE s.p() AS
            BEGIN
              SELECT 1;
              SELECT 2;
            END;
            """),
        new GrammarCase("dbeaver#35607 CREATE TRIGGER with SQL block", """
            CREATE TRIGGER t BEFORE INSERT ON s.tab FOR EACH ROW
            BEGIN
              INSERT INTO audit VALUES (NEW.id);
            END;
            """),
        new GrammarCase("dbeaver#35607 CREATE TRIGGER with function target", """
            CREATE OR REPLACE TRIGGER t AFTER UPDATE ON s.tab FOR EACH ROW
            EXECUTE FUNCTION s.on_change();
            """),
        new GrammarCase("dbeaver#35607 ALTER and DROP TRIGGER", """
            ALTER TRIGGER t ON s.tab RENAME TO t2;
            DROP TRIGGER IF EXISTS t2 ON s.tab;
            """),
        new GrammarCase("dbeaver#35607 anonymous DO block", """
            DO $body$ BEGIN SELECT 1; END; $body$;
            """),
        new GrammarCase("dbeaver#35607 anonymous EXECUTE BLOCK", """
            EXECUTE BLOCK AS BEGIN SELECT 1; END;
            """),
        new GrammarCase("dbeaver#35607 anonymous DECLARE block", """
            DECLARE x integer;
            BEGIN
              IF x = 1 THEN SELECT x; END IF;
            END;
            """),
        new GrammarCase("dbeaver#35607 SQL Server PROC", """
            CREATE OR ALTER PROC dbo.p AS BEGIN SELECT 1; END;
            """),
        new GrammarCase("dbeaver#35607 SQL/PSM trigger", """
            CREATE TRIGGER tr AFTER UPDATE ON tab REFERENCING NEW AS n FOR EACH ROW
            BEGIN ATOMIC IF TRUE THEN SIGNAL SQLSTATE '45000'; END IF; END;
            """),
        new GrammarCase("dbeaver#35607 BigQuery table function", """
            CREATE TABLE FUNCTION dataset.f(x INT64) AS (SELECT x);
            """),
        new GrammarCase("dbeaver#35607 external routine", """
            CREATE FUNCTION f() RETURNS INT LANGUAGE JAVA EXTERNAL NAME 'example.Class.method';
            """),
        new GrammarCase("dbeaver#35607 function with language after body", """
            CREATE FUNCTION f() RETURNS INT AS $body$ SELECT 1; $body$ LANGUAGE sql;
            """),
        new GrammarCase("dbeaver#35607 local declaration and body", """
            CREATE PROCEDURE p IS
              x INTEGER;
            BEGIN
              SELECT x;
            END;
            """),
        new GrammarCase("dbeaver#35607 anonymous NOT ATOMIC", """
            BEGIN NOT ATOMIC SELECT 1; END;
            """),
        new GrammarCase("dbeaver#35607 DO with trailing language", """
            DO $$ BEGIN SELECT 1; END; $$ LANGUAGE plpgsql;
            """),
        new GrammarCase("dbeaver#35607 trigger with several events", """
            CREATE TRIGGER t BEFORE INSERT OR UPDATE ON tab FOR EACH ROW
            BEGIN SET NEW.id = 1; END;
            """),
        new GrammarCase("dbeaver#35607 nested Oracle control flow", """
            CREATE PROCEDURE p AS BEGIN
              IF 1 = 1 THEN SELECT 1; ELSIF 1 = 2 THEN SELECT 2; ELSE SELECT 3; END IF;
              CASE WHEN 1 = 1 THEN SELECT 1; END CASE;
            EXCEPTION WHEN OTHERS THEN SELECT 0;
            END;
            """),
        new GrammarCase("dbeaver#35607 declared cursor", """
            CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM t;
            BEGIN OPEN c; FETCH NEXT FROM c INTO x; CLOSE c; END;
            """),
        new GrammarCase("dbeaver#35607 local table variable", """
            CREATE PROCEDURE p AS BEGIN DECLARE @items TABLE (id int);
              INSERT INTO @items (id) VALUES (1);
              SELECT id FROM @items;
            END;
            """),
        new GrammarCase("dbeaver#35607 declared session temporary table", """
            DECLARE GLOBAL TEMPORARY TABLE SESSION.items (id int);
            """),
        new GrammarCase("dbeaver#35607 separate cursor statements", """
            DECLARE c CURSOR FOR SELECT id FROM tab;
            OPEN c;
            FETCH NEXT FROM c INTO value;
            CLOSE c;
            """),
        new GrammarCase("dbeaver#35607 temporary table inside routine", """
            CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE items (id int);
              SELECT id FROM items;
              DROP TABLE items;
            END;
            """)
    );

    @TestFactory
    @NotNull
    Stream<DynamicTest> acceptsClosedIssueStatements() {
        return CASES.stream().map(testCase -> DynamicTest.dynamicTest(
            testCase.name(),
            () -> assertAccepted(testCase.sql())
        ));
    }

    @Test
    void recognizesNestedSqlAndClosingDollarTag() {
        String sql = "DO $outer$ BEGIN SELECT 1; SELECT '$outer$'; END; $outer$;";
        SQLStandardParser parser = makeParser(sql);
        var tree = parser.sqlQueries();

        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_anonymousBlockStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_dollarQuotedBody).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralBlock).size());
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_sqlQuery).size());
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    void recognizesSqlInsideFunctionAndTriggerBodies() {
        SQLStandardParser routineParser = makeParser("CREATE FUNCTION f() RETURNS int AS $fn$ BEGIN SELECT 1; END; $fn$");
        var routine = routineParser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(routine, SQLStandardParser.RULE_createNamedObjectStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(routine, SQLStandardParser.RULE_proceduralBlock).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(routine, SQLStandardParser.RULE_directSqlDataStatement).size());

        SQLStandardParser triggerParser = makeParser("CREATE TRIGGER t AFTER INSERT ON tab BEGIN INSERT INTO audit VALUES (1); END");
        var trigger = triggerParser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(trigger, SQLStandardParser.RULE_createTriggerStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(trigger, SQLStandardParser.RULE_proceduralBlock).size());
        Assertions.assertFalse(Trees.findAllRuleNodes(trigger, SQLStandardParser.RULE_directSqlDataStatement).isEmpty());
        Assertions.assertEquals(0, routineParser.getNumberOfSyntaxErrors());
        Assertions.assertEquals(0, triggerParser.getNumberOfSyntaxErrors());
    }

    @Test
    void doesNotConfuseTableFunctionsWithCreateTable() {
        SQLStandardParser parser = makeParser("CREATE TABLE FUNCTION dataset.f(x INT64) AS (SELECT x)");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createNamedObjectStatement).size());
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createTableStatement).isEmpty());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    void recognizesParenthesizedTableFunctionSqlWhileTyping() {
        SQLStandardParser complete = makeParser(
            "CREATE TABLE FUNCTION dataset.qa_table_fn(x INT64) AS (SELECT x FROM rects3rects)"
        );
        var tree = complete.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertEquals(0, complete.getNumberOfSyntaxErrors());

        SQLStandardParser unfinished = makeParser("CREATE TABLE FUNCTION dataset.qa_table_fn(x INT64) AS (SELECT x FROM rects3");
        Assertions.assertEquals(1, Trees.findAllRuleNodes(unfinished.sqlQuery(), SQLStandardParser.RULE_directSqlDataStatement).size());
    }

    @Test
    void retainsStatementKindWhileTyping() {
        assertPartialStatement("CREATE FUNCTION", SQLStandardParser.RULE_createNamedObjectStatement);
        assertPartialStatement("CREATE FUNCTION f(", SQLStandardParser.RULE_createNamedObjectStatement);
        assertPartialStatement("CREATE PROCEDURE s.p() AS BEGIN SELECT", SQLStandardParser.RULE_createNamedObjectStatement);
        assertPartialStatement("CREATE TRIGGER t BEFORE INSERT ON", SQLStandardParser.RULE_createTriggerStatement);
        assertPartialStatement("DO $body$ BEGIN SELECT", SQLStandardParser.RULE_anonymousBlockStatement);
        assertPartialStatement("EXECUTE BLOCK AS BEGIN", SQLStandardParser.RULE_anonymousBlockStatement);
    }

    @Test
    void retainsNestedBlockKindsWhileTyping() {
        SQLStandardParser parser = makeParser("DO $body$ BEGIN IF x THEN SELECT");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_anonymousBlockStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralBlock).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
    }

    @Test
    void parsesCursorAndLocalTableStatements() {
        for (GrammarCase testCase : CASES) {
            if (testCase.name().contains("declared cursor") || testCase.name().contains("local table variable") ||
                testCase.name().contains("declared session temporary table") || testCase.name().contains("temporary table inside routine") ||
                testCase.name().contains("separate cursor statements")) {
                assertAccepted(testCase.sql());
            }
        }
    }

    @Test
    void preservesProceduralDeclarationAndSqlNodes() {
        SQLStandardParser parser = makeParser("CREATE PROCEDURE p AS BEGIN DECLARE @items TABLE (id int); " +
            "DECLARE c CURSOR FOR SELECT id FROM @items; OPEN c; FETCH c INTO x; CLOSE c; SELECT id FROM @items; END");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_localRelationDeclaration).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_cursorDeclaration).size());
        Assertions.assertEquals(3, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_cursorOperation).size());
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size() >= 2);
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    void reusesVariableAssignmentRuleInsideProceduralBody() {
        SQLStandardParser parser = makeParser("CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; SELECT x; END");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_variableAssignmentStatement).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_variableAssignment).size());
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    void parsesNativeSqlServerControlFlow() {
        String sql = "CREATE OR ALTER PROC p AS BEGIN DECLARE @n int; " +
            "WHILE @n < 2 BEGIN IF @n = 0 BEGIN SELECT id FROM tab; END " +
            "ELSE BEGIN SELECT id FROM tab; END; SET @n = 1; END; SELECT id FROM tab; END";
        SQLStandardParser parser = makeParser(sql, SQLDialect.ProceduralSyntaxFamily.TSQL);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralLoop).size());
        Assertions.assertEquals(3, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
    }

    @Test
    void parsesNativeFirebirdControlFlowAndDeclarations() {
        String sql = "EXECUTE BLOCK AS DECLARE VARIABLE n INTEGER = 0; BEGIN " +
            "WHILE (n < 2) DO BEGIN IF (n = 0) THEN BEGIN SELECT id FROM tab INTO :n; END " +
            "ELSE BEGIN SELECT id FROM tab INTO :n; END; SELECT id FROM tab INTO :n; END; " +
            "SELECT id FROM tab INTO :n; END";
        SQLStandardParser parser = makeParser(sql, SQLDialect.ProceduralSyntaxFamily.FIREBIRD_PSQL);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_firebirdVariableDeclaration).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralLoop).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_firebirdSelectIntoStatement).size());
    }

    @Test
    void parsesNativeSingleStatementArmsAndIncompleteBlocks() {
        SQLStandardParser tsql = makeParser(
            "CREATE OR ALTER PROC p AS BEGIN IF 1 = 1 SELECT 1; ELSE SELECT 2; WHILE 1 < 2 SELECT 3; END",
            SQLDialect.ProceduralSyntaxFamily.TSQL
        );
        var tsqlTree = tsql.sqlQuery();
        Assertions.assertEquals(0, tsql.getNumberOfSyntaxErrors(), tsqlTree.toStringTree(tsql));
        Assertions.assertEquals(3, Trees.findAllRuleNodes(tsqlTree, SQLStandardParser.RULE_directSqlDataStatement).size());

        SQLStandardParser firebird = makeParser(
            "EXECUTE BLOCK AS DECLARE VARIABLE n INTEGER; BEGIN " +
                "IF (1 = 1) THEN SELECT id FROM tab INTO :n; ELSE SELECT id FROM tab INTO :n; END",
            SQLDialect.ProceduralSyntaxFamily.FIREBIRD_PSQL
        );
        var firebirdTree = firebird.sqlQuery();
        Assertions.assertEquals(0, firebird.getNumberOfSyntaxErrors(), firebirdTree.toStringTree(firebird));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_directSqlDataStatement).size(),
            firebirdTree.toStringTree(firebird));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_firebirdSelectIntoStatement).size());

        assertPartialNativeControlFlow("CREATE OR ALTER PROC p AS BEGIN IF @n = 1 BEGIN SELECT",
            SQLDialect.ProceduralSyntaxFamily.TSQL, SQLStandardParser.RULE_proceduralIf);
        assertPartialNativeControlFlow("EXECUTE BLOCK AS BEGIN WHILE (1 = 1) DO BEGIN SELECT",
            SQLDialect.ProceduralSyntaxFamily.FIREBIRD_PSQL, SQLStandardParser.RULE_proceduralLoop);
    }

    private static void assertPartialNativeControlFlow(
        @NotNull String sql, @NotNull SQLDialect.ProceduralSyntaxFamily family, int rule
    ) {
        SQLStandardParser parser = makeParser(sql, family);
        Assertions.assertEquals(1, Trees.findAllRuleNodes(parser.sqlQuery(), rule).size(), sql);
    }

    @Test
    void distinguishesPrefixedTemporaryTableFromHashCommentByDialect() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TABLE #scratch (id int); SELECT id FROM #scratch; END";
        LSMAnalyzerParameters parameters = new LSMAnalyzerParameters(Map.of(), false, false, '?', List.of(), false, false);
        SQLStandardLexer lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        var parser = new SQLStandardParser(new CommonTokenStream(lexer), parameters);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createTableStatement).size());
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());

        LSMAnalyzerParameters hashComments = new LSMAnalyzerParameters(Map.of(), false, false, '?', List.of(), false, true);
        var comment = new SQLStandardLexer(CharStreams.fromString("#scratch"), hashComments).nextToken();
        Assertions.assertEquals(Token.HIDDEN_CHANNEL, comment.getChannel());
    }

    private static void assertPartialStatement(@NotNull String sql, int statementRule) {
        SQLStandardParser parser = makeParser(sql);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, statementRule).size(), sql);
    }

    @NotNull
    private static SQLStandardParser makeParser(@NotNull String sql) {
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();
        var parser = new SQLStandardParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        return parser;
    }

    @NotNull
    private static SQLStandardParser makeParser(@NotNull String sql, @NotNull SQLDialect.ProceduralSyntaxFamily family) {
        LSMAnalyzerParameters parameters = new LSMAnalyzerParameters(Map.of(), false, false, '?', List.of(), false, false, family);
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        lexer.removeErrorListeners();
        var parser = new SQLStandardParser(new CommonTokenStream(lexer), parameters);
        parser.removeErrorListeners();
        return parser;
    }

    private static void assertAccepted(@NotNull String sql) {
        var lexerErrors = new SyntaxErrorCollector();
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrors);

        var tokens = new CommonTokenStream(lexer);
        tokens.fill();

        var parserErrors = new SyntaxErrorCollector();
        var parser = new SQLStandardParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrors);
        var tree = parser.sqlQueries();

        Assertions.assertAll(
            () -> Assertions.assertTrue(lexerErrors.isEmpty(), () -> "Lexer errors:\n" + lexerErrors.describe()),
            () -> Assertions.assertTrue(parserErrors.isEmpty(),
                () -> "Parser errors:\n" + parserErrors.describe() + "\n" + tree.toStringTree(parser))
        );
    }

    private record GrammarCase(@NotNull String name, @NotNull String sql) {
    }

    private static final class SyntaxErrorCollector extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String message,
            @Nullable RecognitionException exception
        ) {
            errors.add("line " + line + ":" + charPositionInLine + " " + message);
        }

        boolean isEmpty() {
            return errors.isEmpty();
        }

        @NotNull
        String describe() {
            return String.join("\n", errors);
        }
    }
}
