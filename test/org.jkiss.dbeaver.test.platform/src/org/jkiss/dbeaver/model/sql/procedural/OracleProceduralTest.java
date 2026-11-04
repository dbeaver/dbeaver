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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTypeCastExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryCursorOperationStatement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OracleProceduralTest extends ProceduralDialectTestBase {
    @Test
    void columnNamesTakePrecedenceOverLocalVariables() {
        Assertions.assertEquals(SQLDialectFeatures.ProceduralVariableConflictPolicy.PREFER_COLUMN,
            dialect().getDialectFeatures().getProceduralVariableConflictPolicy());
    }

    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("oracle");
    }

    @Test
    void suffixParameterModifiersKeepBaseTypeAndBodyBinding() {
        String sql = "CREATE PROCEDURE p(p_value IN OUT NOCOPY NUMBER) AS BEGIN SELECT p_value; END";
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
            () -> Assertions.assertEquals(List.of("IN", "OUT", "NOCOPY"), parameter.getModifiers()),
            () -> Assertions.assertEquals("NUMBER", parameter.getTypeRefString()),
            () -> Assertions.assertEquals("NUMBER", parameter.getType().getDisplayName()),
            () -> Assertions.assertEquals(List.of(), parameter.getType().getModifiers()),
            () -> Assertions.assertEquals("NUMBER", reference.getValueType().getDisplayName()),
            () -> Assertions.assertEquals(List.of(), reference.getValueType().getModifiers())
        );
    }

    @Test
    void signatureModifiersAreFilteredByOracleDialect() {
        var routine = root("CREATE PROCEDURE p(p_value IN INOUT NUMBER) AS BEGIN SELECT p_value; END",
            SQLQueryRoutineCreateModel.class);
        var parameter = routine.getArguments().getFirst().declaration().getDeclarations().getFirst();
        Assertions.assertEquals(List.of("IN"), parameter.getModifiers());
        Assertions.assertEquals(List.of(), parameter.getType().getModifiers());
    }

    @Test
    void qualifiedCastKeepsBaseSpellingAndRejectsForeignTypeModifiers() {
        String sql = "SELECT CAST(1 AS app.amount_type)";
        assertStructuredControlFlow(sql, SQLStandardParser.RULE_castSpecification);
        var cast = statementAt(sql, "CAST", SQLQueryValueTypeCastExpression.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals("app.amount_type", cast.getTypeRefString()),
            () -> Assertions.assertEquals("app.amount_type", cast.getValueType().getDisplayName()),
            () -> Assertions.assertEquals(List.of(), cast.getValueType().getModifiers())
        );

        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect(), DBWorkbench.getPlatform().getPreferenceStore());
        LSMAnalyzerParameters parameters = LSMAnalyzerParameters.forDialect(dialect(), syntax);
        String unsupported = "SELECT CAST(1 AS app.amount_type UNSIGNED)";
        SQLStandardParser parser = new SQLStandardParser(
            new CommonTokenStream(new SQLStandardLexer(CharStreams.fromString(unsupported), parameters)), parameters);
        var tree = parser.sqlQuery();
        Assertions.assertTrue(parser.getNumberOfSyntaxErrors() > 0, tree.toStringTree(parser));
    }

    /**
     * <ul>
     * <li>The procedure has a routine-creation root model.</li>
     * <li>The {@code CASE WHEN} site has a conditional-statement model.</li>
     * <li>The {@code EXCEPTION WHEN} site has a conditional-statement model.</li>
     * </ul>
     */
    @Test
    void nestedCaseAndExceptionKeepStructuredProcedureBody() {
        String sql = "CREATE PROCEDURE p AS BEGIN CASE WHEN 1 = 1 THEN SELECT 1; END CASE; " +
            "EXCEPTION WHEN OTHERS THEN SELECT 0; END";
        var model = recognize(sql);
        root(model, SQLQueryRoutineCreateModel.class);
        statementAt(model, sql, "CASE WHEN", SQLQueryConditionalStatement.class);
        statementAt(model, sql, "EXCEPTION WHEN", SQLQueryConditionalStatement.class);
    }

    /**
     * <ul>
     * <li>Completion offers a global temporary table's column after an earlier query in the procedure.</li>
     * </ul>
     */
    @Test
    void temporaryRelationProvidesColumnsAfterOtherStatements() throws DBException {
        assertProposes("CREATE PROCEDURE p AS BEGIN CREATE GLOBAL TEMPORARY TABLE scratch (id int); " +
            "SELECT id FROM scratch; SELECT | FROM scratch; END", "id");
    }

    /**
     * <ul>
     * <li>A fetch inside a nested block is recognized as a cursor operation.</li>
     * <li>Cursor references bind to the declared cursor:
     * <ul>
     * <li>At {@code OPEN c}.</li>
     * <li>At the nested {@code FETCH NEXT FROM c}.</li>
     * </ul></li>
     * </ul>
     */
    @Test
    void cursorReferencesSurviveNestedStatementSequences() {
        String sql = "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
            "BEGIN OPEN c; BEGIN FETCH NEXT FROM c INTO x; END; CLOSE c; END";
        var model = recognize(sql);
        statementAt(model, sql, "FETCH NEXT", SQLQueryCursorOperationStatement.class);
        assertBound(model, "c", 0, 1);
        assertBound(model, "c", 0, 2);
    }
}
