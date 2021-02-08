/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.runtime.features;

import org.jkiss.code.NotNull;

/**
 * DBeaver notification description
 */
public class DBRNotificationDescriptor {

    @NotNull
    private DBRNotificationAction action;
    private String soundFile;
    private String shellCommand;

    public DBRNotificationDescriptor() {
        this.action = DBRNotificationAction.NONE;
    }

    public DBRNotificationDescriptor(@NotNull DBRNotificationAction action, String soundFile, String shellCommand) {
        this.action = action;
        this.soundFile = soundFile;
        this.shellCommand = shellCommand;
    }

    @NotNull
    public DBRNotificationAction getAction() {
        return action;
    }

    public void setAction(@NotNull DBRNotificationAction action) {
        this.action = action;
    }

    public String getSoundFile() {
        return soundFile;
    }

    public void setSoundFile(String soundFile) {
        this.soundFile = soundFile;
    }

    public String getShellCommand() {
        return shellCommand;
    }

    public void setShellCommand(String shellCommand) {
        this.shellCommand = shellCommand;
    }
}