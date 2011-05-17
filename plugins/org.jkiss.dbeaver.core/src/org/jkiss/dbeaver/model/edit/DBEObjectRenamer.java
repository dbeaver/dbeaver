/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object describer.
 * Provide object's rename function
 */
public interface DBEObjectRenamer<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Renames object
     *
     *
     * @param monitor progress monitor
     * @param newName new name
     * @param newDescription new description
     * @throws DBException on any error
     */
    void describeObject(DBRProgressMonitor monitor, OBJECT_TYPE object, String newName, String newDescription)
        throws DBException;

}