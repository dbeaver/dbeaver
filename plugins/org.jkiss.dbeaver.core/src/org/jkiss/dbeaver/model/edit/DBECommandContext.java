/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Command context.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBECommandContext extends IDataSourceContainerProvider {

    boolean isDirty();

    boolean canUndoCommand();

    boolean canRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges();

    void undoCommand();

    void redoCommand();

    Collection<? extends DBECommand<?>> getCommands();

    Collection<DBPObject> getEditedObjects();

    void addCommand(DBECommand<?> command, DBECommandReflector<?, DBECommand<?>> reflector);

    void addCommandBatch(List<DBECommand> commands);

    void removeCommand(DBECommand<?> command);

    void updateCommand(DBECommand<?> command);

    void addCommandListener(DBECommandListener listener);

    void removeCommandListener(DBECommandListener listener);

    Map<Object, Object> getUserParams();

}