/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.PasswordEncrypter;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.registry.encode.SimpleStringEncrypter;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.xml.sax.Attributes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataSourceRegistry implements DBPDataSourceRegistry
{
    static final Log log = LogFactory.getLog(DataSourceRegistry.class);

    public static final String CONFIG_FILE_NAME = "data-sources.xml"; //$NON-NLS-1$

    private final String projectId;

    private final List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<DBPEventListener>();

    public DataSourceRegistry(IProject project)
    {
        this.projectId = ProjectRegistry.getProjectId(project);
        IFile configFile = project.getFile(CONFIG_FILE_NAME);

        File dsFile = configFile.getLocation().toFile();
        if (dsFile.exists()) {
            loadDataSources(dsFile, new SimpleStringEncrypter());
        }
        if (dataSources.isEmpty() && DBeaverCore.getInstance().getLiveProjects().size() == 1) {
            // If this is first project then try to read file from DBeaver beta
            dsFile = new File(RuntimeUtils.getBetaDir(), CONFIG_FILE_NAME);
            if (dsFile.exists()) {
                try {
                    loadDataSources(dsFile, new SecuredPasswordEncrypter());
                    saveDataSources();
                } catch (EncryptionException e) {
                    log.warn("Encryption error", e);
                }
            }
        }
        DBeaverCore.getInstance().getDataSourceProviderRegistry().fireRegistryChange(this, true);
        //if (!dsFile.exists()) {
            // Generate empty config file
        //    saveDataSources();
        //}
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getDataSourceProviderRegistry().fireRegistryChange(this, false);
        synchronized (dataSourceListeners) {
            if (!this.dataSourceListeners.isEmpty()) {
                log.warn("Some data source listeners are still registered: " + dataSourceListeners);
            }
            this.dataSourceListeners.clear();
        }

        closeConnections();

        // Dispose and clear all descriptors
        synchronized (dataSources) {
            for (DataSourceDescriptor dataSourceDescriptor : this.dataSources) {
                dataSourceDescriptor.dispose();
            }
            this.dataSources.clear();
        }
    }

    public boolean closeConnections()
    {
        boolean hasConnections = false;
        synchronized (dataSources) {
            for (DataSourceDescriptor dataSource : dataSources) {
                if (dataSource.isConnected()) {
                    hasConnections = true;
                    break;
                }
            }
        }
        if (!hasConnections) {
            return true;
        }
        try {
            DisconnectTask disconnectTask = new DisconnectTask();
            DBeaverCore.getInstance().runInProgressService(disconnectTask);
            return disconnectTask.disconnected;
        } catch (InvocationTargetException e) {
            log.error("Can't close opened connections", e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        return true;
    }

    ////////////////////////////////////////////////////
    // Datasources

    @Override
    public DataSourceDescriptor getDataSource(String id)
    {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getId().equals(id)) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Override
    public DataSourceDescriptor getDataSource(DBPDataSource dataSource)
    {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getDataSource() == dataSource) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Override
    public DataSourceDescriptor findDataSourceByName(String name)
    {
        synchronized (dataSources) {
            for (DataSourceDescriptor dsd : dataSources) {
                if (dsd.getName().equals(name)) {
                    return dsd;
                }
            }
        }
        return null;
    }

    @Override
    public List<DataSourceDescriptor> getDataSources()
    {
        synchronized (dataSources) {
            List<DataSourceDescriptor> dsCopy = new ArrayList<DataSourceDescriptor>(dataSources);
            Collections.sort(dsCopy, new Comparator<DataSourceDescriptor>() {
                @Override
                public int compare(DataSourceDescriptor o1, DataSourceDescriptor o2)
                {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            return dsCopy;
        }
    }

    public void addDataSource(DataSourceDescriptor dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.add(dataSource);
            this.saveDataSources();
        }
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, dataSource);
    }

    public void removeDataSource(DataSourceDescriptor dataSource)
    {
        synchronized (dataSources) {
            this.dataSources.remove(dataSource);
            this.saveDataSources();
        }
        try {
            this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
        } finally {
            dataSource.dispose();
        }
    }

    public void updateDataSource(DataSourceDescriptor dataSource)
    {
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
    }

    @Override
    public void flushConfig()
    {
        this.saveDataSources();
    }

    @Override
    public void addDataSourceListener(DBPEventListener listener)
    {
        synchronized (dataSourceListeners) {
            dataSourceListeners.add(listener);
        }
    }

    @Override
    public boolean removeDataSourceListener(DBPEventListener listener)
    {
        synchronized (dataSourceListeners) {
            return dataSourceListeners.remove(listener);
        }
    }

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object)
    {
        notifyDataSourceListeners(new DBPEvent(action, object));
    }

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object,
        boolean enabled)
    {
        notifyDataSourceListeners(new DBPEvent(action, object, enabled));
    }

    public void fireDataSourceEvent(
        DBPEvent.Action action,
        DBSObject object,
        Object data)
    {
        notifyDataSourceListeners(new DBPEvent(action, object, data));
    }

    public void fireDataSourceEvent(DBPEvent event)
    {
        notifyDataSourceListeners(event);
    }

    private void notifyDataSourceListeners(
        final DBPEvent event)
    {
        if (dataSourceListeners.isEmpty()) {
            return;
        }
        final List<DBPEventListener> listeners;
        synchronized (dataSourceListeners) {
            listeners = new ArrayList<DBPEventListener>(dataSourceListeners);
        }
        //Display display = this.core.getWorkbench().getDisplay();
        for (DBPEventListener listener : listeners) {
            listener.handleDataSourceEvent(event);
        }
/*
            display.asyncExec(
                new Runnable() {
                    public void run() {
                        for (DBPEventListener listener : listeners) {
                            listener.handleDataSourceEvent(event);
                        }
                    }
                }
            );
*/
    }

    private void loadDataSources(File fromFile, PasswordEncrypter encrypter)
    {
        if (!fromFile.exists()) {
            return;
        }
        try {
            InputStream is = new FileInputStream(fromFile);
            try {
                try {
                    loadDataSources(is, encrypter);
                } catch (DBException ex) {
                    log.warn("Error loading datasource config from " + fromFile.getAbsolutePath(), ex);
                }
                finally {
                    is.close();
                }
            }
            catch (IOException ex) {
                log.warn("IO error", ex);
            }
            finally {
                ContentUtils.close(is);
            }
        } catch (IOException e) {
            log.warn("Can't load config file " + fromFile.getAbsolutePath(), e);
        }
    }

    private void loadDataSources(InputStream is, PasswordEncrypter encrypter)
        throws DBException, IOException
    {
        synchronized (dataSources) {
            SAXReader parser = new SAXReader(is);
            try {
                parser.parse(new DataSourcesParser(encrypter));
            }
            catch (XMLException ex) {
                throw new DBException("Datasource config parse error", ex);
            }
        }
    }

    private void saveDataSources()
    {
        synchronized (dataSources) {
            PasswordEncrypter encrypter = new SimpleStringEncrypter();
            IFile configFile = getProject().getFile(CONFIG_FILE_NAME);
            File projectConfig = configFile.getLocation().toFile();
            try {
                OutputStream os = new FileOutputStream(projectConfig);
                try {
                    XMLBuilder xml = new XMLBuilder(os, ContentUtils.DEFAULT_FILE_CHARSET);
                    xml.setButify(true);
                    xml.startElement("data-sources");
                    for (DataSourceDescriptor dataSource : dataSources) {
                        saveDataSource(xml, dataSource, encrypter);
                    }
                    xml.endElement();
                    xml.flush();
                    os.close();
                }
                catch (IOException ex) {
                    log.warn("IO error while saving datasources", ex);
                }
            } catch (FileNotFoundException ex) {
                log.error("Can't open config file " + projectConfig.getPath(), ex);
            }
            try {
                configFile.refreshLocal(IFile.DEPTH_ZERO, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't refresh datasources configuration");
            }
        }
    }

    private void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource, PasswordEncrypter encrypter)
        throws IOException
    {
        xml.startElement("data-source");
        xml.addAttribute("id", dataSource.getId());
        xml.addAttribute("provider", dataSource.getDriver().getProviderDescriptor().getId());
        xml.addAttribute("driver", dataSource.getDriver().getId());
        xml.addAttribute("name", dataSource.getName());
        xml.addAttribute("create-date", dataSource.getCreateDate().getTime());
        if (dataSource.getUpdateDate() != null) {
            xml.addAttribute("update-date", dataSource.getUpdateDate().getTime());
        }
        if (dataSource.getLoginDate() != null) {
            xml.addAttribute("login-date", dataSource.getLoginDate().getTime());
        }
        xml.addAttribute("save-password", dataSource.isSavePassword());
        xml.addAttribute("show-system-objects", dataSource.isShowSystemObjects());
        if (!CommonUtils.isEmpty(dataSource.getCatalogFilter())) {
            xml.addAttribute("filter-catalog", dataSource.getCatalogFilter());
        }
        if (!CommonUtils.isEmpty(dataSource.getSchemaFilter())) {
            xml.addAttribute("filter-schema", dataSource.getSchemaFilter());
        }

        {
            // Connection info
            DBPConnectionInfo connectionInfo = dataSource.getConnectionInfo();
            xml.startElement("connection");
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                xml.addAttribute("host", connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                xml.addAttribute("port", connectionInfo.getHostPort());
            }
            xml.addAttribute("server", CommonUtils.getString(connectionInfo.getServerName()));
            xml.addAttribute("database", CommonUtils.getString(connectionInfo.getDatabaseName()));
            xml.addAttribute("url", CommonUtils.getString(connectionInfo.getUrl()));
            xml.addAttribute("user", CommonUtils.getString(connectionInfo.getUserName()));
            if (dataSource.isSavePassword() && !CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
                String encPassword = connectionInfo.getUserPassword();
                if (!CommonUtils.isEmpty(encPassword)) {
                    try {
                        encPassword = encrypter.encrypt(encPassword);
                    }
                    catch (EncryptionException e) {
                        log.error("Could not encrypt password. Save it as is", e);
                    }
                }
                xml.addAttribute("password", encPassword);
            }
            if (!CommonUtils.isEmpty(connectionInfo.getClientHomeId())) {
                xml.addAttribute("home", connectionInfo.getClientHomeId());
            }
            if (connectionInfo.getProperties() != null) {
                for (Map.Entry<Object, Object> entry : connectionInfo.getProperties().entrySet()) {
                    xml.startElement("property");
                    xml.addAttribute("name", CommonUtils.toString(entry.getKey()));
                    xml.addAttribute("value", CommonUtils.toString(entry.getValue()));
                    xml.endElement();
                }
            }
            // Save events
            for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                DBRShellCommand command = connectionInfo.getEvent(eventType);
                xml.startElement("event");
                xml.addAttribute("type", eventType.name());
                xml.addAttribute("enabled", command.isEnabled());
                xml.addAttribute("show-panel", command.isShowProcessPanel());
                xml.addAttribute("wait-process", command.isWaitProcessFinish());
                xml.addAttribute("terminate-at-disconnect", command.isTerminateAtDisconnect());
                xml.addText(command.getCommand());
                xml.endElement();
            }
            // Save network handlers' configurations
            for (DBWHandlerConfiguration configuration : connectionInfo.getDeclaredHandlers()) {
                xml.startElement("network-handler");
                xml.addAttribute("type", configuration.getType().name());
                xml.addAttribute("id", CommonUtils.getString(configuration.getId()));
                xml.addAttribute("enabled", configuration.isEnabled());
                xml.addAttribute("user", CommonUtils.getString(configuration.getUserName()));
                xml.addAttribute("save-password", configuration.isSavePassword());
                if (configuration.isSavePassword() && !CommonUtils.isEmpty(configuration.getPassword())) {
                    String encPassword = configuration.getPassword();
                    if (!CommonUtils.isEmpty(encPassword)) {
                        try {
                            encPassword = encrypter.encrypt(encPassword);
                        }
                        catch (EncryptionException e) {
                            log.error("Could not encrypt password. Save it as is", e);
                        }
                    }
                    xml.addAttribute("password", encPassword);
                }
                for (Map.Entry<String, String> entry : configuration.getProperties().entrySet()) {
                    xml.startElement("property");
                    xml.addAttribute("name", entry.getKey());
                    xml.addAttribute("value", CommonUtils.getString(entry.getValue()));
                    xml.endElement();
                }
                xml.endElement();
            }
            xml.endElement();
        }

        // Preferences
        {
            // Save only properties who are differs from default values
            AbstractPreferenceStore prefStore = dataSource.getPreferenceStore();
            for (String propName : prefStore.preferenceNames()) {
                String propValue = prefStore.getString(propName);
                String defValue = prefStore.getDefaultString(propName);
                if (propValue == null || (defValue != null && defValue.equals(propValue))) {
                    continue;
                }
                xml.startElement("custom-property");
                xml.addAttribute("name", propName);
                xml.addAttribute("value", propValue);
                xml.endElement();
            }
        }

        //xml.addText(CommonUtils.getString(dataSource.getDescription()));
        xml.endElement();
    }

    @Override
    public IProject getProject()
    {
        return DBeaverCore.getInstance().getProject(projectId);
    }

    private class DataSourcesParser implements SAXListener
    {
        DataSourceDescriptor curDataSource;
        PasswordEncrypter encrypter;
        boolean isDescription = false;
        DBRShellCommand curCommand = null;
        private DBWHandlerConfiguration curNetworkHandler;

        private DataSourcesParser(PasswordEncrypter encrypter)
        {
            this.encrypter = encrypter;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            isDescription = false;
            curCommand = null;
            if (localName.equals("data-source")) {
            	String name = atts.getValue("name");
                String id = atts.getValue("id");
                if (id == null) {
                    // Support of old version without ID
                    id = name;
                }
                String providerId = atts.getValue("provider");
                DataSourceProviderDescriptor provider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(providerId);
                if (provider == null) {
                    log.warn("Can't find datasource provider " + providerId + " for datasource '" + name + "'");
                    curDataSource = null;
                    return;
                }
                DriverDescriptor driver = provider.getDriver(atts.getValue("driver"));
                if (driver == null) {
                    log.warn("Can't find driver " + atts.getValue("driver") + " in datasource provider " + provider.getId() + " for datasource '" + name + "'");
                    curDataSource = null;
                    return;
                }
                curDataSource = new DataSourceDescriptor(
                    DataSourceRegistry.this,
                    id,
                    driver,
                    new DBPConnectionInfo());
                curDataSource.setName(name);
                String createDate = atts.getValue("create-date");
                if (!CommonUtils.isEmpty(createDate)) {
                    curDataSource.setCreateDate(new Date(Long.parseLong(createDate)));
                }
                String udateDate = atts.getValue("update-date");
                if (!CommonUtils.isEmpty(udateDate)) {
                    curDataSource.setUpdateDate(new Date(Long.parseLong(udateDate)));
                }
                String logineDate = atts.getValue("login-date");
                if (!CommonUtils.isEmpty(logineDate)) {
                    curDataSource.setLoginDate(new Date(Long.parseLong(logineDate)));
                }
                curDataSource.setSavePassword(CommonUtils.getBoolean(atts.getValue("save-password")));
                curDataSource.setShowSystemObjects(CommonUtils.getBoolean(atts.getValue("show-system-objects")));
                curDataSource.setCatalogFilter(atts.getValue("filter-catalog"));
                curDataSource.setSchemaFilter(atts.getValue("filter-schema"));

                dataSources.add(curDataSource);
            } else if (localName.equals("connection")) {
                if (curDataSource != null) {
                    curDataSource.getConnectionInfo().setHostName(atts.getValue("host"));
                    curDataSource.getConnectionInfo().setHostPort(atts.getValue("port"));
                    curDataSource.getConnectionInfo().setServerName(atts.getValue("server"));
                    curDataSource.getConnectionInfo().setDatabaseName(atts.getValue("database"));
                    curDataSource.getConnectionInfo().setUrl(atts.getValue("url"));
                    curDataSource.getConnectionInfo().setUserName(atts.getValue("user"));
                    curDataSource.getConnectionInfo().setUserPassword(decryptPassword(atts.getValue("password")));
                    curDataSource.getConnectionInfo().setClientHomeId(atts.getValue("home"));
                }
            } else if (localName.equals("property")) {
                if (curNetworkHandler != null) {
                    curNetworkHandler.getProperties().put(
                        atts.getValue("name"),
                        atts.getValue("value"));
                } else if (curDataSource != null) {
                    curDataSource.getConnectionInfo().getProperties().put(
                        atts.getValue("name"),
                        atts.getValue("value"));
                }
            } else if (localName.equals("event")) {
                if (curDataSource != null) {
                    DBPConnectionEventType eventType = DBPConnectionEventType.valueOf(atts.getValue("type"));
                    curCommand = new DBRShellCommand("");
                    curCommand.setEnabled(CommonUtils.getBoolean(atts.getValue("enabled")));
                    curCommand.setShowProcessPanel(CommonUtils.getBoolean(atts.getValue("show-panel")));
                    curCommand.setWaitProcessFinish(CommonUtils.getBoolean(atts.getValue("wait-process")));
                    curCommand.setTerminateAtDisconnect(CommonUtils.getBoolean(atts.getValue("terminate-at-disconnect")));
                    curDataSource.getConnectionInfo().setEvent(eventType, curCommand);
                }
            } else if (localName.equals("custom-property")) {
                if (curDataSource != null) {
                    curDataSource.getPreferenceStore().getProperties().put(
                        atts.getValue("name"),
                        atts.getValue("value"));
                }
            } else if (localName.equals("network-handler")) {
                if (curDataSource != null) {
                    String handlerId = atts.getValue("id");
                    NetworkHandlerDescriptor handlerDescriptor = DBeaverCore.getInstance().getNetworkHandlerRegistry().getDescriptor(handlerId);
                    if (handlerDescriptor == null) {
                        log.warn("Can't find network handler '" + handlerId + "'");
                        return;
                    }
                    curNetworkHandler = new DBWHandlerConfiguration(handlerDescriptor, curDataSource.getDriver());
                    curNetworkHandler.setEnabled(CommonUtils.getBoolean(atts.getValue("enabled")));
                    curNetworkHandler.setUserName(CommonUtils.getString(atts.getValue("user")));
                    curNetworkHandler.setSavePassword(CommonUtils.getBoolean(atts.getValue("save-password")));
                    curNetworkHandler.setPassword(decryptPassword(atts.getValue("password")));
                    curDataSource.getConnectionInfo().addHandler(curNetworkHandler);
                }
            } else if (localName.equals("description")) {
                isDescription = true;
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
            if (isDescription && curDataSource != null) {
                curDataSource.setDescription(data);
            } else if (curCommand != null) {
                curCommand.setCommand(data);
                curCommand = null;
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals("data-source")) {
                curDataSource = null;
            } else if (localName.equals("network-handler")) {
                curNetworkHandler = null;
            }
            isDescription = false;
        }

        private String decryptPassword(String encPassword)
        {
            if (!CommonUtils.isEmpty(encPassword)) {
                try {
                    encPassword = encrypter.decrypt(encPassword);
                }
                catch (Throwable e) {
                    // could not decrypt - use as is
                    encPassword = null;
                }
            }
            return encPassword;
        }

    }

    private class DisconnectTask implements DBRRunnableWithProgress {
        boolean disconnected;
        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            for (DataSourceDescriptor dataSource : dataSources) {
                if (dataSource.isConnected()) {
                    try {
                        // Disconnect
                        disconnected = dataSource.disconnect(monitor);
                    } catch (Exception ex) {
                        log.error("Can't shutdown data source", ex);
                    }
                }
            }
        }
    }
}
