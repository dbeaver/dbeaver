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

    DBRRunnableContext getViewCallback();

    boolean isShowSystemObjects();

    boolean isConnected();

    void connect(Object source) throws DBException;

    void disconnect(Object source) throws DBException;

    void invalidate() throws DBException;

    void acquire(DBPDataSourceUser user);

    void release(DBPDataSourceUser user);

    /**
     * Preference store associated with this datasource
     * @return preference store
     */
    IPreferenceStore getPreferenceStore();
}
