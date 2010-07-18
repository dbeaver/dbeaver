/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.SAXListener;
import net.sf.jkiss.utils.xml.SAXReader;
import net.sf.jkiss.utils.xml.XMLBuilder;
import net.sf.jkiss.utils.xml.XMLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPRegistry;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.registry.event.IDataSourceListener;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.StringEncrypter;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class DataSourceRegistry implements DBPRegistry
{

    static final Log log = LogFactory.getLog(DataSourceRegistry.class);

    private DBeaverCore core;
    private File workspaceRoot;
    private final List<DataSourceProviderDescriptor> dataSourceProviders = new ArrayList<DataSourceProviderDescriptor>();
    private final List<DataTypeProviderDescriptor> dataTypeProviders = new ArrayList<DataTypeProviderDescriptor>();
    private final List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private final List<IDataSourceListener> dataSourceListeners = new ArrayList<IDataSourceListener>();

    private StringEncrypter encrypter;
    private static final String PASSWORD_ENCRYPTION_KEY = "sdf@!#$verf^wv%6Fwe%$$#FFGwfsdefwfe135s$^H)dg";

    public DataSourceRegistry(DBeaverCore core, IExtensionRegistry registry)
    {
        this.core = core;
        this.workspaceRoot = core.getRootPath().toFile();
        try {
            this.encrypter = new StringEncrypter(StringEncrypter.SCHEME_DES, PASSWORD_ENCRYPTION_KEY);
        }
        catch (StringEncrypter.EncryptionException e) {
            // never be here
            log.error(e);
        }

        // Load datasource providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataSourceProviderDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DataSourceProviderDescriptor provider = new DataSourceProviderDescriptor(this, ext);
                dataSourceProviders.add(provider);
            }
            Collections.sort(dataSourceProviders, new Comparator<DataSourceProviderDescriptor>() {
                public int compare(DataSourceProviderDescriptor o1, DataSourceProviderDescriptor o2)
                {
                    if (o1.isDriversManagable() && !o2.isDriversManagable()) {
                        return 1;
                    }
                    if (o2.isDriversManagable() && !o1.isDriversManagable()) {
                        return -1;
                    }
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }

        // Load data type providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataTypeProviderDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DataTypeProviderDescriptor provider = new DataTypeProviderDescriptor(this, ext);
                dataTypeProviders.add(provider);
            }
        }

        // Load drivers
        loadDrivers();
        // Load datasources
        loadDataSources();
    }

    public void dispose()
    {
        synchronized (dataSourceListeners) {
            if (!this.dataSourceListeners.isEmpty()) {
                log.warn("Some data source listeners are still registered: " + dataSourceListeners);
            }
            this.dataSourceListeners.clear();
        }

        closeConnections();
        for (DataSourceDescriptor dataSourceDescriptor : this.dataSources) {
            dataSourceDescriptor.dispose();
        }
        this.dataSources.clear();

        for (DataSourceProviderDescriptor providerDescriptor : this.dataSourceProviders) {
            providerDescriptor.dispose();
        }
        this.dataSourceProviders.clear();
    }

    public void closeConnections()
    {
        for (DataSourceDescriptor dataSource : dataSources) {
            if (dataSource.isConnected()) {
                try {
                    dataSource.disconnect(this);
                } catch (DBException ex) {
                    log.error("Can't shutdown data source", ex);
                }
            }
        }
    }

    public DBeaverCore getCore()
    {
        return core;
    }

    public DataSourceProviderDescriptor getDataSourceProvider(String id)
    {
        for (DataSourceProviderDescriptor provider : dataSourceProviders) {
            if (provider.getId().equals(id)) {
                return provider;
            }
        }
        return null;
    }

    public List<DataSourceProviderDescriptor> getDataSourceProviders()
    {
        return dataSourceProviders;
    }

    public List<DataTypeProviderDescriptor> getDataTypeProviders()
    {
        return dataTypeProviders;
    }

    public DataTypeProviderDescriptor getDataTypeProvider(DBPDataSource dataSource, DBSTypedObject type)
    {
        DBPDriver driver = dataSource.getContainer().getDriver();
        if (!(driver instanceof DriverDescriptor)) {
            log.warn("Bad datasource specified (driver is not recognized by registry) - " + dataSource);
            return null;
        }
        DataSourceProviderDescriptor dsProvider = ((DriverDescriptor) driver).getProviderDescriptor();

        // First try to find type provider for specific datasource type
        for (DataTypeProviderDescriptor dtProvider : dataTypeProviders) {
            if (!dtProvider.isDefault() && dtProvider.supportsDataSource(dsProvider) && dtProvider.supportsType(type)) {
                return dtProvider;
            }
        }

        // Find in default providers
        for (DataTypeProviderDescriptor dtProvider : dataTypeProviders) {
            if (dtProvider.isDefault() && dtProvider.supportsType(type)) {
                return dtProvider;
            }
        }
        return null;
    }

    public DataSourceDescriptor getDataSource(String name)
    {
        for (DataSourceDescriptor dsd : dataSources) {
            if (dsd.getName().equals(name)) {
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

    public List<DataSourceDescriptor> getDataSources()
    {
        List<DataSourceDescriptor> dsCopy = new ArrayList<DataSourceDescriptor>(dataSources);
        Collections.sort(dsCopy, new Comparator<DataSourceDescriptor>() {
            public int compare(DataSourceDescriptor o1, DataSourceDescriptor o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return dsCopy;
    }

    public void addDataSource(DataSourceDescriptor dataSource)
    {
        this.dataSources.add(dataSource);
        this.saveDataSources();
        this.notifyDataSourceListeners(DataSourceEvent.Action.ADD, dataSource, this);
    }

    public void removeDataSource(DataSourceDescriptor dataSource)
    {
        this.dataSources.remove(dataSource);
        this.saveDataSources();
        this.notifyDataSourceListeners(DataSourceEvent.Action.REMOVE, dataSource, this);
    }

    public void updateDataSource(DataSourceDescriptor dataSource)
    {
        this.saveDataSources();
        this.notifyDataSourceListeners(DataSourceEvent.Action.CHANGE, dataSource, this);
    }

    public void flushConfig()
    {
        this.saveDataSources();
    }

    public void addDataSourceListener(IDataSourceListener listener)
    {
        synchronized (dataSourceListeners) {
            dataSourceListeners.add(listener);
        }
    }

    public boolean removeDataSourceListener(IDataSourceListener listener)
    {
        synchronized (dataSourceListeners) {
            return dataSourceListeners.remove(listener);
        }
    }

    void fireDataSourceEvent(
        DataSourceEvent.Action action,
        DataSourceDescriptor dataSource,
        Object source)
    {
        notifyDataSourceListeners(action, dataSource, source);
    }

    public void fireDataSourceEvent(
        DataSourceEvent.Action action,
        DBPDataSource dataSource,
        Object source)
    {
        DataSourceDescriptor dataSourceDescriptor = getDataSource(dataSource);
        if (dataSourceDescriptor == null) {
            log.error("Bad data source parameter: " + dataSource);
        } else {
            notifyDataSourceListeners(action, dataSourceDescriptor, source);
        }
    }

    private void notifyDataSourceListeners(
        DataSourceEvent.Action action,
        DataSourceDescriptor dataSource,
        Object source)
    {
        if (dataSourceListeners.isEmpty()) {
            return;
        }
        final DataSourceEvent event = new DataSourceEvent(source, action, dataSource);
        final List<IDataSourceListener> listeners;
        synchronized (dataSourceListeners) {
            listeners = new ArrayList<IDataSourceListener>(dataSourceListeners);
        }
        //Display display = this.core.getWorkbench().getDisplay();
        for (IDataSourceListener listener : listeners) {
            listener.handleDataSourceEvent(event);
        }
/*
            display.asyncExec(
                new Runnable() {
                    public void run() {
                        for (IDataSourceListener listener : listeners) {
                            listener.handleDataSourceEvent(event);
                        }
                    }
                }
            );
*/
    }

    public static DataSourceRegistry getDefault()
    {
        return DBeaverCore.getInstance().getDataSourceRegistry();
    }

    private void loadDrivers()
    {
        File driversConfig = new File(workspaceRoot, "drivers.xml");
        if (driversConfig.exists()) {
            try {
                InputStream is = new FileInputStream(driversConfig);
                try {
                    try {
                        loadDrivers(is);
                    } catch (DBException ex) {
                        log.warn("Can't load drivers config from " + driversConfig.getPath(), ex);
                    }
                    finally {
                        is.close();
                    }
                }
                catch (IOException ex) {
                    log.warn("IO error", ex);
                }
            } catch (FileNotFoundException ex) {
                log.warn("Can't open config file " + driversConfig.getPath(), ex);
            }
        }
    }

    private void loadDrivers(InputStream is)
        throws DBException, IOException
    {
        SAXReader parser = new SAXReader(is);
        try {
            parser.parse(new DriversParser());
        }
        catch (XMLException ex) {
            throw new DBException("Datasource config parse error", ex);
        }
    }

    public void saveDrivers()
    {
        File driversConfig = new File(workspaceRoot, "drivers.xml");
        try {
            OutputStream os = new FileOutputStream(driversConfig);
            try {
                XMLBuilder xml = new XMLBuilder(os, "utf-8");
                xml.setButify(true);
                xml.startElement("drivers");
                for (DataSourceProviderDescriptor provider : this.dataSourceProviders) {
                    xml.startElement("provider");
                    xml.addAttribute("id", provider.getId());
                    for (DriverDescriptor driver : provider.getDrivers()) {
                        if (driver.isModified()) {
                            saveDriver(xml, driver);
                        }
                    }
                    xml.endElement();
                }
                xml.endElement();
                xml.flush();
                os.close();
            }
            catch (IOException ex) {
                log.warn("IO error", ex);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Can't open config file " + driversConfig.getPath(), ex);
        }
    }

    private void saveDriver(XMLBuilder xml, DriverDescriptor driver)
        throws IOException
    {
        xml.startElement("driver");
        xml.addAttribute("id", driver.getId());
        if (driver.isDisabled()) {
            xml.addAttribute("disabled", true);
        }
        xml.addAttribute("custom", driver.isCustom());
        xml.addAttribute("name", driver.getName());
        xml.addAttribute("class", driver.getDriverClassName());
        xml.addAttribute("url", driver.getSampleURL());
        if (driver.getDefaultPort() != null) {
            xml.addAttribute("port", driver.getDefaultPort().toString());
        }
        xml.addAttribute("description", CommonUtils.getString(driver.getDescription()));
        for (DriverLibraryDescriptor lib : driver.getLibraries()) {
            if (lib.isCustom() || lib.isDisabled()) {
                xml.startElement("library");
                xml.addAttribute("path", lib.getPath());
                if (lib.isDisabled()) {
                    xml.addAttribute("disabled", true);
                }
                xml.endElement();
            }
        }
        xml.endElement();
    }

    private void loadDataSources()
    {
        File projectConfig = new File(workspaceRoot, "data-sources.xml");
        if (projectConfig.exists()) {
            try {
                InputStream is = new FileInputStream(projectConfig);
                try {
                    try {
                        loadDataSources(is);
                    } catch (DBException ex) {
                        log.warn("Can't load datasource config from " + projectConfig.getPath(), ex);
                    }
                    finally {
                        is.close();
                    }
                }
                catch (IOException ex) {
                    log.warn("IO error", ex);
                }
            } catch (FileNotFoundException ex) {
                log.warn("Can't open config file " + projectConfig.getPath(), ex);
            }
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
        File projectConfig = new File(workspaceRoot, "data-sources.xml");
        try {
            OutputStream os = new FileOutputStream(projectConfig);
            try {
                XMLBuilder xml = new XMLBuilder(os, "utf-8");
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
                log.warn("IO error", ex);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Can't open config file " + projectConfig.getPath(), ex);
        }
    }

    private void saveDataSource(XMLBuilder xml, DataSourceDescriptor dataSource)
        throws IOException
    {
        xml.startElement("data-source");
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
        {
            DBPConnectionInfo connectionInfo = dataSource.getConnectionInfo();
            xml.startElement("connection");
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                xml.addAttribute("host", connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                xml.addAttribute("port", connectionInfo.getHostPort());
            }
            xml.addAttribute("database", CommonUtils.getString(connectionInfo.getDatabaseName()));
            xml.addAttribute("url", CommonUtils.getString(connectionInfo.getJdbcURL()));
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

        // Pereferences
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

    private class DriversParser implements SAXListener
    {
        DataSourceProviderDescriptor curProvider;
        DriverDescriptor curDriver;

        private DriversParser()
        {
        }

        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals("provider")) {
                curProvider = null;
                curDriver = null;
                String idAttr = atts.getValue("id");
                if (CommonUtils.isEmpty(idAttr)) {
                    log.warn("No id for driver provider");
                    return;
                }
                curProvider = getDataSourceProvider(idAttr);
                if (curProvider == null) {
                    log.warn("Provider '" + idAttr + "' not found");
                }
            } else if (localName.equals("driver")) {
                curDriver = null;
                if (curProvider == null) {
                    log.warn("Driver outside of datasource provider");
                    return;
                }
                String idAttr = atts.getValue("id");
                curDriver = curProvider.getDriver(idAttr);
                if (curDriver == null) {
                    curDriver = new DriverDescriptor(curProvider, idAttr);
                    curProvider.addDriver(curDriver);
                }
                curDriver.setName(atts.getValue("name"));
                curDriver.setDescription(atts.getValue("description"));
                curDriver.setDriverClassName(atts.getValue("class"));
                curDriver.setSampleURL(atts.getValue("url"));
                String portStr = atts.getValue("port");
                if (portStr != null) {
                    try {
                        curDriver.setDriverDefaultPort(new Integer(portStr));
                    }
                    catch (NumberFormatException e) {
                        log.warn("Bad driver '" + curDriver.getName() + "' port specified: " + portStr);
                    }
                }
                curDriver.setModified(true);
                String disabledAttr = atts.getValue("disabled");
                if ("true".equals(disabledAttr)) {
                    curDriver.setDisabled(true);
                }
            } else if (localName.equals("library")) {
                if (curDriver == null) {
                    log.warn("Library outside of driver");
                    return;
                }
                String path = atts.getValue("path");
                DriverLibraryDescriptor lib = curDriver.getLibrary(path);
                String disabledAttr = atts.getValue("disabled");
                if (lib != null && "true".equals(disabledAttr)) {
                    lib.setDisabled(true);
                } else if (lib == null) {
                    curDriver.addLibrary(path);
                }
            }
        }

        public void saxText(SAXReader reader, String data) {}

        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {}
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
                DataSourceProviderDescriptor provider = getDataSourceProvider(atts.getValue("provider"));
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
                curDataSource = new DataSourceDescriptor(
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
                dataSources.add(curDataSource);
            } else if (localName.equals("connection")) {
                if (curDataSource != null) {
                    curDataSource.getConnectionInfo().setHostName(atts.getValue("host"));
                    curDataSource.getConnectionInfo().setHostPort(atts.getValue("port"));
                    curDataSource.getConnectionInfo().setDatabaseName(atts.getValue("database"));
                    curDataSource.getConnectionInfo().setJdbcURL(atts.getValue("url"));
                    curDataSource.getConnectionInfo().setUserName(atts.getValue("user"));
                    String encPassword = atts.getValue("password");
                    if (!CommonUtils.isEmpty(encPassword)) {
                        try {
                            encPassword = encrypter.decrypt(encPassword);
                        }
                        catch (Throwable e) {
                            // coould not decrypt - use as is
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

}
