/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBSEntitySelector
 */
public interface DBSObjectSelector
{

    boolean supportsObjectSelect();

    DBSObject getSelectedObject();

    void selectObject(DBRProgressMonitor monitor, DBSObject object) throws DBException;

}