/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntity
 */
public interface DBSEntity<ATTR extends DBSEntityAttribute> extends DBSObject
{
/*
    Collection<ATTR> getAttributes(DBRProgressMonitor monitor)
        throws DBException;

    ATTR getAttribute(DBRProgressMonitor monitor, String name)
        throws DBException;
*/

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
