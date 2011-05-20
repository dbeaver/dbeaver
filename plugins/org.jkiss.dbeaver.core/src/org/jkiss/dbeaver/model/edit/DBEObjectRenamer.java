/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object describer.
 * Provide object's rename functions
 */
public interface DBEObjectRenamer<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Describes object
     *
     *
     * @param commandContext command context. Implementation should add new command to it.
     * @param object object
     * @param newName new name. Not null only if UI somehow determine possible new name
     * @throws DBException on any error
     */
    void renameObject(DBECommandContext commandContext, OBJECT_TYPE object, String newName)
        throws DBException;

}