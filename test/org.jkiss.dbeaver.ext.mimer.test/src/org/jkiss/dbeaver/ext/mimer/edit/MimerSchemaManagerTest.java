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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MimerSchemaManager}'s {@code CREATE SCHEMA} action wiring (Mimer SQL has no {@code
 * ALTER SCHEMA ... RENAME} - Comment is the only editable property).
 * <p>
 * {@code addObjectModifyActions}/{@code addObjectDeleteActions} aren't covered - both take a
 * {@code protected}-nested-class command type ({@code ObjectChangeCommand}/{@code
 * ObjectDeleteCommand}, declared in a different bundle's package). A "test subclass" was tried
 * to get compile-time access to the type name, but Mockito still can't generate a mock for a
 * non-public class across an OSGi bundle boundary at runtime ({@code MockitoException:
 * "The type is not public and its mock class is loaded by a different class loader"}, confirmed
 * live) - the subclass trick only solves the compile-time half of the problem, not this deeper
 * one. Only {@code ObjectCreateCommand} (declared {@code public}) is actually mockable this way,
 * so only the create path is covered here.
 *
 * @author Mimer Information Technology
 */
public class MimerSchemaManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerSchema schema;

    @Mock
    private DBCExecutionContext executionContext;

    private final MimerSchemaManager manager = new MimerSchemaManager();

    @Test
    public void createActionEmitsCreateSchemaDDL() throws Exception {
        when(schema.getName()).thenReturn("mimer_store");
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(schema);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals("CREATE SCHEMA \"mimer_store\"", actions.get(0).getScript());
    }

    @Test
    public void refusesToDeleteABuiltInSystemSchema() {
        when(schema.isSystemSchema()).thenReturn(true);
        Assertions.assertFalse(manager.canDeleteObject(schema), "a system schema must not be droppable");
    }

    @Test
    public void allowsDeletingAnOrdinarySchema() {
        when(schema.isSystemSchema()).thenReturn(false);
        // Falls through to the same live permission check as canCreateObject (not asserted here) -
        // just check the system-schema short-circuit is ours and doesn't blow up.
        Assertions.assertDoesNotThrow(() -> manager.canDeleteObject(schema));
    }
}
