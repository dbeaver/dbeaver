package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Refreshable object.
 * allows refresh of object's content
 */
public interface DBPRefreshableObject
{

    boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException;

}
