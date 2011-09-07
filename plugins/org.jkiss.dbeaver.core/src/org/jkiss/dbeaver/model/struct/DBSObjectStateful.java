/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Object with state.
 */
public interface DBSObjectStateful extends DBSObject
{

    DBSObjectState getObjectState();

    void refreshObjectState(DBRProgressMonitor monitor)
        throws DBCException;

}
