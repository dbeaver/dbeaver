/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.NativeClientDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.VersionUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver {
    private static final Log log = Log.getLog(DriverDescriptor.class);

    private static final String PROP_DRIVERS_LOCATION = "DRIVERS_LOCATION";

    private static final String LICENSE_ACCEPT_KEY = "driver.license.accept.";

    public static final DriverDescriptor NULL_DRIVER = new DriverDescriptor("NULL");

    /**
     * Parent classloader of every driver classloader that loads global libraries.
     * <p>
     * Initializes upon the initialization of the very first driver.
     */
    private static ClassLoader rootClassLoader;

    public static class DriverFileInfo {
        private final String id;
        private final String version;
        private final DBPDriverLibrary.FileType type;
        private final Path file;
        private long fileCRC;

        public DriverFileInfo(String id, String version, DBPDriverLibrary.FileType type, Path file) {
            this.id = id;
            this.version = version;
            this.file = file;
            this.type = type;
        }

        DriverFileInfo(DBPDriverLibrary library) {
            this.id = library.getId();
            this.version = library.getVersion();
            this.file = library.getLocalFile();
            this.type = library.getType();
        }

        public Path getFile() {
            return file;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public DBPDriverLibrary.FileType getType() {
            return type;
        }

        public long getFileCRC() {
            return fileCRC;
        }

        public void setFileCRC(long fileCRC) {
            this.fileCRC = fileCRC;
        }

        @Override
        public String toString() {
            return file != null ? file.getFileName().toString() : this.id;
        }
    }

    private static class ReplaceInfo {
        String providerId;
        String driverId;

        private ReplaceInfo(String providerId, String driverId) {
            this.providerId = providerId;
            this.driverId = driverId;
        }
    }

    private final DataSourceProviderDescriptor providerDescriptor;
    private final String id;
    private String category;
    private final List<String> categories;
    private String name;
    private String description;
    private String driverClassName;
    private String driverDefaultHost;
    private String driverDefaultPort;
    private String driverDefaultDatabase;
    private String driverDefaultServer;
    private String driverDefaultUser;
    private String sampleURL;
    private String dialectId;

    private final String origName;
    private final String origDescription;
    private final String origClassName;
    private final String origDefaultHost, origDefaultPort, origDefaultDatabase, origDefaultServer, origDefaultUser;
    private final String origSampleURL;
    private String origDialectId;

    private String webURL;
    private String propertiesWebURL;
    private DBPImage iconPlain;
    private DBPImage iconNormal;
    private DBPImage iconError;
    private DBPImage iconBig;
    private boolean embedded, origEmbedded;
    private boolean supportsDistributedMode;
    private boolean singleConnection;
    private boolean clientRequired;
    private boolean supportsDriverProperties;
    private boolean anonymousAccess, origAnonymousAccess;
    private boolean allowsEmptyPassword, origAllowsEmptyPassword;
    private boolean licenseRequired;
    private boolean customDriverLoader;
    private boolean useURLTemplate;
    private boolean customEndpointInformation;
    private boolean instantiable, origInstantiable;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private boolean temporary;
    private int promoted;

    private Set<DBPDriverConfigurationType> configurationTypes = new HashSet<>(Collections.singleton(DBPDriverConfigurationType.MANUAL));
    private final List<DBPNativeClientLocation> nativeClientHomes = new ArrayList<>();
    private final List<DriverFileSource> fileSources = new ArrayList<>();
    private final List<DBPDriverLibrary> libraries = new ArrayList<>();
    private final List<DBPDriverLibrary> origFiles = new ArrayList<>();
    private final List<DBPPropertyDescriptor> providerPropertyDescriptors = new ArrayList<>();
    private final List<OSDescriptor> supportedSystems = new ArrayList<>();

    private final List<ReplaceInfo> driverReplacements = new ArrayList<>();
    private DriverDescriptor replacedBy;

    private final Map<String, Object> defaultParameters = new HashMap<>();
    private final Map<String, Object> customParameters = new HashMap<>();

    private final Map<String, Object> defaultConnectionProperties = new HashMap<>();
    private final Map<String, Object> customConnectionProperties = new HashMap<>();

    private final Map<DBPDriverLibrary, List<DriverFileInfo>> resolvedFiles = new HashMap<>();

    private Class<?> driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    private transient boolean isFailed = false;

    static {
        Path driversHome = DriverDescriptor.getCustomDriversHome();
        if (driversHome != null) {
            System.setProperty(PROP_DRIVERS_LOCATION, driversHome.toAbsolutePath().toString());
        }
    }

    private DriverDescriptor(String id) {
        this(DataSourceProviderDescriptor.NULL_PROVIDER, id);
    }

    // New driver constructor
    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id) {
        this(providerDescriptor, id, null);
    }

    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, String id, DriverDescriptor copyFrom) {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = id;
        this.custom = true;
        this.useURLTemplate = true;
        this.customEndpointInformation = false;
        this.instantiable = true;
        this.promoted = 0;

        this.origName = null;
        this.origDescription = null;
        this.origClassName = null;
        this.origDefaultHost = null;
        this.origDefaultPort = null;
        this.origDefaultDatabase = null;
        this.origDefaultServer = null;
        this.origDefaultUser = null;

        this.origSampleURL = null;
        this.origDialectId = null;

        if (copyFrom != null) {
            this.iconPlain = copyFrom.iconPlain;
            this.iconBig = copyFrom.iconBig;
        } else {
            this.iconPlain = providerDescriptor.getIcon();
            this.iconBig = DBIcon.DATABASE_BIG_DEFAULT;
        }
        if (this.iconPlain == null) {
            this.iconPlain = DBIcon.DATABASE_DEFAULT;
        }

        makeIconExtensions();
        if (copyFrom != null) {
            // Copy props from source
            this.category = copyFrom.category;
            this.categories = new ArrayList<>(copyFrom.categories);
            this.name = copyFrom.name;
            this.description = copyFrom.description;
            this.driverClassName = copyFrom.driverClassName;
            this.driverDefaultHost = copyFrom.driverDefaultHost;
            this.driverDefaultPort = copyFrom.driverDefaultPort;
            this.driverDefaultDatabase = copyFrom.driverDefaultDatabase;
            this.driverDefaultServer = copyFrom.driverDefaultServer;
            this.driverDefaultUser = copyFrom.driverDefaultUser;
            this.sampleURL = copyFrom.sampleURL;
            this.dialectId = copyFrom.dialectId;

            this.webURL = copyFrom.webURL;
            this.propertiesWebURL = copyFrom.webURL;
            this.embedded = copyFrom.embedded;
            this.singleConnection = copyFrom.singleConnection;
            this.clientRequired = copyFrom.clientRequired;
            this.supportsDriverProperties = copyFrom.supportsDriverProperties;
            this.anonymousAccess = copyFrom.anonymousAccess;
            this.allowsEmptyPassword = copyFrom.allowsEmptyPassword;
            this.licenseRequired = copyFrom.licenseRequired;
            this.customDriverLoader = copyFrom.customDriverLoader;
            this.useURLTemplate = copyFrom.useURLTemplate;
            this.customEndpointInformation = copyFrom.customEndpointInformation;
            this.instantiable = copyFrom.instantiable;
            this.promoted = copyFrom.promoted;
            this.nativeClientHomes.addAll(copyFrom.nativeClientHomes);
            for (DriverFileSource fs : copyFrom.fileSources) {
                this.fileSources.add(new DriverFileSource(fs));
            }
            for (DBPDriverLibrary library : copyFrom.libraries) {
                if (library instanceof DriverLibraryAbstract) {
                    this.libraries.add(((DriverLibraryAbstract) library).copyLibrary(this));
                } else {
                    this.libraries.add(library);
                }
            }
            this.providerPropertyDescriptors.addAll(copyFrom.providerPropertyDescriptors);

            this.defaultParameters.putAll(copyFrom.defaultParameters);
            this.customParameters.putAll(copyFrom.customParameters);

            this.defaultConnectionProperties.putAll(copyFrom.defaultConnectionProperties);
            this.customConnectionProperties.putAll(copyFrom.customConnectionProperties);
            this.configurationTypes.addAll(copyFrom.configurationTypes);
            this.supportsDistributedMode = copyFrom.supportsDistributedMode;
        } else {
            this.categories = new ArrayList<>();
            this.name = "";
        }
    }

    // Predefined driver constructor
    public DriverDescriptor(DataSourceProviderDescriptor providerDescriptor, IConfigurationElement config) {
        super(providerDescriptor.getPluginId());
        this.providerDescriptor = providerDescriptor;
        this.id = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_ID));
        this.category = config.getAttribute(RegistryConstants.ATTR_CATEGORY);
        this.categories = Arrays.asList(CommonUtils.split(config.getAttribute(RegistryConstants.ATTR_CATEGORIES), ","));
        this.origName = this.name = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_LABEL));
        this.origDescription = this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.origClassName = this.driverClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.origDefaultHost = this.driverDefaultHost = config.getAttribute(RegistryConstants.ATTR_DEFAULT_HOST);
        this.origDefaultPort = this.driverDefaultPort = config.getAttribute(RegistryConstants.ATTR_DEFAULT_PORT);
        this.origDefaultDatabase = this.driverDefaultDatabase = config.getAttribute(RegistryConstants.ATTR_DEFAULT_DATABASE);
        this.origDefaultServer = this.driverDefaultServer = config.getAttribute(RegistryConstants.ATTR_DEFAULT_SERVER);
        this.origDefaultUser = this.driverDefaultUser = config.getAttribute(RegistryConstants.ATTR_DEFAULT_USER);
        this.origSampleURL = this.sampleURL = config.getAttribute(RegistryConstants.ATTR_SAMPLE_URL);
        this.origDialectId = this.dialectId = config.getAttribute(RegistryConstants.ATTR_DIALECT);
        this.webURL = config.getAttribute(RegistryConstants.ATTR_WEB_URL);
        this.propertiesWebURL = config.getAttribute(RegistryConstants.ATTR_PROPERTIES_WEB_URL);
        this.clientRequired = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CLIENT_REQUIRED), false);
        this.customDriverLoader = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false);
        this.useURLTemplate = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_USE_URL_TEMPLATE), true);
        this.customEndpointInformation = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CUSTOM_ENDPOINT), false);
        this.promoted = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_PROMOTED), 0);
        this.supportsDriverProperties = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SUPPORTS_DRIVER_PROPERTIES), true);
        this.origInstantiable = this.instantiable = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_INSTANTIABLE), true);
        this.origEmbedded = this.embedded = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_EMBEDDED));
        this.singleConnection = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SINGLE_CONNECTION));
        this.origAnonymousAccess = this.anonymousAccess = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_ANONYMOUS));
        this.origAllowsEmptyPassword = this.allowsEmptyPassword = CommonUtils.getBoolean("allowsEmptyPassword");
        this.licenseRequired = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_LICENSE_REQUIRED));
        this.supportsDistributedMode = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SUPPORTS_DISTRIBUTED_MODE), true);
        this.custom = false;
        this.isLoaded = false;

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE)) {
            DriverLibraryAbstract library = DriverLibraryAbstract.createFromConfig(this, lib);
            if (library != null) {
                this.libraries.add(library);
            }
        }
        this.origFiles.addAll(this.libraries);

        String[] supportedConfigurationTypes = CommonUtils.split(
            config.getAttribute(RegistryConstants.ATTR_SUPPORTED_CONFIGURATION_TYPES), ",");
        if (supportedConfigurationTypes.length > 0) {
            this.configurationTypes = Stream.of(supportedConfigurationTypes)
                .map(DBPDriverConfigurationType::valueOf)
                .collect(Collectors.toSet());
        }

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE_SOURCE)) {
            this.fileSources.add(new DriverFileSource(lib));
        }

        this.iconPlain = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        if (this.iconPlain == null) {
            this.iconPlain = providerDescriptor.getIcon();
        }
        this.iconBig = this.iconPlain;
        if (config.getAttribute(RegistryConstants.ATTR_ICON_BIG) != null) {
            this.iconBig = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON_BIG));
        }
        makeIconExtensions();

        {
            // OSes
            for (IConfigurationElement os : config.getChildren(RegistryConstants.TAG_OS)) {
                supportedSystems.add(new OSDescriptor(
                        os.getAttribute(RegistryConstants.ATTR_NAME),
                        os.getAttribute(RegistryConstants.ATTR_ARCH)
                ));
            }
        }

        {
            IConfigurationElement[] pp = config.getChildren(RegistryConstants.TAG_PROVIDER_PROPERTIES);
            if (!ArrayUtils.isEmpty(pp)) {
                this.providerPropertyDescriptors.addAll(
                    Arrays.stream(pp[0].getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))
                        .map(PropertyDescriptor::extractProperties)
                        .flatMap(List<DBPPropertyDescriptor>::stream)
                        .collect(Collectors.toList()));
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

    Map<String, Object> getDefaultParameters() {
        return defaultParameters;
    }

    Map<String, Object> getCustomParameters() {
        return customParameters;
    }

    Map<String, Object> getCustomConnectionProperties() {
        return customConnectionProperties;
    }

    Map<DBPDriverLibrary, List<DriverFileInfo>> getResolvedFiles() {
        return resolvedFiles;
    }

    List<DBPNativeClientLocation> getNativeClientHomes() {
        return nativeClientHomes;
    }

    @Override
    public DriverDescriptor getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(DriverDescriptor replaceBy) {
        this.replacedBy = replaceBy;
    }

    public boolean replaces(DriverDescriptor driver) {
        for (ReplaceInfo replaceInfo : driverReplacements) {
            if (driver.getProviderDescriptor().getId().equals(replaceInfo.providerId) &&
                    driver.getId().equals(replaceInfo.driverId)) {
                return true;
            }
        }
        return false;
    }

    void makeIconExtensions() {
        if (isCustom()) {
            this.iconNormal = new DBIconComposite(this.iconPlain, false, null, null, DBIcon.OVER_LAMP, null);
        } else {
            this.iconNormal = this.iconPlain;
        }
        this.iconError = new DBIconComposite(this.iconPlain, false, null, null, isCustom() ? DBIcon.OVER_LAMP : null, DBIcon.OVER_ERROR);
    }

    @Nullable
    @Override
    public DriverClassLoader getClassLoader() {
        return classLoader;
    }

    @NotNull
    @Override
    public DataSourceProviderDescriptor getProviderDescriptor() {
        return providerDescriptor;
    }

    @NotNull
    @Override
    public DBPDataSourceProvider getDataSourceProvider() {
        return providerDescriptor.getInstance(this);
    }

    @Nullable
    @Override
    public DBPNativeClientLocationManager getNativeClientManager() {
        DBPDataSourceProvider provider = getDataSourceProvider();
        if (provider instanceof DBPNativeClientLocationManager) {
            return (DBPNativeClientLocationManager) provider;
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getProviderId() {
        return providerDescriptor.getId();
    }

    @Override
    @Property(viewable = true, order = 2)
    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = CommonUtils.nullIfEmpty(category);
    }

    @NotNull
    @Override
    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    public String getFullName() {
        if (CommonUtils.isEmpty(category) || name.contains(category)) {
            return name;
        } else {
            return category + " / " + name;
        }
    }

    /**
     * Plain icon (without any overlays).
     *
     * @return plain icon
     */
    @Override
    @NotNull
    public DBPImage getPlainIcon() {
        return iconPlain;
    }

    void setIconPlain(DBPImage iconPlain) {
        this.iconPlain = iconPlain;
    }

    /**
     * Driver icon, includes overlays for driver conditions (custom, invalid, etc)..
     *
     * @return icon
     */
    @NotNull
    @Override
    public DBPImage getIcon() {
        if (!isLoaded && isFailed) {
            return iconError;
        } else {
            return iconNormal;
        }
    }

    @NotNull
    @Override
    public DBPImage getIconBig() {
        return iconBig;
    }

    @Override
    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isModified() {
        return !isTemporary() && modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 2)
    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        if (this.driverClassName == null || !this.driverClassName.equals(driverClassName)) {
            this.driverClassName = driverClassName;
            resetDriverInstance();
        }
    }

    @NotNull
    @Override
    public <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        if (driverInstance == null) {
            loadDriver(monitor);
        }
        if (isInternalDriver() && driverInstance == null) {
            return (T)createDriverInstance();
        }
        return (T)driverInstance;
    }

    public void resetDriverInstance() {
        this.driverInstance = null;
        this.driverClass = null;
        this.isLoaded = false;

        if (!DBWorkbench.isDistributed()) {
            this.resolvedFiles.clear();
        }
    }

    private Object createDriverInstance()
            throws DBException {
        try {
            return driverClass.getConstructor().newInstance();
        } catch (InstantiationException ex) {
            throw new DBException("Can't instantiate driver class", ex);
        } catch (IllegalAccessException ex) {
            throw new DBException("Illegal access", ex);
        } catch (ClassCastException ex) {
            throw new DBException("Bad driver class name specified", ex);
        } catch (Throwable ex) {
            throw new DBException("Error during driver instantiation", ex);
        }
    }

    @Nullable
    @Override
    public String getDefaultHost() {
        return driverDefaultHost;
    }

    public void setDriverDefaultHost(String driverDefaultHost) {
        this.driverDefaultHost = driverDefaultHost;
    }

    @Nullable
    @Override
    public String getDefaultPort() {
        return driverDefaultPort;
    }

    public void setDriverDefaultPort(String driverDefaultPort) {
        this.driverDefaultPort = driverDefaultPort;
    }

    @Nullable
    @Override
    public String getDefaultDatabase() {
        return driverDefaultDatabase;
    }

    public void setDriverDefaultDatabase(String driverDefaultDatabase) {
        this.driverDefaultDatabase = driverDefaultDatabase;
    }

    @Nullable
    @Override
    public String getDefaultServer() {
        return driverDefaultServer;
    }

    public void setDriverDefaultServer(String driverDefaultServer) {
        this.driverDefaultServer = driverDefaultServer;
    }

    @Nullable
    @Override
    public String getDefaultUser() {
        return driverDefaultUser;
    }

    public void setDriverDefaultUser(String driverDefaultUser) {
        this.driverDefaultUser = driverDefaultUser;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 3)
    public String getSampleURL() {
        return sampleURL;
    }

    public void setSampleURL(String sampleURL) {
        this.sampleURL = sampleURL;
    }

    @Nullable
    @Override
    public String getWebURL() {
        return webURL;
    }

    @Nullable
    @Override
    public String getPropertiesWebURL() {
        return propertiesWebURL;
    }

    @NotNull
    @Override
    public SQLDialectMetadata getScriptDialect() {
        if (!CommonUtils.isEmpty(dialectId)) {
            SQLDialectDescriptor dialect = SQLDialectRegistry.getInstance().getDialect(dialectId);
            if (dialect != null) {
                return dialect;
            } else {
                log.debug("SQL dialect '" + dialectId + "' not found for driver '" + getFullId() + "'. Using default dialect.");
            }
        }
        return providerDescriptor.getScriptDialect();
    }

    @Override
    public boolean isClientRequired() {
        return clientRequired;
    }

    @Override
    public boolean supportsDriverProperties() {
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
    public boolean isSingleConnection() {
        return singleConnection;
    }

    public void setSingleConnection(boolean singleConnection) {
        this.singleConnection = singleConnection;
    }

    @Override
    public boolean isAnonymousAccess() {
        return anonymousAccess;
    }

    public void setAnonymousAccess(boolean anonymousAccess) {
        this.anonymousAccess = anonymousAccess;
    }

    @Override
    public boolean isAllowsEmptyPassword() {
        return allowsEmptyPassword;
    }

    public void setAllowsEmptyPassword(boolean allowsEmptyPassword) {
        this.allowsEmptyPassword = allowsEmptyPassword;
    }

    @Override
    public boolean isLicenseRequired() {
        return licenseRequired;
    }

    @Override
    public boolean isCustomDriverLoader() {
        return customDriverLoader;
    }

    void setCustomDriverLoader(boolean customDriverLoader) {
        this.customDriverLoader = customDriverLoader;
    }

    @Override
    public boolean isSampleURLApplicable() {
        return useURLTemplate;
    }

    @Override
    public boolean isCustomEndpointInformation() {
        return customEndpointInformation;
    }

    void setUseURL(boolean useURLTemplate) {
        this.useURLTemplate = useURLTemplate;
    }

    @Override
    public int getPromotedScore() {
        return promoted;
    }

    @Override
    public boolean isInstantiable() {
        return instantiable;
    }

    public void setInstantiable(boolean instantiable) {
        this.instantiable = instantiable;
    }

    @Override
    public boolean isTemporary() {
        return temporary || providerDescriptor.isTemporary();
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Nullable
    @Override
    public DBXTreeNode getNavigatorRoot() {
        return providerDescriptor.getTreeDescriptor();
    }

    public boolean isManagable() {
        return getProviderDescriptor().isDriversManagable();
    }

    @Override
    public boolean isInternalDriver() {
        return
                driverClassName != null &&
                        driverClassName.contains("sun.jdbc"); //$NON-NLS-1$
    }

    @NotNull
    @Override
    public List<DBPNativeClientLocation> getNativeClientLocations() {
        List<DBPNativeClientLocation> ids = new ArrayList<>();
        for (NativeClientDescriptor nc : getProviderDescriptor().getNativeClients()) {
            if (nc.findDistribution() != null) {
                ids.add(new RemoteNativeClientLocation(nc));
            }
        }
        ids.addAll(nativeClientHomes);

        return ids;
    }

    public void setNativeClientLocations(Collection<DBPNativeClientLocation> locations) {
        nativeClientHomes.clear();
        nativeClientHomes.addAll(locations);
    }

    void addNativeClientLocation(DBPNativeClientLocation location) {
        if (!nativeClientHomes.contains(location)) {
            nativeClientHomes.add(location);
        }
    }

    @NotNull
    @Override
    public List<? extends DBPDriverLibrary> getDriverLibraries() {
        return libraries;
    }

    public void setDriverLibraries(List<? extends DBPDriverLibrary> libs) {
        List<DBPDriverLibrary> deletedLibs = new ArrayList<>();
        for (DBPDriverLibrary lib : this.libraries) {
            if (!lib.isCustom() && !libs.contains(lib)) {
                lib.setDisabled(true);
                deletedLibs.add(lib);
            }
        }
        for (DBPDriverLibrary lib : libs) {
            lib.setDisabled(false);
        }

        this.libraries.clear();
        this.libraries.addAll(deletedLibs);
        this.libraries.addAll(libs);
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

    DBPDriverLibrary getDriverLibrary(String path) {
        for (DBPDriverLibrary lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
    }

    public void addLibraryFile(DBPDriverLibrary library, DriverFileInfo fileInfo) {
        List<DriverFileInfo> files = resolvedFiles.computeIfAbsent(library, k -> new ArrayList<>());
        files.add(fileInfo);
    }

    public DBPDriverLibrary addDriverLibrary(String path, DBPDriverLibrary.FileType fileType) {
        for (DBPDriverLibrary lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        DriverLibraryAbstract lib = DriverLibraryAbstract.createFromPath(this, fileType, path, null);
        addDriverLibrary(lib, true);
        return lib;
    }

    public boolean addDriverLibrary(DBPDriverLibrary descriptor, boolean resetCache) {
        if (resetCache) {
            descriptor.resetVersion();
            resetDriverInstance();
        }
        if (!libraries.contains(descriptor)) {
            this.libraries.add(descriptor);
            return true;
        }
        return false;
    }

    public void disabledAllDefaultLibraries() {
        libraries.stream()
                .filter(s -> !s.isCustom())
                .forEach(libr -> libr.setDisabled(true));
    }

    @NotNull
    public List<DriverFileSource> getDriverFileSources() {
        return fileSources;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getProviderPropertyDescriptors() {
        return providerPropertyDescriptors.toArray(new DBPPropertyDescriptor[0]);
    }

    public void addProviderPropertyDescriptors(Collection<DBPPropertyDescriptor> props) {
        providerPropertyDescriptors.addAll(props);
    }

    @NotNull
    @Override
    public Map<String, Object> getDefaultConnectionProperties() {
        return defaultConnectionProperties;
    }

    @NotNull
    @Override
    public Map<String, Object> getConnectionProperties() {
        return customConnectionProperties;
    }

    public void setConnectionProperty(String name, String value) {
        customConnectionProperties.put(name, value);
    }

    public void setConnectionProperties(Map<String, Object> parameters) {
        customConnectionProperties.clear();
        customConnectionProperties.putAll(parameters);
    }

    public Map<String, Object> getDefaultDriverParameters() {
        return defaultParameters;
    }

    @NotNull
    @Override
    public Map<String, Object> getDriverParameters() {
        return customParameters;
    }

    @Nullable
    @Override
    public Object getDriverParameter(String name) {
        Object value = customParameters.get(name);
        if (value == null) {
            DBPPropertyDescriptor defProperty = providerDescriptor.getDriverProperty(name);
            if (defProperty != null) {
                return defProperty.getDefaultValue();
            }
        }
        return value;
    }

    public void setDriverParameter(String name, String value, boolean setDefault) {
        DBPPropertyDescriptor prop = getProviderDescriptor().getDriverProperty(name);
        Object valueObject = prop == null ? value : GeneralUtils.convertString(value, prop.getDataType());
        customParameters.put(name, valueObject);
        if (setDefault) {
            defaultParameters.put(name, valueObject);
        }
    }

    public void setDriverParameters(Map<String, Object> parameters) {
        customParameters.clear();
        customParameters.putAll(parameters);
    }

    @Override
    public boolean isSupportedByLocalSystem() {
        if (DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            return supportsDistributedMode;
        }
        if (supportedSystems.isEmpty()) {
            // Multi-platform
            return true;
        }
        OSDescriptor localSystem = DBWorkbench.getPlatform().getLocalSystem();
        for (OSDescriptor system : supportedSystems) {
            if (system.matches(localSystem)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getLicense() {
        for (DBPDriverLibrary file : libraries) {
            if (file.getType() == DBPDriverLibrary.FileType.license) {
                final Path licenseFile = file.getLocalFile();
                if (licenseFile != null && Files.exists(licenseFile)) {
                    try {
                        // Use readAllBytes because readString may fail if file charset is inconsistent
                        return new String(Files.readAllBytes(licenseFile));
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
            throws DBException {
        this.loadDriver(monitor, false);
    }

    public boolean isSampleURLForced() {
        return isSampleURLApplicable() && !CommonUtils.equalObjects(sampleURL, origSampleURL);
    }

    @Override
    public String getConnectionURL(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return connectionInfo.getUrl();
        } else if (isSampleURLForced()) {
            // Generate URL by template
            return JDBCURL.generateUrlByTemplate(this, connectionInfo);
        } else {
            // It can be empty in some cases (e.g. when we create connections from command line command)
            return getDataSourceProvider().getConnectionURL(this, connectionInfo);
        }
    }

    @Override
    public DBPDriver createOriginalCopy() {
        DriverDescriptor driverCopy = getProviderDescriptor().createDriver(this);
        for (DBPDriverLibrary lib : this.origFiles) {
            if (lib instanceof DriverLibraryLocal && !lib.isCustom()) {
                DBPDriverLibrary libCopy = ((DriverLibraryLocal) lib).copyLibrary(this);
                libCopy.setDisabled(false);
                driverCopy.libraries.add(libCopy);
            }
        }

        driverCopy.setName(this.getOrigName());
        driverCopy.setDescription(this.getOrigDescription());
        driverCopy.setDriverClassName(this.getOrigClassName());
        driverCopy.setSampleURL(this.getOrigSampleURL());
        driverCopy.setDriverDefaultHost(this.getDefaultHost());
        driverCopy.setDriverDefaultPort(this.getDefaultPort());
        driverCopy.setDriverDefaultDatabase(this.getDefaultDatabase());
        driverCopy.setDriverDefaultUser(this.getDefaultUser());

        return driverCopy;
    }

    public void loadDriver(DBRProgressMonitor monitor, boolean forceReload)
            throws DBException {
        if (isLoaded && !forceReload) {
            return;
        }
        isLoaded = false;

        loadGlobalLibraries();
        loadLibraries(monitor);

        if (licenseRequired) {
            String licenseText = getLicense();
            if (!CommonUtils.isEmpty(licenseText) && !acceptLicense(licenseText)) {
                throw new DBException("You have to accept driver '" + getFullName() + "' license to be able to connect");
            }
        }

        try {
            if (!isCustomDriverLoader()) {
                try {
                    // Load driver classes into core module using plugin class loader
                    driverClass = Class.forName(driverClassName, true, classLoader);
                } catch (Throwable ex) {
                    throw new DBException("Error creating driver '" + getFullName() + "' instance.\nMost likely required jar files are missing.\nYou should configure jars in driver settings.\n\nReason: can't load driver class '" + driverClassName + "'", ex);
                }

                // Create driver instance
                /*if (!this.isInternalDriver())*/
                {
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

    private void loadLibraries(DBRProgressMonitor monitor) throws DBException {
        this.classLoader = null;

        List<Path> allLibraryFiles = validateFilesPresence(false);

        List<URL> libraryURLs = new ArrayList<>();
        // Load libraries
        for (Path file : allLibraryFiles) {
            URL url;
            try {
                url = file.toUri().toURL();
            } catch (MalformedURLException e) {
                log.error(e);
                continue;
            }
            libraryURLs.add(url);
        }
        // Make class loader
        ClassLoader baseClassLoader = rootClassLoader;
        if (baseClassLoader == null) {
            DBPDataSourceProvider dataSourceProvider = getDataSourceProvider();
            if (dataSourceProvider.providesDriverClasses()) {
                // Use driver provider class loader
                baseClassLoader = dataSourceProvider.getClass().getClassLoader();
            } else {
                // Use model classloader
                baseClassLoader = DBPDataSource.class.getClassLoader();
            }
        }
        this.classLoader = new DriverClassLoader(
            this,
            libraryURLs.toArray(new URL[0]),
            baseClassLoader);
    }

    private static synchronized void loadGlobalLibraries() {
        if (rootClassLoader == null) {
            final List<URL> libraries = new ArrayList<>();
            for (String library : getGlobalLibraries()) {
                try {
                    libraries.add(new File(library).toURI().toURL());
                } catch (Exception e) {
                    log.error("Can't load global library '" + library + "'", e);
                }
            }
            if (libraries.isEmpty()) {
                // No point in creating redundant classloader
                return;
            }
            rootClassLoader = new URLClassLoader(libraries.toArray(new URL[0]), DriverDescriptor.class.getClassLoader());
        }
    }

    @Nullable
    public static ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }

    public List<Path> getAllLibraryFiles() {
        return validateFilesPresence(false);
    }

    public void updateFiles() {
        validateFilesPresence(true);
    }

    @Override
    public boolean needsExternalDependencies() {
        for (DBPDriverLibrary library : libraries) {
            if (library.isDisabled() || library.isOptional() || !library.matchesCurrentPlatform()) {
                continue;
            }
            if (library.getLocalFile() == null || !Files.exists(library.getLocalFile())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private List<Path> validateFilesPresence(boolean resetVersions) {
        if (DBWorkbench.isDistributed()) {
            // We are in distributed mode
            return syncDistributedDependencies();
        }

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
                            if (file.file == null || !Files.exists(file.file)) {
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

        boolean downloaded = false;
        if (!downloadCandidates.isEmpty() || (!localLibsExists && !fileSources.isEmpty())) {
            final DriverDependencies dependencies = new DriverDependencies(downloadCandidates);
            boolean downloadOk = DBWorkbench.getPlatformUI().downloadDriverFiles(this, dependencies);
            if (!downloadOk) {
                return Collections.emptyList();
            }
            if (resetVersions) {
                resetDriverInstance();
            }
            downloaded = true;
            for (DBPDriverDependencies.DependencyNode node : dependencies.getLibraryMap()) {
                List<DriverFileInfo> info = new ArrayList<>();
                resolvedFiles.put(node.library, info);
                collectLibraryFiles(node, info);
            }
            providerDescriptor.getRegistry().saveDrivers();
        }

        List<Path> result = new ArrayList<>();

        for (DBPDriverLibrary library : libraries) {
            if (library.isDisabled() || !library.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            if (library.isDownloadable()) {
                List<DriverFileInfo> files = resolvedFiles.get(library);
                if (files != null) {
                    for (DriverFileInfo file : files) {
                        if (file.file != null && !result.contains(file.file)) {
                            result.add(file.file);
                        }
                    }
                }
            } else {
                if (library.getType() == DBPDriverLibrary.FileType.license) {
                    continue;
                }
                Path localFile = library.getLocalFile();
                if (localFile != null) {
                    if (Files.isDirectory(localFile)) {
                        try {
                            List<Path> folderFiles = Files.list(localFile)
                                .filter(p -> {
                                    String fileName = p.getFileName().toString();
                                    return fileName.endsWith(".jar") || fileName.endsWith(".zip");
                                })
                                .collect(Collectors.toList());

                            if (!folderFiles.isEmpty()) {
                                result.addAll(folderFiles);
                            }
                        } catch (IOException e) {
                            log.error("Error reading driver directory '" + localFile + "'", e);
                        }
                    }
                    if (!result.contains(localFile)) {
                        result.add(localFile);
                    }
                }
            }
        }

        // Check if local files are zip archives with jars inside
        return DriverUtils.extractZipArchives(result);
    }

    /**
     * Sync driver libs with remote server
     */
    private List<Path> syncDistributedDependencies() {
        List<Path> localFilePaths = new ArrayList<>();

        final Map<DBPDriverLibrary, List<DriverFileInfo>> downloadCandidates = new LinkedHashMap<>();
        for (DBPDriverLibrary library : libraries) {
            if (library.isDisabled() || !library.matchesCurrentPlatform()) {
                continue;
            }
            List<DriverFileInfo> files = resolvedFiles.get(library);
            if (files != null) {
                for (DriverFileInfo depFile : files) {
                    Path driverFolder = getWorkspaceStorageFolder();
                    Path localDriverFile = driverFolder.resolve(depFile.getFile());
                    if (!Files.exists(localDriverFile) || depFile.getFileCRC() == 0 ||
                        depFile.getFileCRC() != calculateFileCRC(localDriverFile))
                    {
                        downloadCandidates
                            .computeIfAbsent(library, key -> new ArrayList<>())
                            .add(depFile);
                    } else {
                        localFilePaths.add(localDriverFile);
                    }
                }
            }
        }

        if (!downloadCandidates.isEmpty()) {
            DBFileController fileController = DBWorkbench.getPlatform().getFileController();
            for (var libEntry : downloadCandidates.entrySet()) {
                DBPDriverLibrary library = libEntry.getKey();
                for (DriverFileInfo fileInfo : libEntry.getValue()) {
                    try {
                        Path driverFolder = getWorkspaceStorageFolder();
                        Path localDriverFile = driverFolder.resolve(fileInfo.getFile());
                        if (!Files.exists(localDriverFile.getParent())) {
                            Files.createDirectories(localDriverFile.getParent());
                        }

                        byte[] fileData = fileController.loadFileData(DBFileController.TYPE_DATABASE_DRIVER, fileInfo.getFile().toString());
                        Files.write(localDriverFile, fileData, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

                        localFilePaths.add(localDriverFile);
                    } catch (Exception e) {
                        log.error("Error downloading driver file '" + fileInfo.getFile() + "'", e);
                    }
                }
            }
        }

        return localFilePaths;
    }

    public static long calculateFileCRC(Path localDriverFile) {
        try (InputStream is = Files.newInputStream(localDriverFile)) {
            CRC32 crc = new CRC32();

            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
            return crc.getValue();
        } catch (IOException e) {
            log.error("Error reading file '" + localDriverFile + "', CRC calculation failed", e);
            return 0;
        }
    }

    Path getWorkspaceStorageFolder() {
        return getWorkspaceDriversStorageFolder().resolve(getProviderId()).resolve(getId());
    }

    List<DriverFileInfo> getCachedFiles(DBPDriverLibrary library) {
        return resolvedFiles.get(library);
    }

    private void checkDriverVersion(DBRProgressMonitor monitor) throws IOException {
        for (DBPDriverLibrary library : libraries) {
            final Collection<String> availableVersions = library.getAvailableVersions(monitor);
            if (!CommonUtils.isEmpty(availableVersions)) {
                final String curVersion = library.getVersion();
                String latestVersion = VersionUtils.findLatestVersion(availableVersions);
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

    private boolean acceptLicense(String licenseText) {
        // Check registry
        DBPPreferenceStore prefs = DBWorkbench.getPlatform().getPreferenceStore();
        String acceptedStr = prefs.getString(LICENSE_ACCEPT_KEY + getId());
        if (!CommonUtils.isEmpty(acceptedStr)) {
            return true;
        }

        if (DBWorkbench.getPlatformUI().acceptLicense(
            "You have to accept license of '" + getFullName() + " ' to continue",
            licenseText))
        {
            // Save in registry
            prefs.setValue(LICENSE_ACCEPT_KEY + getId(), true + ":" + System.currentTimeMillis() + ":" + System.getProperty(StandardConstants.ENV_USER_NAME));
            return true;
        }
        return false;
    }

    public String getOrigName() {
        return origName;
    }

    public String getOrigDescription() {
        return origDescription;
    }

    public String getOrigClassName() {
        return origClassName;
    }

    public String getOrigDefaultPort() {
        return origDefaultPort;
    }

    public String getOrigDefaultDatabase() {
        return origDefaultDatabase;
    }

    public String getOrigDefaultServer() {
        return origDefaultServer;
    }

    public String getOrigDefaultUser() {
        return origDefaultUser;
    }

    public String getOrigSampleURL() {
        return origSampleURL;
    }

    public boolean isOrigEmbedded() {
        return origEmbedded;
    }

    public boolean isOrigAnonymousAccess() {
        return origAnonymousAccess;
    }

    public boolean isOrigAllowsEmptyPassword() {
        return origAllowsEmptyPassword;
    }

    public boolean isOrigInstantiable() {
        return origInstantiable;
    }

    public List<DBPDriverLibrary> getOrigFiles() {
        return origFiles;
    }

    public Set<DBPDriverConfigurationType> getSupportedConfigurationTypes() {
        return configurationTypes;
    }

    public DBPNativeClientLocation getDefaultClientLocation() {
        DBPNativeClientLocationManager clientManager = getNativeClientManager();
        if (clientManager != null) {
            return clientManager.getDefaultLocalClientLocation();
        }
        return null;
    }

    /**
     * Add resolved files to all libraries
     */
    public boolean resolveDriverFiles(Path targetFileLocation) {
        if (libraries.isEmpty()) {
            return false;
        }
        resolvedFiles.clear();
        for (DBPDriverLibrary library : libraries) {
            // We need to sync resolved files with real files of library
            // - Local files are linked directly
            // - Local folders are linked to folder's contents
            if (library instanceof DriverLibraryLocal) {
                List<DriverFileInfo> libraryFiles = new ArrayList<>();

                Path srcLocalFile = library.getLocalFile();
                if (srcLocalFile == null) {
                    log.warn("\t-Driver library file '" + library.getPath() + "' is missing");
                    continue;
                }
                if (!Files.exists(srcLocalFile)) {
                    log.warn("\tDriver library file '" + srcLocalFile.toAbsolutePath() + "' doesn't exist");
                    continue;
                }

                String targetPath = library.getPath();
                int divPos = targetPath.indexOf(":");
                if (divPos != -1) {
                    targetPath = targetPath.substring(divPos + 1);
                    while (targetPath.startsWith("/")) targetPath = targetPath.substring(1);
                }

                if (Files.isDirectory(srcLocalFile)) {
                    Path targetFolder = targetFileLocation.resolve(targetPath);
                    if (!Files.exists(targetFolder)) {
                        try {
                            Files.createDirectories(targetFolder);
                        } catch (IOException e) {
                            log.error("Error creating driver target directory '" + targetFolder + "'", e);
                            return false;
                        }
                    }

                    try {
                        resolveDirectories(targetFileLocation, library, srcLocalFile, targetFolder, libraryFiles);
                    } catch (IOException e) {
                        log.error("Error resolving directory files at '" + srcLocalFile + "'", e);
                    }
                } else {
                    Path trgLocalFile = targetFileLocation.resolve(targetPath);
                    Path trgFolder = trgLocalFile.getParent();
                    if (!Files.exists(trgFolder)) {
                        try {
                            Files.createDirectories(trgFolder);
                        } catch (IOException e) {
                            log.error("Error creating driver file directory '" + trgFolder + "'", e);
                        }
                    }
                    DriverFileInfo fileInfo = resolveFile(targetFileLocation, library, srcLocalFile, trgLocalFile);
                    if (fileInfo != null) {
                        libraryFiles.add(fileInfo);
                    }
                }

                if (!libraryFiles.isEmpty()) {
                    resolvedFiles.put(library, libraryFiles);
                }

            } else {
                // Ignore all non-local libraries for now
            }
        }
        if (resolvedFiles.isEmpty()) {
            return false;
        }
        modified = true;
        return true;
    }

    private void resolveDirectories(Path targetFileLocation, DBPDriverLibrary library, Path srcLocalFile, Path trgLocalFile, List<DriverFileInfo> libraryFiles) throws IOException {
        if (!Files.exists(trgLocalFile)) {
            try {
                Files.createDirectories(trgLocalFile);
            } catch (IOException e) {
                log.error("Error creating driver library target directory '" + trgLocalFile + "'", e);
                return;
            }
        }
        // Resolve directory contents
        List<Path> srcDirFiles = Files.list(srcLocalFile).collect(Collectors.toList());
        for (Path dirFile : srcDirFiles) {
            String fileName = dirFile.getFileName().toString();
            // Skip non-libraries
            if (fileName.endsWith(".txt")) {
                continue;
            }
            Path trgDirFile = trgLocalFile.resolve(dirFile.getFileName());
            if (Files.isDirectory(dirFile)) {
                resolveDirectories(targetFileLocation, library, dirFile, trgDirFile, libraryFiles);
            } else {
                DriverFileInfo fileInfo = resolveFile(targetFileLocation, library, dirFile, trgDirFile);
                if (fileInfo != null) {
                    libraryFiles.add(fileInfo);
                }
            }
        }
    }

    private DriverFileInfo resolveFile(Path targetFileLocation, DBPDriverLibrary library, Path srcLocalFile, Path trgLocalFile) {
        Path relPath = targetFileLocation.relativize(trgLocalFile);
        DriverFileInfo info = new DriverFileInfo(trgLocalFile.getFileName().toString(), null, library.getType(), relPath);
        info.fileCRC = calculateFileCRC(srcLocalFile);
        long targetCRC = Files.exists(trgLocalFile) ? calculateFileCRC(trgLocalFile) : 0;
        if (info.fileCRC != targetCRC) {
            // Copy file
            try {
                Files.copy(srcLocalFile, trgLocalFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Error copying library file '" + srcLocalFile + "' into '" + trgLocalFile + "'", e);
                return null;
            }
        }
        return info;
    }

    @Override
    public String toString() {
        return name;
    }

    /////////////////////////////////////////
    // Static utilities

    public static Path getWorkspaceDriversStorageFolder() {
        return DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(DBConstants.DEFAULT_DRIVERS_FOLDER);
    }

    public static Path getDriversContribFolder() throws IOException {
        return Path.of(Platform.getInstallLocation().getDataArea(DBConstants.DEFAULT_DRIVERS_FOLDER).toExternalForm());
    }

    @NotNull
    public static Path getCustomDriversHome() {
        Path homeFolder;
        // Try to use custom drivers path from preferences
        DBPPlatform platform = DBWorkbench.getPlatform();
        String driversHome = platform.getPreferenceStore().getString(ModelPreferences.UI_DRIVERS_HOME);
        if (!CommonUtils.isEmpty(driversHome)) {
            homeFolder = Path.of(driversHome);
        } else {
            if (platform.getWorkspace().getAbsolutePath().getParent() == null) {
                homeFolder = platform.getApplication().getDefaultWorkingFolder();
                if (homeFolder != null && homeFolder.getParent() != null) {
                    homeFolder = homeFolder.getParent().resolve(DBConstants.DEFAULT_DRIVERS_FOLDER);
                } else {
                    log.warn("Can't find folder path for drivers. Use home folder");
                    return RuntimeUtils.getUserHomeDir().toPath().resolve(DBConstants.DEFAULT_DRIVERS_FOLDER);
                }
            } else {
                homeFolder = platform.getWorkspace().getAbsolutePath().getParent().resolve(DBConstants.DEFAULT_DRIVERS_FOLDER);
            }
        }
        if (!Files.exists(homeFolder)) {
            try {
                Files.createDirectories(homeFolder);
            } catch (IOException e) {
                log.warn("Can't create drivers folder '" + homeFolder.toAbsolutePath() + "'", e);
            }
        }

        return homeFolder;
    }

    @NotNull
    public static String[] getDriversSources() {
        String sourcesString = DBWorkbench.getPlatform().getPreferenceStore().getString(ModelPreferences.UI_DRIVERS_SOURCES);
        List<String> pathList = CommonUtils.splitString(sourcesString, '|');
        return pathList.toArray(new String[0]);
    }

    @NotNull
    public static String getDriversPrimarySource() {
        String sourcesString = DBWorkbench.getPlatform().getPreferenceStore().getString(ModelPreferences.UI_DRIVERS_SOURCES);
        int divPos = sourcesString.indexOf('|');
        return divPos == -1 ? sourcesString : sourcesString.substring(0, divPos);
    }

    @NotNull
    public static String[] getGlobalLibraries() {
        final String librariesString = DBWorkbench.getPlatform().getPreferenceStore().getString(ModelPreferences.UI_DRIVERS_GLOBAL_LIBRARIES);
        final List<String> libraries = new ArrayList<>();
        for (String library : CommonUtils.splitString(librariesString, '|')) {
            try {
                libraries.add(URLDecoder.decode(library, GeneralUtils.UTF8_ENCODING));
            } catch (UnsupportedEncodingException e) {
                log.error(e);
            }
        }
        return libraries.toArray(new String[0]);
    }

}
