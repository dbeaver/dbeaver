/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

/**
 * Database Object Command reflector.
 * Updates UI on command undo/redo actions.
 * Note: reflector isn't invoked after command creation (because command is created AFTER UI changes).
 */
public interface IDatabaseObjectCommandReflector<COMMAND extends IDatabaseObjectCommand> {

    void redoCommand(COMMAND command);

    void undoCommand(COMMAND command);

}
