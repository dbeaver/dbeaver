/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

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

    /**
     * Context's purpose
     * @return purpose
     */
    DBCExecutionPurpose getPurpose();

    /**
     * Gets current context's data formatter profile
     * @return profile
     */
    DBDDataFormatterProfile getDataFormatterProfile();

    /**
     * Sets current context's data formatter profile
     */
    void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile);

    /**
     * Prepares statements
     */
    DBCStatement prepareStatement(
        String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

    /**
     * Closes context. No exceptions could be throws from this method.
     */
    void close();

}
