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
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryCursorOperationStatement;
import org.junit.jupiter.api.Test;

public class OracleProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("oracle");
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
