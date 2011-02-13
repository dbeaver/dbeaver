/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.XMLBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;

import java.io.IOException;
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
    private List<PropertyGroupDescriptor> propertyGroups = new ArrayList<PropertyGroupDescriptor>();
    private Map<String, String> defaultParameters = new HashMap<String, String>();
    private Map<String, String> customParameters = new HashMap<String, String>();

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

        // Connection property groups
        IConfigurationElement[] propElements = config.getChildren(PropertyGroupDescriptor.PROPERTY_GROUP_TAG);
        for (IConfigurationElement prop : propElements) {
            propertyGroups.add(new PropertyGroupDescriptor(prop));
        }

        // Driver parameters
        IConfigurationElement[] paramElements = config.getChildren("parameter");
        for (IConfigurationElement param : paramElements) {
            String paramName = param.getAttribute(DataSourceConstants.ATTR_NAME);
            String paramValue = param.getAttribute("value");
            if (CommonUtils.isEmpty(paramValue)) {
                paramValue = param.getValue();
            }
            if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                defaultParameters.put(paramName, paramValue);
                customParameters.put(paramName, paramValue);
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
        return propertyGroups;
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
            URL url = library.getLibraryURL();
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

        xml.endElement();
    }

}

