/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object describer.
 * Provide object's change description function
 */
public interface DBEObjectDescriber<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    /**
     * Renames object
     * @param monitor progress monitor
     * @param newDescription new name
     * @throws org.jkiss.dbeaver.DBException on any error
     */
    void setObjectDescription(DBRProgressMonitor monitor, String newDescription)
        throws DBException;

}