/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public interface DBCExecutionContext {

    String getTaskTitle();

    DBPDataSource getDataSource();

    boolean isConnected();

    DBRProgressMonitor getProgressMonitor();

    DBCTransactionManager getTransactionManager();

    public DBDDataFormatterProfile getDataFormatterProfile();

    DBCStatement prepareStatement(
        String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

    void close();

}
