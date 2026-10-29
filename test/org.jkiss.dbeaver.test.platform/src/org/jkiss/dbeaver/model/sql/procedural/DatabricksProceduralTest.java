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
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariablesContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatabricksProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectFromBundle("org.jkiss.dbeaver.ext.databricks", "org.jkiss.dbeaver.ext.databricks.DatabricksSQLDialect");
    }

    /**
     * <ul>
     * <li>Looks up the value binding by value capability.</li>
     * <li>Looks up the cursor binding by cursor capability.</li>
     * </ul>
     */
    @Test
    void cursorAndValueHaveIndependentCapabilityNamespaces() {
        var node = recognize("SELECT 1").getSyntaxNode();
        SQLQuerySymbolEntry name = new SQLQuerySymbolEntry(node, "slot", "slot", null);
        SQLQueryLocalVariableInfo value = new SQLQueryLocalVariableInfo(
            name, SQLDialect.LocalVariableKind.VALUE, SQLQueryExprType.UNKNOWN, null, false);
        SQLQueryLocalVariableInfo cursor = new SQLQueryLocalVariableInfo(
            name, SQLDialect.LocalVariableKind.CURSOR, SQLQueryExprType.UNKNOWN, null, false);
        SQLQueryLocalVariablesContext bindings = new SQLQueryLocalVariablesContext(dialect())
            .withLocalVariable(value).withLocalVariable(cursor);
        Assertions.assertSame(value, bindings.findLocalVariable(name, SQLDialect.LocalVariableCapability.VALUE));
        Assertions.assertSame(cursor, bindings.findLocalVariable(name, SQLDialect.LocalVariableCapability.CURSOR));
    }

    /**
     * <ul>
     * <li>Recognizes the session variable declaration as a variable statement.</li>
     * <li>Recognizes the session variable assignment as a variable statement.</li>
     * </ul>
     */
    @Test
    void sessionVariableDeclarationAndAssignmentHaveStructuredModels() {
        root("DECLARE OR REPLACE VARIABLE current_value INT DEFAULT 1", SQLQueryVariableStatementModel.class);
        root("SET VAR current_value = 2", SQLQueryVariableStatementModel.class);
    }

    /**
     * <ul>
     * <li>Proposes a declared session variable after an assignment in the same script.</li>
     * </ul>
     */
    @Test
    void sessionVariableIsProposedAfterSeveralScriptStatements() throws DBException {
        assertScriptProposes("DECLARE VARIABLE current_value INT DEFAULT 1; " +
            "SET VAR current_value = 2; SELECT current_|", "current_value");
    }
}
