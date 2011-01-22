/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.SAXListener;
import net.sf.jkiss.utils.xml.SAXReader;
import net.sf.jkiss.utils.xml.XMLBuilder;
import net.sf.jkiss.utils.xml.XMLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.StringEncrypter;
import org.xml.sax.Attributes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DataSourceRegistry implements DBPDataSourceRegistry
{
    static final Log log = LogFactory.getLog(DataSourceRegistry.class);

    public static final String CONFIG_FILE_NAME = "data-sources.xml";

    private static final String PASSWORD_ENCRYPTION_KEY = "sdf@!#$verf^wv%6Fwe%$$#FFGwfsdefwfe135s$^H)dg";

    private final String projectId;

    private final List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private final List<DBPEventListener> dataSourceListeners = new ArrayList<DBPEventListener>();

    private static StringEncrypter encrypter;

    static {
        try {
            encrypter = new StringEncrypter(PASSWORD_ENCRYPTION_KEY);
        } catch (StringEncrypter.EncryptionException e) {
            log.error(e);
        }
    }

    public DataSourceRegistry(IProject project)
    {
        this.projectId = ProjectRegistry.getProjectId(project);
        IFile configFile = project.getFile(CONFIG_FILE_NAME);

        File dsFile = configFile.getLocation().toFile();
        if (dsFile.exists()) {
            loadDataSources(dsFile);
        }
        if (dataSources.isEmpty() && DBeaverCore.getInstance().getLiveProjects().size() == 1) {
            // If this is first project then try to read file from DBeaver beta
            dsFile = new File(new File(System.getProperty("user.home")), ".dbeaver-beta/" + CONFIG_FILE_NAME);
            if (dsFile.exists()) {
                loadDataSources(dsFile);
            }
        }
        //if (!dsFile.exists()) {
            // Generate empty config file
        //    saveDataSources();
        //}
    }

    public void dispose()
    {
        synchronized (dataSourceListeners) {
/*
            if (!this.dataSourceListeners.isEmpty()) {
                log.warn("Some data source listeners are still registered: " + dataSourceListeners);
            }
*/
            this.dataSourceListeners.clear();
        }

        closeConnections();

        // Dispose and clear all descriptors

        for (DataSourceDescriptor dataSourceDescriptor : this.dataSources) {
            dataSourceDescriptor.dispose();
        }
        this.dataSources.clear();
    }

    public synchronized boolean closeConnections()
    {
        boolean hasConnections = false;
        for (DataSourceDescriptor dataSource : dataSources) {
            if (dataSource.isConnected()) {
                hasConnections = true;
                break;
            }
        }
        if (!hasConnections) {
            return true;
        }
        try {
            DisconnectTask disconnectTask = new DisconnectTask();
            DBeaverCore.getInstance().runAndWait2(disconnectTask);
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

    public DataSourceDescriptor getDataSource(String id)
    {
        for (DataSourceDescriptor dsd : dataSources) {
            if (dsd.getId().equals(id)) {
                return dsd;
            }
        }
        return null;
    }

    public DataSourceDescriptor getDataSource(DBPDataSource dataSource)
    {
        for (DataSourceDescriptor dsd : dataSources) {
            if (dsd.getDataSource() == dataSource) {
                return dsd;
            }
        }
        return null;
    }

    public DataSourceDescriptor findDataSourceByName(String name)
    {
        for (DataSourceDescriptor dsd : dataSources) {
            if (dsd.getName().equals(name)) {
                return dsd;
            }
        }
        return null;
    }

    public List<DataSourceDescriptor> getDataSources()
    {
        List<DataSourceDescriptor> dsCopy = new ArrayList<DataSourceDescriptor>(dataSources);
        Collections.sort(dsCopy, new Comparator<DataSourceDescriptor>() {
            public int compare(DataSourceDescriptor o1, DataSourceDescriptor o2)
            {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return dsCopy;
    }

    public void addDataSource(DataSourceDescriptor dataSource)
    {
        this.dataSources.add(dataSource);
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_ADD, dataSource);
    }

    public void removeDataSource(DataSourceDescriptor dataSource)
    {
        this.dataSources.remove(dataSource);
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_REMOVE, dataSource);
    }

    public void updateDataSource(DataSourceDescriptor dataSource)
    {
        this.saveDataSources();
        this.fireDataSourceEvent(DBPEvent.Action.OBJECT_UPDATE, dataSource);
    }

    public void flushConfig()
    {
        this.saveDataSources();
    }

    public void addDataSourceListener(DBPEventListener listener)
    {
        synchronized (dataSourceListeners) {
            dataSourceListeners.add(listener);
        }
    }

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

    private void loadDataSources(File fromFile)
    {
        if (!fromFile.exists()) {
            return;
        }
        try {
            InputStream is = new FileInputStream(fromFile);
            try {
                try {
                    loadDataSources(is);
                } catch (DBException ex) {
                    log.warn("Can't load datasource config from " + fromFile.getAbsolutePath(), ex);
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

    private void loadDataSources(InputStream is)
        throws DBException, IOException
    {
        SAXReader parser = new SAXReader(is);
        try {
            parser.parse(new DataSourcesParser());
        }
        catch (XMLException ex) {
            throw new DBException("Datasource config parse error", ex);
        }
    }

    private void saveDataSources()
    {
        IFile configFile = getProject().getFile(CONFIG_FILE_NAME);
        File projectConfig = configFile.getLocation().toFile();
        try {
            OutputStream os = new FileOutputStream(projectConfig);
            try {
                XMLBuilder xml = new XMLBuilder(os, ContentUtils.DEFAULT_FILE_CHARSET);
                xml.setButify(true);
                xml.startElement("data-sources");
                for (DataSourceDescriptor dataSource : dataSources) {
                    saveDataSource(xml, dataSource);
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
            configFile.refreshLocal(IFile.DEPTH_ZERO, VoidProgressMonitor.INSTANCE.getNestedMonitor());
        } catch (CoreException e) {
            log.error("Can't refresh datasources configuration");
        }
    }

    private void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource)
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
            xml.addAttribute("database", CommonUtils.getString(connectionInfo.getDatabaseName()));
            xml.addAttribute("url", CommonUtils.getString(connectionInfo.getUrl()));
            xml.addAttribute("user", CommonUtils.getString(connectionInfo.getUserName()));
            if (dataSource.isSavePassword() && !CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
                String encPassword = connectionInfo.getUserPassword();
                if (!CommonUtils.isEmpty(encPassword)) {
                    try {
                        encPassword = encrypter.encrypt(encPassword);
                    }
                    catch (StringEncrypter.EncryptionException e) {
                        log.error("Could not encrypt password. Save it as is", e);
                    }
                }
                xml.addAttribute("password", encPassword);
            }
            if (connectionInfo.getProperties() != null) {
                for (Map.Entry<String, String> entry : connectionInfo.getProperties().entrySet()) {
                    xml.startElement("property");
                    xml.addAttribute("name", entry.getKey());
                    xml.addAttribute("value", entry.getValue());
                    xml.endElement();
                }
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

        xml.addText(CommonUtils.getString(dataSource.getDescription()));
        xml.endElement();
    }

    public IProject getProject()
    {
        return DBeaverCore.getInstance().getProject(projectId);
    }

    private class DataSourcesParser implements SAXListener
    {
        DataSourceDescriptor curDataSource;
        boolean isDescription = false;

        private DataSourcesParser()
        {
        }

        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            isDescription = false;
            if (localName.equals("data-source")) {
                DataSourceProviderDescriptor provider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(atts.getValue("provider"));
                if (provider == null) {
                    log.warn("Can't find datasource provider " + atts.getValue("provider"));
                    curDataSource = null;
                    return;
                }
                DriverDescriptor driver = provider.getDriver(atts.getValue("driver"));
                if (driver == null) {
                    log.warn("Can't find driver " + atts.getValue("driver") + " in datasource provider " + provider.getId());
                    curDataSource = null;
                    return;
                }
                String id = atts.getValue("id");
                if (id == null) {
                    // Support of old version without ID
                    id = atts.getValue("name");
                }
                curDataSource = new DataSourceDescriptor(
                    DataSourceRegistry.this,
                    id,
                    driver,
                    new DBPConnectionInfo());
                curDataSource.setName(atts.getValue("name"));
                String createDate = atts.getValue("create-date");
                if (!CommonUtils.isEmpty(createDate)) {
                    curDataSource.setCreateDate(new Date(Long.parseLong(createDate)));
                }
                String udateDate = atts.getValue("udate-date");
                if (!CommonUtils.isEmpty(udateDate)) {
                    curDataSource.setUpdateDate(new Date(Long.parseLong(udateDate)));
                }
                String logineDate = atts.getValue("login-date");
                if (!CommonUtils.isEmpty(logineDate)) {
                    curDataSource.setLoginDate(new Date(Long.parseLong(logineDate)));
                }
                curDataSource.setSavePassword("true".equals(atts.getValue("save-password")));
                curDataSource.setShowSystemObjects("true".equals(atts.getValue("show-system-objects")));
                curDataSource.setCatalogFilter(atts.getValue("filter-catalog"));
                curDataSource.setSchemaFilter(atts.getValue("filter-schema"));

                dataSources.add(curDataSource);
            } else if (localName.equals("connection")) {
                if (curDataSource != null) {
                    curDataSource.getConnectionInfo().setHostName(atts.getValue("host"));
                    curDataSource.getConnectionInfo().setHostPort(atts.getValue("port"));
                    curDataSource.getConnectionInfo().setDatabaseName(atts.getValue("database"));
                    curDataSource.getConnectionInfo().setUrl(atts.getValue("url"));
                    curDataSource.getConnectionInfo().setUserName(atts.getValue("user"));
                    String encPassword = atts.getValue("password");
                    if (!CommonUtils.isEmpty(encPassword)) {
                        try {
                            encPassword = encrypter.decrypt(encPassword);
                        }
                        catch (Throwable e) {
                            // could not decrypt - use as is
                            encPassword = null;
                        }
                    }
                    curDataSource.getConnectionInfo().setUserPassword(encPassword);
                }
            } else if (localName.equals("property")) {
                if (curDataSource != null) {
                    curDataSource.getConnectionInfo().getProperties().put(
                        atts.getValue("name"),
                        atts.getValue("value"));
                }
            } else if (localName.equals("custom-property")) {
                if (curDataSource != null) {
                    curDataSource.getPreferenceStore().getProperties().put(
                        atts.getValue("name"),
                        atts.getValue("value"));
                }
            } else if (localName.equals("description")) {
                isDescription = true;
            }
        }

        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
            if (isDescription && curDataSource != null) {
                curDataSource.setDescription(data);
            }
        }

        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            isDescription = false;
        }
    }

    private class DisconnectTask implements DBRRunnableWithProgress {
        boolean disconnected;
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            for (DataSourceDescriptor dataSource : dataSources) {
                if (dataSource.isConnected()) {
                    try {
                        // Cancel al jobs
                        Job.getJobManager().cancel(dataSource.getDataSource());
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
