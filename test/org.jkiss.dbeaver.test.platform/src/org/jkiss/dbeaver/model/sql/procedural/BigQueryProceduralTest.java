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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BigQueryProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("google_bigquery");
    }

    /**
     * <ul>
     * <li>Recognizes the table function as a routine creation model.</li>
     * <li>Checks the parameter binding:
     * <ul>
     * <li>Finds both the declaration and use of {@code x}.</li>
     * <li>Resolves the use of {@code x} to its declaration.</li>
     * </ul>
     * </li>
     * </ul>
     */
    @Test
    void tableFunctionContainsQueryRatherThanTableDdl() {
        var model = recognize("CREATE TABLE FUNCTION dataset.f(x INT64) AS (SELECT x FROM rects)");
        Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        assertBound(model, "x", 0, 1);
    }

    /**
     * <ul>
     * <li>Proposes a temporary table column after an IF block within the procedure.</li>
     * </ul>
     */
    @Test
    void conditionalBodyRetainsLateTemporaryTableColumns() throws DBException {
        assertProposes("CREATE PROCEDURE dataset.p() BEGIN CREATE TEMP TABLE scratch (id INT64); " +
            "IF TRUE THEN SELECT id FROM scratch; END IF; SELECT | FROM scratch; END", "id");
    }

    /**
     * <ul>
     * <li>Proposes a variable inside its declaring branch's nested block.</li>
     * <li>Does not propose that variable after leaving the branch.</li>
     * </ul>
     */
    @Test
    void branchLocalVariableIsProposedOnlyInsideItsBranch() throws DBException {
        String inside = "CREATE PROCEDURE dataset.p() BEGIN IF TRUE THEN " +
            "BEGIN DECLARE branch_value INT64; SELECT branch_|; END; END IF; SELECT 1; END";
        assertProposes(inside, "branch_value");
        String outside = inside.replace("SELECT branch_|; END; END IF; SELECT 1", "SELECT branch_value; END; END IF; SELECT branch_|");
        assertDoesNotPropose(outside, "branch_value");
    }

    /**
     * <ul>
     * <li>Obtains a non-null recognition result.</li>
     * <li>Obtains a non-null structured semantic model.</li>
     * <li>Checks the variable binding:
     * <ul>
     * <li>Finds the first declaration and the later use among at least three occurrences of {@code shared_value}.</li>
     * <li>Resolves the use of {@code shared_value} to the first declaration.</li>
     * </ul>
     * </li>
     * <li>Finds a diagnostic containing {@code already declared}.</li>
     * </ul>
     */
    @Test
    void rejectsRedeclarationOfVisibleVariableInNestedBlock() {
        String sql = "CREATE PROCEDURE dataset.p() BEGIN DECLARE shared_value INT64; " +
            "BEGIN DECLARE shared_value INT64; SELECT shared_value; END; END";
        SQLDialect dialect = dialect();
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        SQLQueryRecognitionContext context = new SQLQueryRecognitionContext(
            new VoidProgressMonitor(), null, false, false, syntax, dialect);
        var model = SQLQueryModelRecognizer.recognizeQuery(context, sql);
        Assertions.assertNotNull(model, sql);
        Assertions.assertNotNull(model.getQueryModel(), () -> "No structured semantic model for " + sql);
        assertBound(model, "shared_value", 0, 2);
        Assertions.assertTrue(context.getProblems().stream().anyMatch(p -> p.getMessage().contains("already declared")),
            context.getProblems().toString());
    }

    /**
     * <ul>
     * <li>Proposes a script variable after intervening top-level statements.</li>
     * </ul>
     */
    @Test
    void scriptVariableRemainsVisibleAcrossMultipleTopLevelStatements() throws DBException {
        assertScriptProposes("DECLARE current_value INT64 DEFAULT 1; SELECT 1; SELECT current_|", "current_value");
    }

    /**
     * <ul>
     * <li>Finds {@code current_value} among the script proposals.</li>
     * <li>Checks the proposals' replacement ranges:
     * <ul>
     * <li>Confirms the proposal set is not empty.</li>
     * <li>Confirms every replacement starts within the last statement and ends before the cursor.</li>
     * </ul>
     * </li>
     * </ul>
     */
    @Test
    void proposalsInLongScriptReplaceOnlyTheActiveStatementText() throws DBException {
        StringBuilder script = new StringBuilder("DECLARE current_value INT64 DEFAULT 1;\n");
        for (int i = 0; i < 30; i++) {
            script.append("SELECT 1;\n");
        }
        int lastStatement = script.length();
        script.append("SELECT current_|");
        String text = script.toString();
        var proposals = RequestBuilder.empty().dialect(dialect()).prepare().requestNewScriptProposals(text);
        Assertions.assertTrue(proposals.stream().anyMatch(p -> p.getReplacementString().trim().equalsIgnoreCase("current_value")),
            () -> "Missing 'current_value' in " + proposals + " for " + text);
        Assertions.assertFalse(proposals.isEmpty());
        Assertions.assertTrue(proposals.stream().allMatch(p ->
            p.getReplacementOffset() >= lastStatement &&
                p.getReplacementOffset() + p.getReplacementLength() <= text.indexOf('|')),
            () -> "Proposal would replace text outside the active statement: " + proposals);
    }

    /**
     * <ul>
     * <li>Does not propose a branch-created temporary table column after the IF block.</li>
     * </ul>
     */
    @Test
    void temporaryTableFromOneBranchDoesNotLeakToLaterStatement() throws DBException {
        assertDoesNotPropose("CREATE PROCEDURE dataset.p() BEGIN IF TRUE THEN " +
            "CREATE TEMP TABLE branch_only (branch_id INT64); END IF; SELECT | FROM branch_only; END", "branch_id");
    }
}
