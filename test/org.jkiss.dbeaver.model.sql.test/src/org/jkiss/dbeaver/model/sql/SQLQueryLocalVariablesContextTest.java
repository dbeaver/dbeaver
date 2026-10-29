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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariablesContext;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsLocalTableModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

public class SQLQueryLocalVariablesContextTest extends DBeaverUnitTest {
    @Test
    void namespacesSeparateCapabilitiesAndPreserveInnerMasking() {
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(BasicSQLDialect.INSTANCE, DBWorkbench.getPlatform().getPreferenceStore());
        var context = new SQLQueryRecognitionContext(
            new VoidProgressMonitor(), null, false, false, syntax, BasicSQLDialect.INSTANCE);
        var model = SQLQueryModelRecognizer.recognizeQuery(context, "SELECT 1");
        Assertions.assertNotNull(model);
        STMTreeNode node = model.getSyntaxNode();
        SQLQuerySymbolEntry name = new SQLQuerySymbolEntry(node, "slot", "slot", null);
        SQLQueryLocalVariableInfo scalar = new SQLQueryLocalVariableInfo(
            name, SQLDialect.LocalVariableKind.VALUE, SQLQueryExprType.UNKNOWN, null, false);
        SQLQueryLocalVariableInfo cursor = new SQLQueryLocalVariableInfo(
            name, SQLDialect.LocalVariableKind.CURSOR_VALUE, SQLQueryExprType.UNKNOWN, null, false);
        SQLQueryLocalVariablesContext outer = new SQLQueryLocalVariablesContext(BasicSQLDialect.INSTANCE).withLocalVariable(cursor);
        SQLQueryLocalVariablesContext inner = outer.withNestedBlock().withLocalVariable(scalar);
        Assertions.assertSame(scalar, inner.findLocalVariable(name, SQLDialect.LocalVariableCapability.VALUE));
        Assertions.assertNull(inner.findLocalVariable(name, SQLDialect.LocalVariableCapability.CURSOR));
        Assertions.assertSame(cursor, outer.findLocalVariable(name, SQLDialect.LocalVariableCapability.CURSOR));

        SQLDialect separate = new BasicSQLDialect() {
            @NotNull
            @Override
            public List<LocalVariableNamespace> getLocalVariableNamespaces() {
                return List.of(
                    new LocalVariableNamespace(EnumSet.of(LocalVariableCapability.VALUE)),
                    new LocalVariableNamespace(EnumSet.of(LocalVariableCapability.FIELDS)),
                    new LocalVariableNamespace(EnumSet.of(LocalVariableCapability.ROWS)),
                    new LocalVariableNamespace(EnumSet.of(LocalVariableCapability.CURSOR))
                );
            }
        };
        SQLQueryLocalVariableInfo table = new SQLQueryLocalVariableInfo(
            name, SQLDialect.LocalVariableKind.ROWSET, SQLQueryExprType.UNKNOWN,
            new SQLQueryRowsLocalTableModel(node, name, List.of()), false);
        SQLQueryLocalVariablesContext independent = new SQLQueryLocalVariablesContext(separate).withLocalVariable(scalar).withLocalVariable(table);
        Assertions.assertSame(scalar, independent.findLocalVariable(name, SQLDialect.LocalVariableCapability.VALUE));
        Assertions.assertSame(table, independent.findLocalVariable(name, SQLDialect.LocalVariableCapability.ROWS));
        Assertions.assertEquals(List.of(scalar), independent.getVisibleValueVariables());

        SQLDialect programDialect = new BasicSQLDialect() {
            @NotNull
            @Override
            public LocalVariableVisibility getLocalVariableVisibility(@NotNull LocalVariableKind kind) {
                return LocalVariableVisibility.PROGRAM;
            }
        };
        SQLQueryLocalVariablesContext program = new SQLQueryLocalVariablesContext(programDialect).resetForProgram();
        SQLQueryLocalVariablesContext branch = program.withNestedBlock().withLocalVariable(scalar);
        Assertions.assertNull(program.findLocalVariable(name, SQLDialect.LocalVariableCapability.VALUE));
        Assertions.assertSame(scalar, program.withProgramVariablesFrom(branch).findLocalVariable(name, SQLDialect.LocalVariableCapability.VALUE));
    }
}
