/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.ext.ui.IDataSourceUser;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider extends IDataSourceUser {

    boolean isConnected();

    boolean isRunning();

    void extractResultSetData(int offset, int maxRows);

}
