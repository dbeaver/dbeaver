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
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

/**
 * DBSDataSourceContainer
 */
public interface DBSDataSourceContainer extends DBSEntity
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
     * Retrieves datasource instance
     * @return datasource or null (if not connected)
     */
    DBPDataSource getDataSource();

    DBRRunnableContext getViewCallback();

    boolean isShowSystemObjects();

    boolean isConnected();

    /**
     * Connects to datasource.
     * This is async method and returns immediately.
     * Connection will be opened in separate job, so no progress monitor is required.
     * @param source
     * @throws DBException
     */
    void connect(Object source) throws DBException;

    /**
     * Disconnects from datasource.
     * This is async method and returns immediately.
     * Connection will be closed in separate job, so no progress monitor is required.
     * @param source
     * @throws DBException
     */
    void disconnect(Object source) throws DBException;

    void invalidate(Object source) throws DBException;

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
