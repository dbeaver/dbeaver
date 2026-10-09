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

import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
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
 * {@link MimerUserManager}'s create-action wiring - "Create New User" emits {@code CREATE IDENT}
 * plus one {@code GRANT MEMBER} per group picked in the wizard, all in the same create step (see
 * {@link MimerUser#buildGroupGrantDDL}).
 * <p>
 * {@code addObjectModifyActions} (Password-only ALTER, plus Comment) isn't covered - see {@code
 * MimerSchemaManagerTest}'s Javadoc for why ({@code ObjectChangeCommand} can't be mocked across
 * this project's OSGi bundle boundaries, confirmed live).
 *
 * @author Mimer Information Technology
 */
public class MimerUserManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private DBCExecutionContext executionContext;

    private final MimerUserManager manager = new MimerUserManager();

    @Test
    public void createActionsEmitOnlyCreateIdentWithNoGroupsPicked() throws Exception {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(user);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER", actions.get(0).getScript());
    }

    @Test
    public void createActionsAppendOneGrantMemberPerPickedGroup() throws Exception {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setInitialGroups(List.of("developers", "admins"));
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(user);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(3, actions.size());
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER", actions.get(0).getScript());
        Assertions.assertEquals("GRANT MEMBER ON GROUP \"developers\" TO \"fredrik\"", actions.get(1).getScript());
        Assertions.assertEquals("GRANT MEMBER ON GROUP \"admins\" TO \"fredrik\"", actions.get(2).getScript());
    }
}
