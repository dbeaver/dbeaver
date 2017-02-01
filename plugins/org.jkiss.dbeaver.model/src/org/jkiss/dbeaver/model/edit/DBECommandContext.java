/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.Map;

/**
 * Command context.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBECommandContext extends DBPContextProvider {

    boolean isDirty();

    DBECommand getUndoCommand();

    DBECommand getRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges();

    void undoCommand();

    void redoCommand();

    Collection<? extends DBECommand<?>> getFinalCommands();

    Collection<? extends DBECommand<?>> getUndoCommands();

    Collection<DBPObject> getEditedObjects();

    void addCommand(DBECommand command, DBECommandReflector reflector);

    void addCommand(DBECommand command, DBECommandReflector reflector, boolean execute);

    //void addCommandBatch(List<DBECommand> commands, DBECommandReflector reflector, boolean execute);

    void removeCommand(DBECommand<?> command);

    void updateCommand(DBECommand<?> command, DBECommandReflector commandReflector);

    void addCommandListener(DBECommandListener listener);

    void removeCommandListener(DBECommandListener listener);

    Map<Object, Object> getUserParams();

}