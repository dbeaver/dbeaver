/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object renamer.
 * Provide object's rename function
 */
public interface DBEObjectRenamer<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Renames object
     * @param monitor progress monitor
     * @param newName new name
     * @throws DBException on any error
     */
    void renameObject(DBRProgressMonitor monitor, String newName)
        throws DBException;

}