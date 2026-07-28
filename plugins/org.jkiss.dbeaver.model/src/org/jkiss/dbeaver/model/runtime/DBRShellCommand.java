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

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Objects;

/**
 * DBRShellCommand
 */
public class DBRShellCommand {

    public static final int WAIT_PROCESS_TIMEOUT_MAX_SELECTION = 99999;
    public static final int WAIT_PROCESS_TIMEOUT_FOREVER = -1;

    private String command;
    private List<String> commandParams;
    private boolean enabled;
    private boolean showProcessPanel = true;
    private boolean waitProcessFinish;
    private int waitProcessTimeoutMs = WAIT_PROCESS_TIMEOUT_FOREVER;
    private boolean terminateAtDisconnect = true;
    private int pauseAfterExecute = 0;

    @Nullable
    private String workingDirectory;

    public DBRShellCommand(String command) {
        this.command = command;
    }

    public DBRShellCommand(List<String> commandParams) {
        this.commandParams = commandParams;
    }

    public DBRShellCommand(DBRShellCommand command) {
        this.command = command.command;
        this.commandParams = command.commandParams;
        this.enabled = command.enabled;
        this.showProcessPanel = command.showProcessPanel;
        this.waitProcessFinish = command.waitProcessFinish;
        this.waitProcessTimeoutMs = command.waitProcessTimeoutMs;
        this.terminateAtDisconnect = command.terminateAtDisconnect;
        this.pauseAfterExecute = command.pauseAfterExecute;
        this.workingDirectory = command.workingDirectory;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isBlank() {
        return CommonUtils.isEmpty(this.command);
    }

    public List<String> getCommandParams() {
        return commandParams;
    }

    public void setCommandParams(List<String> commandParams) {
        this.commandParams = commandParams;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowProcessPanel() {
        return showProcessPanel;
    }

    public void setShowProcessPanel(boolean showProcessPanel) {
        this.showProcessPanel = showProcessPanel;
    }

    public boolean isWaitProcessFinish() {
        return waitProcessFinish;
    }

    public void setWaitProcessFinish(boolean waitProcessFinish) {
        this.waitProcessFinish = waitProcessFinish;
    }

    public int getWaitProcessTimeoutMs() {
        return waitProcessTimeoutMs;
    }

    public void setWaitProcessTimeoutMs(int waitProcessTimeoutMs) {
        this.waitProcessTimeoutMs = waitProcessTimeoutMs;
    }

    public boolean isTerminateAtDisconnect() {
        return terminateAtDisconnect;
    }

    public void setTerminateAtDisconnect(boolean terminateAtDisconnect) {
        this.terminateAtDisconnect = terminateAtDisconnect;
    }

    public int getPauseAfterExecute() {
        return pauseAfterExecute;
    }

    public void setPauseAfterExecute(int pauseAfterExecute) {
        this.pauseAfterExecute = pauseAfterExecute;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBRShellCommand source)) {
            return false;
        }
        return
            CommonUtils.equalObjects(this.command, source.command) &&
            CommonUtils.equalObjects(this.commandParams, source.commandParams) &&
                this.enabled == source.enabled &&
                this.showProcessPanel == source.showProcessPanel &&
                this.waitProcessFinish == source.waitProcessFinish &&
                this.waitProcessTimeoutMs == source.waitProcessTimeoutMs &&
                this.terminateAtDisconnect == source.terminateAtDisconnect &&
                this.pauseAfterExecute == source.pauseAfterExecute &&
                CommonUtils.equalObjects(this.workingDirectory, source.workingDirectory);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(command);
        result = 31 * result + Objects.hashCode(commandParams);
        result = 31 * result + Boolean.hashCode(enabled);
        result = 31 * result + Boolean.hashCode(showProcessPanel);
        result = 31 * result + Boolean.hashCode(waitProcessFinish);
        result = 31 * result + waitProcessTimeoutMs;
        result = 31 * result + Boolean.hashCode(terminateAtDisconnect);
        result = 31 * result + pauseAfterExecute;
        result = 31 * result + Objects.hashCode(workingDirectory);
        return result;
    }
}
