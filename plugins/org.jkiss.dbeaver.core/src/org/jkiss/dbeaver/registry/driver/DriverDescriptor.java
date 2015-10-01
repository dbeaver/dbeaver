/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AcceptLicenseDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverDownloadDialog;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver
{
    public static final String PROP_DRIVERS_LOCATION = "DRIVERS_LOCATION";
    static final Log log = Log.getLog(DriverDescriptor.class);

    public static final String DRIVERS_FOLDER = "drivers"; //$NON-NLS-1$

    public static final char URL_GROUP_START = '{'; //$NON-NLS-1$
    public static final char URL_GROUP_END = '}'; //$NON-NLS-1$
    public static final char URL_OPTIONAL_START = '['; //$NON-NLS-1$
    public static final char URL_OPTIONAL_END = ']'; //$NON-NLS-1$
    public static final String PROP_HOST = "host"; //$NON-NLS-1$
    public static final String PROP_PORT = "port"; //$NON-NLS-1$
    public static final String PROP_DATABASE = "database"; //$NON-NLS-1$
    public static final String PROP_SERVER = "server"; //$NON-NLS-1$
    public static final String PROP_FOLDER = "folder"; //$NON-NLS-1$
    public static final String PROP_FILE = "file"; //$NON-NLS-1$
    public static final String PROP_USER = "user"; //$NON-NLS-1$
    public static final String PROP_PASSWORD = "password"; //$NON-NLS-1$

    private static final String LICENSE_ACCEPT_KEY = "driver.license.accept.";

    private final DataSourceProviderDescriptor providerDescriptor;
    private final String id;
    private String category;
    private String name, origName;
    private String description, origDescription;
    private String driverClassName, origClassName;
    private String driverDefaultPort, origDefaultPort;
    private String sampleURL, origSampleURL;
    private String note;
    private String webURL;
    private DBPImage iconPlain;
    private DBPImage iconNormal;
    private DBPImage iconError;
    private boolean embedded;
    private boolean clientRequired;
    private boolean supportsDriverProperties;
    private boolean anonymousAccess;
    private boolean customDriverLoader;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private final List<String> clientHomeIds = new ArrayList<String>();
    private final List<DriverFileSource> fileSources = new ArrayList<DriverFileSource>();
    private final List<DriverLibraryDescriptor> libraries = new ArrayList<DriverLibraryDescriptor>();
    private final List<DriverLibraryDescriptor> origFiles = new ArrayList<DriverLibraryDescriptor>();
    private final List<DBPPropertyDescriptor> connectionPropertyDescriptors = new ArrayList<DBPPropertyDescriptor>();
    private final List<OSDescriptor> supportedSystems = new ArrayList<OSDescriptor>();

    private final List<ReplaceInfo> driverReplacements = new ArrayList<ReplaceInfo>();
    private DriverDescriptor replacedBy;

    private final Map<Object, Object> defaultParameters = new HashMap<Object, Object>();
    private final Map<Object, Object> customParameters = new HashMap<Object, Object>();

    private final Map<Object, Object> defaultConnectionProperties = new HashMap<Object, Object>();
    private final Map<Object, Object> customConnectionProperties = new HashMap<Object, Object>();

    private Class driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    private final List<DataSourceDescriptor> usedBy = new ArrayList<DataSourceDescriptor>();

    private transient boolean isFailed = false;

    static {
        File driversHome = DriverDescriptor.getCustomDriversHome();
        System.setProperty(PROP_DRIVERS_LOCATION, driversHome.getAbsolutePath());
    }

    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id)
    {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = id;
        this.custom = true;
        this.iconPlain = providerDescriptor.getIcon();
        makeIconExtensions();
    }

    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, IConfigurationElement config)
    {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_ID));
        this.category = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_CATEGORY));
        this.origName = this.name = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_LABEL));
        this.origDescription = this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.note = config.getAttribute(RegistryConstants.ATTR_NOTE);
        this.origClassName = this.driverClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        if (!CommonUtils.isEmpty(config.getAttribute(RegistryConstants.ATTR_DEFAULT_PORT))) {
            try {
                this.origDefaultPort = this.driverDefaultPort = config.getAttribute(RegistryConstants.ATTR_DEFAULT_PORT);
            }
            catch (NumberFormatException ex) {
                log.warn("Bad default port for driver '" + name + "' specified: " + ex.getMessage());
            }
        }
        this.origSampleURL = this.sampleURL = config.getAttribute(RegistryConstants.ATTR_SAMPLE_URL);
        this.webURL = config.getAttribute(RegistryConstants.ATTR_WEB_URL);
        this.clientRequired = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CLIENT_REQUIRED), false);
        this.customDriverLoader = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false);
        this.supportsDriverProperties = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SUPPORTS_DRIVER_PROPERTIES), true);
        this.embedded = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_EMBEDDED));
        this.anonymousAccess = this.embedded || CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_ANONYMOUS));
        this.custom = false;
        this.isLoaded = false;

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE)) {
            this.libraries.add(new DriverLibraryDescriptor(this, lib));
        }
        this.origFiles.addAll(this.libraries);

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE_SOURCE)) {
            this.fileSources.add(new DriverFileSource(lib));
        }

        this.iconPlain = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        if (this.iconPlain == null) {
            this.iconPlain = providerDescriptor.getIcon();
        }
        makeIconExtensions();

        {
            // OSes
            IConfigurationElement[] osElements = config.getChildren(RegistryConstants.TAG_OS);
            for (IConfigurationElement os : osElements) {
                supportedSystems.add(new OSDescriptor(
                    os.getAttribute(RegistryConstants.ATTR_NAME),
                    os.getAttribute(RegistryConstants.ATTR_ARCH)
                ));
            }
        }

        {
            // Connection property groups
            IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP);
            for (IConfigurationElement prop : propElements) {
                connectionPropertyDescriptors.addAll(PropertyDescriptor.extractProperties(prop));
            }
        }

        // Connection default properties
        //connectionProperties

        {
            // Driver parameters
            IConfigurationElement[] paramElements = config.getChildren(RegistryConstants.TAG_PARAMETER);
            for (IConfigurationElement param : paramElements) {
                String paramName = param.getAttribute(RegistryConstants.ATTR_NAME);
                String paramValue = param.getAttribute(RegistryConstants.ATTR_VALUE);
                if (CommonUtils.isEmpty(paramValue)) {
                    paramValue = param.getValue();
                }
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    setDriverParameter(paramName, paramValue, true);
                }
            }
        }

        {
            // Connection properties
            IConfigurationElement[] propElements = config.getChildren(RegistryConstants.TAG_PROPERTY);
            for (IConfigurationElement param : propElements) {
                String paramName = param.getAttribute(RegistryConstants.ATTR_NAME);
                String paramValue = param.getAttribute(RegistryConstants.ATTR_VALUE);
                if (CommonUtils.isEmpty(paramValue)) {
                    paramValue = param.getValue();
                }
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    defaultConnectionProperties.put(paramName, paramValue);
                    customConnectionProperties.put(paramName, paramValue);
                }
            }
        }

        {
            // Driver replacements
            IConfigurationElement[] replaceElements = config.getChildren(RegistryConstants.TAG_REPLACE);
            for (IConfigurationElement replace : replaceElements) {
                String providerId = replace.getAttribute(RegistryConstants.ATTR_PROVIDER);
                String driverId = replace.getAttribute(RegistryConstants.ATTR_DRIVER);
                if (!CommonUtils.isEmpty(providerId) && !CommonUtils.isEmpty(driverId)) {
                    driverReplacements.add(new ReplaceInfo(providerId, driverId));
                }
            }
        }
        // Create class loader
        this.classLoader = new DriverClassLoader(
            this,
            new URL[0],
            //getClass().getClassLoader());
            getClass().getClassLoader());
    }

    public DriverDescriptor getReplacedBy()
    {
        return replacedBy;
    }

    public void setReplacedBy(DriverDescriptor replaceBy)
    {
        this.replacedBy = replaceBy;
    }

    public boolean replaces(DriverDescriptor driver)
    {
        for (ReplaceInfo replaceInfo : driverReplacements) {
            if (driver.getProviderDescriptor().getId().equals(replaceInfo.providerId) &&
                driver.getId().equals(replaceInfo.driverId))
            {
                return true;
            }
        }
        return false;
    }

    private void makeIconExtensions()
    {
        if (isCustom()) {
            this.iconNormal = new DBIconComposite(this.iconPlain, false, null, null, DBIcon.OVER_LAMP, null);
        } else {
            this.iconNormal = this.iconPlain;
        }
        this.iconError = new DBIconComposite(this.iconPlain, false, null, null, isCustom() ? DBIcon.OVER_LAMP : null, DBIcon.OVER_ERROR);
    }

    public void dispose()
    {
        synchronized (usedBy) {
            if (!usedBy.isEmpty()) {
                log.error("Driver '" + getName() + "' still used by " + usedBy.size() + " data sources");
            }
        }
    }

    void addUser(DataSourceDescriptor dataSourceDescriptor)
    {
        synchronized (usedBy) {
            usedBy.add(dataSourceDescriptor);
        }
    }

    void removeUser(DataSourceDescriptor dataSourceDescriptor)
    {
        synchronized (usedBy) {
            usedBy.remove(dataSourceDescriptor);
        }
    }

    @Override
    public DriverClassLoader getClassLoader()
    {
        return classLoader;
    }

    public List<DataSourceDescriptor> getUsedBy()
    {
        return usedBy;
    }

    public DataSourceProviderDescriptor getProviderDescriptor()
    {
        return providerDescriptor;
    }

    @Override
    public DBPDataSourceProvider getDataSourceProvider()
        throws DBException
    {
        return providerDescriptor.getInstance(this);
    }

    @Override
    public DBPClientManager getClientManager()
    {
        try {
            DBPDataSourceProvider provider = getDataSourceProvider();
            if (provider instanceof DBPClientManager) {
                return (DBPClientManager) provider;
            } else {
                return null;
            }
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Property(viewable = true, order = 2)
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
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
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getNote()
    {
        return note;
    }

    public String getFullName()
    {
        if (CommonUtils.isEmpty(category)) {
            return name;
        } else {
            return category + " / " + name;
        }
    }

    /**
     * Plain icon (without any overlays).
     * @return plain icon
     */
    public DBPImage getPlainIcon()
    {
        return iconPlain;
    }

    /**
     * Driver icon, includes overlays for driver conditions (custom, invalid, etc)..
     * @return icon
     */
    @Override
    public DBPImage getIcon()
    {
        if (!isLoaded && (isFailed /*|| (isManagable() && !isInternalDriver() && !hasValidLibraries())*/)) {
            return iconError;
        } else {
            return iconNormal;
        }
    }

    private boolean hasValidLibraries()
    {
        for (DriverLibraryDescriptor lib : libraries) {
            File file = lib.getLocalFile();
            if (file != null && file.exists()) {
                return true;
            }
        }
        return false;
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

    @Override
    @Property(viewable = true, order = 2)
    public String getDriverClassName()
    {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName)
    {
        if (this.driverClassName == null || !this.driverClassName.equals(driverClassName)) {
            this.driverClassName = driverClassName;
            resetDriverInstance();
        }
    }

    @Override
    public Object getDriverInstance(DBRRunnableContext runnableContext)
        throws DBException
    {
        if (driverInstance == null) {
            loadDriver(runnableContext);
        }
        return driverInstance;
    }

    private void resetDriverInstance() {
        this.driverInstance = null;
        this.driverClass = null;
        this.isLoaded = false;
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

    @Override
    public String getDefaultPort()
    {
        return driverDefaultPort;
    }

    public void setDriverDefaultPort(String driverDefaultPort)
    {
        this.driverDefaultPort = driverDefaultPort;
    }

    @Override
    @Property(viewable = true, order = 3)
    public String getSampleURL()
    {
        return sampleURL;
    }

    public void setSampleURL(String sampleURL)
    {
        this.sampleURL = sampleURL;
    }

    @Override
    public String getWebURL()
    {
        return webURL;
    }

    @Override
    public boolean isClientRequired()
    {
        return clientRequired;
    }

    @Override
    public boolean supportsDriverProperties()
    {
        return this.supportsDriverProperties;
    }

    @Override
    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    @Override
    public boolean isAnonymousAccess()
    {
        return anonymousAccess;
    }

    @Override
    public boolean isCustomDriverLoader()
    {
        return customDriverLoader;
    }

    @Override
    public DBXTreeNode getNavigatorRoot() {
        return providerDescriptor.getTreeDescriptor();
    }

    public void setCustomDriverLoader(boolean customDriverLoader)
    {
        this.customDriverLoader = customDriverLoader;
    }

    public boolean isManagable()
    {
        return getProviderDescriptor().isDriversManagable();
    }

    public boolean isInternalDriver()
    {
        return
            driverClassName != null &&
            driverClassName.contains("sun.jdbc"); //$NON-NLS-1$
    }

/*
    public boolean isLoaded()
    {
        return isLoaded;
    }

    public Class getDriverClass()
    {
        return driverClass;
    }

    public DriverClassLoader getClassLoader()
    {
        return classLoader;
    }
*/

    @Override
    public Collection<String> getClientHomeIds()
    {
        return clientHomeIds;
    }

    public void setClientHomeIds(Collection<String> homeIds)
    {
        clientHomeIds.clear();
        clientHomeIds.addAll(homeIds);
    }

    public void addClientHomeId(String homeId)
    {
        clientHomeIds.add(homeId);
    }

    @NotNull
    @Override
    public Collection<DriverLibraryDescriptor> getDriverLibraries()
    {
        return libraries;
    }

    public DriverLibraryDescriptor getDriverLibrary(String path)
    {
        for (DriverLibraryDescriptor lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
    }

    public DriverLibraryDescriptor addDriverLibrary(String path, DBPDriverLibrary.FileType fileType)
    {
        for (DriverLibraryDescriptor lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        DriverLibraryDescriptor lib = new DriverLibraryDescriptor(this, fileType, path);
        addDriverLibrary(lib);
        return lib;
    }

    public boolean addDriverLibrary(DriverLibraryDescriptor descriptor)
    {
        resetDriverInstance();
        if (!libraries.contains(descriptor)) {
            this.libraries.add(descriptor);
            return true;
        }
        return false;
    }

    public boolean removeDriverLibrary(DriverLibraryDescriptor lib)
    {
        resetDriverInstance();
        if (!lib.isCustom()) {
            lib.setDisabled(true);
            return true;
        } else {
            return this.libraries.remove(lib);
        }
    }

    @NotNull
    public List<DriverFileSource> getDriverFileSources() {
        return fileSources;
    }

    @Override
    public List<DBPPropertyDescriptor> getConnectionPropertyDescriptors()
    {
        return connectionPropertyDescriptors;
    }

    @Override
    public Map<Object, Object> getDefaultConnectionProperties()
    {
        return defaultConnectionProperties;
    }

    @Override
    public Map<Object, Object> getConnectionProperties()
    {
        return customConnectionProperties;
    }

    public void setConnectionProperty(String name, String value)
    {
        customConnectionProperties.put(name, value);
    }

    public void setConnectionProperties(Map<Object, Object> parameters)
    {
        customConnectionProperties.clear();
        customConnectionProperties.putAll(parameters);
    }

    public Map<Object, Object> getDefaultDriverParameters()
    {
        return defaultParameters;
    }

    @Override
    public Map<Object, Object> getDriverParameters()
    {
        return customParameters;
    }

    @Override
    public Object getDriverParameter(String name)
    {
        return customParameters.get(name);
    }

    public void setDriverParameter(String name, String value, boolean setDefault)
    {
        DBPPropertyDescriptor prop = getProviderDescriptor().getDriverProperty(name);
        Object valueObject = prop == null ? value : GeneralUtils.convertString(value, prop.getDataType());
        customParameters.put(name, valueObject);
        if (setDefault) {
            defaultParameters.put(name, valueObject);
        }
    }

    public void setDriverParameters(Map<Object, Object> parameters)
    {
        customParameters.clear();
        customParameters.putAll(parameters);
    }

//    public List<OSDescriptor> getSupportedSystems()
//    {
//        return supportedSystems;
//    }

    @Override
    public boolean isSupportedByLocalSystem()
    {
        if (supportedSystems.isEmpty()) {
            // Multi-platform
            return true;
        }
        OSDescriptor localSystem = DBeaverCore.getInstance().getLocalSystem();
        for (OSDescriptor system : supportedSystems) {
            if (system.matches(localSystem)) {
                return true;
            }
        }
        return false;
    }

    public String getLicense()
    {
        for (DriverLibraryDescriptor file : libraries) {
            if (file.getType() == DBPDriverLibrary.FileType.license) {
                final File licenseFile = file.getLocalFile();
                if (licenseFile != null && licenseFile.exists()) {
                    try {
                        return ContentUtils.readFileToString(licenseFile);
                    } catch (IOException e) {
                        log.warn(e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void loadDriver(DBRRunnableContext runnableContext)
        throws DBException
    {
        this.loadDriver(runnableContext, false, false);
    }

    public void loadDriver(DBRRunnableContext runnableContext, boolean forceReload, boolean omitDownload)
        throws DBException
    {
        if (isLoaded && !forceReload) {
            return;
        }
        isLoaded = false;

        loadLibraries(runnableContext, omitDownload);

        if (!acceptDriverLicenses(runnableContext)) {
            throw new DBException("You have to accept driver '" + getFullName() + "' license to be able to connect");
        }

        try {
            if (!isCustomDriverLoader()) {
                try {
                    if (this.isInternalDriver()) {
                        // Use system class loader
                        driverClass = Class.forName(driverClassName);
                    } else {
                        // Load driver classes into core module using plugin class loader
                        //driverClass = classLoader.loadClass(driverClassName);
                        driverClass = Class.forName(driverClassName, true, classLoader);
                    }
                }
                catch (Throwable ex) {
                    throw new DBException("Can't load driver class '" + driverClassName + "'", ex);
                }
    
                // Create driver instance
                if (!this.isInternalDriver()) {
                    driverInstance = createDriverInstance();
                }
    
                isLoaded = true;
                isFailed = false;
            }
        } catch (DBException e) {
            isFailed = true;
            throw e;
        }
    }

    private void loadLibraries(DBRRunnableContext runnableContext, boolean omitDownload)
        throws DBException
    {
        this.classLoader = null;

        if (!omitDownload) {
            validateFilesPresence(runnableContext);
        }

        List<URL> libraryURLs = new ArrayList<URL>();
        // Load libraries
        for (DriverLibraryDescriptor file : libraries) {
            if (file.isDisabled() || file.getType() != DBPDriverLibrary.FileType.jar) {
                continue;
            }
            File localFile = file.getLocalFile();
            if (localFile == null) {
                continue;
            }
            URL url;
            try {
                url = localFile.toURI().toURL();
            } catch (MalformedURLException e) {
                log.error(e);
                continue;
            }
            libraryURLs.add(url);
        }
        // Make class loader
        this.classLoader = new DriverClassLoader(
            this,
            libraryURLs.toArray(new URL[libraryURLs.size()]),
            getDataSourceProvider().getClass().getClassLoader());
    }

    @Override
    public void validateFilesPresence(final DBRRunnableContext runnableContext)
    {
        for (DriverLibraryDescriptor file : libraries) {
            if (file.isCustom()) {
                File localFile = file.getLocalFile();
                if (localFile != null && localFile.exists()) {
                    // there are custom files - not need to
                    return;
                }
            }
        }

        final List<DBPDriverLibrary> downloadCandidates = new ArrayList<DBPDriverLibrary>();
        for (DBPDriverLibrary file : libraries) {
            if (file.isDisabled() || !file.isDownloadable()) {
                // Nothing we can do about it
                continue;
            }
            if (!file.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            final File libraryFile = file.getLocalFile();
            if (libraryFile == null || !libraryFile.exists()) {
                downloadCandidates.add(file);
            }
        }

        if (!downloadCandidates.isEmpty() || !fileSources.isEmpty()) {
            UIUtils.runInUI(null, new Runnable() {
                @Override
                public void run() {
                    DriverDownloadDialog.downloadDriverFiles(null, DriverDescriptor.this, downloadCandidates);
                }
            });
        }
    }

    public boolean acceptDriverLicenses(DBRRunnableContext runnableContext)
    {
        // User must accept all licenses before actual drivers download
        for (final DriverLibraryDescriptor file : libraries) {
            if (file.getType() == DBPDriverLibrary.FileType.license) {
                final File libraryFile = file.getLocalFile();
                if (libraryFile == null || !libraryFile.exists()) {
                    try {
                        runnableContext.run(true, true, new DBRRunnableWithProgress() {
                            @Override
                            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                            {
                                try {
                                    DriverFileManager.downloadLibraryFile(monitor, file, false);
                                } catch (final Exception e) {
                                    log.warn("Can't obtain driver license", e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.warn(e);
                    }
                }
            }
        }
        String licenseText = getLicense();
        if (!CommonUtils.isEmpty(licenseText)) {
            return acceptLicense(licenseText);
        }
        // No license
        return true;
    }

    private boolean acceptLicense(String licenseText) {
        // Check registry
        DBPPreferenceStore prefs = DBeaverCore.getGlobalPreferenceStore();
        String acceptedStr = prefs.getString(LICENSE_ACCEPT_KEY + getId());
        if (!CommonUtils.isEmpty(acceptedStr)) {
            return true;
        }

        LicenceAcceptor licenceAcceptor = new LicenceAcceptor(licenseText);
        UIUtils.runInUI(null, licenceAcceptor);
        if (licenceAcceptor.result) {
            // Save in registry
            prefs.setValue(LICENSE_ACCEPT_KEY + getId(), true + ":" + System.currentTimeMillis() + ":" + System.getProperty("user.name"));
            return true;
        }
        return false;
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

    public String getOrigDefaultPort()
    {
        return origDefaultPort;
    }

    public String getOrigSampleURL()
    {
        return origSampleURL;
    }

    public List<DriverLibraryDescriptor> getOrigFiles()
    {
        return origFiles;
    }

    public static File getDriversContribFolder() throws IOException
    {
        return new File(Platform.getInstallLocation().getDataArea(DRIVERS_FOLDER).toExternalForm());
    }

    public void serialize(XMLBuilder xml, boolean export)
        throws IOException
    {
        xml.startElement(RegistryConstants.TAG_DRIVER);
        if (export) {
            xml.addAttribute(RegistryConstants.ATTR_PROVIDER, providerDescriptor.getId());
        }
        xml.addAttribute(RegistryConstants.ATTR_ID, this.getId());
        if (this.isDisabled()) {
            xml.addAttribute(RegistryConstants.ATTR_DISABLED, true);
        }
        if (!CommonUtils.isEmpty(this.getCategory())) {
            xml.addAttribute(RegistryConstants.ATTR_CATEGORY, this.getCategory());
        }
        xml.addAttribute(RegistryConstants.ATTR_CUSTOM, this.isCustom());
        xml.addAttribute(RegistryConstants.ATTR_EMBEDDED, this.isEmbedded());
        xml.addAttribute(RegistryConstants.ATTR_NAME, this.getName());
        xml.addAttribute(RegistryConstants.ATTR_CLASS, this.getDriverClassName());
        if (!CommonUtils.isEmpty(this.getSampleURL())) {
            xml.addAttribute(RegistryConstants.ATTR_URL, this.getSampleURL());
        }
        if (this.getDefaultPort() != null) {
            xml.addAttribute(RegistryConstants.ATTR_PORT, this.getDefaultPort());
        }
        xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, CommonUtils.notEmpty(this.getDescription()));
        if (this.isCustomDriverLoader()) {
            xml.addAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER, this.isCustomDriverLoader());
        }

        // Libraries
        for (DriverLibraryDescriptor lib : libraries) {
            if ((export && !lib.isDisabled()) || lib.isCustom() || lib.isDisabled()) {
                xml.startElement(RegistryConstants.TAG_LIBRARY);
                xml.addAttribute(RegistryConstants.ATTR_TYPE, lib.getType().name());
                xml.addAttribute(RegistryConstants.ATTR_PATH, lib.getPath());
                if (lib.isDisabled()) {
                    xml.addAttribute(RegistryConstants.ATTR_DISABLED, true);
                }
                xml.endElement();
            }
        }

        // Client homes
        for (String homeId : clientHomeIds) {
            xml.startElement(RegistryConstants.TAG_CLIENT_HOME);
            xml.addAttribute(RegistryConstants.ATTR_ID, homeId);
            xml.endElement();
        }

        // Parameters
        for (Map.Entry<Object, Object> paramEntry : customParameters.entrySet()) {
            if (!CommonUtils.equalObjects(paramEntry.getValue(), defaultParameters.get(paramEntry.getKey()))) {
                xml.startElement(RegistryConstants.TAG_PARAMETER);
                xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(paramEntry.getKey()));
                xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(paramEntry.getValue()));
                xml.endElement();
            }
        }

        // Properties
        for (Map.Entry<Object, Object> propEntry : customConnectionProperties.entrySet()) {
            if (!CommonUtils.equalObjects(propEntry.getValue(), defaultConnectionProperties.get(propEntry.getKey()))) {
                xml.startElement(RegistryConstants.TAG_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(propEntry.getKey()));
                xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(propEntry.getValue()));
                xml.endElement();
            }
        }

        xml.endElement();
    }

    @Override
    public DBPClientHome getClientHome(String homeId)
    {
        DBPClientManager clientManager = getClientManager();
        if (clientManager != null) {
            return clientManager.getClientHome(homeId);
        }
        return null;
    }

    public String getDefaultClientHomeId()
    {
        DBPClientManager clientManager = getClientManager();
        if (clientManager != null) {
            return clientManager.getDefaultClientHomeId();
        }
        return null;
    }

    public static File getCustomDriversHome()
    {
        File homeFolder;
        // Try to use custom drivers path from preferences
        String driversHome = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.UI_DRIVERS_HOME);
        if (!CommonUtils.isEmpty(driversHome)) {
            homeFolder = new File(driversHome);
        } else {
            homeFolder = DBeaverActivator.getInstance().getStateLocation().toFile();
        }
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create drivers folder '" + homeFolder.getAbsolutePath() + "'");
            }
        }

        return homeFolder;
    }

    public static String[] getDriversSources()
    {
        String sourcesString = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.UI_DRIVERS_SOURCES);
        List<String> pathList = CommonUtils.splitString(sourcesString, '|');
        return pathList.toArray(new String[pathList.size()]);
    }

    public static String getDriversPrimarySource()
    {
        String sourcesString = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.UI_DRIVERS_SOURCES);
        int divPos = sourcesString.indexOf('|');
        return divPos == -1 ? sourcesString : sourcesString.substring(0, divPos);
    }

    @Override
    public String toString() {
        return name;
    }

    static class DriversParser implements SAXListener
    {
        DataSourceProviderDescriptor curProvider;
        DriverDescriptor curDriver;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(RegistryConstants.TAG_PROVIDER)) {
                curProvider = null;
                curDriver = null;
                String idAttr = atts.getValue(RegistryConstants.ATTR_ID);
                if (CommonUtils.isEmpty(idAttr)) {
                    log.warn("No id for driver provider");
                    return;
                }
                curProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(idAttr);
                if (curProvider == null) {
                    log.warn("Datasource provider '" + idAttr + "' not found. Bad provider description.");
                }
            } else if (localName.equals(RegistryConstants.TAG_DRIVER)) {
                curDriver = null;
                if (curProvider == null) {
                    String providerId = atts.getValue(RegistryConstants.ATTR_PROVIDER);
                    if (!CommonUtils.isEmpty(providerId)) {
                        curProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(providerId);
                        if (curProvider == null) {
                            log.warn("Datasource provider '" + providerId + "' not found. Bad driver description.");
                        }
                    }
                    if (curProvider == null) {
                        log.warn("Driver outside of datasource provider");
                        return;
                    }
                }
                String idAttr = atts.getValue(RegistryConstants.ATTR_ID);
                curDriver = curProvider.getDriver(idAttr);
                if (curDriver == null) {
                    curDriver = new DriverDescriptor(curProvider, idAttr);
                    curProvider.addDriver(curDriver);
                }
                if (curProvider.isDriversManagable()) {
                    curDriver.setCategory(atts.getValue(RegistryConstants.ATTR_CATEGORY));
                    curDriver.setName(atts.getValue(RegistryConstants.ATTR_NAME));
                    curDriver.setDescription(atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                    curDriver.setDriverClassName(atts.getValue(RegistryConstants.ATTR_CLASS));
                    curDriver.setSampleURL(atts.getValue(RegistryConstants.ATTR_URL));
                    curDriver.setDriverDefaultPort(atts.getValue(RegistryConstants.ATTR_PORT));
                    curDriver.setEmbedded(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_EMBEDDED), false));
                }
                curDriver.setCustomDriverLoader(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false));
                curDriver.setModified(true);
                String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                if (CommonUtils.getBoolean(disabledAttr)) {
                    curDriver.setDisabled(true);
                }
            } else if (localName.equals(RegistryConstants.TAG_FILE) || localName.equals(RegistryConstants.TAG_LIBRARY)) {
                if (curDriver == null) {
                    log.warn("File outside of driver");
                    return;
                }
                DBPDriverLibrary.FileType type;
                String typeStr = atts.getValue(RegistryConstants.ATTR_TYPE);
                if (CommonUtils.isEmpty(typeStr)) {
                    type = DBPDriverLibrary.FileType.jar;
                } else {
                    try {
                        type = DBPDriverLibrary.FileType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        log.warn(e);
                        type = DBPDriverLibrary.FileType.jar;
                    }
                }
                String path = atts.getValue(RegistryConstants.ATTR_PATH);
                DriverLibraryDescriptor lib = curDriver.getDriverLibrary(path);
                String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                if (lib != null && CommonUtils.getBoolean(disabledAttr)) {
                    lib.setDisabled(true);
                } else if (lib == null) {
                    curDriver.addDriverLibrary(path, type);
                }
            } else if (localName.equals(RegistryConstants.TAG_CLIENT_HOME)) {
                curDriver.addClientHomeId(atts.getValue(RegistryConstants.ATTR_ID));
            } else if (localName.equals(RegistryConstants.TAG_PARAMETER)) {
                if (curDriver == null) {
                    log.warn("Parameter outside of driver");
                    return;
                }
                final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    curDriver.setDriverParameter(paramName, paramValue, false);
                }
            } else if (localName.equals(RegistryConstants.TAG_PROPERTY)) {
                if (curDriver == null) {
                    log.warn("Property outside of driver");
                    return;
                }
                final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                    curDriver.setConnectionProperty(paramName, paramValue);
                }
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) {}

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {}
    }

    private static class ReplaceInfo {
        String providerId;
        String driverId;

        private ReplaceInfo(String providerId, String driverId)
        {
            this.providerId = providerId;
            this.driverId = driverId;
        }
    }

    public static class MetaURL {

        private List<String> urlComponents = new ArrayList<String>();
        private Set<String> availableProperties = new HashSet<String>();
        private Set<String> requiredProperties = new HashSet<String>();

        public List<String> getUrlComponents()
        {
            return urlComponents;
        }

        public Set<String> getAvailableProperties()
        {
            return availableProperties;
        }

        public Set<String> getRequiredProperties()
        {
            return requiredProperties;
        }
    }

    public static MetaURL parseSampleURL(String sampleURL) throws DBException
    {
        MetaURL metaURL = new MetaURL();
        int offsetPos = 0;
        for (; ;) {
            int divPos = sampleURL.indexOf(URL_GROUP_START, offsetPos);
            if (divPos == -1) {
                break;
            }
            int divPos2 = sampleURL.indexOf(URL_GROUP_END, divPos);
            if (divPos2 == -1) {
                throw new DBException("Bad sample URL: " + sampleURL);
            }
            String propName = sampleURL.substring(divPos + 1, divPos2);
            boolean isOptional = false;
            int optDiv1 = sampleURL.lastIndexOf(URL_OPTIONAL_START, divPos);
            int optDiv1c = sampleURL.lastIndexOf(URL_OPTIONAL_END, divPos);
            int optDiv2 = sampleURL.indexOf(URL_OPTIONAL_END, divPos2);
            int optDiv2c = sampleURL.indexOf(URL_OPTIONAL_START, divPos2);
            if (optDiv1 != -1 && optDiv2 != -1 && (optDiv1c == -1 || optDiv1c < optDiv1) && (optDiv2c == -1 || optDiv2c > optDiv2)) {
                divPos = optDiv1;
                divPos2 = optDiv2;
                isOptional = true;
            }
            if (divPos > offsetPos) {
                metaURL.urlComponents.add(sampleURL.substring(offsetPos, divPos));
            }
            metaURL.urlComponents.add(sampleURL.substring(divPos, divPos2 + 1));
            metaURL.availableProperties.add(propName);
            if (!isOptional) {
                metaURL.requiredProperties.add(propName);
            }
            offsetPos = divPos2 + 1;
        }
        if (offsetPos < sampleURL.length() - 1) {
            metaURL.urlComponents.add(sampleURL.substring(offsetPos));
        }
/*
        // Check for required parts
        for (String component : urlComponents) {
            boolean isRequired = !component.startsWith("[");
            int divPos = component.indexOf('{');
            if (divPos != -1) {
                int divPos2 = component.indexOf('}', divPos);
                if (divPos2 != -1) {
                    String propName = component.substring(divPos + 1, divPos2);
                    availableProperties.add(propName);
                    if (isRequired) {
                        requiredProperties.add(propName);
                    }
                }
            }
        }
*/
        return metaURL;
    }

    private class LicenceAcceptor implements Runnable {
        private boolean result;
        private String licenseText;

        private LicenceAcceptor(String licenseText)
        {
            this.licenseText = licenseText;
        }

        @Override
        public void run()
        {
            result =  AcceptLicenseDialog.acceptLicense(
                DBeaverUI.getActiveWorkbenchShell(),
                "You have to accept license of '" + getFullName() + " ' to continue",
                licenseText);
        }
    }

}

