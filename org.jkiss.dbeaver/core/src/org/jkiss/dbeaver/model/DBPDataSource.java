/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Data Source.
 * Root object of all database structure and data objects.
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
     * Acquires new database session
     * @param monitor
     *@param forceNew if true opens new physical session, otherwise uses datasourse internal session  @return new session
     * @throws DBException on any DB error
     */
    DBCSession getSession(DBRProgressMonitor monitor, boolean forceNew) throws DBException;

    /**
     * Executes test query agains connected database.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor
     */
    void checkConnection(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reads base metadata from remote database or do any neccessary initialization routines.
     * @throws DBException on any DB error  @param monitor progress monitor
     */
    void initialize(DBRProgressMonitor monitor) throws DBException;

    /**
     * Refresh data source
     * @throws DBException on any DB error  @param monitor progress monitor
     */
    void refreshDataSource(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Closes datasource
     */
    void close();

}
