/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Object commander.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBEObjectCommander extends IDataSourceContainerProvider {

    boolean isDirty();

    boolean canUndoCommand();

    boolean canRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges();

    void undoCommand();

    void redoCommand();

    Collection<? extends DBECommand<?>> getCommands();

    void addCommand(DBECommand<?> command, DBECommandReflector<?, DBECommand<?>> reflector);

    void removeCommand(DBECommand<?> command);

    void updateCommand(DBECommand<?> command);

    void addCommandListener(DBECommandListener listener);

    void removeCommandListener(DBECommandListener listener);
}