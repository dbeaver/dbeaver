/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandListener;

/**
 * Command adapter
 */
public abstract class DBECommandAdapter implements DBECommandListener {

    @Override
    public void onCommandChange(DBECommand<?> command)
    {
    }

    @Override
    public void onSave()
    {
    }

    @Override
    public void onReset()
    {
    }

    @Override
    public void onCommandDo(DBECommand<?> command)
    {
    }

    @Override
    public void onCommandUndo(DBECommand<?> command)
    {
    }
}
