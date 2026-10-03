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

import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
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
 * {@link MimerDatabankManager}'s create-action wiring - delegates straight to {@link
 * MimerDatabank#buildCreateDDL()}, whose own branching (which optional clauses appear) is
 * covered in full by {@code MimerDatabankTest}; this just checks the wiring calls it and wraps
 * the result in one action.
 * <p>
 * {@code addObjectModifyActions} (the property-set collection feeding {@code buildAlterDDL}, plus
 * the separate Change-state / COMMENT ON handling) isn't covered - see {@code MimerSchemaManagerTest}'s
 * Javadoc for why ({@code ObjectChangeCommand} can't be mocked across this project's OSGi bundle
 * boundaries, confirmed live, not just a compile-time access problem).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private DBCExecutionContext executionContext;

    private final MimerDatabankManager manager = new MimerDatabankManager();

    @Test
    public void createActionDelegatesToTheDatabankSOwnBuildCreateDDL() throws Exception {
        MimerDatabank databank = new MimerDatabank(dataSource, "MYDB");
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(databank);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(databank.buildCreateDDL(), actions.get(0).getScript());
    }

    @Test
    public void refusesToDeleteABuiltInSystemDatabank() {
        for (String name : new String[]{"SQLDB", "TRANSDB", "LOGDB", "SYSDB"}) {
            Assertions.assertFalse(
                manager.canDeleteObject(new MimerDatabank(dataSource, name)),
                () -> name + " must not be droppable");
        }
        // A normal databank falls through to the same live permission check as canCreateObject
        // (not asserted here) - just check the system-databank short-circuit is ours.
        Assertions.assertDoesNotThrow(() -> manager.canDeleteObject(new MimerDatabank(dataSource, "MYDB")));
    }
}
