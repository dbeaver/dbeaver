/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSDataSourceContainer
 */
public interface DBSDataSourceContainer extends DBSObject, DBDPreferences
{
    /**
     * Container unique ID
     * @return id
     */
    String getId();

    /**
     * Associated driver
     * @return driver descriptor reference
     */
    DBPDriver getDriver();

    /**
     * Connection info
     * @return connection details
     */
    DBPConnectionInfo getConnectionInfo();

    /**
     * Actual connection info. Contains actual parameters used to connect to this datasource.
     * Differs from getConnectionInfo() in case if tunnel or proxy was used.
     * @return actual connection info.
     */
    DBPConnectionInfo getActualConnectionInfo();

    /**
     * Retrieves datasource instance
     * @return datasource or null (if not connected)
     */
    @Override
    DBPDataSource getDataSource();

    boolean isShowSystemObjects();

    boolean isConnectionReadOnly();

    /**
     * Search for object filter which corresponds specified object type and parent object.
     * Search filter which match any super class or interface implemented by specified type.
     * @param type object type
     * @param parentObject parent object (in DBS objects hierarchy)
     * @param firstMatch if true then this function will return first found filter
     *   even if it is disabled. Won't return default filter of parent object specified.
     *   Generally this function used by filter editors
     * @return
     */
    DBSObjectFilter getObjectFilter(Class<?> type, DBSObject parentObject);

    DBPClientHome getClientHome();

    boolean isConnected();

    /**
     * Connects to datasource.
     * This is async method and returns immediately.
     * Connection will be opened in separate job, so no progress monitor is required.
     * @param monitor progress monitor
     * @throws DBException on error
     */
    void connect(DBRProgressMonitor monitor) throws DBException;

    /**
     * Disconnects from datasource.
     * This is async method and returns immediately.
     * Connection will be closed in separate job, so no progress monitor is required.
     * @param monitor progress monitor
     * @throws DBException on error
     * @return true on disconnect, false if disconnect action was canceled
     */
    boolean disconnect(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reconnects datasource.
     * @param monitor progress monitor
     * @return true on reconnect, false if reconnect action was canceled
     * @throws org.jkiss.dbeaver.DBException on any DB error
     */
    boolean reconnect(DBRProgressMonitor monitor) throws DBException;

    Collection<DBPDataSourceUser> getUsers();

    void acquire(DBPDataSourceUser user);

    void release(DBPDataSourceUser user);

    void fireEvent(DBPEvent event);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    IPreferenceStore getPreferenceStore();

    DBPDataSourceRegistry getRegistry();

    DBPKeywordManager getKeywordManager();

}
