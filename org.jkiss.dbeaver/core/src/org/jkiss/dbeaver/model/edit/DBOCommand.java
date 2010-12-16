/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Object change command
 */
public interface DBOCommand<OBJECT_TYPE extends DBSObject> {

    String getTitle();

    Image getIcon();

    boolean isUndoable();

    /**
     * Validates command.
     * If command is fine then just returns, otherwise throws an exception
     * @throws DBException contains information about invalid command state
     * @param object
     */
    void validateCommand(OBJECT_TYPE object) throws DBException;

    void updateModel(OBJECT_TYPE object);

    DBOCommand<OBJECT_TYPE> merge(
        DBOCommand<OBJECT_TYPE> prevCommand,
        Map<String, Object> userParams);

    IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object);

}
