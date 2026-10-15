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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * Native auth credentials whose password is produced by a shell command at connect time.
 *
 * The command is persisted (as an auth property); the resolved password is runtime-only
 * and never stored.
 */
public class AuthModelDatabaseShellCommandCredentials extends AuthModelDatabaseNativeCredentials {

    public static final String PROP_COMMAND = "passwordCommand";
    public static final String PROP_WORKING_DIR = "passwordCommandWorkingDir";
    public static final String PROP_TIMEOUT = "passwordCommandTimeoutMs";

    public static final int DEFAULT_TIMEOUT_MS = 60_000;

    private String command;
    private String workingDirectory;
    private int commandTimeoutMs = DEFAULT_TIMEOUT_MS;

    @Nullable
    @Property(order = 10)
    public String getCommand() {
        return command;
    }

    public void setCommand(@Nullable String command) {
        this.command = command;
    }

    @Nullable
    @Property(order = 11)
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Property(order = 12)
    public int getCommandTimeoutMs() {
        return commandTimeoutMs;
    }

    public void setCommandTimeoutMs(int commandTimeoutMs) {
        this.commandTimeoutMs = commandTimeoutMs > 0 ? commandTimeoutMs : DEFAULT_TIMEOUT_MS;
    }

    @Override
    public boolean isComplete() {
        return !CommonUtils.isEmptyTrimmed(command);
    }
}
