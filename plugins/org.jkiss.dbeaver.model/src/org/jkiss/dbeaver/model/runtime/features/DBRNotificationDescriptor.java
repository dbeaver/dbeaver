/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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