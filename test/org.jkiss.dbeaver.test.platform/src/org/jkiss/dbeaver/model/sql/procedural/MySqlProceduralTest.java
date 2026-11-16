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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQuerySelectIntoResultModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSetCorrespondingOperationModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTypeCastExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryVariableDeclarationStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MySqlProceduralTest extends ProceduralDialectTestBase {
    @Test
    void localVariablesTakePrecedenceOverColumns() {
        Assertions.assertEquals(SQLDialectFeatures.ProceduralVariableConflictPolicy.PREFER_VARIABLE,
            dialect().getDialectFeatures().getProceduralVariableConflictPolicy());
    }

    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("mysql");
    }

    @Test
    void selectIntoInsideProcedureBindsTargetsAndTemporaryTable() {
        String sql = "CREATE PROCEDURE p() BEGIN " +
            "DECLARE chosen_id int; DECLARE chosen_amount decimal(10, 2); " +
            "CREATE TEMPORARY TABLE demo_scope_items (id int, amount decimal(10, 2)); " +
            "SELECT s.id, s.amount INTO chosen_id, chosen_amount " +
            "FROM demo_scope_items AS s WHERE s.id = chosen_id; " +
            "SELECT chosen_amount FROM demo_scope_items; END";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_selectStatementSingleRow);
        var model = recognize(sql);
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 2);
        assertBound(model, "chosen_id", 0, 1);
        assertBound(model, "chosen_id", 0, 2);
        assertBound(model, "chosen_amount", 0, 1);
        assertBound(model, "chosen_amount", 0, 2);
        assertBound(model, "demo_scope_items", 0, 1);
        assertBound(model, "demo_scope_items", 0, 2);
    }

    @Test
    void selectIntoInsideNestedBlockUsesLexicalScope() throws DBException {
        String sql = "CREATE PROCEDURE p() BEGIN DECLARE outer_id int; " +
            "CREATE TEMPORARY TABLE scratch (id int); " +
            "IF outer_id = 1 THEN BEGIN DECLARE inner_id int; " +
            "SELECT s.id INTO inner_id FROM scratch AS s WHERE s.id = outer_id; " +
            "END; END IF; SELECT outer_id FROM scratch; END";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_selectStatementSingleRow);
        var model = recognize(sql);
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 2);
        assertBound(model, "inner_id", 0, 1);
        assertBound(model, "scratch", 0, 1);
        assertBound(model, "scratch", 0, 2);
        assertDoesNotPropose(sql.replace("SELECT outer_id FROM scratch; END", "SELECT inner_| FROM scratch; END"), "inner_id");
    }

    @Test
    void selectIntoAfterFromAndWithoutFromBindLocalTargets() {
        for (String selection : List.of("SELECT id FROM scratch INTO chosen_id", "SELECT 1 INTO chosen_id")) {
            String sql = "CREATE PROCEDURE p() BEGIN DECLARE chosen_id int; " +
                "CREATE TEMPORARY TABLE scratch (id int); " + selection + "; END";
            assertStructuredControlFlow(sql, SQLStandardParser.RULE_selectIntoClause);
            var model = recognize(sql);
            assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 1);
            assertBound(model, "chosen_id", 0, 1);
        }
    }

    @Test
    void unionIntoSessionVariableTargetsTheCombinedResult() {
        String sql = "SELECT id FROM first_tab UNION ALL SELECT id FROM second_tab INTO @result";
        var model = recognize(sql);
        var into = root(model, SQLQuerySelectIntoResultModel.class);
        Assertions.assertInstanceOf(SQLQueryRowsSetCorrespondingOperationModel.class, into.getResultQuery());
        Assertions.assertEquals(SQLQuerySymbolClass.SQL_BATCH_VARIABLE, model.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("@result"))
            .findFirst().orElseThrow().getSymbolClass());
    }

    @Test
    void prefixParameterDirectionIsSeparateFromUnsignedType() {
        String sql = "CREATE PROCEDURE p(IN p_value int UNSIGNED) BEGIN SELECT p_value; END";
        var model = recognize(sql);
        var routine = root(model, SQLQueryRoutineCreateModel.class);
        Assertions.assertEquals(1, routine.getArguments().size());
        var declarations = routine.getArguments().getFirst().declaration().getDeclarations();
        Assertions.assertEquals(1, declarations.size());
        var parameter = declarations.getFirst();
        assertEmbeddedSqlCount(routine, 1);
        assertBound(model, "p_value", 0, 1);
        var reference = statementAt(model, sql, "p_value;", SQLQueryValueExpression.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals("p_value", parameter.getName().getRawName()),
            () -> Assertions.assertTrue(parameter.isParameter()),
            () -> Assertions.assertEquals(List.of("IN"), parameter.getModifiers()),
            () -> Assertions.assertEquals("int", parameter.getTypeRefString()),
            () -> Assertions.assertEquals("int UNSIGNED", parameter.getType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), parameter.getType().getModifiers()),
            () -> Assertions.assertEquals("int UNSIGNED", reference.getValueType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), reference.getValueType().getModifiers())
        );
    }

    @Test
    void signatureModifiersAreFilteredByMySqlDialect() {
        var routine = root("CREATE PROCEDURE p(IN p_value int NOCOPY) BEGIN SELECT p_value; END",
            SQLQueryRoutineCreateModel.class);
        var parameter = routine.getArguments().getFirst().declaration().getDeclarations().getFirst();
        Assertions.assertEquals(List.of("IN"), parameter.getModifiers());
        Assertions.assertEquals(List.of(), parameter.getType().getModifiers());
    }

    @Test
    void unsignedLocalKeepsBaseTypeReferenceAndBodyType() {
        String sql = "CREATE PROCEDURE p() BEGIN DECLARE local_value int UNSIGNED; SELECT local_value; END";
        var model = recognize(sql);
        root(model, SQLQueryRoutineCreateModel.class);
        var statement = statementAt(model, sql, "DECLARE", SQLQueryVariableDeclarationStatement.class);
        Assertions.assertEquals(1, statement.getDeclarations().size());
        var local = statement.getDeclarations().getFirst();
        assertBound(model, "local_value", 0, 1);
        var reference = statementAt(model, sql, "local_value;", SQLQueryValueExpression.class);
        Assertions.assertAll(
            () -> Assertions.assertFalse(local.isParameter()),
            () -> Assertions.assertEquals(List.of(), local.getModifiers()),
            () -> Assertions.assertEquals("int", local.getTypeRefString()),
            () -> Assertions.assertEquals("int UNSIGNED", local.getType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), local.getType().getModifiers()),
            () -> Assertions.assertEquals("int UNSIGNED", reference.getValueType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), reference.getValueType().getModifiers())
        );
    }

    @Test
    void sharedUnsignedTypeIsPreservedForEveryLocal() {
        String sql = "CREATE PROCEDURE p() BEGIN DECLARE first_value, second_value int UNSIGNED DEFAULT 0; " +
            "SELECT first_value, second_value; END";
        var model = recognize(sql);
        var statement = statementAt(model, sql, "DECLARE", SQLQueryVariableDeclarationStatement.class);
        Assertions.assertEquals(List.of("first_value", "second_value"), statement.getDeclarations().stream()
            .map(declaration -> declaration.getName().getRawName()).toList());
        assertBound(model, "first_value", 0, 1);
        assertBound(model, "second_value", 0, 1);
        Assertions.assertAll(statement.getDeclarations().stream().map(local -> () -> Assertions.assertAll(
            local.getName().getRawName(),
            () -> Assertions.assertFalse(local.isParameter()),
            () -> Assertions.assertEquals(List.of(), local.getModifiers()),
            () -> Assertions.assertEquals("int", local.getTypeRefString()),
            () -> Assertions.assertEquals("int UNSIGNED", local.getType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), local.getType().getModifiers())
        )));
    }

    @Test
    void qualifiedCastPreservesUnsignedTypeModifier() {
        String sql = "SELECT CAST(1 AS app.amount_type UNSIGNED)";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_castSpecification);
        var cast = statementAt(sql, "CAST", SQLQueryValueTypeCastExpression.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals("app.amount_type", cast.getTypeRefString()),
            () -> Assertions.assertEquals("app.amount_type UNSIGNED", cast.getValueType().getDisplayName()),
            () -> Assertions.assertEquals(List.of("UNSIGNED"), cast.getValueType().getModifiers())
        );
    }

    @Test
    void multipleTypeModifiersKeepDeclarationOrderInBodyType() {
        String sql = "CREATE PROCEDURE p() BEGIN DECLARE local_value int UNSIGNED ZEROFILL; SELECT local_value; END";
        var model = recognize(sql);
        var statement = statementAt(model, sql, "DECLARE", SQLQueryVariableDeclarationStatement.class);
        var local = statement.getDeclarations().getFirst();
        var reference = statementAt(model, sql, "local_value;", SQLQueryValueExpression.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(List.of("UNSIGNED", "ZEROFILL"), local.getType().getModifiers()),
            () -> Assertions.assertEquals(List.of("UNSIGNED", "ZEROFILL"), reference.getValueType().getModifiers()),
            () -> Assertions.assertEquals("int UNSIGNED ZEROFILL", local.getType().getDisplayName()),
            () -> Assertions.assertEquals("int UNSIGNED ZEROFILL", reference.getValueType().getDisplayName())
        );
    }

    /**
     * <ul>
     * <li>A procedure containing a local declaration, assignment, and query has a routine-creation root model.</li>
     * </ul>
     */
    @Test
    void localAssignmentAndLaterSqlAreStructured() {
        root("CREATE PROCEDURE p() BEGIN DECLARE x int; SET x = 1; SELECT x; END",
            SQLQueryRoutineCreateModel.class);
    }

    @Test
    void sharedTypeLocalDeclarationBindsBothNames() throws DBException {
        String sql = "CREATE PROCEDURE p() BEGIN " +
            "DECLARE first_value, second_value int DEFAULT 0; SELECT second_value; END";
        var model = recognize(sql);
        assertEmbeddedSqlCount(root(model, SQLQueryRoutineCreateModel.class), 1);
        assertBound(model, "second_value", 0, 1);
        assertProposes(sql.replace("SELECT second_value", "SELECT second_|"), "second_value");
    }

    /**
     * <ul>
     * <li>Completion offers a temporary table's column after a nested query in the procedure.</li>
     * </ul>
     */
    @Test
    void temporaryTableColumnsAreProposedAfterNestedStatement() throws DBException {
        assertProposes("CREATE PROCEDURE p() BEGIN CREATE TEMPORARY TABLE scratch (id int); " +
            "BEGIN SELECT id FROM scratch; END; SELECT | FROM scratch; END", "id");
    }

    /**
     * <ul>
     * <li>The simple {@code IF ... ELSEIF} input parses as structured procedural control flow.</li>
     * <li>The nested-block {@code IF ... ELSEIF ... ELSE} input parses as structured procedural control flow.</li>
     * <li>The nested-block procedure has a routine-creation root model.</li>
     * <li>Its {@code IF} site has a conditional-statement model.</li>
     * <li>That conditional has {@code IF} kind.</li>
     * <li>That conditional has three branches.</li>
     * <li>The routine contains four embedded SQL statements with nonnull SQL models.</li>
     * <li>Completion at the branch and after the conditional:
     * <ul>
     * <li>Offers {@code second_value} in the second branch.</li>
     * <li>Does not offer {@code first_value} in the second branch.</li>
     * <li>Does not offer {@code first_value} after the conditional.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void elseifRetainsSeparateSqlArmsAndLexicalScopes() throws DBException {
        assertStructuredControlFlow(
            "CREATE PROCEDURE p() BEGIN IF x = 1 THEN SELECT 1; ELSEIF x = 2 THEN SELECT 2; END IF; END",
            SQLStandardParser.RULE_proceduralIf
        );
        String sql = "CREATE PROCEDURE p() BEGIN IF x = 1 THEN " +
            "BEGIN DECLARE first_value int; SELECT first_value FROM tab; END; " +
            "ELSEIF x = 2 THEN BEGIN DECLARE second_value int; SELECT second_value FROM tab; END; " +
            "ELSE SELECT 3; END IF; SELECT 4; END";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_proceduralIf);
        SQLQueryRoutineCreateModel routine = root(sql, SQLQueryRoutineCreateModel.class);
        SQLQueryConditionalStatement conditional = statementAt(sql, "IF x = 1", SQLQueryConditionalStatement.class);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF, conditional.getKind());
        Assertions.assertEquals(3, conditional.getBranches().size());
        assertEmbeddedSqlCount(routine, 4);
        assertProposes(sql.replace("SELECT second_value", "SELECT second_|"), "second_value");
        assertDoesNotPropose(sql.replace("SELECT second_value", "SELECT first_|"), "first_value");
        assertDoesNotPropose(sql.replace("SELECT 4; END", "SELECT first_|; END"), "first_value");
    }

    /**
     * <ul>
     * <li>The simple {@code REPEAT ... UNTIL} input parses as structured procedural control flow.</li>
     * <li>The nested-block {@code REPEAT ... UNTIL} input parses as structured procedural control flow.</li>
     * <li>The nested-block procedure has a routine-creation root model.</li>
     * <li>Its {@code REPEAT} site has a conditional-statement model.</li>
     * <li>That conditional has {@code REPEAT} kind.</li>
     * <li>That conditional has one branch.</li>
     * <li>The routine contains three embedded SQL statements with nonnull SQL models.</li>
     * <li>The {@code inner_value} reference binds to its declaration.</li>
     * <li>Completion offers {@code inner_value} inside its block.</li>
     * <li>Completion does not offer {@code inner_value} after the repeat.</li>
     * </ul>
     */
    @Test
    void repeatUntilKeepsItsNestedSqlAndLocalScope() throws DBException {
        assertStructuredControlFlow(
            "CREATE PROCEDURE p() BEGIN REPEAT INSERT INTO t VALUES (1); UNTIL done = 1 END REPEAT; END",
            SQLStandardParser.RULE_proceduralRepeat
        );
        String sql = "CREATE PROCEDURE p() BEGIN DECLARE outer_value int; " +
            "REPEAT BEGIN DECLARE inner_value int; SELECT inner_value FROM tab; END; " +
            "SELECT 1; UNTIL outer_value = 1 END REPEAT; SELECT outer_value FROM tab; END";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_proceduralRepeat);
        var model = recognize(sql);
        SQLQueryRoutineCreateModel routine = root(model, SQLQueryRoutineCreateModel.class);
        SQLQueryConditionalStatement repeat = statementAt(model, sql, "REPEAT BEGIN", SQLQueryConditionalStatement.class);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.REPEAT, repeat.getKind());
        Assertions.assertEquals(1, repeat.getBranches().size());
        assertEmbeddedSqlCount(routine, 3);
        assertBound(model, "inner_value", 0, 1);
        assertProposes(sql.replace("SELECT inner_value", "SELECT inner_|"), "inner_value");
        assertDoesNotPropose(sql.replace("SELECT outer_value FROM tab; END", "SELECT inner_| FROM tab; END"), "inner_value");
    }

    /**
     * <ul>
     * <li>Completion inside a repeat body offers the column of a temporary table created earlier in that body.</li>
     * </ul>
     */
    @Test
    void repeatUntilRetainsTemporaryRelationInsideBody() throws DBException {
        assertProposes("CREATE PROCEDURE p() BEGIN REPEAT CREATE TEMPORARY TABLE scratch (id int); " +
            "SELECT | FROM scratch; UNTIL TRUE END REPEAT; END", "id");
    }
}
