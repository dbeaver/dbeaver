/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBSEntity
 */
public interface DBSEntity extends DBSObject
{


    /**
     * Refresh entity's (and all of it's children) state
     * @param monitor progress monitor
     * @return true if object refreshed and false if parent object have to be refreshed
     * to perform requested operation
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException;

}
