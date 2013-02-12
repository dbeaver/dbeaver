/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AcceptLicenseDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.properties.IPropertyDescriptorEx;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * DriverDescriptor
 */
public class DriverDescriptor extends AbstractDescriptor implements DBPDriver
{
    static final Log log = LogFactory.getLog(DriverDescriptor.class);

    public static final String DRIVERS_FOLDER = "drivers"; //$NON-NLS-1$

    public static final char URL_GROUP_START = '{';
    public static final char URL_GROUP_END = '}';
    public static final char URL_OPTIONAL_START = '[';
    public static final char URL_OPTIONAL_END = ']';
    public static final String PROP_HOST = "host"; //$NON-NLS-1$
    public static final String PROP_PORT = "port"; //$NON-NLS-1$
    public static final String PROP_DATABASE = "database"; //$NON-NLS-1$
    public static final String PROP_SERVER = "server"; //$NON-NLS-1$
    public static final String PROP_FOLDER = "folder"; //$NON-NLS-1$
    public static final String PROP_FILE = "file"; //$NON-NLS-1$

    private DataSourceProviderDescriptor providerDescriptor;
    private String id;
    private String category;
    private String name, origName;
    private String description, origDescription;
    private String driverClassName, origClassName;
    private String driverDefaultPort, origDefaultPort;
    private String sampleURL, origSampleURL;
    private String webURL;
    private Image iconPlain;
    private Image iconNormal;
    private Image iconError;
    private boolean clientRequired;
    private boolean supportsDriverProperties;
    private boolean anonymousAccess;
    private boolean customDriverLoader;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private final List<String> clientHomeIds = new ArrayList<String>();
    private final List<DriverFileDescriptor> files = new ArrayList<DriverFileDescriptor>();
    private final List<DriverFileDescriptor> origFiles = new ArrayList<DriverFileDescriptor>();
    private final List<DriverPathDescriptor> pathList = new ArrayList<DriverPathDescriptor>();
    private final List<IPropertyDescriptor> connectionPropertyDescriptors = new ArrayList<IPropertyDescriptor>();
    private final List<OSDescriptor> supportedSystems = new ArrayList<OSDescriptor>();

    private final List<ReplaceInfo> driverReplacements = new ArrayList<ReplaceInfo>();
    private DriverDescriptor replacedBy;

    private Map<Object, Object> defaultParameters = new HashMap<Object, Object>();
    private Map<Object, Object> customParameters = new HashMap<Object, Object>();

    private Map<Object, Object> defaultConnectionProperties = new HashMap<Object, Object>();
    private Map<Object, Object> customConnectionProperties = new HashMap<Object, Object>();

    private Class driverClass;
    private boolean isLoaded;
    private Object driverInstance;
    private DriverClassLoader classLoader;

    private final List<DataSourceDescriptor> usedBy = new ArrayList<DataSourceDescriptor>();

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
        this.id = CommonUtils.getString(config.getAttribute(RegistryConstants.ATTR_ID));
        this.category = CommonUtils.getString(config.getAttribute(RegistryConstants.ATTR_CATEGORY));
        this.origName = this.name = CommonUtils.getString(config.getAttribute(RegistryConstants.ATTR_LABEL));
        this.origDescription = this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
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
        this.anonymousAccess = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_ANONYMOUS));
        this.custom = false;
        this.isLoaded = false;

        for (IConfigurationElement lib : config.getChildren(RegistryConstants.TAG_FILE)) {
            this.files.add(new DriverFileDescriptor(this, lib));
        }
        this.origFiles.addAll(this.files);

        String iconName = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconName)) {
            this.iconPlain = iconToImage(iconName);
        }
        if (this.iconPlain == null) {
            this.iconPlain = new Image(null, providerDescriptor.getIcon(), SWT.IMAGE_COPY);
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
            IConfigurationElement[] propElements = config.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP);
            for (IConfigurationElement prop : propElements) {
                connectionPropertyDescriptors.addAll(PropertyDescriptorEx.extractProperties(prop));
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
            ((BundleHost)providerDescriptor.getContributorBundle()).getClassLoader());
    }

    DriverDescriptor getReplacedBy()
    {
        return replacedBy;
    }

    void setReplacedBy(DriverDescriptor replaceBy)
    {
        this.replacedBy = replaceBy;
    }

    boolean replaces(DriverDescriptor driver)
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
            OverlayImageDescriptor customDescriptor = new OverlayImageDescriptor(this.iconPlain.getImageData());
            customDescriptor.setBottomLeft(new ImageDescriptor[]{DBIcon.OVER_LAMP.getImageDescriptor()});
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

    public String getFullName()
    {
        if (CommonUtils.isEmpty(category)) {
            return name;
        } else {
            return category + " " + name;
        }
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
    @Override
    public Image getIcon()
    {
        if (!isLoaded && (isFailed || (isManagable() && !isInternalDriver() && !hasValidLibraries()))) {
            return iconError;
        } else {
            return iconNormal;
        }
    }

    private boolean hasValidLibraries()
    {
        for (DriverFileDescriptor lib : files) {
            if (lib.getFile().exists() || (!lib.isDisabled() && !CommonUtils.isEmpty(lib.getExternalURL()))) {
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

    void setDisabled(boolean disabled)
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
            this.driverInstance = null;
            this.driverClass = null;
            this.isLoaded = false;
        }
    }

    @Override
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
    public boolean isAnonymousAccess()
    {
        return anonymousAccess;
    }

    @Override
    public boolean isCustomDriverLoader()
    {
        return customDriverLoader;
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

    @Override
    public Collection<DriverFileDescriptor> getFiles()
    {
        return files;
    }

    public DriverFileDescriptor getLibrary(String path)
    {
        for (DriverFileDescriptor lib : files) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        return null;
    }

    public DriverFileDescriptor addLibrary(String path)
    {
        for (DriverFileDescriptor lib : files) {
            if (lib.getPath().equals(path)) {
                return lib;
            }
        }
        DriverFileDescriptor lib = new DriverFileDescriptor(this, DBPDriverFileType.jar, path);
        this.files.add(lib);
        return lib;
    }

    public boolean addLibrary(DriverFileDescriptor descriptor)
    {
        if (!files.contains(descriptor)) {
            this.files.add(descriptor);
            return true;
        }
        return false;
    }

    public boolean removeLibrary(DriverFileDescriptor lib)
    {
        if (!lib.isCustom()) {
            lib.setDisabled(true);
            return true;
        } else {
            return this.files.remove(lib);
        }
    }

    public Collection<String> getOrderedPathList()
    {
        if (pathList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(pathList.size());
        for (DriverPathDescriptor path : pathList) {
            if (path.isEnabled()) {
                result.add(path.getPath());
            }
        }
        return result;
    }

    @Override
    public Collection<DriverPathDescriptor> getPathList()
    {
        return pathList;
    }

    void addPath(DriverPathDescriptor path)
    {
        pathList.add(path);
    }
    
    @Override
    public List<IPropertyDescriptor> getConnectionPropertyDescriptors()
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
        Object valueObject = value;
        IPropertyDescriptor prop = getProviderDescriptor().getDriverProperty(name);
        if (prop instanceof IPropertyDescriptorEx) {
            valueObject = RuntimeUtils.convertString(value, ((IPropertyDescriptorEx)prop).getDataType());
        }
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
        for (DriverFileDescriptor file : files) {
            if (file.getType() == DBPDriverFileType.license) {
                final File licenseFile = file.getFile();
                if (licenseFile.exists()) {
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

        loadLibraries();

        try {
            if (!isCustomDriverLoader()) {
                try {
                    if (this.isInternalDriver()) {
                        // Use system class loader
                        driverClass = Class.forName(driverClassName);
                    } else {
                        // Load driver classes into core module using plugin class loader
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

    private void loadLibraries()
    {
        this.classLoader = null;

        validateFilesPresence();

        List<URL> libraryURLs = new ArrayList<URL>();
        // Load libraries
        for (DriverFileDescriptor file : files) {
            if (file.isDisabled() || file.getType() != DBPDriverFileType.jar) {
                continue;
            }
            URL url;
            try {
                url = file.getFile().toURI().toURL();
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
            ((BundleHost)providerDescriptor.getContributorBundle()).getClassLoader());
    }

    @Override
    public void validateFilesPresence()
    {
        for (DriverFileDescriptor file : files) {
            if (file.isCustom() && file.getFile().exists()) {
                // there are custom files - not need to
                return;
            }
        }

        final List<DriverFileDescriptor> downloadCandidates = new ArrayList<DriverFileDescriptor>();
        for (DriverFileDescriptor file : files) {
            if (file.isDisabled() || file.getExternalURL() == null || !file.isLocal()) {
                // Nothing we can do about it
                continue;
            }
            if (!file.matchesCurrentPlatform()) {
                // Wrong OS or architecture
                continue;
            }
            final File libraryFile = file.getLocalFile();
            if (!libraryFile.exists()) {
                downloadCandidates.add(file);
            }
        }

        if (!downloadCandidates.isEmpty()) {
            final StringBuilder libNames = new StringBuilder();
            for (DriverFileDescriptor lib : downloadCandidates) {
                if (libNames.length() > 0) libNames.append(", ");
                libNames.append(lib.getPath());
            }
            UIUtils.runInUI(null, new Runnable() {
                @Override
                public void run()
                {
                    if (ConfirmationDialog.showConfirmDialog(
                        null,
                        PrefConstants.CONFIRM_DRIVER_DOWNLOAD,
                        ConfirmationDialog.QUESTION,
                        getName(),
                        libNames) == IDialogConstants.YES_ID) {
                        // Download drivers
                        downloadLibraryFiles(downloadCandidates);
                    }
                }
            });
        }
    }

    private void downloadLibraryFiles(final List<DriverFileDescriptor> files)
    {
        if (!acceptDriverLicenses()) {
            return;
        }

        for (int i = 0, filesSize = files.size(); i < filesSize; ) {
            DriverFileDescriptor lib = files.get(i);
            int result = downloadLibraryFile(lib);
            switch (result) {
                case IDialogConstants.CANCEL_ID:
                case IDialogConstants.ABORT_ID:
                    return;
                case IDialogConstants.RETRY_ID:
                    continue;
                case IDialogConstants.OK_ID:
                case IDialogConstants.IGNORE_ID:
                    i++;
                    break;
            }
        }
    }

    private boolean acceptDriverLicenses()
    {
        // User must accept all licenses before actual drivers download
        for (final DriverFileDescriptor file : getFiles()) {
            if (file.getType() == DBPDriverFileType.license) {
                final File libraryFile = file.getLocalFile();
                if (!libraryFile.exists()) {
                    try {
                        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                            @Override
                            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                            {
                                try {
                                    downloadLibraryFile(monitor, file);
                                } catch (final Exception e) {
                                    log.warn("Can't obtain driver license", e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.warn(e);
                    }
                }
                if (libraryFile.exists()) {
                    try {
                        String licenseText = ContentUtils.readFileToString(libraryFile);
                        if (!AcceptLicenseDialog.acceptLicense(
                            DBeaverUI.getActiveWorkbenchShell(),
                            "You have to accept license of '" + this.getFullName() + " ' to continue",
                            licenseText)) {
                            return false;
                        }

                    } catch (IOException e) {
                        log.warn("Can't read license text", e);
                    }
                }
            }
        }
        return true;
    }

    private int downloadLibraryFile(final DriverFileDescriptor file)
    {
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        downloadLibraryFile(monitor, file);
                    } catch (IOException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            return IDialogConstants.OK_ID;
        } catch (InterruptedException e) {
            // User just canceled download
            return IDialogConstants.CANCEL_ID;
        } catch (InvocationTargetException e) {
            if (file.getType() == DBPDriverFileType.license) {
                return IDialogConstants.OK_ID;
            }
            DownloadErrorDialog dialog = new DownloadErrorDialog(
                null,
                file.getPath(),
                "Driver file download failed.\nDo you want to retry?",
                e.getTargetException());
            return dialog.open();
        }
    }

    private void downloadLibraryFile(DBRProgressMonitor monitor, DriverFileDescriptor file) throws IOException, InterruptedException
    {
        IPreferenceStore prefs = DBeaverCore.getGlobalPreferenceStore();
        String proxyHost = prefs.getString(PrefConstants.UI_PROXY_HOST);
        Proxy proxy = null;
        if (!CommonUtils.isEmpty(proxyHost)) {
            int proxyPort = prefs.getInt(PrefConstants.UI_PROXY_PORT);
            if (proxyPort <= 0) {
                log.warn("Invalid proxy port: " + proxyPort);
            }
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }
        String externalURL = file.getExternalURL();
        if (RegistryConstants.MAPPED_URL.equals(externalURL)) {
            String primarySource = DriverDescriptor.getDriversPrimarySource();
            if (!primarySource.endsWith("/") && !file.getPath().startsWith("/")) {
                primarySource += '/';
            }
            externalURL = primarySource + file.getPath();
        }

        URL url = new URL(externalURL);
        monitor.beginTask("Check file " + url.toString() + "...", 1);
        monitor.subTask("Connecting to the server");
        final HttpURLConnection connection = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET"); //$NON-NLS-1$
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty(
            "User-Agent",  //$NON-NLS-1$
            DBeaverCore.getProductTitle());
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new IOException("Can't find driver file '" + url + "': " + connection.getResponseMessage());
        }
        monitor.worked(1);
        monitor.done();

        final int contentLength = connection.getContentLength();
        //final String contentType = connection.getContentType();
        monitor.beginTask("Download " + file.getExternalURL(), contentLength);
        boolean success = false;
        final File localFile = file.getLocalFile();
        final File localDir = localFile.getParentFile();
        if (!localDir.exists()) {
            if (!localDir.mkdirs()) {
                log.warn("Can't create directory for local driver file '" + localDir.getAbsolutePath() + "'");
            }
        }
        final OutputStream outputStream = new FileOutputStream(localFile);
        try {
            final InputStream inputStream = connection.getInputStream();
            try {
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                byte[] buffer = new byte[10000];
                int totalRead = 0;
                for (;;) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    monitor.subTask(numberFormat.format(totalRead) + "/" + numberFormat.format(contentLength));
                    final int count = inputStream.read(buffer);
                    if (count <= 0) {
                        success = true;
                        break;
                    }
                    outputStream.write(buffer, 0, count);
                    monitor.worked(count);
                    totalRead += count;
                }
            }
            finally {
                ContentUtils.close(inputStream);
            }
        } finally {
            ContentUtils.close(outputStream);
            if (!success) {
                if (!localFile.delete()) {
                    log.warn("Can't delete local driver file '" + localFile.getAbsolutePath() + "'");
                }
            }
        }
        monitor.done();
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

    public List<DriverFileDescriptor> getOrigFiles()
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
        xml.addAttribute(RegistryConstants.ATTR_NAME, this.getName());
        xml.addAttribute(RegistryConstants.ATTR_CLASS, this.getDriverClassName());
        if (!CommonUtils.isEmpty(this.getSampleURL())) {
            xml.addAttribute(RegistryConstants.ATTR_URL, this.getSampleURL());
        }
        if (this.getDefaultPort() != null) {
            xml.addAttribute(RegistryConstants.ATTR_PORT, this.getDefaultPort());
        }
        xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, CommonUtils.getString(this.getDescription()));
        if (this.isCustomDriverLoader()) {
            xml.addAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER, this.isCustomDriverLoader());
        }

        // Libraries
        for (DriverFileDescriptor lib : this.getFiles()) {
            if ((export && !lib.isDisabled()) || lib.isCustom() || lib.isDisabled()) {
                xml.startElement(RegistryConstants.TAG_LIBRARY);
                xml.addAttribute(RegistryConstants.ATTR_PATH, lib.getPath());
                if (lib.getType() == DBPDriverFileType.jar && lib.isDisabled()) {
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

        // Path list
        for (DriverPathDescriptor path : this.getPathList()) {
            xml.startElement(RegistryConstants.TAG_PATH);
            xml.addAttribute(RegistryConstants.ATTR_PATH, path.getPath());
            if (!CommonUtils.isEmpty(path.getComment())) {
                xml.addAttribute(RegistryConstants.ATTR_COMMENT, path.getComment());
            }
            xml.addAttribute(RegistryConstants.ATTR_ENABLED, path.isEnabled());
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
        String driversHome = DBeaverCore.getGlobalPreferenceStore().getString(PrefConstants.UI_DRIVERS_HOME);
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
        String sourcesString = DBeaverCore.getGlobalPreferenceStore().getString(PrefConstants.UI_DRIVERS_SOURCES);
        List<String> pathList = CommonUtils.splitString(sourcesString, '|');
        return pathList.toArray(new String[pathList.size()]);
    }

    public static String getDriversPrimarySource()
    {
        String sourcesString = DBeaverCore.getGlobalPreferenceStore().getString(PrefConstants.UI_DRIVERS_SOURCES);
        int divPos = sourcesString.indexOf('|');
        return divPos == -1 ? sourcesString : sourcesString.substring(0, divPos);
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
                curProvider = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProvider(idAttr);
                if (curProvider == null) {
                    log.warn("Datasource provider '" + idAttr + "' not found");
                }
            } else if (localName.equals(RegistryConstants.TAG_DRIVER)) {
                curDriver = null;
                if (curProvider == null) {
                    String providerId = atts.getValue(RegistryConstants.ATTR_PROVIDER);
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
                String idAttr = atts.getValue(RegistryConstants.ATTR_ID);
                curDriver = curProvider.getDriver(idAttr);
                if (curDriver == null) {
                    curDriver = new DriverDescriptor(curProvider, idAttr);
                    curProvider.addDriver(curDriver);
                }
                curDriver.setCategory(atts.getValue(RegistryConstants.ATTR_CATEGORY));
                curDriver.setName(atts.getValue(RegistryConstants.ATTR_NAME));
                curDriver.setDescription(atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                curDriver.setDriverClassName(atts.getValue(RegistryConstants.ATTR_CLASS));
                curDriver.setSampleURL(atts.getValue(RegistryConstants.ATTR_URL));
                curDriver.setDriverDefaultPort(atts.getValue(RegistryConstants.ATTR_PORT));
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
                String path = atts.getValue(RegistryConstants.ATTR_PATH);
                DriverFileDescriptor lib = curDriver.getLibrary(path);
                String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                if (lib != null && CommonUtils.getBoolean(disabledAttr)) {
                    lib.setDisabled(true);
                } else if (lib == null) {
                    curDriver.addLibrary(path);
                }
            } else if (localName.equals(RegistryConstants.TAG_CLIENT_HOME)) {
                curDriver.addClientHomeId(atts.getValue(RegistryConstants.ATTR_ID));
            } else if (localName.equals(RegistryConstants.TAG_PATH)) {
                DriverPathDescriptor path = new DriverPathDescriptor();
                path.setPath(atts.getValue(RegistryConstants.ATTR_PATH));
                path.setComment(atts.getValue(RegistryConstants.ATTR_COMMENT));
                path.setEnabled(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ENABLED), true));
                curDriver.addPath(path);
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

    public static class DownloadErrorDialog extends ErrorDialog {

        public DownloadErrorDialog(
            Shell parentShell,
            String dialogTitle,
            String message,
            Throwable error)
        {
            super(parentShell, dialogTitle, message,
                RuntimeUtils.makeExceptionStatus(error),
                IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(
                parent,
                IDialogConstants.ABORT_ID,
                IDialogConstants.ABORT_LABEL,
                true);
            createButton(
                parent,
                IDialogConstants.RETRY_ID,
                IDialogConstants.RETRY_LABEL,
                false);
            createButton(
                parent,
                IDialogConstants.IGNORE_ID,
                IDialogConstants.IGNORE_LABEL,
                false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int buttonId) {
            setReturnCode(buttonId);
            close();
        }
    }

}

