/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.data.DBDDataReciever;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;

/**
 * Data container.
 * Provides facilities to query object for data
 */
public interface DBSDataContainer extends DBSObject {

    int pumpAllData(
        DBRProgressMonitor monitor,
        DBDDataReciever dataReciever,
        int firstRow,
        int maxRows)
        throws DBException;

}
