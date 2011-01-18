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
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DataSourceProviderRegistry
{
    static final Log log = LogFactory.getLog(DataSourceProviderRegistry.class);

    private final List<DataSourceProviderDescriptor> dataSourceProviders = new ArrayList<DataSourceProviderDescriptor>();
    private final List<DataTypeProviderDescriptor> dataTypeProviders = new ArrayList<DataTypeProviderDescriptor>();

    public DataSourceProviderRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
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
    }

    public void dispose()
    {
        for (DataSourceProviderDescriptor providerDescriptor : this.dataSourceProviders) {
            providerDescriptor.dispose();
        }
        this.dataSourceProviders.clear();

        this.dataSourceProviders.clear();
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

    ////////////////////////////////////////////////////
    // DataType providers

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

    public static DataSourceProviderRegistry getDefault()
    {
        return DBeaverCore.getInstance().getDataSourceProviderRegistry();
    }

    private void loadDrivers()
    {
        File driversConfig = new File(DBeaverCore.getInstance().getRootPath().toFile(), "drivers.xml");
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
        File driversConfig = new File(DBeaverCore.getInstance().getRootPath().toFile(), "drivers.xml");
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
}
