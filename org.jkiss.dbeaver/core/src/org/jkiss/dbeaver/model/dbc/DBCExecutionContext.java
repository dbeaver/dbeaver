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

    /**
     * Data source of this context
     * @return data source
     */
    DBPDataSource getDataSource();

    /**
     * Performs check that this context is really connected to remote database
     * @return connected state
     */
    boolean isConnected();

    /**
     * Context's progress monitor.
     * Each context has it's progress monitor which is passed at context creation time and never changes.
     * @return progress monitor
     */
    DBRProgressMonitor getProgressMonitor();

    /**
     * Associated transaction manager
     * @return transaction manager
     */
    DBCTransactionManager getTransactionManager();

    DBDDataFormatterProfile getDataFormatterProfile();

    void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile);

    DBCStatement prepareStatement(
        String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

    void close();

}
