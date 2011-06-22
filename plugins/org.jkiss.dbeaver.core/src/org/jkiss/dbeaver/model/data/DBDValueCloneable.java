/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBDContentClonable
 */
public interface DBDValueCloneable extends DBDValue {

    /**
     * Makes exact copy of content object
     * @return copy  @param monitor
     */
    DBDValueCloneable cloneValue(DBRProgressMonitor monitor)
        throws DBCException;

}