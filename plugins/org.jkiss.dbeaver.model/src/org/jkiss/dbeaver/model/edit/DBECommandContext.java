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