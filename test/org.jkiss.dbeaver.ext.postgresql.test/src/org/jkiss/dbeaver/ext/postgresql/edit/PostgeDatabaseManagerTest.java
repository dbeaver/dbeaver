/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgeDatabaseManagerTest extends DBeaverUnitTest {

    private PostgreDatabaseManager databaseManager;

    @Mock
    private DBRProgressMonitor monitor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PostgreDatabase object;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PostgreDataSource dataSource;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DBPDataSourceContainer container;

    private DBPConnectionConfiguration configuration;

    private List<DBEPersistAction> actions;

    private Map<String, Object> options;

    @Before
    public void setUp() {
        //instances
        configuration = new DBPConnectionConfiguration();
        actions = new ArrayList<>();
        options = new HashMap<>();

        //mocks
        when(object.getDataSource()).thenReturn(dataSource);
        when(dataSource.getContainer()).thenReturn(container);
        when(container.getActualConnectionConfiguration()).thenReturn(configuration);

        //test instance
        databaseManager = new PostgreDatabaseManager();
    }

    @Test
    public void whenShowAllDBsEnabledShouldCreateDB() throws Exception {
        //given
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(object);
        configuration.setProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB, "true");
        //when
        databaseManager.addObjectCreateActions(monitor, mock(DBCExecutionContext.class), actions, command, options);
        //then
        assertFalse(actions.isEmpty());
    }

    @Test
    public void whenShowAllDBsDisabledShouldThrowInDBCreation() {
        //when
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(object);
        configuration.setProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB, "false");
        //then
        assertThrows(DBException.class,
            () -> databaseManager.addObjectCreateActions(monitor, mock(DBCExecutionContext.class), actions, command, options));
        assertTrue(actions.isEmpty());
    }

    @Test
    public void whenShowAllDBsNotSetShouldThrowInDBCreation() {
        //when
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(object);
        //then
        assertThrows(DBException.class,
            () -> databaseManager.addObjectCreateActions(monitor, mock(DBCExecutionContext.class), actions, command, options));
        assertTrue(actions.isEmpty());
    }
}
