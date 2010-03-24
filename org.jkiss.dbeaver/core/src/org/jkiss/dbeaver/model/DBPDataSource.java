package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * DBPDataSource
 */
public interface DBPDataSource extends DBPObject
{
    /**
     * Datasource container
     * @return contaner implementation
     */
    DBSDataSourceContainer getContainer();

    DBPDataSourceInfo getInfo() throws DBException;

    /**
     * Acquires new database session
     * @param forceNew if true opens new physical session, otherwise uses datasourdei nternal session
     * @return new session
     * @throws DBException on any DB error
     */
    DBCSession getSession(boolean forceNew) throws DBException;

    /**
     * Executes test query agains connected database.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     */
    void checkConnection() throws DBException;

    /**
     * Reads base metadata from remote database or do any neccessary initialization routines.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void initialize(IProgressMonitor monitor) throws DBException;

    /**
     * Refresh data source
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void refreshDataSource(IProgressMonitor monitor)
        throws DBException;

    /**
     * Closes datasource
     */
    void close();

    /**
     * Cancels current operation
     */
    void cancelCurrentOperation();
}
