/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.Map;

/**
 * Command context.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBECommandContext extends IDataSourceProvider {

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