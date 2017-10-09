/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.dialogs.AcceptLicenseDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverDownloadDialog;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
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
    private static final Log log = Log.getLog(DriverDescriptor.class);

    private static final String DRIVERS_FOLDER = "drivers"; //$NON-NLS-1$
    private static final String PROP_DRIVERS_LOCATION = "DRIVERS_LOCATION";

    private static final char URL_GROUP_START = '{'; //$NON-NLS-1$
    private static final char URL_GROUP_END = '}'; //$NON-NLS-1$
    private static final char URL_OPTIONAL_START = '['; //$NON-NLS-1$
    private static final char URL_OPTIONAL_END = ']'; //$NON-NLS-1$

    public static final String PROP_HOST = "host"; //$NON-NLS-1$
    public static final String PROP_PORT = "port"; //$NON-NLS-1$
    public static final String PROP_DATABASE = "database"; //$NON-NLS-1$
    public static final String PROP_SERVER = "server"; //$NON-NLS-1$
    public static final String PROP_FOLDER = "folder"; //$NON-NLS-1$
    public static final String PROP_FILE = "file"; //$NON-NLS-1$
    public static final String PROP_USER = "user"; //$NON-NLS-1$
    public static final String PROP_PASSWORD = "password"; //$NON-NLS-1$

    private static final String LICENSE_ACCEPT_KEY = "driver.license.accept.";

    public static class DriverFileInfo {
        private final String id;
        private final String version;
        private final File file;

        DriverFileInfo(String id, String version, File file) {
            this.id = id;
            this.version = version;
            this.file = file;
        }
        DriverFileInfo(DBPDriverLibrary library) {
            this.id = library.getId();
            this.version = library.getVersion();
            this.file = library.getLocalFile();
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    private final DataSourceProviderDescriptor providerDescriptor;
    private final String id;
    private String category;
    private final String origName;
    private final String origDescription;
    private final String origClassName;
    private final String origDefaultPort;
    private final String origSampleURL;
    private String name;
    private String description;
    private String driverClassName;
    private String driverDefaultPort;
    private String sampleURL;

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
    private final List<String> clientHomeIds = new ArrayList<>();
    private final List<DriverFileSource> fileSources = new ArrayList<>();
    private final List<DBPDriverLibrary> libraries = new ArrayList<>();
    private final List<DBPDriverLibrary> origFiles = new ArrayList<>();
    private final List<DBPPropertyDescriptor> connectionPropertyDescriptors = new ArrayList<>();
    private final List<OSDescriptor> supportedSystems = new ArrayList<>();

    private final List<ReplaceInfo> driverReplacements = new ArrayList<>();
    private DriverDescriptor replacedBy;

    private final Map<Object, Object> defaultParameters = new HashMap<>();
    private final Map<Object, Object> customParameters = new HashMap<>();

    private final Map<Object, Object> defaultConnectionProperties = new HashMap<>();
    private final Map<Object, Object> customConnectionProperties = new HashMap<>();

    private Map<DBPDriverLibrary, List<DriverFileInfo>> resolvedFiles = new HashMap<>();

    private Class driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    private transient boolean isFailed = false;

    static {
        File driversHome = DriverDescriptor.getCustomDriversHome();
        System.setProperty(PROP_DRIVERS_LOCATION, driversHome.getAbsolutePath());
    }

    // New driver constructor
    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id)
    {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = id;
        this.custom = true;
        this.iconPlain = providerDescriptor.getIcon();
        if (this.iconPlain == null) {
            this.iconPlain = DBIcon.TREE_DATABASE;
        }
        makeIconExtensions();
        this.origName = null;
        this.origDescription = null;
        this.origClassName = null;
        this.origDefaultPort = null;
        this.origSampleURL = null;
    }

    // Predefined driver constructor
    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, IConfigurationElement config)
    {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_ID));
        this.category = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_CATEGORY));
        this.origName = this.name = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_LABEL));
        this.origDescription = this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.origClassName = this.driverClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        if (!CommonUtils.isEmpty(config.getAttribute(RegistryConstants.ATTR_DEFAULT_PORT))) {
            this.origDefaultPort = this.driverDefaultPort = config.getAttribute(RegistryConstants.ATTR_DEFAULT_PORT);
        } else {
            this.origDefaultPort = this.driverDefaultPort = null;
        }
        this.origSampleURL = this.sampleURL = config.getAttribute(RegistryConstants.ATTR_SAMPLE_URL);
        this.webURL = config.getAttribute(RegistryConstants.ATTR_WEB_URL);
        this.clientRequired = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CLIENT_REQUIRED), false);
        this.customDriverLoader = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false);
        this.supportsDriverProperties = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SUPPORTS_DRIVER_PROPERTIES), true);
        this.embedded = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_EMBEDDED));
        this.anonymousAccess = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_ANONYMOUS));
        this.custom = false;
        this.isLoaded = false;

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE)) {
            DriverLibraryAbstract library = DriverLibraryAbstract.createFromConfig(this, lib);
            if (library != null) {
                this.libraries.add(library);
            }
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
                    if (!paramName.startsWith(DBConstants.INTERNAL_PROP_PREFIX)) {
                        customConnectionProperties.put(paramName, paramValue);
                    }
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

    @Nullable
    @Override
    public DriverClassLoader getClassLoader()
    {
        return classLoader;
    }

    public List<DataSourceDescriptor> getUsedBy()
    {
        List<DataSourceDescriptor> usedBy = new ArrayList<>();
        for (DataSourceDescriptor ds : DataSourceRegistry.getAllDataSources()) {
            if (ds.getDriver() == this) {
                usedBy.add(ds);
            }
        }
        return usedBy;
    }

    public DataSourceProviderDescriptor getProviderDescriptor()
    {
        return providerDescriptor;
    }

    @NotNull
    @Override
    public DBPDataSourceProvider getDataSourceProvider()
    {
        return providerDescriptor.getInstance(this);
    }

    @Nullable
    @Override
    public DBPClientManager getClientManager()
    {
        DBPDataSourceProvider provider = getDataSourceProvider();
        if (provider instanceof DBPClientManager) {
            return (DBPClientManager) provider;
        } else {
            return null;
        }
    }

    @NotNull
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

    @NotNull
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

    @NotNull
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
    @NotNull
    public DBPImage getPlainIcon()
    {
        return iconPlain;
    }

    /**
     * Driver icon, includes overlays for driver conditions (custom, invalid, etc)..
     * @return icon
     */
    @NotNull
    @Override
    public DBPImage getIcon()
    {
        if (!isLoaded && isFailed) {
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

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    @Nullable
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

    @NotNull
    @Override
    public Object getDriverInstance(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (driverInstance == null) {
            loadDriver(monitor);
        }
        if (isInternalDriver() && driverInstance == null) {
            return createDriverInstance();
        }
        return driverInstance;
    }

    private void resetDriverInstance() {
        this.driverInstance = null;
        this.driverClass = null;
        this.isLoaded = false;
        this.resolvedFiles.clear();
    }

    private Object createDriverInstance()
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

    @Nullable
    @Override
    public String getDefaultPort()
    {
        return driverDefaultPort;
    }

    public void setDriverDefaultPort(String driverDefaultPort)
    {
        this.driverDefaultPort = driverDefaultPort;
    }

    @Nullable
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

    @Nullable
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

    @Nullable
    @Override
    public DBXTreeNode getNavigatorRoot() {
        return providerDescriptor.getTreeDescriptor();
    }

    void setCustomDriverLoader(boolean customDriverLoader)
    {
        this.customDriverLoader = customDriverLoader;
    }

    public boolean isManagable()
    {
        return getProviderDescriptor().isDriversManagable();
    }

    @Override
    public boolean isInternalDriver()
    {
        return
            driverClassName != null &&
            driverClassName.contains("sun.jdbc"); //$NON-NLS-1$
    }

    @NotNull
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

    void addClientHomeId(String homeId)
    {
        clientHomeIds.add(homeId);
    }

    @NotNull
    @Override
    public Collection<? extends DBPDriverLibrary> getDriverLibraries()
    {
        return libraries;
    }

    public List<DBPDriverLibrary> getEnabledDriverLibraries() {
        List<DBPDriverLibrary> filtered = new ArrayList<>();
        for (DBPDriverLibrary lib : libraries) {
            if (!lib.isDisabled()) {
                filtered.add(lib);
            }
        }
        return filtered;
    }

    DBPDriverLibrary getDriverLibrary(String path)
    {
        for (DBPDriverLibrary lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
    }

    private void addLibraryFile(DBPDriverLibrary library, DriverFileInfo fileInfo) {
        List<DriverFileInfo> files = resolvedFiles.get(library);
        if (files == null) {
            files = new ArrayList<>();
            resolvedFiles.put(library, files);
        }
        files.add(fileInfo);
    }

    public DBPDriverLibrary addDriverLibrary(String path, DBPDriverLibrary.FileType fileType)
    {
        for (DBPDriverLibrary lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        DriverLibraryAbstract lib = DriverLibraryAbstract.createFromPath(this, fileType, path, null);
        addDriverLibrary(lib);
        return lib;
    }

    public boolean addDriverLibrary(DBPDriverLibrary descriptor)
    {
        if (!libraries.contains(descriptor)) {
            resetDriverInstance();
            this.libraries.add(descriptor);
            return true;
        }
        return false;
    }

    public boolean removeDriverLibrary(DBPDriverLibrary lib)
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

    @NotNull
    @Override
    public List<DBPPropertyDescriptor> getConnectionPropertyDescriptors()
    {
        return connectionPropertyDescriptors;
    }

    @NotNull
    @Override
    public Map<Object, Object> getDefaultConnectionProperties()
    {
        return defaultConnectionProperties;
    }

    @NotNull
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

    @NotNull
    @Override
    public Map<Object, Object> getDriverParameters()
    {
        return customParameters;
    }

    @Nullable
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
        for (DBPDriverLibrary file : libraries) {
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
    public void loadDriver(DBRProgressMonitor monitor)
        throws DBException
    {
        this.loadDriver(monitor, false);
    }

    private void loadDriver(DBRProgressMonitor monitor, boolean forceReload)
        throws DBException
    {
        if (isLoaded && !forceReload) {
            return;
        }
        isLoaded = false;

        loadLibraries();

        if (!acceptDriverLicenses()) {
            throw new DBException("You have to accept driver '" + getFullName() + "' license to be able to connect");
        }

        try {
            if (!isCustomDriverLoader()) {
                try {
                    // Load driver classes into core module using plugin class loader
                    driverClass = Class.forName(driverClassName, true, classLoader);
                }
                catch (Throwable ex) {
                    throw new DBException("Can't load driver class '" + driverClassName + "'", ex);
                }
    
                // Create driver instance
                /*if (!this.isInternalDriver())*/ {
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

    private void loadLibraries()
        throws DBException
    {
        this.classLoader = null;

        List<File> allLibraryFiles = validateFilesPresence(false);

        List<URL> libraryURLs = new ArrayList<>();
        // Load libraries
        for (File file : allLibraryFiles) {
            URL url;
            try {
                url = file.toURI().toURL();
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

    public void updateFiles()
    {
        validateFilesPresence(true);
    }

    @NotNull
    private List<File> validateFilesPresence(boolean resetVersions)
    {
        boolean localLibsExists = false;
        final List<DBPDriverLibrary> downloadCandidates = new ArrayList<>();
        for (DBPDriverLibrary library : libraries) {
            if (library.isDisabled()) {
                // Nothing we can do about it
                continue;
            }
            if (!library.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            if (library.isDownloadable()) {
                boolean allExists = true;
                if (resetVersions) {
                    allExists = false;
                } else {
                    List<DriverFileInfo> files = resolvedFiles.get(library);
                    if (files == null) {
                        allExists = false;
                    } else {
                        for (DriverFileInfo file : files) {
                            if (file.file == null || !file.file.exists()) {
                                allExists = false;
                                break;
                            }
                        }
                    }
                }
                if (!allExists) {
                    downloadCandidates.add(library);
                }
            } else {
                localLibsExists = true;
            }
        }
//        if (!CommonUtils.isEmpty(fileSources)) {
//            for (DriverFileSource source : fileSources) {
//                for (DriverFileSource.FileInfo fileInfo : source.getFiles()) {
//                    DriverLibraryLocal libraryLocal = new DriverLibraryLocal(this, DBPDriverLibrary.FileType.jar, fileInfo.getName());
//                    final File localFile = libraryLocal.getLocalFile();
//                }
//            }
//        }

        boolean downloaded = false;
        if (!downloadCandidates.isEmpty() || (!localLibsExists && !fileSources.isEmpty())) {
            final DriverDependencies dependencies = new DriverDependencies(downloadCandidates);
            boolean downloadOk = new UITask<Boolean>() {
                @Override
                protected Boolean runTask() {
                    return DriverDownloadDialog.downloadDriverFiles(null, DriverDescriptor.this, dependencies);
                }
            }.execute();
            if (!downloadOk) {
                return Collections.emptyList();
            }
            if (resetVersions) {
                resetDriverInstance();

/*
                for (DBPDriverLibrary library : libraries) {
                    if (!library.isDisabled()) {
                        library.resetVersion();
                    }
                }
*/
            }
            downloaded = true;
            for (DBPDriverDependencies.DependencyNode node : dependencies.getLibraryMap()) {
                List<DriverFileInfo> info = new ArrayList<>();
                resolvedFiles.put(node.library, info);
                collectLibraryFiles(node, info);
            }
            providerDescriptor.getRegistry().saveDrivers();
        }

        List<File> result = new ArrayList<>();

        for (DBPDriverLibrary library : libraries) {
            if (library.isDisabled() || !library.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            if (library.isDownloadable()) {
                List<DriverFileInfo> files = resolvedFiles.get(library);
                if (files != null) {
                    for (DriverFileInfo file : files) {
                        result.add(file.file);
                    }
                }
            } else {
                result.add(library.getLocalFile());
            }
        }

        // Now check driver version
        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_DRIVERS_VERSION_UPDATE) && !downloaded) {
            // TODO: implement new version check
            if (false) {
                try {
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            try {
                                checkDriverVersion(monitor);
                            } catch (IOException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        return result;
    }

    private void checkDriverVersion(DBRProgressMonitor monitor) throws IOException {
        for (DBPDriverLibrary library : libraries) {
            final Collection<String> availableVersions = library.getAvailableVersions(monitor);
            if (!CommonUtils.isEmpty(availableVersions)) {
                final String curVersion = library.getVersion();
                String latestVersion = DriverUtils.findLatestVersion(availableVersions);
                if (latestVersion != null && !latestVersion.equals(curVersion)) {
                    log.debug("Update driver " + getName() + " " + curVersion + "->" + latestVersion);
                }
            }
        }

    }

    public boolean isLibraryResolved(DBPDriverLibrary library) {
        return !library.isDownloadable() || !CommonUtils.isEmpty(resolvedFiles.get(library));
    }

    public Collection<DriverFileInfo> getLibraryFiles(DBPDriverLibrary library) {
        return resolvedFiles.get(library);
    }

    private void collectLibraryFiles(DBPDriverDependencies.DependencyNode node, List<DriverFileInfo> files) {
        if (node.duplicate) {
            return;
        }
        files.add(new DriverFileInfo(node.library));
        for (DBPDriverDependencies.DependencyNode sub : node.dependencies) {
            collectLibraryFiles(sub, files);
        }
    }

    public boolean acceptDriverLicenses()
    {
/*
        // User must accept all licenses before actual drivers download
        for (final DBPDriverLibrary file : libraries) {
            if (file.getType() == DBPDriverLibrary.FileType.license) {
                final File libraryFile = file.getLocalFile();
                if (libraryFile == null || !libraryFile.exists()) {
                    try {
                        runnableContext.run(true, true, new DBRRunnableWithProgress() {
                            @Override
                            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                            {
                                try {
                                    file.downloadLibraryFile(monitor, false);
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
*/
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
        DBeaverUI.syncExec(licenceAcceptor);
        if (licenceAcceptor.result) {
            // Save in registry
            prefs.setValue(LICENSE_ACCEPT_KEY + getId(), true + ":" + System.currentTimeMillis() + ":" + System.getProperty(StandardConstants.ENV_USER_NAME));
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

    public List<DBPDriverLibrary> getOrigFiles()
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
        Map<String, String> pathSubstitutions = new HashMap<>();
        {
            DriverVariablesResolver varResolver = new DriverVariablesResolver();
            String[] variables = new String[]{
                    DriverVariablesResolver.VAR_DRIVERS_HOME,
                    SystemVariablesResolver.VAR_WORKSPACE,
                    SystemVariablesResolver.VAR_HOME,
                    SystemVariablesResolver.VAR_DBEAVER_HOME};
            for (String varName : variables) {
                String varValue = varResolver.get(varName);
                if (!CommonUtils.isEmpty(varValue)) {
                    pathSubstitutions.put(varValue, varName);
                }
            }
        }

        try (XMLBuilder.Element e0 = xml.startElement(RegistryConstants.TAG_DRIVER)) {
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
            for (DBPDriverLibrary lib : libraries) {
                if (export && !lib.isDisabled()) {
                    continue;
                }
                try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_LIBRARY)) {
                    xml.addAttribute(RegistryConstants.ATTR_TYPE, lib.getType().name());
                    xml.addAttribute(RegistryConstants.ATTR_PATH, substitutePathVariables(pathSubstitutions, lib.getPath()));
                    xml.addAttribute(RegistryConstants.ATTR_CUSTOM, lib.isCustom());
                    if (lib.isDisabled()) {
                        xml.addAttribute(RegistryConstants.ATTR_DISABLED, true);
                    }
                    if (!CommonUtils.isEmpty(lib.getPreferredVersion())) {
                        xml.addAttribute(RegistryConstants.ATTR_VERSION, lib.getPreferredVersion());
                    }
                    //xml.addAttribute(RegistryConstants.ATTR_CUSTOM, lib.isCustom());
                    List<DriverFileInfo> files = resolvedFiles.get(lib);
                    if (files != null) {
                        for (DriverFileInfo file : files) {
                            try (XMLBuilder.Element e2 = xml.startElement(RegistryConstants.TAG_FILE)) {
                                if (file.file == null) {
                                    log.warn("File missing in " + file.id);
                                    continue;
                                }
                                xml.addAttribute(RegistryConstants.ATTR_ID, file.id);
                                if (!CommonUtils.isEmpty(file.version)) {
                                    xml.addAttribute(RegistryConstants.ATTR_VERSION, file.version);
                                }
                                xml.addAttribute(RegistryConstants.ATTR_PATH, substitutePathVariables(pathSubstitutions, file.file.getAbsolutePath()));
                            }
                        }
                    }
                }
            }

            // Client homes
            for (String homeId : clientHomeIds) {
                try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_CLIENT_HOME)) {
                    xml.addAttribute(RegistryConstants.ATTR_ID, homeId);
                }
            }

            // Parameters
            for (Map.Entry<Object, Object> paramEntry : customParameters.entrySet()) {
                if (!CommonUtils.equalObjects(paramEntry.getValue(), defaultParameters.get(paramEntry.getKey()))) {
                    try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PARAMETER)) {
                        xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(paramEntry.getKey()));
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(paramEntry.getValue()));
                    }
                }
            }

            // Properties
            for (Map.Entry<Object, Object> propEntry : customConnectionProperties.entrySet()) {
                if (!CommonUtils.equalObjects(propEntry.getValue(), defaultConnectionProperties.get(propEntry.getKey()))) {
                    try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PROPERTY)) {
                        xml.addAttribute(RegistryConstants.ATTR_NAME, CommonUtils.toString(propEntry.getKey()));
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(propEntry.getValue()));
                    }
                }
            }
        }
    }

    @Nullable
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
            homeFolder = new File(
                System.getProperty(StandardConstants.ENV_USER_HOME),
                DBConstants.DEFAULT_DRIVERS_FOLDER);
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

    static String getDriversPrimarySource()
    {
        String sourcesString = DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.UI_DRIVERS_SOURCES);
        int divPos = sourcesString.indexOf('|');
        return divPos == -1 ? sourcesString : sourcesString.substring(0, divPos);
    }

    @Override
    public String toString() {
        return name;
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

        private List<String> urlComponents = new ArrayList<>();
        private Set<String> availableProperties = new HashSet<>();
        private Set<String> requiredProperties = new HashSet<>();

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

    public static class DriversParser implements SAXListener {
        DataSourceProviderDescriptor curProvider;
        DriverDescriptor curDriver;
        DBPDriverLibrary curLibrary;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            switch (localName) {
                case RegistryConstants.TAG_PROVIDER: {
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
                    break;
                }
                case RegistryConstants.TAG_DRIVER: {
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
                    break;
                }
                case RegistryConstants.TAG_LIBRARY: {
                    if (curDriver == null) {
                        log.warn("Library outside of driver");
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
                    String path = normalizeLibraryPath(atts.getValue(RegistryConstants.ATTR_PATH));
                    if (!CommonUtils.isEmpty(path)) {
                        path = replacePathVariables(path);
                    }
                    boolean custom = CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_CUSTOM), true);
                    String version = atts.getValue(RegistryConstants.ATTR_VERSION);
                    DBPDriverLibrary lib = curDriver.getDriverLibrary(path);
                    if (!custom && lib == null) {
                        // Perhaps this library isn't included in driver bundle
                        // Or this is predefined library from some previous version - as it wasn't defined in plugin.xml
                        // so let's just skip it
                        //log.debug("Skip obsolete custom library '" + path + "'");
                        return;
                    }
                    String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                    if (lib != null && CommonUtils.getBoolean(disabledAttr)) {
                        lib.setDisabled(true);
                    } else if (lib == null) {
                        lib = DriverLibraryAbstract.createFromPath(curDriver, type, path, version);
                        curDriver.libraries.add(lib);
                    } else if (!CommonUtils.isEmpty(version)) {
                        lib.setPreferredVersion(version);
                    }
                    curLibrary = lib;
                    break;
                }
                case RegistryConstants.TAG_FILE: {
                    if (curDriver != null && curLibrary != null) {
                        String path = atts.getValue(RegistryConstants.ATTR_PATH);
                        if (path != null) {
                            path = replacePathVariables(path);
                            if (CommonUtils.isEmpty(path)) {
                                log.warn("Empty path for library file");
                            } else {
                                DriverFileInfo info = new DriverFileInfo(
                                        atts.getValue(CommonUtils.notEmpty(RegistryConstants.ATTR_ID)),
                                        atts.getValue(CommonUtils.notEmpty(RegistryConstants.ATTR_VERSION)),
                                        new File(path));
                                curDriver.addLibraryFile(curLibrary, info);
                            }
                        }
                    }
                    break;
                }
                case RegistryConstants.TAG_CLIENT_HOME:
                    if (curDriver != null) {
                        curDriver.addClientHomeId(atts.getValue(RegistryConstants.ATTR_ID));
                    }
                    break;
                case RegistryConstants.TAG_PARAMETER: {
                    if (curDriver != null) {
                        final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                        final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                            curDriver.setDriverParameter(paramName, paramValue, false);
                        }
                    }
                    break;
                }
                case RegistryConstants.TAG_PROPERTY: {
                    if (curDriver != null) {
                        final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                        final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                            curDriver.setConnectionProperty(paramName, paramValue);
                        }
                    }
                    break;
                }
            }
        }

        // TODO: support of 3.5.1 -> 3.5.2 maven dependencies migration
        private static final String PATH_VERSION_OBSOLETE_RELEASE = ":release";

        private static String normalizeLibraryPath(String value) {
            if (value.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
                if (value.endsWith(PATH_VERSION_OBSOLETE_RELEASE)) {
                    value = value.substring(0, value.length() - PATH_VERSION_OBSOLETE_RELEASE.length()) + ":" + MavenArtifactReference.VERSION_PATTERN_RELEASE;
                }
            }
            return value;
        }

        @Override
        public void saxText(SAXReader reader, String data) {}

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
            switch (localName) {
                case RegistryConstants.TAG_LIBRARY:
                    curLibrary = null;
                    break;
            }

        }
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

    private static String replacePathVariables(String path) {
        return GeneralUtils.replaceVariables(path, new DriverVariablesResolver());
    }

    private static String substitutePathVariables(Map<String, String> pathSubstitutions, String path) {
        for (Map.Entry<String, String> ps : pathSubstitutions.entrySet()) {
            if (path.startsWith(ps.getKey())) {
                path = GeneralUtils.variablePattern(ps.getValue()) + path.substring(ps.getKey().length());
                break;
            }
        }
        return path;
    }

    private static class DriverVariablesResolver extends SystemVariablesResolver {
        private static final String VAR_DRIVERS_HOME = "drivers_home";

        @Override
        public String get(String name) {
            if (name.equalsIgnoreCase(VAR_DRIVERS_HOME)) {
                return getCustomDriversHome().getAbsolutePath();
            } else {
                return super.get(name);
            }
        }
    }
}
