/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider {

    DBPDataSource getDataSource();

    boolean isConnected();

    boolean isRunning();

    void extractResultSetData(int offset, int maxRows);

}
