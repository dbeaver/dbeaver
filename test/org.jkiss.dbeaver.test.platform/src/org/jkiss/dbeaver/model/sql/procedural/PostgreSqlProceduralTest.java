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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreSqlProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("postgresql");
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

}
