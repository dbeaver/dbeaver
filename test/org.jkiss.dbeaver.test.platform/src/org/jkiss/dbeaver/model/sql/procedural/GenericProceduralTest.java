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
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryDeleteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryInsertModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.*;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class GenericProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return BasicSQLDialect.INSTANCE;
    }

    /**
     * <ul>
     * <li>The set of unfiltered local-variable proposals contains exactly {@code counter_value}: the later
     * {@code future_value} is absent.</li>
     * </ul>
     */
    @Test
    void unfilteredLocalVariableProposalsExcludeFutureDeclarations() throws DBException {
        var proposals = RequestBuilder.empty().dialect(dialect()).prepare().requestNewProposals(
            "CREATE PROCEDURE p AS BEGIN DECLARE counter_value int; SELECT | FROM tab; " +
                "DECLARE future_value int; END");
        Set<String> localNames = proposals.stream()
            .filter(p -> p.getItemKind() == SQLQueryCompletionItemKind.LOCAL_VARIABLE)
            .map(p -> p.getReplacementString().trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("counter_value"), localNames);
    }

    /**
     * <ul>
     * <li>Inside the conditional branch:
     * <ul>
     * <li>Completion offers {@code inner_value}.</li>
     * <li>Completion offers {@code outer_value}.</li>
     * </ul></li>
     * <li>After the branch:
     * <ul>
     * <li>The {@code outer_value} reference binds to its declaration.</li>
     * <li>Completion offers {@code outer_value}.</li>
     * <li>Completion does not offer {@code inner_value}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void proposesOnlyVariablesVisibleAtTheSelect() throws DBException {
        String procedure = "CREATE PROCEDURE p AS BEGIN DECLARE outer_value int; " +
            "IF 1 = 1 THEN DECLARE inner_value int; SELECT inner_|; END IF; SELECT outer_value; END";
        assertProposes(procedure, "inner_value");
        assertProposes(procedure.replace("inner_|", "outer_|"), "outer_value");
        String afterBlock = procedure.replace("SELECT inner_|; END IF; SELECT outer_value", "SELECT inner_value; END IF; SELECT outer_|");
        assertBound(recognize(afterBlock.replace("outer_|", "outer_value")), "outer_value", 0, 1);
        assertProposes(afterBlock, "outer_value");
        assertDoesNotPropose(afterBlock.replace("outer_|", "inner_|"), "inner_value");
    }

    /**
     * <ul>
     * <li>A function parameter is offered inside its body.</li>
     * <li>A shadowed variable name appears only once in completion inside the nested block.</li>
     * </ul>
     */
    @Test
    void proposesRoutineParameterAndNearestShadowedVariable() throws DBException {
        assertProposes("CREATE FUNCTION f(input_value int) RETURNS int AS BEGIN SELECT input_|; END", "input_value");
        var proposals = RequestBuilder.empty().dialect(dialect()).prepare().requestNewProposals(
            "CREATE PROCEDURE p AS BEGIN DECLARE shared_value int; " +
                "BEGIN DECLARE shared_value text; SELECT shared_|; END; END"
        );
        Assertions.assertEquals(1, proposals.stream().filter(p ->
            p.getItemKind() == SQLQueryCompletionItemKind.LOCAL_VARIABLE &&
                p.getReplacementString().trim().equalsIgnoreCase("shared_value")
        ).count(), proposals.toString());
    }

    /**
     * <ul>
     * <li>Inside the loop's nested conditional block:
     * <ul>
     * <li>Completion offers {@code loop_value}.</li>
     * <li>Completion offers {@code outer_value}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void proposesVariablesInsideNestedLoopAndConditional() throws DBException {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE outer_value int; " +
            "WHILE TRUE DO IF TRUE THEN BEGIN DECLARE loop_value int; SELECT loop_|; END; END IF; " +
            "END WHILE; SELECT outer_value; END";
        assertProposes(sql, "loop_value");
        assertProposes(sql.replace("loop_|", "outer_|"), "outer_value");
    }

    /**
     * <ul>
     * <li>The procedure has a routine-creation model.</li>
     * <li>The {@code SET x} site has an assignment-statement model.</li>
     * <li>The {@code IF} site has a conditional-statement model.</li>
     * <li>References to {@code x} bind to its declaration:
     * <ul>
     * <li>At the {@code SET x} assignment.</li>
     * <li>At the {@code SELECT x} inside the conditional.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void recognizesOrderedNestedAssignmentsAndBranchIsolation() {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; " +
            "IF 1 = 1 THEN DECLARE branch_only int; SELECT x; END IF; SELECT x; END";
        SQLQueryModel model = recognize(sql);
        Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryAssignmentStatement.class, model.findNodeContaining(sql.indexOf("SET x") + 1));
        Assertions.assertInstanceOf(SQLQueryConditionalStatement.class, model.findNodeContaining(sql.indexOf("IF 1") + 1));
        assertBound(model, "x", 0, 1);
        assertBound(model, "x", 0, 2);
    }

    /**
     * <ul>
     * <li>A trigger definition is recognized as a trigger.</li>
     * <li>Completion still offers a temporary table's column after many earlier queries in a procedure.</li>
     * </ul>
     */
    @Test
    void triggerBodyAndLargeProcedureRetainLateTemporaryTableProposals() throws DBException {
        root("CREATE TRIGGER audit AFTER UPDATE ON tab REFERENCING NEW AS n FOR EACH ROW " +
            "BEGIN SELECT n.id; END", SQLQueryTriggerCreateModel.class);

        StringBuilder body = new StringBuilder("CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (id int); ");
        for (int i = 0; i < 24; i++) {
            body.append("SELECT id FROM scratch; ");
        }
        assertProposes(body + "SELECT | FROM scratch; END", "id");
    }

    /**
     * <ul>
     * <li>Routine definitions have their respective root models:
     * <ul>
     * <li>{@code CREATE FUNCTION} has a routine-creation model.</li>
     * <li>{@code ALTER FUNCTION} has a routine-alteration model.</li>
     * <li>{@code DROP PROCEDURE} has a routine-drop model.</li>
     * </ul></li>
     * <li>Trigger definitions have their respective root models:
     * <ul>
     * <li>{@code CREATE TRIGGER} has a trigger-creation model.</li>
     * <li>{@code ALTER TRIGGER} has a trigger-alteration model.</li>
     * <li>{@code DROP TRIGGER} has a trigger-drop model.</li>
     * </ul></li>
     * <li>The {@code DO} block has an anonymous-block root model.</li>
     * </ul>
     */
    @Test
    void recognizesEachProgramDefinitionFamilyWithoutConflatingBodies() {
        root("CREATE FUNCTION f(x int) RETURNS int AS (SELECT x FROM tab)", SQLQueryRoutineCreateModel.class);
        root("ALTER FUNCTION f", SQLQueryRoutineAlterModel.class);
        root("DROP PROCEDURE p", SQLQueryRoutineDropModel.class);
        root("CREATE TRIGGER tr AFTER INSERT ON tab BEGIN SELECT NEW.id; END", SQLQueryTriggerCreateModel.class);
        root("ALTER TRIGGER tr", SQLQueryTriggerAlterModel.class);
        root("DROP TRIGGER tr ON tab", SQLQueryTriggerDropModel.class);
        root("DO $body$ BEGIN SELECT 1; SELECT 2; END; $body$", SQLQueryAnonymousBlockModel.class);
    }

    /**
     * <ul>
     * <li>The procedure has a routine-creation root model.</li>
     * <li>Its body is a statement sequence.</li>
     * <li>The body sequence is not empty.</li>
     * <li>Cursor operation sites have cursor-operation models:
     * <ul>
     * <li>{@code OPEN c}.</li>
     * <li>{@code FETCH NEXT}.</li>
     * <li>{@code CLOSE c}.</li>
     * <li>{@code DEALLOCATE c}.</li>
     * </ul></li>
     * <li>The cursor operations occur in open-fetch-close-deallocate source order.</li>
     * </ul>
     */
    @Test
    void orderedCursorAndSqlOperationsStayInsideOneBody() {
        String sql = "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
            "BEGIN OPEN c; FETCH NEXT FROM c INTO x; CLOSE c; DEALLOCATE c; SELECT id FROM tab; END";
        SQLQueryModel model = recognize(sql);
        SQLQueryRoutineCreateModel routine = root(model, SQLQueryRoutineCreateModel.class);
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        Assertions.assertFalse(body.getStatements().isEmpty(), sql);
        SQLQueryCursorOperationStatement open = statementAt(model, sql, "OPEN c", SQLQueryCursorOperationStatement.class);
        SQLQueryCursorOperationStatement fetch = statementAt(model, sql, "FETCH NEXT", SQLQueryCursorOperationStatement.class);
        SQLQueryCursorOperationStatement close = statementAt(model, sql, "CLOSE c", SQLQueryCursorOperationStatement.class);
        SQLQueryCursorOperationStatement deallocate = statementAt(model, sql, "DEALLOCATE c", SQLQueryCursorOperationStatement.class);
        Assertions.assertTrue(open.getInterval().a < fetch.getInterval().a && fetch.getInterval().a < close.getInterval().a &&
            close.getInterval().a < deallocate.getInterval().a);
    }

    /**
     * <ul>
     * <li>The procedure has a routine-creation root model.</li>
     * <li>Its body is a statement sequence.</li>
     * <li>The first body statement is a block.</li>
     * <li>Completion at the later query:
     * <ul>
     * <li>Offers {@code new_id} from {@code replaced}.</li>
     * <li>Does not offer {@code old_id} from {@code removed}.</li>
     * <li>Offers {@code kept_id} from {@code kept}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void nestedBlockDropsAndReplacesTemporaryRelationsBeforeLaterCompletion() throws DBException {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE removed (old_id int); " +
            "CREATE TEMP TABLE kept (kept_id int); BEGIN DROP TABLE removed; " +
            "CREATE TEMP TABLE replaced (new_id int); END; SELECT | FROM replaced; END";
        SQLQueryRoutineCreateModel routine = root(sql.replace("|", ""), SQLQueryRoutineCreateModel.class);
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        Assertions.assertInstanceOf(SQLQueryStatementBlock.class, body.getStatements().getFirst());
        assertProposes(sql, "new_id");
        assertDoesNotPropose(sql.replace("FROM replaced", "FROM removed"), "old_id");
        assertProposes(sql.replace("FROM replaced", "FROM kept"), "kept_id");
    }

    /**
     * <ul>
     * <li>The {@code WHILE} site has a conditional-statement model.</li>
     * <li>That conditional has loop kind.</li>
     * <li>Embedded DML sites have their respective models:
     * <ul>
     * <li>{@code INSERT INTO} has an insert model.</li>
     * <li>{@code UPDATE scratch} has an update model.</li>
     * <li>{@code DELETE FROM} has a delete model.</li>
     * </ul></li>
     * <li>The insert, update, and delete occur in that source order.</li>
     * </ul>
     */
    @Test
    void loopAndEmbeddedDmlKeepTheirStatementKindsInOrder() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (id int); " +
            "WHILE TRUE DO INSERT INTO scratch (id) VALUES (1); UPDATE scratch SET id = 2; " +
            "DELETE FROM scratch; END WHILE; SELECT id FROM scratch; END";
        SQLQueryModel model = recognize(sql);
        SQLQueryConditionalStatement loop = Assertions.assertInstanceOf(SQLQueryConditionalStatement.class,
            model.findNodeContaining(sql.indexOf("WHILE TRUE") + 1));
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP, loop.getKind());
        SQLQueryInsertModel insert = statementAt(model, sql, "INSERT INTO", SQLQueryInsertModel.class);
        SQLQueryUpdateModel update = statementAt(model, sql, "UPDATE scratch", SQLQueryUpdateModel.class);
        SQLQueryDeleteModel delete = statementAt(model, sql, "DELETE FROM", SQLQueryDeleteModel.class);
        Assertions.assertTrue(insert.getInterval().a < update.getInterval().a && update.getInterval().a < delete.getInterval().a);
    }
}
