/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.ext.IDataSourceProvider;

/**
 * IResultSetProvider
 */
public interface IResultSetProvider extends IDataSourceProvider {

    boolean isConnected();

    boolean isRunning();

    boolean isReadyToRun();

    void extractResultSetData(int offset, int maxRows);

}
