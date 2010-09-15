/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDDataReciever;

/**
 * IResultSetProvider
 */
public interface IResultSetProvider extends IDataSourceProvider {

    DBPObject getResultSetSource();

    boolean isRunning();

    boolean isReadyToRun();

    void extractResultSetData(DBDDataReciever dataReciever, int offset, int maxRows);

}
