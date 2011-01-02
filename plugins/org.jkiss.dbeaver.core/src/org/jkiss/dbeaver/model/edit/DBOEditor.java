/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * DBOManager
 */
public interface DBOEditor<OBJECT_TYPE extends DBSObject> extends DBOManager<OBJECT_TYPE> {

    boolean isDirty();

    boolean canUndoCommand();

    boolean canRedoCommand();

    void saveChanges(DBRProgressMonitor monitor) throws DBException;

    void resetChanges();

    void undoCommand();

    void redoCommand();

    Collection<? extends DBOCommand<OBJECT_TYPE>> getCommands();

    <COMMAND extends DBOCommand<OBJECT_TYPE>>
    void addCommand(COMMAND command, DBOCommandReflector<OBJECT_TYPE, COMMAND> reflector);

    <COMMAND extends DBOCommand<OBJECT_TYPE>>
    void removeCommand(COMMAND command);

    <COMMAND extends DBOCommand<OBJECT_TYPE>>
    void updateCommand(COMMAND command);

    void addCommandListener(DBOCommandListener listener);

    void removeCommandListener(DBOCommandListener listener);
}