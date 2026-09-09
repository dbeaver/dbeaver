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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthModelDatabaseShellCommandTest extends DBeaverUnitTest {

    private static final int LARGE_OUTPUT_LENGTH = 6200;

    @Test
    public void resolvesPasswordWhenCommandOutputExceedsPipeBuffer() throws Exception {
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        DBPDataSource dataSource = mock(DBPDataSource.class);
        DBPDriver driver = mock(DBPDriver.class);
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        when(dataSource.getContainer()).thenReturn(container);
        when(container.getDriver()).thenReturn(driver);
        when(container.getVariablesResolver(true)).thenReturn(null);
        when(driver.isAllowsEmptyPassword()).thenReturn(false);

        AuthModelDatabaseShellCommandCredentials credentials = new AuthModelDatabaseShellCommandCredentials();
        credentials.setCommand(createLargeOutputCommand());
        credentials.setCommandTimeoutMs(10_000);
        credentials.setUserName("test-user");

        new AuthModelDatabaseShellCommand<>().initAuthentication(
            monitor,
            dataSource,
            credentials,
            new DBPConnectionConfiguration(),
            new Properties());

        Assertions.assertEquals("A".repeat(LARGE_OUTPUT_LENGTH), credentials.getUserPassword());
    }

    private static String createLargeOutputCommand() {
        if (RuntimeUtils.isWindows()) {
            return "powershell.exe -NoProfile -Command \"[Console]::Write('A' * " + LARGE_OUTPUT_LENGTH + ")\"";
        }
        return "/usr/bin/printf '%s' '" + "A".repeat(LARGE_OUTPUT_LENGTH) + "'";
    }

}
