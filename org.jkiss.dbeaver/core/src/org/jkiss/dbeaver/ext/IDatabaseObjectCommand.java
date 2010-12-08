/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object change command
 */
public interface IDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> {

    public enum MergeResult {
        NONE,
        CANCEL_PREVIOUS,
        CANCEL_BOTH,
        ABSORBED
    }

    String getTitle();

    Image getIcon();

    /**
     * Validates command.
     * If command is fine then just returns, otherwise throws an exception
     * @throws DBException contains information about invalid command state
     * @param object
     */
    void validateCommand(OBJECT_TYPE object) throws DBException;

    void updateModel(OBJECT_TYPE object);

    MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand);

    IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object);

}
