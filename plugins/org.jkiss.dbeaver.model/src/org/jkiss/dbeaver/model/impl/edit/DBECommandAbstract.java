/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBECommandAbstract<OBJECT_TYPE extends DBPObject> implements DBECommand<OBJECT_TYPE> {
    private final OBJECT_TYPE object;
    private final String title;
    private boolean isDisableSessionLogging = false;
    private boolean ignoreNestedCommands;

    public DBECommandAbstract(OBJECT_TYPE object, String title)
    {
        this.object = object;
        this.title = title;
    }

    @NotNull
    @Override
    public OBJECT_TYPE getObject()
    {
        return object;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public boolean isUndoable()
    {
        return true;
    }

    @Override
    public boolean isDisableSessionLogging() {
        return isDisableSessionLogging;
    }

    public void setDisableSessionLogging(boolean disableSessionLogging) {
        isDisableSessionLogging = disableSessionLogging;
    }

    @Override
    public boolean ignoreNestedCommands() {
        return ignoreNestedCommands;
    }

    public void setIgnoreNestedCommands(boolean ignoreNestedCommands) {
        this.ignoreNestedCommands = ignoreNestedCommands;
    }

    @Override
    public void validateCommand(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException
    {
        // do nothing by default
    }

    @Override
    public void updateModel()
    {
    }

    @NotNull
    @Override
    public DBECommand<?> merge(@Nullable DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams)
    {
        return this;
    }

    @NotNull
    @Override
    public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options) throws DBException
    {
        return null;
    }

}
