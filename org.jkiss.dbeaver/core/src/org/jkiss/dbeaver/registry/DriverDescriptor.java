/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPDriverCustomQuery;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver
{
    static final Log log = LogFactory.getLog(DriverDescriptor.class);

    private DataSourceProviderDescriptor providerDescriptor;
    private String id;
    private String name;
    private String description;
    private Image icon;
    private String driverClassName;
    private Integer driverDefaultPort;
    private String sampleURL;
    private String webURL;
    private boolean supportsDriverProperties;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private List<DriverLibraryDescriptor> libraries = new ArrayList<DriverLibraryDescriptor>();
    private List<DriverPropertyGroupDescriptor> propertyGroups = new ArrayList<DriverPropertyGroupDescriptor>();
    private List<DriverCustomQueryDescriptor> customQueries = new ArrayList<DriverCustomQueryDescriptor>();

    private Class driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id)
    {
        super(providerDescriptor.getContributor());
        this.providerDescriptor = providerDescriptor;
        this.id = id;
        this.custom = true;
        this.icon = DBIcon.GEN_DATABASE.getImage();
    }

    DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, IConfigurationElement config)
    {
        super(providerDescriptor.getContributor());
        this.providerDescriptor = providerDescriptor;
        this.id = CommonUtils.getString(config.getAttribute("id"));
        this.name = CommonUtils.getString(config.getAttribute("label"));
        this.description = config.getAttribute("description");
        String iconName = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconName)) {
            this.icon = iconToImage(iconName);
        }
        if (this.icon == null) {
            this.icon = DBIcon.GEN_DATABASE.getImage();
        }
        this.driverClassName = config.getAttribute("class");
        if (config.getAttribute("defaultPort") != null) {
            try {
                this.driverDefaultPort = new Integer(config.getAttribute("defaultPort"));
            }
            catch (NumberFormatException ex) {
                log.warn("Bad default port for driver '" + name + "' specified: " + ex.getMessage());
            }
        }
        this.sampleURL = config.getAttribute("sampleURL");
        this.webURL = config.getAttribute("webURL");
        this.supportsDriverProperties = !"false".equals(config.getAttribute("supportsDriverProperties"));
        this.custom = false;
        this.isLoaded = false;

        IConfigurationElement[] libElements = config.getChildren("library");
        for (IConfigurationElement lib : libElements) {
            libraries.add(new DriverLibraryDescriptor(this, lib));
        }

        IConfigurationElement[] propElements = config.getChildren("propertyGroup");
        for (IConfigurationElement prop : propElements) {
            propertyGroups.add(new DriverPropertyGroupDescriptor(this, prop));
        }

        IConfigurationElement[] queryElements = config.getChildren("query");
        for (IConfigurationElement query : queryElements) {
            customQueries.add(new DriverCustomQueryDescriptor(query));
        }

        // Create class loader
        this.classLoader = new DriverClassLoader(
            new URL[0],
            //getClass().getClassLoader());
            providerDescriptor.getContributorBundle().getBundleContext().getClass().getClassLoader());
    }

    public void dispose()
    {
        if (icon != null) {
            icon.dispose();
        }
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

    public void setId(String id)
    {
        this.id = id;
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

    public Image getIcon()
    {
        return icon;
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

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
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

    public List<DriverPropertyGroupDescriptor> getPropertyGroups()
    {
        return propertyGroups;
    }

    public List<? extends DBPDriverCustomQuery> getCustomQueries()
    {
        return customQueries;
    }

    public String getCustomQuery(String name)
    {
        for (DBPDriverCustomQuery query : customQueries) {
            if (query.getName().equals(name)) {
                return query.getQuery();
            }
        }
        return null;
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
            if (!isManagable()) {
                // Use plugin's classloader to load driver
                driverClass = getProviderDescriptor().getContributorBundle().loadClass(driverClassName);
            } else {
                // Load driver classes into core module using plugin class loader
                driverClass = Class.forName(driverClassName, true, classLoader);
            }
        }
        catch (ClassNotFoundException ex) {
            throw new DBException("Can't load driver class '" + driverClassName + "'", ex);
        }

        // Create driver instance
        driverInstance = createDriverInstance();

        isLoaded = true;
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
}

