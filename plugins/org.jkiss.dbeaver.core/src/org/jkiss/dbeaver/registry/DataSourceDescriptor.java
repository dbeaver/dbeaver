/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.edit.DBEPrivateObjectEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.EmptyKeywordManager;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.meta.QMMCollector;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.text.DateFormat;
import java.util.*;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor implements DBSDataSourceContainer, IObjectImageProvider, IAdaptable, DBEPrivateObjectEditor
{
    static final Log log = LogFactory.getLog(DataSourceDescriptor.class);

    private DataSourceRegistry registry;
    private DriverDescriptor driver;
    private DBPConnectionInfo connectionInfo;

    private String id;
    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private String catalogFilter;
    private String schemaFilter;
    private Date createDate;
    private Date updateDate;
    private Date loginDate;
    private DBDDataFormatterProfile formatterProfile;
    private DataSourcePreferenceStore preferenceStore;

    private DBPDataSource dataSource;

    private final List<DBPDataSourceUser> users = new ArrayList<DBPDataSourceUser>();
    private Image iconNormal;
    private Image iconConnected;
    private Image iconError;

    private DataSourceKeywordManager keywordManager;

    private volatile boolean connectFailed = false;
    private volatile Date connectTime = null;


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

        this.driver.addUser(this);
    }

    public boolean isDisposed()
    {
        return driver != null;
    }

    public void dispose()
    {
        users.clear();
        iconNormal = null;
        if (iconConnected != null) {
            iconConnected.dispose();
            iconConnected = null;
        }
        if (iconError != null) {
            iconError.dispose();
            iconError = null;
        }
        if (driver != null) {
            driver.removeUser(this);
            driver = null;
        }
    }

    public String getId() {
        return id;
    }

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

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(DBPConnectionInfo connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Property(name = "Description", order = 100)
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

    public boolean isShowSystemObjects()
    {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects)
    {
        this.showSystemObjects = showSystemObjects;
    }

    public String getCatalogFilter()
    {
        return catalogFilter;
    }

    public void setCatalogFilter(String catalogFilter)
    {
        this.catalogFilter = catalogFilter;
    }

    public String getSchemaFilter()
    {
        return schemaFilter;
    }

    public void setSchemaFilter(String schemaFilter)
    {
        this.schemaFilter = schemaFilter;
    }

    public DBSObject getParentObject()
    {
        return null;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        this.reconnect(monitor, false);

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

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public DataSourceRegistry getRegistry()
    {
        return registry;
    }

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

    public boolean isConnected()
    {
        return connectTime != null;
    }

    public void connect(DBRProgressMonitor monitor)
        throws DBException
    {
        connect(monitor, true);
    }

    void connect(DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (this.isConnected()) {
            return;
        }

        try {
            dataSource = getDriver().getDataSourceProvider().openDataSource(monitor, this);

            dataSource.initialize(monitor);
            // Change connection properties

            DBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Set session defaults ...");
            try {
                DBCTransactionManager txnManager = context.getTransactionManager();
                boolean autoCommit = txnManager.isAutoCommit();
                boolean newAutoCommit = getPreferenceStore().getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT);
                if (autoCommit != newAutoCommit) {
                    // Change auto-commit state
                    txnManager.setAutoCommit(newAutoCommit);
                }
            }
            catch (DBCException e) {
                log.error("Can't set session auto-commit state", e);
            }
            finally {
                context.close();
            }

            connectFailed = false;
            connectTime = new Date();

            if (reflect) {
                getRegistry().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    true);
                firePropertyChange();
            }
        } catch (Exception e) {
            // Failed
            connectFailed = true;
            if (reflect) {
                getRegistry().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    false);
            }
            if (e instanceof DBException) {
                throw (DBException)e;
            } else {
                throw new DBException("Internal error connecting to " + getName(), e);
            }
        }
    }

    public boolean disconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        return disconnect(monitor, true);
    }

    boolean disconnect(final DBRProgressMonitor monitor, boolean reflect)
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
                } else if (user instanceof ISaveablePart) {
                    if (!RuntimeUtils.validateAndSave(monitor, (ISaveablePart) user)) {
                        return false;
                    }
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

        monitor.beginTask("Disconnect from '" + getName() + "'", 3);

        // First rollback active transaction
        monitor.subTask("Rollback active transaction");
        DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL, "Rollback transaction");
        try {
            if (context.isConnected() && !context.getTransactionManager().isAutoCommit()) {
                // Check current transaction
                // If there are some executions in last savepoint then ask user about commit/rollback
                QMMCollector qmm = DBeaverCore.getInstance().getQueryManager().getMetaCollector();
                QMMSessionInfo qmmSession = qmm.getSession(getDataSource());
                QMMTransactionInfo txn = qmmSession == null ? null : qmmSession.getTransaction();
                QMMTransactionSavepointInfo sp = txn == null ? null : txn.getCurrentSavepoint();
                if (sp != null && (sp.getPrevious() != null || sp.hasUserExecutions())) {
                    // Ask for confirmation
                    TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer();
                    Display display = Display.getDefault();
                    if (display != null) {
                        display.syncExec(closeConfirmer);
                    }
                    switch (closeConfirmer.result) {
                        case IDialogConstants.YES_ID:
                            context.getTransactionManager().commit();
                            break;
                        case IDialogConstants.NO_ID:
                            context.getTransactionManager().rollback(null);
                            break;
                        default:
                            return false;
                    }
                }
            }
        }
        catch (Throwable e) {
            log.warn("Could not rollback active transaction before disconnect", e);
        }
        finally {
            context.close();
        }
        monitor.worked(1);

        // Close datasource
        monitor.subTask("Close connection");
        getDataSource().close(monitor);
        monitor.worked(1);

        monitor.done();

        dataSource = null;
        connectTime = null;
        keywordManager = null;

        if (reflect) {
            getRegistry().fireDataSourceEvent(
                DBPEvent.Action.OBJECT_UPDATE,
                this,
                false);
            firePropertyChange();
        }

        // Clear users
        synchronized (users) {
            if (!users.isEmpty()) {
                log.debug(users.size() + " users still present in datasource '" + getName() + "' after disconnect");
            }
            users.clear();
        }

        return true;
    }

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
        connect(monitor, reflect);

        return true;
    }

    public Collection<DBPDataSourceUser> getUsers()
    {
        synchronized (users) {
            return new ArrayList<DBPDataSourceUser>(users);
        }
    }

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

    public void fireEvent(DBPEvent event) {
        registry.fireDataSourceEvent(event);
    }

    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        if (this.formatterProfile == null) {
            this.formatterProfile = new DataFormatterProfile(getId(), preferenceStore);
        }
        return this.formatterProfile;
    }

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

    public Object getAdapter(Class adapter)
    {
        if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return this;
        }
        return null;
    }

    public Image getObjectImage()
    {
        if (iconNormal == null) {
            iconNormal = driver.getPlainIcon();

            // Create overlay image for connected icon
            {
                OverlayImageDescriptor connectedDescriptor = new OverlayImageDescriptor(iconNormal.getImageData());
                connectedDescriptor.setBottomRight(new ImageDescriptor[] {DBIcon.OVER_SUCCESS.getImageDescriptor()} );
                iconConnected = new Image(iconNormal.getDevice(), connectedDescriptor.getImageData());
            }
            {
                OverlayImageDescriptor failedDescriptor = new OverlayImageDescriptor(iconNormal.getImageData());
                failedDescriptor.setBottomRight(new ImageDescriptor[] {DBIcon.OVER_ERROR.getImageDescriptor()} );
                iconError = new Image(iconNormal.getDevice(), failedDescriptor.getImageData());
            }
        }
        if (isConnected()) {
            return iconConnected;
        } else if (connectFailed) {
            return iconError;
        } else {
            return iconNormal;
        }
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

    @Property(name = "Driver Type", viewable = true, order = 2)
    public String getPropertyDriverType()
    {
        return driver.getName();
    }

    @Property(name = "Address", order = 3)
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

    @Property(name = "Database", order = 4)
    public String getPropertyDatabase()
    {
        return connectionInfo.getDatabaseName();
    }

    @Property(name = "URL", order = 5)
    public String getPropertyURL()
    {
        return connectionInfo.getUrl();
    }

    @Property(name = "Server", order = 6)
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

    @Property(name = "Driver", order = 7)
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

    @Property(name = "Connect Time", order = 8)
    public String getPropertyConnectTime()
    {
        if (connectTime != null) {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(connectTime);
        }
        return null;
    }

    public void editObject(IWorkbenchWindow workbenchWindow)
    {
        ConnectionDialog dialog = new ConnectionDialog(
            workbenchWindow,
            new EditConnectionWizard(this));
        dialog.open();
    }

    private class TransactionCloseConfirmer implements Runnable {
        int result = IDialogConstants.NO_ID;
        public void run()
        {
            result = ConfirmationDialog.showConfirmDialog(
                null,
                PrefConstants.CONFIRM_TXN_DISCONNECT,
                ConfirmationDialog.QUESTION_WITH_CANCEL,
                getName());
        }
    }
}
