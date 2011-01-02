/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Database Object Command reflector.
 * Updates UI on command undo/redo actions.
 * Note: reflector isn't invoked after command creation (because command is created AFTER UI changes).
 */
public interface DBOCommandReflector<OBJECT_TYPE extends DBSObject, COMMAND extends DBOCommand<OBJECT_TYPE>> {

    void redoCommand(COMMAND command);

    void undoCommand(COMMAND command);

}
