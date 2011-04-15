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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.xml.sax.Attributes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;

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
    private String webURL;
    private Image iconPlain;
    private Image iconNormal;
    private Image iconError;
    private boolean supportsDriverProperties;
    private boolean anonymousAccess;
    private boolean custom;
    private boolean modified;
    private boolean disabled;
    private List<DriverFileDescriptor> files = new ArrayList<DriverFileDescriptor>(), origFiles;
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
        if (!CommonUtils.isEmpty(config.getAttribute("defaultPort"))) {
            try {
                this.origDefaultPort = this.driverDefaultPort = Integer.valueOf(config.getAttribute("defaultPort"));
            }
            catch (NumberFormatException ex) {
                log.warn("Bad default port for driver '" + name + "' specified: " + ex.getMessage());
            }
        }
        this.origSampleURL = this.sampleURL = config.getAttribute("sampleURL");
        this.webURL = config.getAttribute("webURL");
        this.supportsDriverProperties = !"false".equals(config.getAttribute("supportsDriverProperties"));
        this.anonymousAccess = "true".equals(config.getAttribute("anonymous"));
        this.custom = false;
        this.isLoaded = false;

        for (IConfigurationElement lib : config.getChildren(DataSourceConstants.TAG_FILE)) {
            this.files.add(new DriverFileDescriptor(this, lib));
        }
        this.origFiles = new ArrayList<DriverFileDescriptor>(this.files);

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
            IConfigurationElement[] propElements = config.getChildren(PropertyGroupDescriptor.TAG_PROPERTY_GROUP);
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

    @Property(name = "Driver Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Property(name = "Description", viewable = true, order = 100)
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

    @Property(name = "Driver Class", viewable = true, order = 2)
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

    @Property(name = "URL", viewable = true, order = 3)
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

    public boolean isAnonymousAccess()
    {
        return anonymousAccess;
    }

    public void setAnonymousAccess(boolean anonymousAccess)
    {
        this.anonymousAccess = anonymousAccess;
    }

    public boolean isManagable()
    {
        return getProviderDescriptor().isDriversManagable();
    }

    public boolean isInternalDriver()
    {
        return driverClassName != null && driverClassName.indexOf("sun.jdbc") != -1;
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

    public List<DriverFileDescriptor> getFiles()
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
        DriverFileDescriptor lib = new DriverFileDescriptor(this, path);
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

    public String getLicense()
    {
        for (DriverFileDescriptor file : files) {
            if (file.getType() == DriverFileType.license) {
                final File licenseFile = file.getFile();
                if (licenseFile.exists()) {
                    return extractLicenseText(licenseFile);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private String extractLicenseText(File licenseFile)
    {
        final int length = (int) licenseFile.length();
        try {
            StringBuilder licenseText = new StringBuilder(length);
            Reader fileReader = new FileReader(licenseFile);
            try {
                char[] buffer = new char[10000];
                for (;;) {
                    final int count = fileReader.read(buffer);
                    if (count <= 0) {
                        break;
                    }
                    licenseText.append(buffer, 0, count);
                }
            }
            finally {
                ContentUtils.close(fileReader);
            }
            return licenseText.toString();
        } catch (IOException e) {
            log.warn(e);
            return e.getMessage();
        }
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
            catch (Throwable ex) {
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

        validateFilesPresence();

        List<URL> libraryURLs = new ArrayList<URL>();
        // Load libraries
        for (DriverFileDescriptor file : files) {
            if (file.isDisabled() || file.getType() != DriverFileType.library) {
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
            libraryURLs.toArray(new URL[libraryURLs.size()]),
            ClassLoader.getSystemClassLoader());
    }

    public void validateFilesPresence()
    {
        final List<DriverFileDescriptor> downloadCandidates = new ArrayList<DriverFileDescriptor>();
        for (DriverFileDescriptor file : files) {
            if (file.isDisabled() || file.getExternalURL() == null || !file.isLocal()) {
                // Nothing we can do about it
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
            Display.getDefault().syncExec(new Runnable() {
                public void run()
                {
                    if (ConfirmationDialog.showConfirmDialog(
                        null,
                        PrefConstants.CONFIRM_DRIVER_DOWNLOAD,
                        ConfirmationDialog.QUESTION,
                        ConfirmationDialog.WARNING,
                        getName(),
                        libNames) == IDialogConstants.YES_ID)
                    {
                        // Download drivers
                        downloadLibraryFiles(downloadCandidates);
                    }
                }
            });
        }
    }

    private void downloadLibraryFiles(final List<DriverFileDescriptor> files)
    {
//        try {
            DBeaverCore.getInstance().runInProgressDialog(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    for (DriverFileDescriptor lib : files) {
                        try {
                            final boolean success = downloadLibraryFiles(monitor, lib);
                            if (!success) {
                                break;
                            }
                        } catch (final Exception e) {
                            Display.getDefault().syncExec(new Runnable() {
                                public void run()
                                {
                                    UIUtils.showErrorDialog(null, "Download driver", "Can't download '" + getName() + "' libraries", e);
                                }
                            });
                            break;
                            //throw new InvocationTargetException(e);
                        }
                    }
                }
            });
//        } catch (InvocationTargetException e) {
//            UIUtils.showErrorDialog(null, "Download driver", "Can't download '" + getName() + "' libraries", e.getTargetException());
//        } catch (InterruptedException e) {
//            // do nothing
//        }
    }

    private boolean downloadLibraryFiles(DBRProgressMonitor monitor, DriverFileDescriptor file) throws IOException
    {
        URL url = new URL(file.getExternalURL());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new IOException("Can't find driver file: " + connection.getResponseMessage());
        }
        final int contentLength = connection.getContentLength();
        final String contentType = connection.getContentType();
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
                        break;
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
        return success;
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

    public List<DriverFileDescriptor> getOrigFiles()
    {
        return origFiles;
    }

    public static File getDriversContribFolder() throws IOException
    {
        return new File(Platform.getInstallLocation().getDataArea("drivers").toExternalForm());
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
        for (DriverFileDescriptor lib : this.getFiles()) {
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
            } else if (localName.equals(DataSourceConstants.TAG_FILE) || localName.equals(DataSourceConstants.TAG_LIBRARY)) {
                if (curDriver == null) {
                    log.warn("File outside of driver");
                    return;
                }
                String path = atts.getValue(DataSourceConstants.ATTR_PATH);
                DriverFileDescriptor lib = curDriver.getLibrary(path);
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
            int divPos = sampleURL.indexOf('{', offsetPos);
            if (divPos == -1) {
                break;
            }
            int divPos2 = sampleURL.indexOf('}', divPos);
            if (divPos2 == -1) {
                throw new DBException("Bad sample URL: " + sampleURL);
            }
            String propName = sampleURL.substring(divPos + 1, divPos2);
            boolean isOptional = false;
            int optDiv1 = sampleURL.lastIndexOf('[', divPos);
            int optDiv1c = sampleURL.lastIndexOf(']', divPos);
            int optDiv2 = sampleURL.indexOf(']', divPos2);
            int optDiv2c = sampleURL.indexOf('[', divPos2);
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

}

