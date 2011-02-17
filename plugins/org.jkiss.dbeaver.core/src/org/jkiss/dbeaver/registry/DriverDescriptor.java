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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver
{
    static final Log log = LogFactory.getLog(DriverDescriptor.class);

    private DataSourceProviderDescriptor providerDescriptor;
    private String id;
    private String name, origName;
    private String description, origDescription;
    private String driverClassName, origClassName;
    private Integer driverDefaultPort, origDefaultPort;
    private String sampleURL, origSampleURL;
    private String webURL, origWebURL;
    private Image iconPlain;
    private Image iconNormal;
    private Image iconError;
    private boolean supportsDriverProperties;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private List<DriverLibraryDescriptor> libraries = new ArrayList<DriverLibraryDescriptor>(), origLibraries;
    private List<PropertyGroupDescriptor> connectionPropertyGroups = new ArrayList<PropertyGroupDescriptor>();

    private Map<String, String> defaultParameters = new HashMap<String, String>();
    private Map<String, String> customParameters = new HashMap<String, String>();

    private Map<String, String> defaultConnectionProperties = new HashMap<String, String>();
    private Map<String, String> customConnectionProperties = new HashMap<String, String>();

    private Class driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    private transient List<DataSourceDescriptor> usedBy = new ArrayList<DataSourceDescriptor>();

    private transient boolean isFailed = false;

    DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id)
    {
        super(providerDescriptor.getContributor());
        this.providerDescriptor = providerDescriptor;
        this.id = id;
        this.custom = true;
        this.iconPlain = new Image(null, providerDescriptor.getIcon(), SWT.IMAGE_COPY);
        makeIconExtensions();
    }

    DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, IConfigurationElement config)
    {
        super(providerDescriptor.getContributor());
        this.providerDescriptor = providerDescriptor;
        this.id = CommonUtils.getString(config.getAttribute(DataSourceConstants.ATTR_ID));
        this.origName = this.name = CommonUtils.getString(config.getAttribute("label"));
        this.origDescription = this.description = config.getAttribute(DataSourceConstants.ATTR_DESCRIPTION);
        this.origClassName = this.driverClassName = config.getAttribute(DataSourceConstants.ATTR_CLASS);
        if (config.getAttribute("defaultPort") != null) {
            try {
                this.origDefaultPort = this.driverDefaultPort = Integer.valueOf(config.getAttribute("defaultPort"));
            }
            catch (NumberFormatException ex) {
                log.warn("Bad default port for driver '" + name + "' specified: " + ex.getMessage());
            }
        }
        this.origSampleURL = this.sampleURL = config.getAttribute("sampleURL");
        this.origWebURL = this.webURL = config.getAttribute("webURL");
        this.supportsDriverProperties = !"false".equals(config.getAttribute("supportsDriverProperties"));
        this.custom = false;
        this.isLoaded = false;

        IConfigurationElement[] libElements = config.getChildren(DataSourceConstants.TAG_LIBRARY);
        for (IConfigurationElement lib : libElements) {
            this.libraries.add(new DriverLibraryDescriptor(this, lib));
        }
        this.origLibraries = new ArrayList<DriverLibraryDescriptor>(this.libraries);

        String iconName = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconName)) {
            this.iconPlain = iconToImage(iconName);
        }
        if (this.iconPlain == null) {
            this.iconPlain = new Image(null, providerDescriptor.getIcon(), SWT.IMAGE_COPY);
        }
        makeIconExtensions();

        {
            // Connection property groups
            IConfigurationElement[] propElements = config.getChildren(PropertyGroupDescriptor.PROPERTY_GROUP_TAG);
            for (IConfigurationElement prop : propElements) {
                connectionPropertyGroups.add(new PropertyGroupDescriptor(prop));
            }
        }

        // Connection default properties
        //connectionProperties

        {
            // Driver parameters
            IConfigurationElement[] paramElements = config.getChildren(DataSourceConstants.TAG_PARAMETER);
            for (IConfigurationElement param : paramElements) {
                String paramName = param.getAttribute(DataSourceConstants.ATTR_NAME);
                String paramValue = param.getAttribute(DataSourceConstants.ATTR_VALUE);
                if (CommonUtils.isEmpty(paramValue)) {
                    paramValue = param.getValue();
                }
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    defaultParameters.put(paramName, paramValue);
                    customParameters.put(paramName, paramValue);
                }
            }
        }

        {
            // Connection properties
            IConfigurationElement[] propElements = config.getChildren(DataSourceConstants.TAG_PROPERTY);
            for (IConfigurationElement param : propElements) {
                String paramName = param.getAttribute(DataSourceConstants.ATTR_NAME);
                String paramValue = param.getAttribute(DataSourceConstants.ATTR_VALUE);
                if (CommonUtils.isEmpty(paramValue)) {
                    paramValue = param.getValue();
                }
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    defaultConnectionProperties.put(paramName, paramValue);
                    customConnectionProperties.put(paramName, paramValue);
                }
            }
        }

        // Create class loader
        this.classLoader = new DriverClassLoader(
            new URL[0],
            //getClass().getClassLoader());
            providerDescriptor.getContributorBundle().getBundleContext().getClass().getClassLoader());
    }

    private void makeIconExtensions()
    {
        if (isCustom()) {
            OverlayImageDescriptor customDescriptor = new OverlayImageDescriptor(this.iconPlain.getImageData());
            customDescriptor.setBottomLeft(new ImageDescriptor[]{DBIcon.OVER_CONDITION.getImageDescriptor()});
            this.iconNormal = new Image(this.iconPlain.getDevice(), customDescriptor.getImageData());
        } else {
            this.iconNormal = new Image(this.iconPlain.getDevice(), iconPlain, SWT.IMAGE_COPY);
        }
        OverlayImageDescriptor failedDescriptor = new OverlayImageDescriptor(this.iconNormal.getImageData());
        failedDescriptor.setBottomRight(new ImageDescriptor[] {DBIcon.OVER_ERROR.getImageDescriptor()} );
        this.iconError = new Image(this.iconNormal.getDevice(), failedDescriptor.getImageData());
    }

    public void dispose()
    {
        if (iconPlain != null) {
            iconPlain.dispose();
            iconPlain = null;
        }
        if (iconNormal != null) {
            iconNormal.dispose();
            iconNormal = null;
        }
        if (iconError != null) {
            iconError.dispose();
            iconError = null;
        }
        if (!usedBy.isEmpty()) {
            log.error("Driver '" + getName() + "' still used by " + usedBy.size() + " data sources");
        }
    }

    void addUser(DataSourceDescriptor dataSourceDescriptor)
    {
        usedBy.add(dataSourceDescriptor);
    }

    void removeUser(DataSourceDescriptor dataSourceDescriptor)
    {
        usedBy.remove(dataSourceDescriptor);
    }

    public List<DataSourceDescriptor> getUsedBy()
    {
        return usedBy;
    }

    public DataSourceProviderDescriptor getProviderDescriptor()
    {
        return providerDescriptor;
    }

    public DBPDataSourceProvider getDataSourceProvider()
        throws DBException
    {
        return providerDescriptor.getInstance();
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Plain icon (without any overlays).
     * @return plain icon
     */
    public Image getPlainIcon()
    {
        return iconPlain;
    }

    /**
     * Driver icon, includes overlays for driver conditions (custom, invalid, etc)..
     * @return icon
     */
    public Image getIcon()
    {
        if (!isLoaded && (isFailed || (isManagable() && !isInternalDriver() && libraries.isEmpty()))) {
            return iconError;
        } else {
            return iconNormal;
        }
    }

    public boolean isCustom()
    {
        return custom;
    }

    public boolean isModified()
    {
        return modified;
    }

    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName)
    {
        if (this.driverClassName == null || !this.driverClassName.equals(driverClassName)) {
            this.driverClassName = driverClassName;
            this.driverInstance = null;
            this.driverClass = null;
            this.isLoaded = false;
        }
    }

    public Object getDriverInstance()
        throws DBException
    {
        if (driverInstance == null) {
            loadDriver();
        }
        return driverInstance;
    }

    Object createDriverInstance()
        throws DBException
    {
        try {
            return driverClass.newInstance();
        }
        catch (InstantiationException ex) {
            throw new DBException("Can't instantiate driver class", ex);
        }
        catch (IllegalAccessException ex) {
            throw new DBException("Illegal access", ex);
        }
        catch (ClassCastException ex) {
            throw new DBException("Bad driver class name specified", ex);
        }
        catch (Throwable ex) {
            throw new DBException("Error during driver instantiation", ex);
        }
    }

    public Integer getDefaultPort()
    {
        return driverDefaultPort;
    }

    public void setDriverDefaultPort(Integer driverDefaultPort)
    {
        this.driverDefaultPort = driverDefaultPort;
    }

    public String getSampleURL()
    {
        return sampleURL;
    }

    public void setSampleURL(String sampleURL)
    {
        this.sampleURL = sampleURL;
    }

    public String getWebURL()
    {
        return webURL;
    }

    public boolean supportsDriverProperties()
    {
        return this.supportsDriverProperties;
    }

    public boolean isLoaded()
    {
        return isLoaded;
    }

    public boolean isManagable()
    {
        return getProviderDescriptor().isDriversManagable();
    }

    public boolean isInternalDriver()
    {
        return driverClassName != null && driverClassName.indexOf("sun.jdbc") != -1;
    }

    public Class getDriverClass()
    {
        return driverClass;
    }

    public DriverClassLoader getClassLoader()
    {
        return classLoader;
    }

    public List<DriverLibraryDescriptor> getLibraries()
    {
        return libraries;
    }

    public DriverLibraryDescriptor getLibrary(String path)
    {
        for (DriverLibraryDescriptor lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
    }

    public DriverLibraryDescriptor addLibrary(String path)
    {
        DriverLibraryDescriptor lib = new DriverLibraryDescriptor(this, path);
        this.libraries.add(lib);
        return lib;
    }

    public boolean removeLibrary(DriverLibraryDescriptor lib)
    {
        if (!lib.isCustom()) {
            lib.setDisabled(true);
            return true;
        } else {
            return this.libraries.remove(lib);
        }
    }

    public List<PropertyGroupDescriptor> getConnectionPropertyGroups()
    {
        return connectionPropertyGroups;
    }

    public Map<String, String> getDefaultConnectionProperties()
    {
        return defaultConnectionProperties;
    }

    public Map<String, String> getConnectionProperties()
    {
        return customConnectionProperties;
    }

    public void setConnectionProperty(String name, String value)
    {
        customConnectionProperties.put(name, value);
    }

    public void setConnectionProperties(Map<String, String> parameters)
    {
        customConnectionProperties.clear();
        customConnectionProperties.putAll(parameters);
    }

    public Map<String, String> getDefaultDriverParameters()
    {
        return defaultParameters;
    }

    public Map<String, String> getDriverParameters()
    {
        return customParameters;
    }

    public String getDriverParameter(String name)
    {
        return customParameters.get(name);
    }

    public void setDriverParameter(String name, String value)
    {
        customParameters.put(name, value);
    }

    public void setDriverParameters(Map<String, String> parameters)
    {
        customParameters.clear();
        customParameters.putAll(parameters);
    }

    public void loadDriver()
        throws DBException
    {
        this.loadDriver(false);
    }

    public void loadDriver(boolean forceReload)
        throws DBException
    {
        if (isLoaded && !forceReload) {
            return;
        }
        isLoaded = false;

        if (isManagable()) {
            loadLibraries();
        }

        try {
            try {
                if (!isManagable()) {
                    // Use plugin's classloader to load driver
                    driverClass = super.getObjectClass(driverClassName);
                } else {
                    if (this.isInternalDriver()) {
                        // Use system classloader
                        driverClass = Class.forName(driverClassName);
                    } else {
                        // Load driver classes into core module using plugin class loader
                        driverClass = Class.forName(driverClassName, true, classLoader);
                    }
                }
            }
            catch (ClassNotFoundException ex) {
                throw new DBException("Can't load driver class '" + driverClassName + "'", ex);
            }

            // Create driver instance
            if (!this.isInternalDriver()) {
                driverInstance = createDriverInstance();
            }

            isLoaded = true;
            isFailed = false;
        } catch (DBException e) {
            isFailed = true;
            throw e;
        }
    }

    private void loadLibraries()
    {
        this.classLoader = null;

        List<URL> libraryURLs = new ArrayList<URL>();
        // Load libraries
        for (DriverLibraryDescriptor library : libraries) {
            if (library.isDisabled()) {
                continue;
            }
            URL url = getLibraryURL(library.getPath());
            if (url != null) {
                libraryURLs.add(url);
            }
        }
        // Make class loader
        this.classLoader = new DriverClassLoader(
            libraryURLs.toArray(new URL[libraryURLs.size()]),
            ClassLoader.getSystemClassLoader());
    }

    public String getOrigName()
    {
        return origName;
    }

    public String getOrigDescription()
    {
        return origDescription;
    }

    public String getOrigClassName()
    {
        return origClassName;
    }

    public Integer getOrigDefaultPort()
    {
        return origDefaultPort;
    }

    public String getOrigSampleURL()
    {
        return origSampleURL;
    }

    public String getOrigWebURL()
    {
        return origWebURL;
    }

    public List<DriverLibraryDescriptor> getOrigLibraries()
    {
        return origLibraries;
    }

    public URL getLibraryURL(String path)
    {
        URL url = getProviderDescriptor().getContributorBundle().getEntry(path);
        if (url != null) {
            try {
                url = FileLocator.toFileURL(url);
            }
            catch (IOException ex) {
                log.warn(ex);
            }
        }
        // Try to use direct path
        if (url == null) {
            File libraryFile = new File(path);
            if (!libraryFile.exists()) {
                // File not exists - try to use relative path
                Location location = Platform.getInstallLocation();
                try {
                    url = location.getDataArea(path);
                    File platformFile = new File(url.getFile());
                    if (!platformFile.exists()) {
                        // Relative file do not exists - use plain one
                        url = libraryFile.toURI().toURL();
                    }
                } catch (IOException e) {
                    log.warn(e);
                }
            } else {
                try {
                    url = new URL("file:/" + libraryFile.toString());
                }
                catch (MalformedURLException ex) {
                    log.warn(ex);
                }
            }
        }
        return url;
    }

    public void serialize(XMLBuilder xml, boolean export)
        throws IOException
    {
        xml.startElement(DataSourceConstants.TAG_DRIVER);
        if (export) {
            xml.addAttribute(DataSourceConstants.ATTR_PROVIDER, providerDescriptor.getId());
        }
        xml.addAttribute(DataSourceConstants.ATTR_ID, this.getId());
        if (this.isDisabled()) {
            xml.addAttribute(DataSourceConstants.ATTR_DISABLED, true);
        }
        xml.addAttribute(DataSourceConstants.ATTR_CUSTOM, this.isCustom());
        xml.addAttribute(DataSourceConstants.ATTR_NAME, this.getName());
        xml.addAttribute(DataSourceConstants.ATTR_CLASS, this.getDriverClassName());
        xml.addAttribute(DataSourceConstants.ATTR_URL, this.getSampleURL());
        if (this.getDefaultPort() != null) {
            xml.addAttribute(DataSourceConstants.ATTR_PORT, this.getDefaultPort().toString());
        }
        xml.addAttribute(DataSourceConstants.ATTR_DESCRIPTION, CommonUtils.getString(this.getDescription()));

        // Libraries
        for (DriverLibraryDescriptor lib : this.getLibraries()) {
            if ((export && !lib.isDisabled()) || lib.isCustom() || lib.isDisabled()) {
                xml.startElement(DataSourceConstants.TAG_LIBRARY);
                xml.addAttribute(DataSourceConstants.ATTR_PATH, lib.getPath());
                if (lib.isDisabled()) {
                    xml.addAttribute(DataSourceConstants.ATTR_DISABLED, true);
                }
                xml.endElement();
            }
        }

        // Parameters
        for (Map.Entry<String, String> paramEntry : customParameters.entrySet()) {
            if (!CommonUtils.equalObjects(paramEntry.getValue(), defaultParameters.get(paramEntry.getKey()))) {
                xml.startElement(DataSourceConstants.TAG_PARAMETER);
                xml.addAttribute(DataSourceConstants.ATTR_NAME, paramEntry.getKey());
                xml.addAttribute(DataSourceConstants.ATTR_VALUE, paramEntry.getValue());
                xml.endElement();
            }
        }

        // Properties
        for (Map.Entry<String, String> propEntry : customConnectionProperties.entrySet()) {
            if (!CommonUtils.equalObjects(propEntry.getValue(), defaultConnectionProperties.get(propEntry.getKey()))) {
                xml.startElement(DataSourceConstants.TAG_PROPERTY);
                xml.addAttribute(DataSourceConstants.ATTR_NAME, propEntry.getKey());
                xml.addAttribute(DataSourceConstants.ATTR_VALUE, propEntry.getValue());
                xml.endElement();
            }
        }

        xml.endElement();
    }

    static class DriversParser implements SAXListener
    {
        DataSourceProviderDescriptor curProvider;
        DriverDescriptor curDriver;

        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(DataSourceConstants.TAG_PROVIDER)) {
                curProvider = null;
                curDriver = null;
                String idAttr = atts.getValue(DataSourceConstants.ATTR_ID);
                if (CommonUtils.isEmpty(idAttr)) {
                    log.warn("No id for driver provider");
                    return;
                }
                curProvider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(idAttr);
                if (curProvider == null) {
                    log.warn("Datasource provider '" + idAttr + "' not found");
                }
            } else if (localName.equals(DataSourceConstants.TAG_DRIVER)) {
                curDriver = null;
                if (curProvider == null) {
                    String providerId = atts.getValue(DataSourceConstants.ATTR_PROVIDER);
                    if (!CommonUtils.isEmpty(providerId)) {
                        curProvider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(providerId);
                        if (curProvider == null) {
                            log.warn("Datasource provider '" + providerId + "' not found");
                        }
                    }
                    if (curProvider == null) {
                        log.warn("Driver outside of datasource provider");
                        return;
                    }
                }
                String idAttr = atts.getValue(DataSourceConstants.ATTR_ID);
                curDriver = curProvider.getDriver(idAttr);
                if (curDriver == null) {
                    curDriver = new DriverDescriptor(curProvider, idAttr);
                    curProvider.addDriver(curDriver);
                }
                curDriver.setName(atts.getValue(DataSourceConstants.ATTR_NAME));
                curDriver.setDescription(atts.getValue(DataSourceConstants.ATTR_DESCRIPTION));
                curDriver.setDriverClassName(atts.getValue(DataSourceConstants.ATTR_CLASS));
                curDriver.setSampleURL(atts.getValue(DataSourceConstants.ATTR_URL));
                String portStr = atts.getValue(DataSourceConstants.ATTR_PORT);
                if (portStr != null) {
                    try {
                        curDriver.setDriverDefaultPort(Integer.valueOf(portStr));
                    }
                    catch (NumberFormatException e) {
                        log.warn("Bad driver '" + curDriver.getName() + "' port specified: " + portStr);
                    }
                }
                curDriver.setModified(true);
                String disabledAttr = atts.getValue(DataSourceConstants.ATTR_DISABLED);
                if ("true".equals(disabledAttr)) {
                    curDriver.setDisabled(true);
                }
            } else if (localName.equals(DataSourceConstants.TAG_LIBRARY)) {
                if (curDriver == null) {
                    log.warn("Library outside of driver");
                    return;
                }
                String path = atts.getValue(DataSourceConstants.ATTR_PATH);
                DriverLibraryDescriptor lib = curDriver.getLibrary(path);
                String disabledAttr = atts.getValue(DataSourceConstants.ATTR_DISABLED);
                if (lib != null && "true".equals(disabledAttr)) {
                    lib.setDisabled(true);
                } else if (lib == null) {
                    curDriver.addLibrary(path);
                }
            } else if (localName.equals(DataSourceConstants.TAG_PARAMETER)) {
                if (curDriver == null) {
                    log.warn("Parameter outside of driver");
                    return;
                }
                final String paramName = atts.getValue(DataSourceConstants.ATTR_NAME);
                final String paramValue = atts.getValue(DataSourceConstants.ATTR_VALUE);
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    curDriver.setDriverParameter(paramName, paramValue);
                }
            } else if (localName.equals(DataSourceConstants.TAG_PROPERTY)) {
                if (curDriver == null) {
                    log.warn("Property outside of driver");
                    return;
                }
                final String paramName = atts.getValue(DataSourceConstants.ATTR_NAME);
                final String paramValue = atts.getValue(DataSourceConstants.ATTR_VALUE);
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    curDriver.setConnectionProperty(paramName, paramValue);
                }
            }
        }

        public void saxText(SAXReader reader, String data) {}

        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {}
    }

}

