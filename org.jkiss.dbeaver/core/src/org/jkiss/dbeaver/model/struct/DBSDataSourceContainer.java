/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

/**
 * DBSDataSourceContainer
 */
public interface DBSDataSourceContainer extends DBSObject
{
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
     * Retrieves datasource instance
     * @return datasource or null (if not connected)
     */
    DBPDataSource getDataSource();

    /**
     * Opens connection (if needed) and acquires session from underlying data source.
     * Additionally sets custom properties on new obtained session.
     * @param forceNew force opening of new physical session
     * @return new session
     * @throws DBException on any DB error
     * @see org.jkiss.dbeaver.model.DBPDataSource#getSession(boolean)
     */
    DBCSession getSession(boolean forceNew) throws DBException;

    DBRRunnableContext getViewCallback();

    boolean isShowSystemObjects();

    boolean isConnected();

    void connect(Object source) throws DBException;

    void disconnect(Object source) throws DBException;

    void invalidate() throws DBException;

    void acquire(DBPDataSourceUser user);

    void release(DBPDataSourceUser user);

    void addListener(DBSListener listener);

    void removeListener(DBSListener listener);

    void fireEvent(DBSObjectAction action, DBSObject object);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    IPreferenceStore getPreferenceStore();
}
