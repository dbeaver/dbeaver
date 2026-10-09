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
import org.jkiss.dbeaver.ext.mimer.model.MimerLibrary;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MimerLibraryManager#addObjectCreateActions} - the {@code CREATE LIBRARY} action wiring.
 * <p>
 * {@code addObjectDeleteActions} (the {@code DROP LIBRARY [CASCADE]} side) is deliberately not
 * covered here the same way - {@code SQLObjectEditor.ObjectDeleteCommand} is a {@code protected}
 * nested type (unlike the {@code public} {@code ObjectCreateCommand}), so it can't be mocked from
 * a test outside {@code org.jkiss.dbeaver.model.impl.sql.edit} without a compile error; no other
 * manager test in the whole repo works around this either (confirmed via a repo-wide grep for
 * {@code ObjectDeleteCommand}), so this is an accepted, pre-existing limitation of the framework,
 * not something specific to this class.
 *
 * @author Mimer Information Technology
 */
public class MimerLibraryManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private DBCExecutionContext executionContext;

    private MimerLibraryManager manager;
    private List<DBEPersistAction> actions;
    private Map<String, Object> options;

    @BeforeEach
    public void setUp() {
        manager = new MimerLibraryManager();
        actions = new ArrayList<>();
        options = new HashMap<>();
    }

    @Test
    public void createActionsEmitCreateLibraryDDL() throws Exception {
        MimerLibrary library = new MimerLibrary(dataSource, "NETLIB");
        library.setFileName("C:\\dev\\MyLib.dll");
        library.setLanguage("CLR");

        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(library);

        manager.addObjectCreateActions(monitor, executionContext, actions, command, options);

        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            "CREATE LIBRARY \"NETLIB\" FILE 'C:\\dev\\MyLib.dll' LANGUAGE CLR",
            actions.get(0).getScript());
    }
}
