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
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryVariableDeclarationStatement;
import org.junit.jupiter.api.Test;

public class H2ProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("h2");
    }

    /**
     * <ul>
     * <li>The procedure has a routine-creation root model.</li>
     * <li>The {@code DECLARE local_value} site has a variable-declaration model.</li>
     * <li>Completion offers the column of a local table declared earlier in the procedure.</li>
     * </ul>
     */
    @Test
    void recognizesVariableInOrderedProcedureAndLocalRelationProposals() throws DBException {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE local_value int; SET local_value = 1; " +
            "SELECT local_value; END";
        var model = recognize(sql);
        root(model, SQLQueryRoutineCreateModel.class);
        statementAt(model, sql, "DECLARE local_value", SQLQueryVariableDeclarationStatement.class);
        assertProposes("CREATE PROCEDURE p AS BEGIN DECLARE @items TABLE (id int); " +
            "SELECT | FROM @items; END", "id");
    }
}
