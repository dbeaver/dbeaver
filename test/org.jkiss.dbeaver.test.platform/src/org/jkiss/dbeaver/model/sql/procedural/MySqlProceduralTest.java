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
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MySqlProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("mysql");
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
