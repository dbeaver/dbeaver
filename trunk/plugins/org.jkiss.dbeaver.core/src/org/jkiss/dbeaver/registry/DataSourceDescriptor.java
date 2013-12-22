/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBEPrivateObjectEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.impl.EmptyKeywordManager;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.qm.meta.QMMCollector;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.*;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor
    implements
        DBSDataSourceContainer,
        IObjectImageProvider,
        IAdaptable,
        DBEPrivateObjectEditor,
        DBSObjectStateful,
        DBPGuardedObject,
        DBPRefreshableObject
{
    static final Log log = LogFactory.getLog(DataSourceDescriptor.class);

    public static class FilterMapping {
        public final Class<?> type;
        public DBSObjectFilter defaultFilter;
        public Map<String, DBSObjectFilter> customFilters = new HashMap<String, DBSObjectFilter>();

        FilterMapping(Class<?> type)
        {
            this.type = type;
        }

        FilterMapping(FilterMapping mapping)
        {
            this.type = mapping.type;
            this.defaultFilter = mapping.defaultFilter == null ? null : new DBSObjectFilter(mapping.defaultFilter);
            for (Map.Entry<String, DBSObjectFilter> entry : mapping.customFilters.entrySet()) {
                this.customFilters.put(entry.getKey(), new DBSObjectFilter(entry.getValue()));
            }
        }

        public DBSObjectFilter getFilter(DBSObject parentObject, boolean firstMatch)
        {
            if (parentObject == null) {
                return defaultFilter;
            }
            if (!customFilters.isEmpty()) {
                String objectID = DBUtils.getObjectUniqueName(parentObject);
                DBSObjectFilter filter = customFilters.get(objectID);
                if ((filter != null && filter.isEnabled()) || firstMatch) {
                    return filter;
                }
            }

            return firstMatch ? null : defaultFilter;
        }
    }

    private DataSourceRegistry registry;
    private DriverDescriptor driver;
    private DBPConnectionInfo connectionInfo;
    private DBPConnectionInfo tunnelConnectionInfo;

    private String id;
    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private boolean connectionReadOnly;
    private final Map<Class<?>, FilterMapping> filterMap = new IdentityHashMap<Class<?>, FilterMapping>();
    private Date createDate;
    private Date updateDate;
    private Date loginDate;
    private DBDDataFormatterProfile formatterProfile;
    private DBPClientHome clientHome;
    private DataSourcePreferenceStore preferenceStore;

    private DBPDataSource dataSource;

    private final List<DBPDataSourceUser> users = new ArrayList<DBPDataSourceUser>();

    private DataSourceKeywordManager keywordManager;

    private volatile boolean connectFailed = false;
    private volatile Date connectTime = null;
    private final List<DBRProcessDescriptor> childProcesses = new ArrayList<DBRProcessDescriptor>();
    private DBWTunnel tunnel;
    private String folderPath;

    private DBVModel virtualModel;

    public DataSourceDescriptor(
        DataSourceRegistry registry,
        String id,
        DriverDescriptor driver,
        DBPConnectionInfo connectionInfo)
    {
        this.registry = registry;
        this.id = id;
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        this.createDate = new Date();
        this.preferenceStore = new DataSourcePreferenceStore(this);
        this.virtualModel = new DBVModel(this);

        this.driver.addUser(this);
        refreshConnectionInfo();
    }

    void refreshConnectionInfo()
    {
        if (!CommonUtils.isEmpty(connectionInfo.getClientHomeId())) {
            this.clientHome = driver.getClientHome(connectionInfo.getClientHomeId());
        }
    }

    public boolean isDisposed()
    {
        return driver != null;
    }

    public void dispose()
    {
        synchronized (users) {
            users.clear();
        }
        if (driver != null) {
            driver.removeUser(this);
            driver = null;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public void setDriver(DriverDescriptor driver)
    {
        if (driver == this.driver) {
            return;
        }
        this.driver.removeUser(this);
        this.driver = driver;
        this.driver.addUser(this);
    }

    @Override
    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(DBPConnectionInfo connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public DBPConnectionInfo getActualConnectionInfo()
    {
        return tunnelConnectionInfo != null ? tunnelConnectionInfo : connectionInfo;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    @Property(order = 100)
    public String getDescription()
    {
        return description;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    @Override
    public boolean isShowSystemObjects()
    {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects)
    {
        this.showSystemObjects = showSystemObjects;
    }

    @Override
    public boolean isConnectionReadOnly()
    {
        return connectionReadOnly;
    }

    public void setConnectionReadOnly(boolean connectionReadOnly)
    {
        this.connectionReadOnly = connectionReadOnly;
    }

    @Override
    public boolean isDefaultAutoCommit()
    {
        if (getPreferenceStore().contains(PrefConstants.DEFAULT_AUTO_COMMIT)) {
            return getPreferenceStore().getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT);
        } else {
            return getConnectionInfo().getConnectionType().isAutocommit();
        }
    }

    @Override
    public void setDefaultAutoCommit(final boolean autoCommit, boolean updateConnection)
    {
        if (updateConnection && dataSource != null) {
            try {
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.UTIL, "Change '" + getName() + "' transactional mode");
                        try {
                            DBCTransactionManager txnManager = session.getTransactionManager();
                            // Change auto-commit mode
                            txnManager.setAutoCommit(autoCommit);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            session.close();
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(null, "Auto-Commit", "Error while toggle auto-commit", e.getTargetException());
                return;
            } catch (InterruptedException e) {
                // do nothing
                return;
            }
        }
        // Save in preferences
        if (autoCommit == getConnectionInfo().getConnectionType().isAutocommit()) {
            getPreferenceStore().setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        } else {
            getPreferenceStore().setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommit);
        }
    }

    @Override
    public boolean isConnectionAutoCommit()
    {
        if (dataSource != null && dataSource.isConnected()) {
            // We read this one synchronously because this function invoked many times per second by UI
            DBCSession session = dataSource.openSession(VoidProgressMonitor.INSTANCE,
                DBCExecutionPurpose.UTIL, "Get '" + getName() + "' auto-commit mode");
            try {
                return session.getTransactionManager().isAutoCommit();
            } catch (DBCException e) {
                log.debug("Can't check auto-commit flag", e);
                return false;
            } finally {
                session.close();
            }
        }
        return false;
    }

    @Override
    public DBPTransactionIsolation getDefaultTransactionsIsolation()
    {
        if (isConnected()) {
            // We read this one synchronously because this function invoked many times per second by UI
            DBCSession session = dataSource.openSession(VoidProgressMonitor.INSTANCE,
                DBCExecutionPurpose.UTIL, "Get '" + getName() + "' transactions isolation level");
            try {
                return session.getTransactionManager().getTransactionIsolation();
            } catch (DBCException e) {
                log.debug("Can't determine isolation level", e);
                return null;
            } finally {
                session.close();
            }
        } else {
            return null;
        }
    }

    @Override
    public void setDefaultTransactionsIsolation(final DBPTransactionIsolation isolationLevel)
    {
        try {
            if (isolationLevel == null) {
                getPreferenceStore().setToDefault(PrefConstants.DEFAULT_ISOLATION);
            } else {
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.META, "Check connection's auto-commit state");
                        final DBCTransactionManager txnManager = session.getTransactionManager();
                        try {
                            if (!txnManager.getTransactionIsolation().equals(isolationLevel)) {
                                txnManager.setTransactionIsolation(isolationLevel);
                                getPreferenceStore().setValue(PrefConstants.DEFAULT_ISOLATION, isolationLevel.getCode());
                            }
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            session.close();
                        }
                    }
                });
            }
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                null,
                "Transactions Isolation",
                "Can't set transaction isolation level to '" + isolationLevel.getTitle() + "'",
                e.getTargetException());
        } catch (InterruptedException e) {
            // ok
        }
    }

    public Collection<FilterMapping> getObjectFilters()
    {
        return filterMap.values();
    }

    @Override
    public DBSObjectFilter getObjectFilter(Class<?> type, DBSObject parentObject)
    {
        return getObjectFilter(type, parentObject, false);
    }

    public DBSObjectFilter getObjectFilter(Class<?> type, DBSObject parentObject, boolean firstMatch)
    {
        if (filterMap.isEmpty()) {
            return null;
        }
        // Test all super classes
        for (Class<?> testType = type; testType != null; testType = testType.getSuperclass()) {
            FilterMapping filterMapping = filterMap.get(testType);
            DBSObjectFilter filter;
            if (filterMapping == null) {
                // Try to find using interfaces and superclasses
                for (Class<?> it : testType.getInterfaces()) {
                    filterMapping = filterMap.get(it);
                    if (filterMapping != null) {
                        filter = filterMapping.getFilter(parentObject, firstMatch);
                        if (filter != null && (firstMatch || filter.isEnabled())) return filter;
                    }
                }
            }
            if (filterMapping != null) {
                filter = filterMapping.getFilter(parentObject, firstMatch);
                if (filter != null && (firstMatch || filter.isEnabled())) return filter;
            }
        }

        return null;
    }

    public void setObjectFilter(Class<?> type, DBSObject parentObject, DBSObjectFilter filter)
    {
        updateObjectFilter(type, parentObject == null ? null : DBUtils.getObjectUniqueName(parentObject), filter);
    }

    void updateObjectFilter(Class<?> type, String objectID, DBSObjectFilter filter)
    {
        FilterMapping filterMapping = filterMap.get(type);
        if (filterMapping == null) {
            filterMapping = new FilterMapping(type);
            filterMap.put(type, filterMapping);
        }
        if (objectID == null) {
            filterMapping.defaultFilter = filter;
        } else {
            filterMapping.customFilters.put(objectID, filter);
        }
    }

    @Override
    public DBVModel getVirtualModel()
    {
        return virtualModel;
    }

    @Override
    public DBPClientHome getClientHome()
    {
        return clientHome;
    }

    @Override
    public DBSObject getParentObject()
    {
        return null;
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        this.reconnect(monitor, true);

        getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            DataSourceDescriptor.this);

        return true;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public Date getUpdateDate()
    {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate)
    {
        this.updateDate = updateDate;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public DataSourceRegistry getRegistry()
    {
        return registry;
    }

    @Override
    public DBPKeywordManager getKeywordManager()
    {
        if (!isConnected()) {
            return EmptyKeywordManager.INSTANCE;
        }
        if (keywordManager == null) {
            keywordManager = new DataSourceKeywordManager(dataSource);
        }
        return keywordManager;
    }

    public String getFolderPath()
    {
        return folderPath;
    }

    public void setFolderPath(String folderPath)
    {
        this.folderPath = folderPath;
    }

    @Override
    public void persistConfiguration()
    {
        registry.saveDataSources();
    }

    @Override
    public boolean isConnected()
    {
        return connectTime != null;
    }

    @Override
    public void connect(DBRProgressMonitor monitor)
        throws DBException
    {
        connect(monitor, true, true);
    }

    public void connect(DBRProgressMonitor monitor, boolean initialize, boolean reflect)
        throws DBException
    {
        if (this.isConnected()) {
            return;
        }

        DBPConnectionInfo savedConnectionInfo = null;
        tunnelConnectionInfo = null;
        try {
            // Handle tunnel
            // Open tunnel and replace connection info with new one
            this.tunnel = null;
            DBWHandlerConfiguration handlerConfiguration = connectionInfo.getHandler(DBWHandlerType.TUNNEL);
            if (handlerConfiguration != null) {
                tunnel = handlerConfiguration.createHandler(DBWTunnel.class);
                try {
                    tunnelConnectionInfo = tunnel.initializeTunnel(monitor, handlerConfiguration, connectionInfo);
                } catch (Exception e) {
                    throw new DBCException("Can't initialize tunnel", e);
                }
            }
            if (tunnelConnectionInfo != null) {
                savedConnectionInfo = connectionInfo;
                connectionInfo = tunnelConnectionInfo;
            }

            dataSource = getDriver().getDataSourceProvider().openDataSource(monitor, this);

            if (initialize) {
                dataSource.initialize(monitor);
                // Change connection properties

                DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.UTIL, "Set session defaults ...");
                try {
                    DBCTransactionManager txnManager = session.getTransactionManager();
                    boolean autoCommit = txnManager.isAutoCommit();
                    AbstractPreferenceStore store = getPreferenceStore();
                    boolean newAutoCommit;
                    if (!store.contains(PrefConstants.DEFAULT_AUTO_COMMIT)) {
                        newAutoCommit = connectionInfo.getConnectionType().isAutocommit();
                    } else {
                        newAutoCommit = store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT);
                    }
                    if (autoCommit != newAutoCommit) {
                        // Change auto-commit state
                        txnManager.setAutoCommit(newAutoCommit);
                    }
                    if (store.contains(PrefConstants.DEFAULT_ISOLATION)) {
                        int isolationCode = store.getInt(PrefConstants.DEFAULT_ISOLATION);
                        Collection<DBPTransactionIsolation> supportedLevels = dataSource.getInfo().getSupportedTransactionsIsolation();
                        if (!CommonUtils.isEmpty(supportedLevels)) {
                            for (DBPTransactionIsolation level : supportedLevels) {
                                if (level.getCode() == isolationCode) {
                                    txnManager.setTransactionIsolation(level);
                                    break;
                                }
                            }
                        }
                    }
                }
                catch (DBCException e) {
                    log.error("Can't set session transactions state", e);
                }
                finally {
                    session.close();
                }
            }

            connectFailed = false;
            connectTime = new Date();

            if (reflect) {
                getRegistry().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    true);
            }
            firePropertyChange();
        } catch (Exception e) {
            // Failed
            connectFailed = true;
            //if (reflect) {
                getRegistry().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    false);
            //}
            if (e instanceof DBException) {
                throw (DBException)e;
            } else {
                throw new DBException("Internal error connecting to " + getName(), e);
            }
        } finally {
            if (savedConnectionInfo != null) {
                connectionInfo = savedConnectionInfo;
            }
        }
    }

    @Override
    public boolean disconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        return disconnect(monitor, true);
    }

    public boolean disconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return true;
        }
        {
            List<DBPDataSourceUser> usersStamp;
            synchronized (users) {
                usersStamp = new ArrayList<DBPDataSourceUser>(users);
            }
            int jobCount = 0;
            // Save all unsaved data
            for (DBPDataSourceUser user : usersStamp) {
                if (user instanceof Job) {
                    jobCount++;
                }
                if (user instanceof ISaveablePart) {
                    if (!RuntimeUtils.validateAndSave(monitor, (ISaveablePart) user)) {
                        return false;
                    }
                }
                if (user instanceof DBPDataSourceHandler) {
                    ((DBPDataSourceHandler)user).beforeDisconnect();
                }
            }
            if (jobCount > 0) {
                monitor.beginTask("Waiting for all active tasks to finish", jobCount);
                // Stop all jobs
                for (DBPDataSourceUser user : usersStamp) {
                    if (user instanceof Job) {
                        Job job = (Job)user;
                        monitor.subTask("Stop '" + job.getName() + "'");
                        if (job.getState() == Job.RUNNING) {
                            job.cancel();
                            try {
                                job.join();
                            } catch (InterruptedException e) {
                                // its ok, do nothing
                            }
                        }
                        monitor.worked(1);
                    }
                }
                monitor.done();
            }
        }

        monitor.beginTask("Disconnect from '" + getName() + "'", 4);

        // Close datasource
        monitor.subTask("Close connection");
        if (dataSource != null) {
            dataSource.close();
        }
        monitor.worked(1);

        // Close tunnel
        if (tunnel != null) {
            monitor.subTask("Close tunnel");
            try {
                tunnel.closeTunnel(monitor, connectionInfo);
            } catch (Exception e) {
                log.warn("Error closing tunnel", e);
            } finally {
                this.tunnel = null;
            }
        }
        monitor.worked(1);

        monitor.done();

        // Terminate child processes
        synchronized (childProcesses) {
            for (Iterator<DBRProcessDescriptor> iter = childProcesses.iterator(); iter.hasNext(); ) {
                DBRProcessDescriptor process = iter.next();
                if (process.isRunning() && process.getCommand().isTerminateAtDisconnect()) {
                    process.terminate();
                }
                iter.remove();
            }
        }

        dataSource = null;
        connectTime = null;
        keywordManager = null;

        if (reflect) {
            // Reflect UI
            getRegistry().fireDataSourceEvent(
                DBPEvent.Action.OBJECT_UPDATE,
                this,
                false);
            firePropertyChange();
        }

        return true;
    }

    public boolean closeActiveTransaction(final DBRProgressMonitor monitor)
    {
        if (dataSource == null) {
            return true;
        }

        // First rollback active transaction
        monitor.subTask("Rollback active transaction");
        DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback transaction");
        try {
            if (session.isConnected() && !session.getTransactionManager().isAutoCommit()) {
                // Check current transaction

                // If there are some executions in last savepoint then ask user about commit/rollback
                QMMCollector qmm = DBeaverCore.getInstance().getQueryManager().getMetaCollector();
                if (qmm != null) {
                    QMMSessionInfo qmmSession = qmm.getSessionInfo(dataSource);
                    QMMTransactionInfo txn = qmmSession == null ? null : qmmSession.getTransaction();
                    QMMTransactionSavepointInfo sp = txn == null ? null : txn.getCurrentSavepoint();
                    if (sp != null && (sp.getPrevious() != null || sp.getLastExecute() != null)) {
                        boolean hasUserExec = false;
                        if (true) {
                            // Do not check whether we have user queries, just ask for confirmation
                            hasUserExec = true;
                        } else {
                            for (QMMTransactionSavepointInfo psp = sp; psp != null; psp = psp.getPrevious()) {
                                if (psp.hasUserExecutions()) {
                                    hasUserExec = true;
                                    break;
                                }
                            }
                        }
                        if (hasUserExec) {
                            // Ask for confirmation
                            TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer();
                            UIUtils.runInUI(null, closeConfirmer);
                            switch (closeConfirmer.result) {
                                case IDialogConstants.YES_ID:
                                    session.getTransactionManager().commit();
                                    break;
                                case IDialogConstants.NO_ID:
                                    session.getTransactionManager().rollback(null);
                                    break;
                                default:
                                    return false;
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable e) {
            log.warn("Could not rollback active transaction before disconnect", e);
        }
        finally {
            session.close();
            monitor.worked(1);
        }
        return true;
    }

    @Override
    public boolean reconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        return reconnect(monitor, true);
    }

    public boolean reconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (isConnected()) {
            if (!disconnect(monitor, reflect)) {
                return false;
            }
        }
        connect(monitor, true, reflect);

        return true;
    }

    @Override
    public Collection<DBPDataSourceUser> getUsers()
    {
        synchronized (users) {
            return new ArrayList<DBPDataSourceUser>(users);
        }
    }

    @Override
    public void acquire(DBPDataSourceUser user)
    {
        synchronized (users) {
            if (users.contains(user)) {
                log.warn("Datasource user '" + user + "' already registered in datasource '" + getName() + "'");
            } else {
                users.add(user);
            }
        }
    }

    @Override
    public void release(DBPDataSourceUser user)
    {
        synchronized (users) {
            if (!users.remove(user)) {
                if (!isDisposed()) {
                    log.warn("Datasource user '" + user + "' is not registered in datasource '" + getName() + "'");
                }
            }
        }
    }

    @Override
    public void fireEvent(DBPEvent event) {
        registry.fireDataSourceEvent(event);
    }

    @Override
    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        if (this.formatterProfile == null) {
            this.formatterProfile = new DataFormatterProfile(getId(), preferenceStore);
        }
        return this.formatterProfile;
    }

    @Override
    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public DBDValueHandler getDefaultValueHandler()
    {
        return DBCDefaultValueHandler.INSTANCE;
    }

    @Override
    public AbstractPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void resetPassword()
    {
        if (connectionInfo != null) {
            connectionInfo.setUserPassword(null);
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return this;
        }
        return null;
    }

    @Override
    public Image getObjectImage()
    {
        return driver.getPlainIcon();
    }

    @Override
    public DBSObjectState getObjectState()
    {
        if (isConnected()) {
            return DBSObjectState.ACTIVE;
        } else if (connectFailed) {
            return DBSObjectState.INVALID;
        } else {
            return DBSObjectState.NORMAL;
        }
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor)
    {
        // just do nothing
    }

    @Override
    public boolean isObjectLocked()
    {
        return isConnectionReadOnly();
    }

    @Override
    public void setObjectLock(DBRProgressMonitor monitor, boolean locked) throws DBCException
    {
        // just do nothing
    }


    public static String generateNewId(DriverDescriptor driver)
    {
        return driver.getId() + "-" + System.currentTimeMillis() + "-" + new Random().nextInt();
    }

    private void firePropertyChange()
    {
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
    }

    @Property(viewable = true, order = 2)
    public String getPropertyDriverType()
    {
        return driver.getName();
    }

    @Property(order = 3)
    public String getPropertyAddress()
    {
        StringBuilder addr = new StringBuilder();
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            addr.append(connectionInfo.getHostName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            addr.append(':').append(connectionInfo.getHostPort());
        }
        return addr.toString();
    }

    @Property(order = 4)
    public String getPropertyDatabase()
    {
        return connectionInfo.getDatabaseName();
    }

    @Property(order = 5)
    public String getPropertyURL()
    {
        return connectionInfo.getUrl();
    }

    @Property(order = 6)
    public String getPropertyServerName()
    {
        if (isConnected() && dataSource.getInfo() != null) {
            String serverName = dataSource.getInfo().getDatabaseProductName();
            String serverVersion = dataSource.getInfo().getDatabaseProductVersion();
            if (serverName != null) {
                return serverName + (serverVersion == null ? "" : " [" + serverVersion + "]");
            }
        }
        return null;
    }

    @Property(order = 7)
    public String getPropertyDriver()
    {
        if (isConnected() && dataSource.getInfo() != null) {
            String driverName = dataSource.getInfo().getDriverName();
            String driverVersion = dataSource.getInfo().getDriverVersion();
            if (driverName != null) {
                return driverName + (driverVersion == null ? "" : " [" + driverVersion + "]");
            }
        }
        return null;
    }

    @Property(order = 8)
    public String getPropertyConnectTime()
    {
        if (connectTime != null) {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(connectTime);
        }
        return null;
    }

    @Property(order = 9)
    public String getPropertyConnectType()
    {
        return connectionInfo.getConnectionType().getName();
    }

    @Override
    public void editObject(IWorkbenchWindow workbenchWindow)
    {
//        CreateConnectionDialog dialog = new CreateConnectionDialog(
//            workbenchWindow,
//            new EditConnectionWizard(this));
        EditConnectionDialog dialog = new EditConnectionDialog(workbenchWindow, new EditConnectionWizard(this));
        dialog.open();
        //workbenchWindow.
    }

    public void addChildProcess(DBRProcessDescriptor process)
    {
        synchronized (childProcesses) {
            childProcesses.add(process);
        }
    }

    private class TransactionCloseConfirmer implements Runnable {
        int result = IDialogConstants.NO_ID;
        @Override
        public void run()
        {
            result = ConfirmationDialog.showConfirmDialog(
                null,
                PrefConstants.CONFIRM_TXN_DISCONNECT,
                ConfirmationDialog.QUESTION_WITH_CANCEL,
                getName());
        }
    }

    void copyFrom(DataSourceDescriptor descriptor) {
        filterMap.clear();
        for (FilterMapping mapping : descriptor.getObjectFilters()) {
            filterMap.put(mapping.type, new FilterMapping(mapping));
        }
        virtualModel.copyFrom(descriptor.getVirtualModel());

        setDescription(descriptor.getDescription());
        setSavePassword(descriptor.isSavePassword());
        setShowSystemObjects(descriptor.isShowSystemObjects());
        setConnectionReadOnly(descriptor.isConnectionReadOnly());
    }

}
