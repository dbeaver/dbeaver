/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public interface DBCExecutionContext {

    DBPDataSource getDataSource();
    
    DBRProgressMonitor getProgressMonitor();

    DBPTransactionManager getTransactionManager();

    DBCStatement prepareStatement(
        String query,
        boolean scrollable,
        boolean updatable) throws DBCException;

    void close();

}
