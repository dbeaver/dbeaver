/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Data Source.
 * Root object of all database structure and data objects.
 * Note: do not store direct references on datasource objects in any GUI components -
 * datasource instance could be refreshed at any time. Obtain references on datasource only
 * from DBSObject or IDataSourceProvider interfaces.
 */
public interface DBPDataSource extends DBPObject
{
    /**
     * Datasource container
     * @return contaner implementation
     */
    DBSDataSourceContainer getContainer();

    DBPDataSourceInfo getInfo();

    /**
     * Opens new execution context
     * @param monitor progress monitor
     * @return execution context
     */
    DBCExecutionContext openContext(DBRProgressMonitor monitor);

    /**
     * Opens new execution context
     * @param monitor progress monitor
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openContext(DBRProgressMonitor monitor, String task);

    /**
     * Opens new isolated execution context.
     * @param monitor progress monitor
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, String task);

    /**
     * checks connection is alive and reconnects if needed.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor
     */
    void invalidateConnection(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reads base metadata from remote database or do any neccessary initialization routines.
     * @throws DBException on any DB error  @param monitor progress monitor
     */
    void initialize(DBRProgressMonitor monitor) throws DBException;

    /**
     * Closes datasource
     * @param monitor
     */
    void close(DBRProgressMonitor monitor);

}
