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
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryCallModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQuerySelectIntoModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineReturn;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryTriggerCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryEmbeddedStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementSequence;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreSqlProceduralTest extends ProceduralDialectTestBase {
    @Test
    void nestedDeclarationSectionsKeepDefaultsAndLexicalBindings() throws DBException {
        String sql = "CREATE PROCEDURE p() LANGUAGE plpgsql AS $body$ " +
            "DECLARE outer_value numeric DEFAULT 1; BEGIN " +
            "DECLARE inner_value numeric DEFAULT outer_value; BEGIN " +
            "SELECT outer_value + inner_value; END; SELECT outer_value; END; $body$";
        SQLQueryModel model = recognize(sql);
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 2);
        assertBound(model, "outer_value", 0, 1);
        assertBound(model, "outer_value", 0, 2);
        assertBound(model, "outer_value", 0, 3);
        assertBound(model, "inner_value", 0, 1);
        assertProposes("CREATE PROCEDURE p() LANGUAGE plpgsql AS $body$ " +
            "DECLARE outer_value numeric DEFAULT 1; BEGIN IF TRUE THEN SELECT outer_|", "outer_value");
    }

    @Test
    void typedDeclarationsBeforeBeginAreRecognizedAndResolved() {
        SQLQueryModel model = recognize("CREATE PROCEDURE p() AS first_value numeric DEFAULT 1; " +
            "second_value numeric DEFAULT first_value; BEGIN SELECT first_value + second_value; END");
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 1);
        assertBound(model, "first_value", 0, 1);
        assertBound(model, "first_value", 0, 2);
        assertBound(model, "second_value", 0, 1);
    }

    @Test
    void untypedDeclarationKeepsDefaultExpressionAndFollowingSql() {
        SQLQueryModel model = recognize("CREATE PROCEDURE p AS BEGIN DECLARE untyped_value DEFAULT 1; " +
            "SELECT untyped_value; END");
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 1);
        assertBound(model, "untyped_value", 0, 1);
    }

    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("postgresql");
    }

    @Test
    void selectIntoTargetsDependOnProceduralContext() {
        String topLevelSql = "SELECT id INTO new_table FROM tab";
        var topLevel = recognize(topLevelSql);
        root(topLevel, SQLQuerySelectIntoModel.class);
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.RowsSourceRef.class, topLevel.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("new_table"))
            .findFirst().orElseThrow().getOrigin());

        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE result_id int; " +
            "SELECT id INTO result_id FROM tab; END";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_selectStatementSingleRow);
        var model = recognize(sql);
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 1);
        assertBound(model, "result_id", 0, 1);

        String trailingSql = "CREATE PROCEDURE p AS BEGIN DECLARE result_id int; " +
            "SELECT id FROM tab INTO result_id; END";
        assertStructuredControlFlow(trailingSql, SQLStandardParser.RULE_selectIntoClause);
        assertBound(recognize(trailingSql), "result_id", 0, 1);
    }

    @Test
    void topLevelTrailingIntoIsNotASelectIntoClause() {
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect(), DBWorkbench.getPlatform().getPreferenceStore());
        LSMAnalyzerParameters params = LSMAnalyzerParameters.forDialect(dialect(), syntax);
        var parser = new SQLStandardParser(new CommonTokenStream(
            new SQLStandardLexer(CharStreams.fromString("SELECT id FROM tab INTO new_table"), params)), params);
        var tree = parser.sqlQuery();
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_selectIntoClause).isEmpty(),
            tree.toStringTree(parser));
    }

    /**
     * <ul>
     * <li>The tagged function yields four {@code Dollar} tokens.</li>
     * <li>The positional-parameter input has a {@code CustomNamedParameterPrefix} token.</li>
     * <li>The single-query function has a routine-creation root model.</li>
     * <li>That routine has one argument.</li>
     * <li>That routine has one embedded SQL statement with a nonnull SQL model.</li>
     * <li>In the two-query function, references to {@code x} bind to its declaration:
     * <ul>
     * <li>At the first {@code SELECT x}.</li>
     * <li>At the second {@code SELECT x}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void taggedFunctionBodyResolvesParameterAcrossNestedSql() {
        String sql = "CREATE FUNCTION f(x int) RETURNS int AS $body$ BEGIN SELECT x; SELECT x; END; $body$ LANGUAGE sql";
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect(), DBWorkbench.getPlatform().getPreferenceStore());
        var tokens = new SQLStandardLexer(CharStreams.fromString(sql), LSMAnalyzerParameters.forDialect(dialect(), syntax)).getAllTokens();
        Assertions.assertEquals(4, tokens.stream().filter(t -> t.getType() == SQLStandardLexer.Dollar).count(), tokens.toString());
        var positional = new SQLStandardLexer(CharStreams.fromString("SELECT $1"),
            LSMAnalyzerParameters.forDialect(dialect(), syntax)).getAllTokens();
        Assertions.assertTrue(positional.stream().anyMatch(t -> t.getType() == SQLStandardLexer.CustomNamedParameterPrefix),
            positional.toString());
        SQLQueryModel model = recognize(sql);
        SQLQueryRoutineCreateModel routine = root(
            "CREATE FUNCTION f(x int) RETURNS int AS $body$ BEGIN SELECT x; END; $body$ LANGUAGE sql",
            SQLQueryRoutineCreateModel.class
        );
        Assertions.assertEquals(1, routine.getArguments().size(),
            () -> "Body: " + routine.getBody() + "; syntax: " + routine.getSyntaxNode().getTextContent());
        assertEmbeddedSqlCount(routine, 1);
        assertBound(model, "x", 0, 1);
        assertBound(model, "x", 0, 2);
    }

    @Test
    void tableResultColumnsAreVisibleAsOutputParameters() {
        String sql = "CREATE FUNCTION f() RETURNS TABLE (out_value int) " +
            "AS $body$ BEGIN SELECT out_value; END; $body$ LANGUAGE plpgsql";
        SQLQueryModel model = recognize(sql);
        var routine = root(model, SQLQueryRoutineCreateModel.class);
        Assertions.assertInstanceOf(SQLQueryRoutineReturn.TableColumns.class, routine.getReturnSpec());
        assertBound(model, "out_value", 0, 1);
    }

    /**
     * <ul>
     * <li>References to the declared refcursor bind:
     * <ul>
     * <li>At {@code OPEN c}.</li>
     * <li>At {@code SELECT c}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void refcursorCanBeUsedAsCursorAndValue() {
        String sql = "CREATE FUNCTION f() RETURNS int AS $body$ DECLARE c refcursor; " +
            "BEGIN OPEN c; SELECT c; END; $body$ LANGUAGE plpgsql";
        SQLQueryModel model = recognize(sql);
        assertBound(model, "c", 0, 1);
        assertBound(model, "c", 0, 2);
    }

    /**
     * <ul>
     * <li>Completion offers a temporary table's column after a nested block in the procedure.</li>
     * </ul>
     */
    @Test
    void temporaryTableSurvivesNestedBlockForLateProposal() throws DBException {
        assertProposes("CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (id int); " +
            "BEGIN SELECT id FROM scratch; END; SELECT | FROM scratch; END", "id");
    }

    @Test
    void callKeywordsUseSharedRoutineTargetType() {
        for (String prefix : new String[] {"CALL", "EXEC", "EXECUTE", "EXEC PROCEDURE", "EXECUTE PROCEDURE",
            "EXEC FUNCTION", "EXECUTE FUNCTION"}) {
            String sql = prefix + " public.target(1, 'argument')";
            assertStructuredControlFlow(sql, SQLStandardParser.RULE_callStatement);
            SQLQueryModel model = recognize(sql);
            SQLQueryCallModel call = root(model, SQLQueryCallModel.class);
            Assertions.assertNotNull(call.getObject(), sql);
            Assertions.assertEquals(RelationalObjectType.TYPE_PROCEDURE, call.getObject().getObjectType(), sql);
            Assertions.assertEquals(2, call.getExpressions().size(), sql);
        }
    }

    @Test
    void triggerExecutionUsesStructuredCallBeforeFallback() {
        for (String kind : new String[] {"FUNCTION", "PROCEDURE"}) {
            String sql = "CREATE TRIGGER t AFTER INSERT ON source FOR EACH ROW EXECUTE " + kind + " public.target('argument')";
            assertStructuredControlFlow(sql, SQLStandardParser.RULE_callStatement);
            SQLQueryModel model = recognize(sql);
            SQLQueryTriggerCreateModel trigger = root(model, SQLQueryTriggerCreateModel.class);
            SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, trigger.getBody());
            Assertions.assertEquals(1, body.getStatements().size());
            SQLQueryEmbeddedStatement embedded = Assertions.assertInstanceOf(SQLQueryEmbeddedStatement.class, body.getStatements().getFirst());
            SQLQueryCallModel call = Assertions.assertInstanceOf(SQLQueryCallModel.class, embedded.getSql());
            Assertions.assertNotNull(call.getObject(), sql);
            Assertions.assertEquals(RelationalObjectType.TYPE_PROCEDURE, call.getObject().getObjectType(), sql);
            Assertions.assertEquals(1, call.getExpressions().size());
        }
    }

    @Test
    void procedureCallInsideTriggerBlockRemainsProcedure() {
        String sql = "CREATE TRIGGER t AFTER INSERT ON source FOR EACH ROW " +
            "AS BEGIN EXECUTE PROCEDURE public.target(1); END";
        SQLQueryModel model = recognize(sql);
        root(model, SQLQueryTriggerCreateModel.class);
        SQLQueryCallModel call = statementAt(model, sql, "EXECUTE PROCEDURE", SQLQueryCallModel.class);
        Assertions.assertNotNull(call.getObject());
        Assertions.assertEquals(RelationalObjectType.TYPE_PROCEDURE, call.getObject().getObjectType());
        Assertions.assertEquals(1, call.getExpressions().size());
    }

    @Test
    void callArgumentsResolveValuesConditionsAndSubqueries() {
        String sql = "CREATE FUNCTION f(x int) RETURNS int AS $body$ BEGIN "
            + "CALL public.target(x + 1, NOT x > 0, (SELECT x)); END; $body$ LANGUAGE plpgsql";
        SQLQueryModel model = recognize(sql);
        assertBound(model, "x", 0, 1);
        assertBound(model, "x", 0, 2);
        assertBound(model, "x", 0, 3);
        SQLQueryCallModel call = statementAt(model, sql, "CALL", SQLQueryCallModel.class);
        Assertions.assertEquals(3, call.getExpressions().size());
    }

}
