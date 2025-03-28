/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.impl.ProviderPropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.NativeClientDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver {
    private static final Log log = Log.getLog(DriverDescriptor.class);

    private static final String PROP_DRIVERS_LOCATION = "DRIVERS_LOCATION";

    private static final String LICENSE_ACCEPT_KEY = "driver.license.accept.";

    public static final DriverDescriptor NULL_DRIVER = new DriverDescriptor("NULL");

    private boolean propagateDriverProperties;
    private boolean origPropagateDriverProperties;

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
    private String databaseDocumentationSuffixURL;
    private DBPImage iconPlain;
    private DBPImage iconNormal;
    private DBPImage iconError;
    private DBPImage iconBig;
    private DBPImage logoImage;
    private boolean embedded, origEmbedded;
    private boolean supportsDistributedMode;
    private boolean notAvailableDriver;
    private boolean singleConnection;
    private boolean origThreadSafe, threadSafe;
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
    private Set<String> supportedPageFields = new HashSet<>(Set.of(DBConstants.PROP_HOST, DBConstants.PROP_PORT, DBConstants.PROP_DATABASE));
    private final List<DBPNativeClientLocation> nativeClientHomes = new ArrayList<>();
    private final List<DriverFileSource> fileSources = new ArrayList<>();
    private final List<DBPDriverLibrary> libraries = new ArrayList<>();
    private final List<DBPDriverLibrary> origLibraries = new ArrayList<>();
    private final List<ProviderPropertyDescriptor> mainPropertyDescriptors = new ArrayList<>();
    private final Set<ProviderPropertyDescriptor> providerPropertyDescriptors = new LinkedHashSet<>();
    private final List<OSDescriptor> supportedSystems = new ArrayList<>();

    private final List<ReplaceInfo> driverReplacements = new ArrayList<>();
    private DriverDescriptor replacedBy;
    private String nonAvailabilityTitle;
    private String nonAvailabilityDescription;
    private String nonAvailabilityReason;

    private final Map<String, Object> defaultParameters = new HashMap<>();
    private final Map<String, Object> customParameters = new HashMap<>();

    private final Map<String, Object> defaultConnectionProperties = new HashMap<>();
    private final Map<String, Object> customConnectionProperties = new HashMap<>();
    private final Map<String, Object> originalConnectionProperties = new HashMap<>();

    // Map of driver loaders. Key=auth model ID
    private volatile Map<String, DriverLoaderDescriptor> driverLoaders;
    private volatile boolean loadersInitialized = false;
    private volatile DriverLoaderDescriptor defaultDriverLoader;

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
        this.origThreadSafe = true;
        this.threadSafe = true;
        this.supportsDistributedMode = true;
        this.notAvailableDriver = false;

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
            this.databaseDocumentationSuffixURL = copyFrom.databaseDocumentationSuffixURL;
            this.embedded = copyFrom.embedded;
            this.propagateDriverProperties = copyFrom.propagateDriverProperties;
            this.singleConnection = copyFrom.singleConnection;
            this.threadSafe = copyFrom.threadSafe;
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
            this.mainPropertyDescriptors.addAll(copyFrom.mainPropertyDescriptors);
            this.providerPropertyDescriptors.addAll(copyFrom.providerPropertyDescriptors);

            this.defaultParameters.putAll(copyFrom.defaultParameters);
            this.customParameters.putAll(copyFrom.customParameters);

            this.defaultConnectionProperties.putAll(copyFrom.defaultConnectionProperties);
            this.customConnectionProperties.putAll(copyFrom.customConnectionProperties);
            this.configurationTypes.addAll(copyFrom.configurationTypes);
            this.supportedPageFields.addAll(copyFrom.supportedPageFields);
            this.supportsDistributedMode = copyFrom.supportsDistributedMode;
            this.notAvailableDriver = copyFrom.notAvailableDriver;
            this.nonAvailabilityTitle = copyFrom.nonAvailabilityTitle;
            this.nonAvailabilityDescription = copyFrom.nonAvailabilityDescription;
            this.nonAvailabilityReason = copyFrom.nonAvailabilityReason;
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
        this.databaseDocumentationSuffixURL = config.getAttribute(RegistryConstants.ATTR_DATABASE_DOCUMENTATION_SUFFIX_URL);
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
        this.origThreadSafe = this.threadSafe = CommonUtils.getBoolean(config.getAttribute("threadSafe"), true);
        this.origAnonymousAccess = this.anonymousAccess = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_ANONYMOUS));
        this.origAllowsEmptyPassword = this.allowsEmptyPassword = CommonUtils.getBoolean("allowsEmptyPassword");
        this.origPropagateDriverProperties = this.propagateDriverProperties =
            CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_PROPAGATE_DRIVER_PROPERTIES));
        this.licenseRequired = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_LICENSE_REQUIRED));
        this.supportsDistributedMode = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SUPPORTS_DISTRIBUTED_MODE), true);
        this.custom = false;

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE)) {
            DriverLibraryAbstract library = DriverLibraryAbstract.createFromConfig(this, lib);
            if (library != null) {
                this.libraries.add(library);
            }
        }
        this.origLibraries.addAll(this.libraries);

        String[] supportedConfigurationTypes = CommonUtils.split(
            config.getAttribute(RegistryConstants.ATTR_SUPPORTED_CONFIGURATION_TYPES), ",");
        if (supportedConfigurationTypes.length > 0) {
            this.configurationTypes = Stream.of(supportedConfigurationTypes)
                .map(DBPDriverConfigurationType::valueOf)
                .collect(Collectors.toSet());
        }

        String[] supportedPageFields = CommonUtils.split(
            config.getAttribute(RegistryConstants.ATTR_SUPPORTED_PAGE_FIELDS), ",");
        if (supportedPageFields.length > 0) {
            this.supportedPageFields = Stream.of(supportedPageFields).collect(Collectors.toSet());
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
        String logoImageAttr = config.getAttribute("logoImage");
        if (!CommonUtils.isEmpty(logoImageAttr)) {
            this.logoImage = iconToImage(logoImageAttr);
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
            IConfigurationElement[] pp = config.getChildren(RegistryConstants.TAG_MAIN_PROPERTIES);
            if (!ArrayUtils.isEmpty(pp)) {
                String copyFromDriverId = pp[0].getAttribute("copyFrom");
                if (!CommonUtils.isEmpty(copyFromDriverId)) {
                    DriverDescriptor copyFromDriver = providerDescriptor.getDriver(copyFromDriverId);
                    if (copyFromDriver == null) {
                        log.debug("Driver '" + copyFromDriverId + "' not found. Cannot copy main properties into '" + getId() + "'");
                    } else {
                        this.mainPropertyDescriptors.addAll(copyFromDriver.mainPropertyDescriptors);
                    }
                }
                this.mainPropertyDescriptors.addAll(
                    Arrays.stream(pp[0].getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))
                        .map(ProviderPropertyDescriptor::extractProviderProperties)
                        .flatMap(List<ProviderPropertyDescriptor>::stream)
                        .toList());
            }
        }

        {
            IConfigurationElement[] pp = config.getChildren(RegistryConstants.TAG_PROVIDER_PROPERTIES);
            if (!ArrayUtils.isEmpty(pp)) {
                String copyFromDriverId = pp[0].getAttribute("copyFrom");
                if (!CommonUtils.isEmpty(copyFromDriverId)) {
                    DriverDescriptor copyFromDriver = providerDescriptor.getDriver(copyFromDriverId);
                    if (copyFromDriver == null) {
                        log.debug("Driver '" + copyFromDriverId + "' not found. Cannot copy provider properties into '" + getId() + "'");
                    } else {
                        this.providerPropertyDescriptors.addAll(copyFromDriver.providerPropertyDescriptors);
                    }
                }
                this.providerPropertyDescriptors.addAll(
                    Arrays.stream(pp[0].getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))
                        .map(ProviderPropertyDescriptor::extractProviderProperties)
                        .flatMap(List<ProviderPropertyDescriptor>::stream)
                        .toList());
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
                        originalConnectionProperties.put(paramName, paramValue);
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

        {
            IConfigurationElement[] notAvailable = config.getChildren(RegistryConstants.ATTR_NOT_AVAILABLE_DRIVER);
            for (IConfigurationElement element : notAvailable) {
                this.nonAvailabilityReason = element.getAttribute(RegistryConstants.ATTR_MESSAGE);
                this.nonAvailabilityTitle = element.getAttribute(RegistryConstants.ATTR_TITLE);
                this.nonAvailabilityDescription = element.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
            }
        }
    }

    Map<String, Object> getDefaultParameters() {
        return defaultParameters;
    }

    Map<String, Object> getCustomParameters() {
        return customParameters;
    }

    List<DBPNativeClientLocation> getNativeClientHomes() {
        return nativeClientHomes;
    }

    @Override
    public DriverDescriptor getReplacedBy() {
        return replacedBy;
    }

    @Override
    public boolean isNotAvailable() {
        return nonAvailabilityReason != null;
    }

    @NotNull
    @Override
    public String getNonAvailabilityReason() {
        return nonAvailabilityReason;
    }

    @Nullable
    @Override
    public String getNonAvailabilityTitle() {
        return nonAvailabilityTitle;
    }

    @Nullable
    @Override
    public String getNonAvailabilityDescription() {
        return nonAvailabilityDescription;
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
    
    @NotNull
    @Override
    public List<Pair<String,String>> getDriverReplacementsInfo() {
        List<Pair<String, String>> result = new ArrayList<>();
        for (ReplaceInfo replaceInfo : driverReplacements) {
            result.add(new Pair<String, String>(replaceInfo.providerId, replaceInfo.driverId));
        }
        return result;
    }

    void makeIconExtensions() {
        if (isCustom()) {
            this.iconNormal = new DBIconComposite(this.iconPlain, false, null, null, DBIcon.OVER_LAMP, null);
        } else {
            this.iconNormal = this.iconPlain;
        }
        this.iconError = new DBIconComposite(this.iconPlain, false, null, null, isCustom() ? DBIcon.OVER_LAMP : null, DBIcon.OVER_ERROR);
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
        DriverLoaderDescriptor loader = getDefaultDriverLoader();
        if (!loader.isLoaded() && loader.isFailed()) {
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

    @Nullable
    @Override
    public DBPImage getLogoImage() {
        return logoImage;
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

    @Nullable
    @Override
    public String getDatabaseDocumentationSuffixURL() {
        return databaseDocumentationSuffixURL;
    }

    @NotNull
    @Override
    public SQLDialectMetadata getScriptDialect() {
        if (!CommonUtils.isEmpty(dialectId)) {
            SQLDialectMetadata dialect = DBWorkbench.getPlatform().getSQLDialectRegistry().getDialect(dialectId);
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

    public boolean isPropagateDriverProperties() {
        return propagateDriverProperties;
    }

    public void setPropagateDriverProperties(boolean propagateDriverProperties) {
        this.propagateDriverProperties = propagateDriverProperties;
    }

    @Override
    public boolean isSingleConnection() {
        return singleConnection;
    }

    @Override
    public boolean isThreadSafeDriver() {
        return threadSafe;
    }

    public void setThreadSafeDriver(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    public boolean isOrigThreadSafeDriver() {
        return origThreadSafe;
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
        return driverClassName != null && driverClassName.contains("sun.jdbc"); //$NON-NLS-1$
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

    public DBPDriverLibrary getDriverLibrary(String path) {
        for (DBPDriverLibrary lib : libraries) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
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
    public synchronized DriverLoaderDescriptor getDefaultDriverLoader() {
        if (defaultDriverLoader == null) {
            defaultDriverLoader = new DriverLoaderDescriptor(DriverLoaderDescriptor.DEFAULT_LOADER_ID, this);
        }
        return defaultDriverLoader;
    }

    @NotNull
    @Override
    public DBPDriverLoader getDriverLoader(@NotNull DBPDataSourceContainer dataSourceContainer) {
        getAllDriverLoaders();

        DBPAuthModelDescriptor authModel = dataSourceContainer.getConnectionConfiguration().getAuthModelDescriptor();
        DriverLoaderDescriptor loader = driverLoaders.get(authModel.getId());
        if (loader != null) {
            return loader;
        }
        return getDefaultDriverLoader();
    }

    /**
     * For internal use only.
     */
    @Nullable
    public DriverLoaderDescriptor preCreateDriverLoader(String loaderId) {
        if (driverLoaders == null) {
            driverLoaders = new LinkedHashMap<>();
        }
        DriverLoaderDescriptor loader = driverLoaders.get(loaderId);
        if (loader == null) {
            loader = new DriverLoaderDescriptor(loaderId, this);
            driverLoaders.put(loaderId, loader);
        }
        return loader;
    }

    @NotNull
    @Override
    public List<DBPDriverLoader> getAllDriverLoaders() {
        if (!loadersInitialized) {
            synchronized (this) {
                if (!loadersInitialized) {
                    if (driverLoaders == null) {
                        driverLoaders = new LinkedHashMap<>();
                    }
                    for (DBPAuthModelDescriptor authModel : DataSourceProviderRegistry.getInstance().getApplicableAuthModels(this)) {
                        List<? extends DBPDriverLibrary> driverLibraries = authModel.getDriverLibraries();
                        if (!CommonUtils.isEmpty(driverLibraries) && !driverLoaders.containsKey(authModel.getId())) {
                            DriverLoaderDescriptor loader = new DriverLoaderDescriptor(authModel.getId(), this);
                            loader.addLibraryProvider(authModel);
                            driverLoaders.put(authModel.getId(), loader);
                        }
                    }
                }
            }
        }
        ArrayList<DBPDriverLoader> loaders = new ArrayList<>();
        loaders.add(getDefaultDriverLoader());
        loaders.addAll(driverLoaders.values());
        return loaders;
    }

    @Override
    public void validateFilesPresence(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSourceContainer
    ) throws DBException {
        getDriverLoader(dataSourceContainer).validateFilesPresence(monitor);
    }

    @Override
    public void resetDriverInstance() {
        for (DBPDriverLoader dld : getAllDriverLoaders()) {
            ((DriverLoaderDescriptor)dld).resetDriverInstance();
        }
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getMainPropertyDescriptors() {
        return mainPropertyDescriptors.toArray(new DBPPropertyDescriptor[0]);
    }

    public void addMainPropertyDescriptors(Collection<ProviderPropertyDescriptor> props) {
        mainPropertyDescriptors.addAll(props);
    }

    @NotNull
    @Override
    public ProviderPropertyDescriptor[] getProviderPropertyDescriptors() {
        return providerPropertyDescriptors.toArray(new ProviderPropertyDescriptor[0]);
    }

    public void addProviderPropertyDescriptors(Collection<ProviderPropertyDescriptor> props) {
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

    @NotNull
    private Map<String, Object> getOriginalConnectionProperties() {
        return originalConnectionProperties;
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

    @Nullable
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

    public boolean isSampleURLForced() {
        return isSampleURLApplicable() && !CommonUtils.equalObjects(sampleURL, origSampleURL);
    }

    @Nullable
    @Override
    public String getConnectionURL(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return connectionInfo.getUrl();
        } else if (isSampleURLForced()) {
            // Generate URL by template
            return DatabaseURL.generateUrlByTemplate(this, connectionInfo);
        } else {
            // It can be empty in some cases (e.g. when we create connections from command line command)
            return getDataSourceProvider().getConnectionURL(this, connectionInfo);
        }
    }

    @NotNull
    @Override
    public DBPDriver createOriginalCopy() {
        DriverDescriptor driverCopy = getProviderDescriptor().createDriver(this);
        for (DBPDriverLibrary lib : this.origLibraries) {
            if (lib instanceof DriverLibraryLocal && !lib.isCustom()) {
                DBPDriverLibrary libCopy = ((DriverLibraryLocal) lib).copyLibrary(this);
                libCopy.setDisabled(false);
                if (libCopy instanceof DriverLibraryLocal) {
                    ((DriverLibraryLocal) libCopy).setUseOriginalJar(true);
                }
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
        driverCopy.setConnectionProperties(this.getOriginalConnectionProperties());
        driverCopy.setThreadSafeDriver(this.isOrigThreadSafeDriver());
        return driverCopy;
    }

    boolean acceptLicense(String licenseText) {
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

    public boolean isOrigPropagateDriverProperties() {
        return origPropagateDriverProperties;
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

    public List<DBPDriverLibrary> getOrigLibraries() {
        return origLibraries;
    }

    @NotNull
    public Set<DBPDriverConfigurationType> getSupportedConfigurationTypes() {
        return configurationTypes;
    }

    @NotNull
    public Set<String> getSupportedPageFields() {
        return supportedPageFields;
    }

    public boolean isSupportsDistributedMode() {
        return supportsDistributedMode;
    }

    public void setSupportsDistributedMode(boolean supportsDistributedMode) {
        this.supportsDistributedMode = supportsDistributedMode;
    }

    public DBPNativeClientLocation getDefaultClientLocation() {
        DBPNativeClientLocationManager clientManager = getNativeClientManager();
        if (clientManager != null) {
            return clientManager.getDefaultLocalClientLocation();
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    /////////////////////////////////////////
    // Static utilities

    // used to download drivers from external fs or distributed to a temp folder
    @NotNull
    public static Path getExternalDriversStorageFolder() {
        DBPPlatform platform = DBWorkbench.getPlatform();
        if (platform.getApplication().isMultiuser()) {
            try {
                return platform.getTempFolder(new LoggingProgressMonitor(), DBFileController.DATA_FOLDER)
                    .resolve(DBFileController.TYPE_DATABASE_DRIVER);
            } catch (IOException e) {
                throw new RuntimeException("Error getting drivers temp folder", e);
            }
        }

        Path customFolder = getCustomDriversHome();
        String distributedFolderName = platform.getApplication().defaultDistributedDriversFolderName();
        if (distributedFolderName != null) {
            customFolder = customFolder.resolve(distributedFolderName);
        }
        return customFolder;
    }

    public static Path getWorkspaceDriversStorageFolder() {
        return DBWorkbench.getPlatform().getWorkspace().getAbsolutePath()
            .resolve(DBFileController.DATA_FOLDER)
            .resolve(DBFileController.TYPE_DATABASE_DRIVER);
    }

    public static Path getProvidedDriversStorageFolder() {
        return DBWorkbench.getPlatform()
            .getWorkspace()
            .getMetadataFolder()
            .resolve(DBConstants.DEFAULT_DRIVERS_FOLDER);
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

    @Override
    public boolean matchesId(@NotNull String driverId) {
        if (driverId.equals(this.id)) {
            return true;
        }
        for (ReplaceInfo replace : driverReplacements) {
            if (driverId.equals(replace.driverId)) {
                return true;
            }
        }
        return false;
    }

}
