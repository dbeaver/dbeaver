/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
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
     * @return container implementation
     */
    DBSDataSourceContainer getContainer();

    /**
     * Datasource information/options
     * Info SHOULD be read at datasource initialization stage and should be cached and available
     * at the moment of invocation of this function.
     * @return datasource info.
     */
    DBPDataSourceInfo getInfo();

    /**
     * Checks this datasource is really connected to remote database.
     * Usually DBSDataSourceContainer.getDataSource() returns datasource only if datasource is connected.
     * But in some cases (e.g. connection invalidation) datasource remains disconnected for some period of time.
     * @return true if underlying connection is alive.
     */
    boolean isConnected();

    /**
     * Opens new execution context
     * @param monitor progress monitor
     * @param purpose context purpose
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task);

    /**
     * Opens new isolated execution context.
     * @param monitor progress monitor
     * @param purpose context purpose
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task);

    /**
     * checks connection is alive and reconnects if needed.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor progress monitor
     */
    void invalidateConnection(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reads base metadata from remote database or do any necessarily initialization routines.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void initialize(DBRProgressMonitor monitor) throws DBException;

    /**
     * Closes datasource
     * @param monitor progress monitor
     */
    void close(DBRProgressMonitor monitor);

}
