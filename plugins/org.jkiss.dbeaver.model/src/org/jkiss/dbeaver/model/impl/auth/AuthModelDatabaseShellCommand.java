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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * Auth model that obtains the database password by running a shell command at connect time.
 *
 * The username is stored like the native model; the command is stored as an auth property.
 * No password is ever persisted - it is resolved on each connect from the command's stdout.
 */
public class AuthModelDatabaseShellCommand<CREDENTIALS extends AuthModelDatabaseShellCommandCredentials>
    extends AuthModelDatabaseNative<CREDENTIALS> {

    public static final String ID = "shell_command";

    private static final int COMMAND_TIMEOUT_MS = 60_000;

    @NotNull
    @Override
    public CREDENTIALS createCredentials() {
        return (CREDENTIALS) new AuthModelDatabaseShellCommandCredentials();
    }

    @NotNull
    @Override
    public CREDENTIALS loadCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration) {
        CREDENTIALS credentials = super.loadCredentials(dataSource, configuration);
        credentials.setCommand(configuration.getAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND));
        return credentials;
    }

    @Override
    public void saveCredentials(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull CREDENTIALS credentials
    ) {
        configuration.setAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND, credentials.getCommand());
        // Password is produced at runtime, never persisted
        credentials.setUserPassword(null);
        super.saveCredentials(dataSource, configuration, credentials);
    }

    @NotNull
    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull CREDENTIALS credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProps
    ) throws DBException {
        credentials.setUserPassword(resolvePassword(monitor, dataSource.getContainer(), credentials));
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProps);
    }

    // Password field is not entered manually - it comes from the command output
    @Override
    public boolean isUserPasswordApplicable() {
        return false;
    }

    @NotNull
    private String resolvePassword(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull CREDENTIALS credentials
    ) throws DBException {
        String commandLine = credentials.getCommand();
        if (CommonUtils.isEmptyTrimmed(commandLine)) {
            throw new DBException("Password command is not configured");
        }

        monitor.subTask("Resolve password from shell command");
        DBRShellCommand command = new DBRShellCommand(commandLine);
        command.setEnabled(true);
        command.setWaitProcessFinish(true);
        DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, container.getVariablesResolver(true));
        try {
            processDescriptor.execute();
            int exitCode = processDescriptor.waitFor(COMMAND_TIMEOUT_MS);
            String output = CommonUtils.notEmpty(processDescriptor.dumpOutput());
            if (exitCode != 0) {
                String errors = CommonUtils.notEmpty(processDescriptor.dumpErrors()).trim();
                throw new DBException("Password command exited with code " + exitCode +
                    (errors.isEmpty() ? "" : ": " + errors));
            }
            String password = output.replace("\r", "");
            int newline = password.indexOf('\n');
            if (newline >= 0) {
                password = password.substring(0, newline);
            }
            password = password.trim();
            if (password.isEmpty()) {
                throw new DBException("Password command returned empty output");
            }
            return password;
        } finally {
            if (processDescriptor.isRunning()) {
                processDescriptor.terminate();
            }
        }
    }
}
