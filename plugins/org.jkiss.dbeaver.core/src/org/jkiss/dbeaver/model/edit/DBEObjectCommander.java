/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Object commander.
 * Provides facilities for object edit commands, undo/redo, save/revert
 */
public interface DBEObjectCommander<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    boolean isDirty();

    boolean canUndoCommand();

    boolean canRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges();

    void undoCommand();

    void redoCommand();

    Collection<? extends DBECommand<OBJECT_TYPE>> getCommands();

    <COMMAND extends DBECommand<OBJECT_TYPE>>
    void addCommand(COMMAND command, DBECommandReflector<OBJECT_TYPE, COMMAND> reflector);

    <COMMAND extends DBECommand<OBJECT_TYPE>>
    void removeCommand(COMMAND command);

    <COMMAND extends DBECommand<OBJECT_TYPE>>
    void updateCommand(COMMAND command);

    void addCommandListener(DBECommandListener listener);

    void removeCommandListener(DBECommandListener listener);
}