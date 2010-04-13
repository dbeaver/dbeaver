/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider extends IDataSourceUser {

    boolean isConnected();

    void extractResultSetData(int offset);

}
