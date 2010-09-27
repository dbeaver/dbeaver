/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor implements DBSDataSourceContainer, IObjectImageProvider, IAdaptable
{
    static final Log log = LogFactory.getLog(DataSourceDescriptor.class);

    private DriverDescriptor driver;
    private DBPConnectionInfo connectionInfo;

    private String id;
    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private Date createDate;
    private Date updateDate;
    private Date loginDate;
    private DBDDataFormatterProfile formatterProfile;
    private DataSourcePreferenceStore preferenceStore;

    private DBPDataSource dataSource;

    private transient List<DBPDataSourceUser> users = new ArrayList<DBPDataSourceUser>();
    private transient Image iconNormal;
    private transient Image iconConnected;
    private transient Image iconError;
    private transient boolean connectFailed = false;
    private transient Date connectTime = null;


    public DataSourceDescriptor(
        String id,
        DriverDescriptor driver,
        DBPConnectionInfo connectionInfo)
    {
        this.id = id;
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        this.createDate = new Date();
        this.preferenceStore = new DataSourcePreferenceStore(this);
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
    }

    public String getId() {
        return id;
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(DBPConnectionInfo connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getObjectId() {
        return id;
    }

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

    public DBSObject getParentObject()
    {
        return null;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        this.reconnect(monitor, false);
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

    public DBeaverCore getViewCallback()
    {
        return driver.getProviderDescriptor().getRegistry().getCore();
    }

    public boolean isConnected()
    {
        return dataSource != null;
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

            DBCExecutionContext context = dataSource.openContext(monitor, "Set session defaults ...");
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
                getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    DataSourceDescriptor.this,
                    true);
                firePropertyChange();
            }
        } catch (Exception e) {
            // Failed
            connectFailed = true;
            if (reflect) {
                getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
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

    public void disconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        disconnect(monitor, true);
    }

    void disconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return;
        }

        monitor.beginTask("Disconnect from '" + getName() + "'", 3);

        // Join all running jobs of the same family
        monitor.subTask("Waiting for all active tasks to finish");
        try {
            // Cancel all currently running jobs
            Job.getJobManager().cancel(dataSource);

            Job.getJobManager().join(dataSource, monitor.getNestedMonitor());
        } catch (Exception e) {
            // do nothing
            log.debug(e);
        }
        monitor.worked(1);

        // First rollback active transaction
        monitor.subTask("Rollback active transaction");
        DBCExecutionContext context = getDataSource().openContext(monitor);
        try {
            if (context.isConnected() && !context.getTransactionManager().isAutoCommit()) {
                context.getTransactionManager().rollback(null);
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

        if (reflect) {
            getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                DBPEvent.Action.OBJECT_UPDATE,
                this,
                false);
            firePropertyChange();
        }
    }

    public void reconnect(final DBRProgressMonitor monitor)
        throws DBException
    {
        reconnect(monitor, true);
    }

    public void reconnect(final DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        if (isConnected()) {
            disconnect(monitor, reflect);
        }
        connect(monitor, reflect);
    }

    public void acquire(DBPDataSourceUser user)
    {
        users.add(user);
    }

    public void release(DBPDataSourceUser user)
    {
        users.remove(user);
    }

    public void fireEvent(DBPEvent event) {
        DataSourceRegistry.getDefault().fireDataSourceEvent(event);
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
        } else if (adapter == IPropertySource.class) {
            DBPDataSourceInfo info = null;
            if (this.isConnected()) {
                info = this.getDataSource().getInfo();
            }
            StringBuilder addr = new StringBuilder();
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                addr.append(connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                addr.append(':').append(connectionInfo.getHostPort());
            }

            PropertyCollector props = new PropertyCollector(this, false);
            props.addProperty("driverType", "Driver Type", driver.getName());
            if (info != null) {
                //props.addProperty("driverName", "Driver Name", info.getDriverName() + " " + info.getDriverVersion());
            }
            props.addProperty("address", "Address", addr.toString());
            props.addProperty("database", "Database Name", connectionInfo.getDatabaseName());
            if (info != null) {
                //props.addProperty("databaseType", "Database Type", info.getDatabaseProductName() + " " + info.getDatabaseProductVersion());
            }
            props.addProperty("url", "URL", connectionInfo.getUrl());
            if (isConnected() && dataSource.getInfo() != null) {
                String serverName = dataSource.getInfo().getDatabaseProductName();
                String serverVersion = dataSource.getInfo().getDatabaseProductVersion();
                if (serverName != null) {
                    props.addProperty("server-name", "Server", serverName + (serverVersion == null ? "" : " [" + serverVersion + "]"));
                }

                String driverName = dataSource.getInfo().getDriverName();
                String driverVersion = dataSource.getInfo().getDriverVersion();
                if (driverName != null) {
                    props.addProperty("driver-name", "Driver", driverName + (driverVersion == null ? "" : " [" + driverVersion + "]"));
                }
                if (connectTime != null) {
                    props.addProperty("connect-time", "Connect Time", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(connectTime));
                }
            }
            return props;
        }
        return null;
    }

    public Image getObjectImage()
    {
        if (iconNormal == null) {
            iconNormal = driver.getIcon();

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

    public static String generateNewId(DriverDescriptor driver) {
        return driver.getId() + "-" + System.currentTimeMillis() + "-" + new Random().nextInt();
    }

    private void firePropertyChange()
    {
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
        DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
    }

}
