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
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTargetReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

public class SqlServerProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("sqlserver");
    }

    /**
     * <ul>
     * <li>Procedure structure:
     * <ul>
     * <li>The root is a routine creation model.</li>
     * <li>Its body contains 11 embedded SQL statements with non-null SQL models.</li>
     * </ul></li>
     * <li>Nested control flow:
     * <ul>
     * <li>The {@code IF @n = 0} site resolves to a conditional statement.</li>
     * <li>That statement has kind IF.</li>
     * <li>The {@code WHILE @n &lt; 1} site resolves to a conditional statement.</li>
     * <li>That statement has kind LOOP.</li>
     * <li>The {@code IF @n = 2} site resolves to a conditional statement.</li>
     * <li>That statement has kind IF.</li>
     * <li>The {@code WHILE @n &lt; 3} site resolves to a conditional statement.</li>
     * <li>That statement has kind LOOP.</li>
     * </ul></li>
     * <li>Column proposals at the 11 SELECT sites:
     * <ul>
     * <li>The initial SELECT from {@code #scratch} proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the first IF arm proposes {@code id}.</li>
     * <li>The SELECT from {@code #scratch} in the first inner WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the first ELSE arm proposes {@code id}.</li>
     * <li>The SELECT from {@code #scratch} after the first IF proposes {@code id}.</li>
     * <li>The SELECT from {@code #scratch} after the outer WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the second inner IF arm proposes {@code id}.</li>
     * <li>The SELECT from {@code #scratch} in the second inner WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the second inner ELSE arm proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the outer ELSE arm proposes {@code id}.</li>
     * <li>The final SELECT from {@code #scratch} proposes {@code id}.</li>
     * <li>The proposal check visits exactly 11 SELECT sites.</li>
     * </ul></li>
     * <li>Full-script completion at the final {@code #scratch} SELECT contains {@code id}.</li>
     * </ul>
     */
    @Test
    void nativeConditionsAndLoopsRetainAllEmbeddedQueriesAndProposals() throws DBException {
        String sql = """
            CREATE OR ALTER PROC p AS BEGIN
              DECLARE @n int;
              CREATE TABLE #scratch (id int);
              SELECT id FROM #scratch;
              WHILE @n < 2 BEGIN
                IF @n = 0 BEGIN
                  SELECT id FROM tab;
                  WHILE @n < 1 BEGIN SELECT id FROM #scratch; SET @n = 1; END
                END ELSE BEGIN SELECT id FROM tab; END
                SELECT id FROM #scratch;
                SET @n = 2;
              END
              SELECT id FROM #scratch;
              IF @n >= 2 BEGIN
                IF @n = 2 BEGIN
                  SELECT id FROM tab;
                  WHILE @n < 3 BEGIN SELECT id FROM #scratch; SET @n = 3; END
                END ELSE BEGIN SELECT id FROM tab; END
              END ELSE BEGIN SELECT id FROM tab; END
              SELECT id FROM #scratch;
            END
            """;
        var model = recognize(sql);
        var routine = root(model, SQLQueryRoutineCreateModel.class);
        assertEmbeddedSqlCount(routine, 11);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF,
            statementAt(model, sql, "IF @n = 0", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP,
            statementAt(model, sql, "WHILE @n < 1", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF,
            statementAt(model, sql, "IF @n = 2", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP,
            statementAt(model, sql, "WHILE @n < 3", SQLQueryConditionalStatement.class).getKind());

        var request = RequestBuilder.tables(tables -> tables.table("tab", table -> table.attribute("id")))
            .dialect(dialect()).prepare();
        assertEverySelectProposes(request, sql, "id", 11);
        int lastSelect = sql.lastIndexOf("SELECT id FROM #scratch");
        String last = sql.substring(0, lastSelect) + sql.substring(lastSelect).replace("SELECT id", "SELECT |");
        Assertions.assertTrue(request.requestNewScriptStrings(last).contains("id"), last);
    }

    /**
     * <ul>
     * <li>Recognizes a routine creation model for the procedure containing both declarations.</li>
     * <li>Proposes the temporary table's name column in a later SELECT.</li>
     * </ul>
     */
    @Test
    void tableVariableAndPrefixedTemporaryTableHaveLaterColumns() throws DBException {
        String sql = "CREATE OR ALTER PROC p AS BEGIN DECLARE @items TABLE (id int); " +
            "CREATE TABLE #scratch (name text); SELECT id FROM @items; SELECT | FROM #scratch; END";
        root(sql.replace("|", ""), SQLQueryRoutineCreateModel.class);
        assertProposes(sql, "name");
    }

    /**
     * <ul>
     * <li>Proposes a declared variable in the same script batch.</li>
     * <li>Does not propose it after a GO batch boundary.</li>
     * </ul>
     */
    @Test
    void goSeparatesBatchLocalVariables() throws DBException {
        assertScriptProposes("DECLARE @current_value int; SELECT @current_|", "@current_value");
        assertScriptDoesNotPropose("DECLARE @current_value int;\nGO\nSELECT @current_|", "@current_value");
    }

    /**
     * <ul>
     * <li>Proposes a declared @-prefixed local variable inside a procedure.</li>
     * </ul>
     */
    @Test
    void prefixedLocalVariableIsProposedInsideProcedure() throws DBException {
        assertProposes("CREATE OR ALTER PROC p AS BEGIN DECLARE @local_value int; SELECT @local_|; END", "@local_value");
    }

    /**
     * <ul>
     * <li>Proposes {@code @nested_value} after its nested BEGIN block.</li>
     * <li>Proposes {@code @branch_value} after its IF block.</li>
     * </ul>
     */
    @Test
    void programVariableSurvivesNestedBlocksAndConditions() throws DBException {
        assertProposes("CREATE OR ALTER PROC p AS BEGIN BEGIN DECLARE @nested_value int; END; " +
            "SELECT @nested_|; END", "@nested_value");
        assertProposes("CREATE OR ALTER PROC p AS BEGIN IF 1 = 1 BEGIN DECLARE @branch_value int; END; " +
            "SELECT @branch_|; END", "@branch_value");
    }

    /**
     * <ul>
     * <li>Proposes a declared table variable as a SELECT row source inside a procedure.</li>
     * </ul>
     */
    @Test
    void tableVariableIsProposedAsLocalRowSource() throws DBException {
        assertProposes("CREATE OR ALTER PROC p AS BEGIN DECLARE @items TABLE (id int); " +
            "SELECT * FROM |; END", "@items");
    }

    /**
     * <ul>
     * <li>Variable symbols:
     * <ul>
     * <li>Finds two {@code @value} symbol entries.</li>
     * <li>The second entry's definition is the first entry.</li>
     * <li>The second entry has a script-variable-reference origin.</li>
     * </ul></li>
     * <li>UPDATE and variable target:
     * <ul>
     * <li>The node at {@code SET @value} is an UPDATE model.</li>
     * <li>The first SET target is a target-reference expression.</li>
     * <li>That target has a local variable.</li>
     * <li>That target has no script variable.</li>
     * <li>That target has no trivial column expression.</li>
     * </ul></li>
     * <li>Column target:
     * <ul>
     * <li>The {@code id} symbol has a column-from-rows-data origin.</li>
     * <li>The second SET target is a target-reference expression.</li>
     * <li>That target has no local variable.</li>
     * <li>That target has no script variable.</li>
     * </ul></li>
     * <li>The SET clause proposes {@code @value} at {@code @val|}.</li>
     * </ul>
     */
    @Test
    void updateSetDistinguishesVariableAndColumnTargets() throws DBException {
        String sql = "CREATE OR ALTER PROC p AS BEGIN DECLARE @value int; " +
            "UPDATE tab SET @value = 1, id = 2; END";
        var model = recognize(sql);
        List<SQLQuerySymbolEntry> variableNames = model.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("@value"))
            .sorted(Comparator.comparingInt(entry -> entry.getInterval().a)).toList();
        Assertions.assertEquals(2, variableNames.size());
        Assertions.assertSame(variableNames.getFirst(), variableNames.getLast().getDefinition());
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.ScriptVariableRef.class, variableNames.getLast().getOrigin());
        SQLQueryUpdateModel update = Assertions.assertInstanceOf(
            SQLQueryUpdateModel.class, model.findNodeContaining(sql.indexOf("SET @value") + 5));
        SQLQueryValueTargetReferenceExpression variableTarget = Assertions.assertInstanceOf(
            SQLQueryValueTargetReferenceExpression.class, update.getSetClauseList().getFirst().targets.getFirst());
        Assertions.assertNotNull(variableTarget.getLocalVariable());
        Assertions.assertNull(variableTarget.getScriptVariable());
        Assertions.assertNull(variableTarget.getColumnIfTrivialExpression());

        SQLQuerySymbolEntry columnName = model.getAllSymbols().stream()
            .filter(entry -> entry.getRawName().equals("id"))
            .findFirst().orElseThrow();
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.ColumnNameFromRowsData.class, columnName.getOrigin());
        SQLQueryValueTargetReferenceExpression columnTarget = Assertions.assertInstanceOf(
            SQLQueryValueTargetReferenceExpression.class, update.getSetClauseList().get(1).targets.getFirst());
        Assertions.assertNull(columnTarget.getLocalVariable());
        Assertions.assertNull(columnTarget.getScriptVariable());

        assertProposes("CREATE OR ALTER PROC p AS BEGIN DECLARE @value int; " +
            "UPDATE tab SET @val| = 1, id = 2; END", "@value");
    }

    /**
     * <ul>
     * <li>Procedure and control flow:
     * <ul>
     * <li>The root is a routine creation model.</li>
     * <li>The BEGIN TRY site resolves to a conditional statement.</li>
     * <li>That statement has kind TRY_CATCH.</li>
     * <li>That statement has two branches.</li>
     * <li>The routine body contains four embedded SQL statements with non-null SQL models.</li>
     * </ul></li>
     * <li>Proposes {@code id} at the CATCH SELECT from {@code #scratch}.</li>
     * <li>Proposes {@code @inside} at the CATCH SELECT.</li>
     * <li>Proposes {@code @inside} at the SELECT following CATCH.</li>
     * </ul>
     */
    @Test
    void tryCatchKeepsBothSqlArmsAndProgramVariables() throws DBException {
        String sql = "CREATE OR ALTER PROCEDURE p AS BEGIN " +
            "CREATE TABLE #scratch (id int); " +
            "BEGIN TRY DECLARE @inside int; SELECT id FROM #scratch; END TRY " +
            "BEGIN CATCH SELECT id FROM #scratch; SELECT @inside FROM #scratch; END CATCH " +
            "SELECT @inside FROM #scratch; END";
        var model = recognize(sql);
        SQLQueryRoutineCreateModel routine = root(model, SQLQueryRoutineCreateModel.class);
        SQLQueryConditionalStatement tryCatch = statementAt(model, sql, "BEGIN TRY", SQLQueryConditionalStatement.class);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.TRY_CATCH, tryCatch.getKind());
        Assertions.assertEquals(2, tryCatch.getBranches().size());
        assertEmbeddedSqlCount(routine, 4);
        assertProposes(sql.replace("BEGIN CATCH SELECT id", "BEGIN CATCH SELECT |"), "id");
        assertProposes(sql.replace("SELECT @inside FROM #scratch; END CATCH", "SELECT @ins| FROM #scratch; END CATCH"), "@inside");
        assertProposes(sql.replace("SELECT @inside FROM #scratch; END", "SELECT @ins| FROM #scratch; END"), "@inside");
    }

    /**
     * <ul>
     * <li>Structured parsing:
     * <ul>
     * <li>The parser reports no syntax errors.</li>
     * <li>The parser consumes through EOF.</li>
     * <li>The parse tree has one procedural TRY/CATCH rule node.</li>
     * <li>The parse tree has no routine-body token fallback nodes.</li>
     * <li>The parse tree has no procedural-statement token fallback nodes.</li>
     * </ul></li>
     * <li>The root is a routine creation model.</li>
     * <li>The BEGIN TRY site resolves to a conditional statement.</li>
     * <li>That statement has kind TRY_CATCH.</li>
     * <li>The routine body contains two embedded SQL statements with non-null SQL models.</li>
     * </ul>
     */
    @Test
    void unwrappedTryCatchBodyRemainsStructured() {
        String sql = "CREATE OR ALTER PROCEDURE p AS " +
            "BEGIN TRY SELECT 1; END TRY BEGIN CATCH SELECT ERROR_MESSAGE(); END CATCH";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_proceduralTryCatch);
        var model = recognize(sql);
        SQLQueryRoutineCreateModel routine = root(model, SQLQueryRoutineCreateModel.class);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.TRY_CATCH,
            statementAt(model, sql, "BEGIN TRY", SQLQueryConditionalStatement.class).getKind());
        assertEmbeddedSqlCount(routine, 2);
    }

}
