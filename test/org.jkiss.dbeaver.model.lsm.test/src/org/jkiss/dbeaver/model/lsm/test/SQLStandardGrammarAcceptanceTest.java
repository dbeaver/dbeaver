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
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void parsesNestedPostgresProcedure() {
        String sql = """
            CREATE OR REPLACE PROCEDURE demo_completion_pg()
            LANGUAGE plpgsql
            AS $yyyy$
            DECLARE
                base_amount numeric DEFAULT 1;
            BEGIN
                CREATE TEMP TABLE demo_scope_items (id integer, amount numeric) ON COMMIT DROP;
                CREATE TEMP TABLE demo_scope_log (id integer, amount numeric, stage text) ON COMMIT DROP;
                INSERT INTO demo_scope_items (id, amount) VALUES (1, 12.50), (2, 25.00);

                -- The FOR query supplies a block-local record without an explicit assignment.
                FOR item IN SELECT s.id, s.amount FROM demo_scope_items AS s LOOP
                    IF item.id = 1 THEN
                        DECLARE
                            branch_bonus numeric DEFAULT 5;
                        BEGIN
                            INSERT INTO demo_scope_log (id, amount, stage)
                            SELECT s.id, s.amount + base_amount + branch_bonus, 'branch'
                              FROM demo_scope_items AS s WHERE s.id = item.id;

                            LOOP
                                DECLARE
                                    loop_bonus numeric DEFAULT 2;
                                BEGIN
                                    INSERT INTO demo_scope_log (id, amount, stage)
                                    SELECT s.id, s.amount + branch_bonus + loop_bonus, 'inner loop'
                                      FROM demo_scope_items AS s WHERE s.id = item.id;
                                    EXIT; -- One inner iteration is sufficient for the demo.
                                END;
                            END LOOP;
                        END;
                    ELSE
                        -- branch_bonus and loop_bonus are not visible in this sibling arm.
                        INSERT INTO demo_scope_log (id, amount, stage)
                        SELECT s.id, s.amount + base_amount, 'other branch'
                          FROM demo_scope_items AS s WHERE s.id = item.id;
                    END IF;

                    -- Sequenced after the IF: only outer locals and the FOR record remain.
                    INSERT INTO demo_scope_log (id, amount, stage)
                    SELECT s.id, s.amount + base_amount, 'after if'
                      FROM demo_scope_items AS s WHERE s.id = item.id;
                END LOOP;

                -- Another nested declaration is visible only in this final block.
                DECLARE
                    final_bonus numeric DEFAULT 3;
                BEGIN
                    INSERT INTO demo_scope_log (id, amount, stage)
                    SELECT s.id, s.amount + base_amount + final_bonus, 'final block'
                      FROM demo_scope_items AS s;
                END;

                RAISE NOTICE 'demo complete; temporary tables disappear at commit';
            END;
            $yyyy$;
            """;
        SQLStandardParser parser = makeParser(sql);
        parser.setProfile(true);
        var tree = parser.sqlQueries();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineDefinition).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralLoop).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralBlock).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralDeclaration).size());
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createTableStatement).size());
        Assertions.assertEquals(6, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_insertStatement).size());
        // Assert prediction work, not wall-clock time: this used to look through nearly the entire 533-token input.
        long maximumLookahead = Arrays.stream(parser.getParseInfo().getDecisionInfo())
            .filter(info -> {
                int rule = parser.getATN().decisionToState.get(info.decision).ruleIndex;
                return rule == SQLStandardParser.RULE_proceduralStatements ||
                    rule == SQLStandardParser.RULE_proceduralStatement || rule == SQLStandardParser.RULE_proceduralCondition;
            })
            .mapToLong(info -> info.LL_MaxLook).max().orElse(0);
        Assertions.assertTrue(maximumLookahead < 64, () -> "Procedural lookahead: " + maximumLookahead);
    }

    @Test
    void keepsTypedAndUntypedDeclarationInitializersInTheirOwnNodes() {
        SQLStandardParser parser = makeParser("CREATE PROCEDURE p AS BEGIN " +
            "DECLARE typed_value numeric DEFAULT 1; DECLARE untyped_value DEFAULT 2; " +
            "SELECT typed_value + untyped_value; END");
        var tree = parser.sqlQueries();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_localVariableDeclaration).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_localTypedVariable).size());
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_valueExpression).stream()
            .filter(value -> value.getParent() instanceof SQLStandardParser.LocalTypedVariableContext ||
                value.getParent() instanceof SQLStandardParser.LocalVariableDeclarationContext).count());
    }

    @Test
    void separatesRoutineParametersResultsAndPostBodyExtensions() {
        SQLStandardParser parser = makeParser(
            "CREATE FUNCTION f(x int) RETURNS TABLE (result int, text) AS $body$ SELECT x; $body$ LANGUAGE sql");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        var definition = (SQLStandardParser.RoutineDefinitionContext) Trees.findAllRuleNodes(
            tree, SQLStandardParser.RULE_routineDefinition).iterator().next();
        Assertions.assertEquals(1, definition.routineSignature().routineArgument().size());
        Assertions.assertNotNull(definition.routinePreamble().routineReturnClause());
        Assertions.assertEquals(2, definition.routinePreamble().routineReturnClause()
            .routineResultList().routineResultField().size());
        Assertions.assertNotNull(definition.routineTrailing().routineBody());
        Assertions.assertEquals(1, definition.routineTrailing().routineDefinitionExtension().size());

        SQLStandardParser firebird = makeParser(
            "CREATE PROCEDURE p(x int) RETURNS (result int) AS BEGIN SELECT result; END",
            SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL);
        var firebirdTree = firebird.sqlQuery();
        Assertions.assertEquals(0, firebird.getNumberOfSyntaxErrors(), firebirdTree.toStringTree(firebird));
        var returnClauses = Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_routineReturnClause);
        Assertions.assertEquals(1, returnClauses.size());
        var output = (SQLStandardParser.RoutineReturnClauseContext) returnClauses.iterator().next();
        Assertions.assertEquals("result", output.routineResultList().routineResultField(0).identifier().getText());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_routineSignature).size());
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
        SQLStandardParser parser = makeParser(sql, SQLDialectFeatures.ProceduralSyntaxFamily.TSQL);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralLoop).size());
        Assertions.assertEquals(3, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
    }

    @Test
    void parsesIndividuallyTypedLocalVariablesAndSharedTypeDeclarations() {
        SQLStandardParser tsql = makeParser(
            "CREATE OR ALTER PROC p AS BEGIN DECLARE @first int = 1, @second varchar(20) = @first; " +
                "SELECT @second; END", SQLDialectFeatures.ProceduralSyntaxFamily.TSQL);
        var tsqlTree = tsql.sqlQuery();
        Assertions.assertEquals(0, tsql.getNumberOfSyntaxErrors(), tsqlTree.toStringTree(tsql));
        Assertions.assertEquals(Token.EOF, tsql.getInputStream().LA(1));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(tsqlTree, SQLStandardParser.RULE_localTypedVariable).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tsqlTree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertTrue(Trees.findAllRuleNodes(tsqlTree, SQLStandardParser.RULE_proceduralStatementTokens).isEmpty());

        SQLStandardParser shared = makeParser("CREATE PROCEDURE p() BEGIN " +
            "DECLARE first_value, second_value int DEFAULT 0; SELECT second_value; END");
        var sharedTree = shared.sqlQuery();
        Assertions.assertEquals(0, shared.getNumberOfSyntaxErrors(), sharedTree.toStringTree(shared));
        Assertions.assertEquals(Token.EOF, shared.getInputStream().LA(1));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(sharedTree, SQLStandardParser.RULE_localVariableDeclaration).size());
        Assertions.assertTrue(Trees.findAllRuleNodes(sharedTree, SQLStandardParser.RULE_localTypedVariable).isEmpty());
    }

    @Test
    void parsesNativeFirebirdControlFlowAndDeclarations() {
        String sql = "EXECUTE BLOCK AS DECLARE VARIABLE n INTEGER = 0; BEGIN " +
            "WHILE (n < 2) DO BEGIN IF (n = 0) THEN BEGIN SELECT id FROM tab INTO :n; END " +
            "ELSE BEGIN SELECT id FROM tab INTO :n; END; SELECT id FROM tab INTO :n; END; " +
            "SELECT id FROM tab INTO :n; END";
        SQLStandardParser parser = makeParser(sql, SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_firebirdBlock).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_localVariableDeclaration).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralIf).size());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralLoop).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_directSqlDataStatement).size());
        Assertions.assertEquals(4, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_selectStatement).size(),
            tree.toStringTree(parser));
    }

    @Test
    void parsesFirebirdSelectIntoTarget() {
        SQLStandardParser parser = makeParser("EXECUTE BLOCK AS BEGIN SELECT id FROM tab INTO :n; END",
            SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_selectStatement).size());
    }

    @Test
    void trailingIntoIsNotASelectClauseOutsideEnabledDialects() {
        for (SQLDialectFeatures.ProceduralSyntaxFamily family : List.of(
            SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC, SQLDialectFeatures.ProceduralSyntaxFamily.TSQL)) {
            SQLStandardParser parser = makeParser("SELECT id FROM tab INTO target", family);
            var tree = parser.sqlQuery();
            Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_selectIntoClause).isEmpty(),
                tree.toStringTree(parser));
        }
        SQLStandardParser firebird = makeParser("SELECT id FROM tab INTO :n",
            SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL);
        var firebirdTree = firebird.sqlQuery();
        Assertions.assertTrue(Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_selectIntoClause).isEmpty(),
            firebirdTree.toStringTree(firebird));
    }

    @Test
    void parsesNativeSingleStatementArmsAndIncompleteBlocks() {
        SQLStandardParser tsql = makeParser(
            "CREATE OR ALTER PROC p AS BEGIN IF 1 = 1 SELECT 1; ELSE SELECT 2; WHILE 1 < 2 SELECT 3; END",
            SQLDialectFeatures.ProceduralSyntaxFamily.TSQL
        );
        var tsqlTree = tsql.sqlQuery();
        Assertions.assertEquals(0, tsql.getNumberOfSyntaxErrors(), tsqlTree.toStringTree(tsql));
        Assertions.assertEquals(3, Trees.findAllRuleNodes(tsqlTree, SQLStandardParser.RULE_directSqlDataStatement).size());

        SQLStandardParser firebird = makeParser(
            "EXECUTE BLOCK AS DECLARE VARIABLE n INTEGER; BEGIN " +
                "IF (1 = 1) THEN SELECT id FROM tab INTO :n; ELSE SELECT id FROM tab INTO :n; END",
            SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL
        );
        var firebirdTree = firebird.sqlQuery();
        Assertions.assertEquals(0, firebird.getNumberOfSyntaxErrors(), firebirdTree.toStringTree(firebird));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_directSqlDataStatement).size(),
            firebirdTree.toStringTree(firebird));
        Assertions.assertEquals(2, Trees.findAllRuleNodes(firebirdTree, SQLStandardParser.RULE_selectStatement).size());

        assertPartialNativeControlFlow("CREATE OR ALTER PROC p AS BEGIN IF @n = 1 BEGIN SELECT",
            SQLDialectFeatures.ProceduralSyntaxFamily.TSQL, SQLStandardParser.RULE_proceduralIf);
        assertPartialNativeControlFlow("EXECUTE BLOCK AS BEGIN WHILE (1 = 1) DO BEGIN SELECT",
            SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL, SQLStandardParser.RULE_proceduralLoop);
    }

    @Test
    void retainsPartialNumericTypeArgumentsWhileTyping() {
        for (String type : List.of("numeric(", "numeric(10,")) {
            SQLStandardParser parser = makeParser("CREATE FUNCTION f(amount " + type);
            var tree = parser.sqlQuery();
            Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
            Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
            Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineArgument).size());
            var types = Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_dataType);
            Assertions.assertEquals(1, types.size(), tree.toStringTree(parser));
            Assertions.assertEquals(type, types.iterator().next().getText());
            Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_typeArguments).size());
        }
    }

    @TestFactory
    @NotNull
    Stream<DynamicTest> keepsTwoRoutineParametersSeparateFromTypeArgumentsAndModifiers() {
        return Stream.concat(
            Stream.of("numeric(10,2)", "app.amount_type", "double precision", "character varying(20)",
                "LARGE OBJECT", "int(10) UNSIGNED")
                .flatMap(type -> Stream.of("IN first_value " + type + ", OUT second_value int", type + ", int")
                    .map(parameters -> Map.entry(type, parameters))),
            Stream.of(
                Map.entry("double precision", "IN double precision, int"),
                Map.entry("LONG RAW", "LONG RAW, int"),
                Map.entry("timestamp without time zone", "value timestamp without time zone, int")
            )
        ).map(testCase -> {
            String type = testCase.getKey();
            String parameters = testCase.getValue();
            return DynamicTest.dynamicTest(parameters, () -> {
                SQLStandardParser parser = type.endsWith("UNSIGNED")
                    ? makeParserWithTypes("(" + parameters + ")", Set.of("UNSIGNED"), List.of("INT"))
                    : makeParserWithTypes("(" + parameters + ")", Set.of(),
                        List.of("DOUBLE PRECISION", "LONG RAW", "LARGE OBJECT", "DOUBLE", "LONG", "LARGE"));
                var signature = parser.routineSignature();
                Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), signature.toStringTree(parser));
                Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), parameters);
                Assertions.assertEquals(2, signature.routineArgument().size(), signature.toStringTree(parser));
                Assertions.assertEquals(type.replace(" ", "").replace("UNSIGNED", ""),
                    signature.routineArgument(0).dataType().getText(), parameters);
                Assertions.assertEquals("int", signature.routineArgument(1).dataType().getText(), parameters);
                Assertions.assertEquals(type.endsWith("UNSIGNED") ? 1 : 0,
                    signature.routineArgument(0).typeModifier().size(), parameters);
            });
        });
    }

    @Test
    void distinguishesKnownMultiwordTypesFromNamedArguments() {
        for (String parameters : List.of("(double precision, result text)", "(LONG RAW, amount RAW)")) {
            SQLStandardParser parser = makeParserWithTypes(parameters, Set.of(),
                List.of("DOUBLE PRECISION", "DOUBLE", "LONG RAW", "LONG", "RAW", "TEXT"));
            var signature = parser.routineSignature();
            Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), signature.toStringTree(parser));
            Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
            Assertions.assertEquals(2, signature.routineArgument().size(), signature.toStringTree(parser));
            Assertions.assertNull(signature.routineArgument(0).identifier(), parameters);
            Assertions.assertNotNull(signature.routineArgument(1).identifier(), parameters);
            Assertions.assertEquals(1, Trees.findAllRuleNodes(signature.routineArgument(0).dataType(),
                SQLStandardParser.RULE_typeModifier).size(), parameters);
        }
    }

    @Test
    void acceptsModesAfterOracleParameterNamesWithoutConfusingTypeOnlyArguments() {
        SQLStandardParser parser = makeParserWithModifiers(
            "CREATE PROCEDURE p(p_value IN OUT NOCOPY NUMBER, p_other OUT VARCHAR2, IN double precision)",
            Set.of(), Set.of("IN", "OUT", "NOCOPY"),
            List.of("NUMBER", "VARCHAR2", "DOUBLE", "DOUBLE PRECISION"));
        var tree = parser.sqlQueries();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), tree.toStringTree(parser));
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createNamedObjectStatement).size());
        var arguments = Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineArgument).stream()
            .map(node -> (SQLStandardParser.RoutineArgumentContext) node).toList();
        Assertions.assertEquals(3, arguments.size(), tree.toStringTree(parser));
        Assertions.assertEquals("p_value", arguments.get(0).identifier().getText());
        Assertions.assertEquals("INOUTNOCOPY", arguments.get(0).signatureModifier().stream()
            .map(node -> node.getText()).collect(java.util.stream.Collectors.joining()));
        Assertions.assertEquals("NUMBER", arguments.get(0).dataType().getText());
        Assertions.assertEquals("p_other", arguments.get(1).identifier().getText());
        Assertions.assertEquals("OUT", arguments.get(1).signatureModifier(0).getText());
        Assertions.assertEquals("VARCHAR2", arguments.get(1).dataType().getText());
        Assertions.assertNull(arguments.get(2).identifier());
        Assertions.assertEquals("IN", arguments.get(2).signatureModifier(0).getText());
        Assertions.assertEquals("doubleprecision", arguments.get(2).dataType().getText());

        SQLStandardParser incomplete = makeParserWithModifiers("p_value OUT)", Set.of(),
            Set.of("IN", "OUT", "NOCOPY"), List.of("NUMBER"));
        var unfinished = incomplete.routineArgument();
        Assertions.assertNull(unfinished.identifier(), unfinished.toStringTree(incomplete));
        Assertions.assertEquals("OUT", incomplete.getInputStream().LT(1).getText());
    }

    @Test
    void acceptsSnowflakeModesAfterParameterNames() {
        SQLStandardParser parser = makeParserWithModifiers("(arg INPUT NUMBER, result OUTPUT VARCHAR2)",
            Set.of(), Set.of("INPUT", "OUTPUT"), List.of("NUMBER", "VARCHAR2"));
        var signature = parser.routineSignature();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), signature.toStringTree(parser));
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
        Assertions.assertEquals(2, signature.routineArgument().size());
        Assertions.assertEquals("arg", signature.routineArgument(0).identifier().getText());
        Assertions.assertEquals("INPUT", signature.routineArgument(0).signatureModifier(0).getText());
        Assertions.assertEquals("NUMBER", signature.routineArgument(0).dataType().getText());
        Assertions.assertEquals("result", signature.routineArgument(1).identifier().getText());
        Assertions.assertEquals("OUTPUT", signature.routineArgument(1).signatureModifier(0).getText());
    }

    @Test
    void preservesQualifiedAndQuotedArgumentNamesAndModifierOrder() {
        SQLStandardParser parser = makeParserWithTypes(
            "(\"result\" app.amount_type, amount CHARACTER VARYING(20), value INT(10) UNSIGNED)",
            Set.of("UNSIGNED"), List.of("INT", "CHARACTER VARYING", "CHARACTER"));
        var signature = parser.routineSignature();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), signature.toStringTree(parser));
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
        Assertions.assertEquals(3, signature.routineArgument().size());
        Assertions.assertEquals("\"result\"", signature.routineArgument(0).identifier().getText());
        Assertions.assertEquals("app.amount_type", signature.routineArgument(0).dataType().getText());
        Assertions.assertEquals("CHARACTERVARYING(20)", signature.routineArgument(1).dataType().getText());
        Assertions.assertEquals("INT(10)", signature.routineArgument(2).dataType().getText());
        Assertions.assertEquals("UNSIGNED", signature.routineArgument(2).typeModifier(0).getText());

        SQLStandardParser inline = makeParserWithTypes("(INT UNSIGNED, result TEXT)",
            Set.of("UNSIGNED"), List.of("INT", "TEXT"));
        var inlineSignature = inline.routineSignature();
        Assertions.assertEquals(0, inline.getNumberOfSyntaxErrors(), inlineSignature.toStringTree(inline));
        Assertions.assertEquals(Token.EOF, inline.getInputStream().LA(1));
        Assertions.assertEquals(2, inlineSignature.routineArgument().size());
        Assertions.assertNull(inlineSignature.routineArgument(0).identifier());
        Assertions.assertEquals("INTUNSIGNED", inlineSignature.routineArgument(0).dataType().getText());
        Assertions.assertEquals(1, Trees.findAllRuleNodes(inlineSignature.routineArgument(0).dataType(),
            SQLStandardParser.RULE_typeModifier).size());
    }

    @TestFactory
    @NotNull
    Stream<DynamicTest> keepsNamedTupleFieldsInsideTheFirstParameterType() {
        return Stream.of(
            "Tuple(id Int32, label String)",
            "Tuple(payload Tuple(x Int32, y String), amount Decimal(10,2))",
            "app.Tuple(id Int32, label String)",
            "\"Tuple\"(\"id\" Int32, \"label\" String)"
        ).map(type -> DynamicTest.dynamicTest(type, () -> {
            String parameters = "(item " + type + ", next_value int)";
            SQLStandardParser parser = makeParser(parameters);
            var signature = parser.routineSignature();
            Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), signature.toStringTree(parser));
            Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), parameters);
            Assertions.assertEquals(parameters.replace(" ", ""), signature.getText(), signature.toStringTree(parser));
            Assertions.assertEquals(2, signature.routineArgument().size(), signature.toStringTree(parser));
            Assertions.assertEquals(type.replace(" ", ""), signature.routineArgument(0).dataType().getText(), parameters);
            Assertions.assertEquals("int", signature.routineArgument(1).dataType().getText(), parameters);
            Assertions.assertFalse(Trees.findAllRuleNodes(signature.routineArgument(0).dataType(),
                SQLStandardParser.RULE_typeArguments).isEmpty(), parameters);
        }));
    }

    @Test
    void retainsUnfinishedNamedTupleAsATypeWhileTyping() {
        SQLStandardParser parser = makeParser("CREATE FUNCTION f(item Tuple(id Int32,");
        var tree = parser.sqlQuery();
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineArgument).size());
        var types = Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_dataType);
        Assertions.assertFalse(types.isEmpty(), tree.toStringTree(parser));
        Assertions.assertTrue(types.iterator().next().getText().startsWith("Tuple("), tree.toStringTree(parser));
        Assertions.assertEquals(1, Trees.findAllRuleNodes(types.iterator().next(), SQLStandardParser.RULE_typeArguments).size());
    }

    private static void assertPartialNativeControlFlow(
        @NotNull String sql, @NotNull SQLDialectFeatures.ProceduralSyntaxFamily family, int rule
    ) {
        SQLStandardParser parser = makeParser(sql, family);
        Assertions.assertEquals(1, Trees.findAllRuleNodes(parser.sqlQuery(), rule).size(), sql);
    }

    @Test
    void distinguishesPrefixedTemporaryTableFromHashCommentByDialect() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TABLE #scratch (id int); SELECT id FROM #scratch; END";
        LSMAnalyzerParameters parameters = createParameters(SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC, false);
        SQLStandardLexer lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        var parser = new SQLStandardParser(new CommonTokenStream(lexer), parameters);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_createTableStatement).size());
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors());

        LSMAnalyzerParameters hashComments = createParameters(SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC, true);
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
        return makeParser(sql, SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC);
    }

    @NotNull
    private static SQLStandardParser makeParserWithTypes(
        @NotNull String sql, @NotNull Set<String> extraModifiers, @NotNull Collection<String> knownTypes
    ) {
        return makeParserWithModifiers(sql, extraModifiers, Set.of("IN", "OUT"), knownTypes);
    }

    @NotNull
    private static SQLStandardParser makeParserWithModifiers(
        @NotNull String sql, @NotNull Set<String> extraTypeModifiers,
        @NotNull Set<String> signatureModifiers, @NotNull Collection<String> knownTypes
    ) {
        var dialect = new BasicSQLDialect() {
            @Override
            public Collection<String> getDataTypes(@Nullable org.jkiss.dbeaver.model.DBPDataSource dataSource) {
                return knownTypes;
            }

            @Override
            public SQLDialectFeatures getDialectFeatures() {
                return new SQLDialectFeatures() {
                    @Override
                    public Set<String> getTypeModifierKeywords() {
                        var modifiers = new java.util.HashSet<>(SQLDialectFeatures.Default.getInstance().getTypeModifierKeywords());
                        modifiers.addAll(extraTypeModifiers);
                        return Set.copyOf(modifiers);
                    }

                    @Override
                    public Set<String> getSignatureModifierKeywords() {
                        return signatureModifiers;
                    }
                };
            }
        };
        return makeParser(sql, createParameters(dialect));
    }

    @NotNull
    private static SQLStandardParser makeParser(@NotNull String sql, @NotNull SQLDialectFeatures.ProceduralSyntaxFamily family) {
        return makeParser(sql, createParameters(family, false));
    }

    private static SQLStandardParser makeParser(@NotNull String sql, @NotNull LSMAnalyzerParameters parameters) {
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        lexer.removeErrorListeners();
        var parser = new SQLStandardParser(new CommonTokenStream(lexer), parameters);
        parser.removeErrorListeners();
        return parser;
    }

    @NotNull
    private static LSMAnalyzerParameters createParameters(
        @NotNull SQLDialectFeatures.ProceduralSyntaxFamily family, boolean hashComments
    ) {
        var dialect = new BasicSQLDialect() {
            @NotNull
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return new String[0][];
            }

            @NotNull
            @Override
            public String[] getSingleLineComments() {
                return hashComments ? new String[]{"#"} : new String[0];
            }

            @NotNull
            @Override
            public SQLDialectFeatures getDialectFeatures() {
                return new SQLDialectFeatures() {
                    @NotNull
                    @Override
                    public SQLDialectFeatures.ProceduralSyntaxFamily getProceduralSyntaxFamily() {
                        return family;
                    }

                    @NotNull
                    @Override
                    public SQLDialectFeatures.TrailingSelectIntoSupport getTrailingSelectIntoSupport() {
                        return family == SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL
                            ? SQLDialectFeatures.TrailingSelectIntoSupport.PROCEDURAL : SQLDialectFeatures.TrailingSelectIntoSupport.NONE;
                    }
                };
            }
        };
        return createParameters(dialect);
    }

    private static LSMAnalyzerParameters createParameters(@NotNull SQLDialect dialect) {
        var preferences = new SimplePreferenceStore() {
            @Override
            public void save() {
                // In-memory test preferences.
            }
        };
        var syntaxManager = new SQLSyntaxManager(preferences);
        syntaxManager.init(dialect, preferences);
        return LSMAnalyzerParameters.forDialect(dialect, syntaxManager);
    }

    private static void assertAccepted(@NotNull String sql) {
        var lexerErrors = new SyntaxErrorCollector();
        LSMAnalyzerParameters parameters = createParameters(SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC, false);
        var lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrors);

        var tokens = new CommonTokenStream(lexer);
        tokens.fill();

        var parserErrors = new SyntaxErrorCollector();
        var parser = new SQLStandardParser(tokens, parameters);
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
