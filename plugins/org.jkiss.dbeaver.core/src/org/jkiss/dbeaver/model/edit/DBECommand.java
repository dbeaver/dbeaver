/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;

import java.util.Map;

/**
 * Object change command
 */
public interface DBECommand<OBJECT_TYPE extends DBPObject> {

    String getTitle();

    OBJECT_TYPE getObject();

    boolean isUndoable();

    /**
     * Validates command.
     * If command is fine then just returns, otherwise throws an exception
     * @throws DBException contains information about invalid command state
     */
    void validateCommand() throws DBException;

    void updateModel();

    DBECommand<?> merge(
        DBECommandQueue<OBJECT_TYPE> commandQueue,
        DBECommand<?> prevCommand,
        Map<String, Object> userParams);

    IDatabasePersistAction[] getPersistActions();

}
