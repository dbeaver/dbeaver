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
package org.jkiss.dbeaver.model.impl.auth;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.runtime.ServiceRegistry;
import org.jkiss.dbeaver.runtime.ui.UIServiceShellCommands;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthModelDatabaseShellCommandTest extends DBeaverUnitTest {
    @TempDir
    Path workingDirectory;

    private final AuthModelDatabaseShellCommand<AuthModelDatabaseShellCommandCredentials> authModel =
        new AuthModelDatabaseShellCommand<>();
    private final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
    private final Properties connectionProperties = new Properties();
    private DBPDataSource dataSource;
    private DBPDataSourceContainer container;
    private UIServiceShellCommands shellCommandsService;
    private ServiceRegistry serviceRegistry;
    private Field registryInstanceField;
    private Object originalRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        shellCommandsService = mock(UIServiceShellCommands.class);
        serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.getService(UIServiceShellCommands.class)).thenReturn(shellCommandsService);
        // Substitute the UI service in the headless test application and restore the registry after each test.
        registryInstanceField = ServiceRegistry.class.getDeclaredField("instance");
        registryInstanceField.setAccessible(true);
        originalRegistry = registryInstanceField.get(null);
        registryInstanceField.set(null, serviceRegistry);

        dataSource = mock(DBPDataSource.class);
        container = mock(DBPDataSourceContainer.class);
        DBPProject project = mock(DBPProject.class);
        when(dataSource.getContainer()).thenReturn(container);
        when(container.getDriver()).thenReturn(mock(DBPDriver.class));
        when(container.getProject()).thenReturn(project);
        when(container.getName()).thenReturn("Test connection");
        when(project.getName()).thenReturn("Test project");
        configuration.setUserName("test-user");
        configuration.setAuthProperty(
            AuthModelDatabaseShellCommandCredentials.PROP_WORKING_DIR, workingDirectory.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        registryInstanceField.set(null, originalRegistry);
    }

    @Test
    public void rejectedCommandDoesNotExecute() throws Exception {
        Path marker = workingDirectory.resolve("command-executed");
        String command = (RuntimeUtils.isWindows() ? "cmd /c " : "") + "mkdir \"" + marker + "\"";
        configuration.setAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND, command);
        var credentials = authModel.loadCredentials(container, configuration);
        DBException rejection = new DBException("Command execution declined");
        doThrow(rejection).when(shellCommandsService).validateByUser(any(), anyMap());

        DBException error = assertThrows(DBException.class, () ->
            authModel.initAuthentication(monitor, dataSource, credentials, configuration, connectionProperties));

        assertFalse(Files.exists(marker), "Declined password command must not execute");
        assertSame(rejection, error);
        assertNull(credentials.getUserPassword());
        assertFalse(connectionProperties.containsKey(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD));
    }

    @Test
    public void approvedCommandResolvesPassword() throws Exception {
        String command = RuntimeUtils.isWindows() ? "cmd /c echo test-password" : "/bin/echo test-password";
        configuration.setAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND, command);
        var credentials = authModel.loadCredentials(container, configuration);

        authModel.initAuthentication(monitor, dataSource, credentials, configuration, connectionProperties);

        verify(shellCommandsService).validateByUser(argThat(shellCommand ->
            command.equals(shellCommand.getCommand())
                && workingDirectory.toString().equals(shellCommand.getWorkingDirectory())
                && shellCommand.isEnabled()
                && shellCommand.isWaitProcessFinish()), eq(Map.of(
                    ModelMessages.auth_shell_command_context_project, "Test project",
                    ModelMessages.auth_shell_command_context_connection, "Test connection"
                )));
        assertEquals("test-password", credentials.getUserPassword());
        assertEquals("test-password", connectionProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD));
        assertEquals("test-user", connectionProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_USER));
    }

    @Test
    public void commandResolvesPasswordWithoutUiService() throws Exception {
        when(serviceRegistry.getService(UIServiceShellCommands.class)).thenReturn(null);
        configuration.setAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND,
            RuntimeUtils.isWindows() ? "cmd /c echo test-password" : "/bin/echo test-password");
        var credentials = authModel.loadCredentials(container, configuration);

        authModel.initAuthentication(monitor, dataSource, credentials, configuration, connectionProperties);

        assertEquals("test-password", connectionProperties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD));
        verifyNoInteractions(shellCommandsService);
    }

    @Test
    public void missingCommandDoesNotRequestApproval() {
        var credentials = authModel.loadCredentials(container, configuration);

        DBException error = assertThrows(DBException.class, () ->
            authModel.initAuthentication(monitor, dataSource, credentials, configuration, connectionProperties));

        assertEquals("Password command is not configured", error.getMessage());
        verifyNoInteractions(shellCommandsService);
    }
}
