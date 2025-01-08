/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.LocalNativeClientLocation;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.VersionUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DriverDescriptorSerializerLegacy
 */
public class DriverDescriptorSerializerLegacy extends DriverDescriptorSerializer {

    public static final String DRIVERS_FILE_NAME = "drivers.xml"; //$NON-NLS-1$

    private static final boolean isDistributed = DBWorkbench.isDistributed();
    // In detached process we usually have just one driver
    private static final boolean isDetachedProcess = DBWorkbench.getPlatform().getApplication().isDetachedProcess();

    private static final Log log = Log.getLog(DriverDescriptorSerializerLegacy.class);

    public void serializeDrivers(OutputStream os, List<DataSourceProviderDescriptor> providers) throws IOException {
        XMLBuilder xml = new XMLBuilder(os, GeneralUtils.UTF8_ENCODING);
        xml.setButify(true);
        xml.startElement(RegistryConstants.TAG_DRIVERS);
        for (DataSourceProviderDescriptor provider : providers) {
            if (provider.isTemporary()) {
                continue;
            }
            List<DriverDescriptor> drivers = provider.getDrivers().stream().filter(DriverDescriptor::isModified).collect(Collectors.toList());
            drivers.removeIf(driverDescriptor -> driverDescriptor.getReplacedBy() != null);
            if (drivers.isEmpty()) {
                continue;
            }
            xml.startElement(RegistryConstants.TAG_PROVIDER);
            xml.addAttribute(RegistryConstants.ATTR_ID, provider.getId());
            for (DriverDescriptor driver : drivers) {
                serializeDriver(xml, driver, false);
            }
            xml.endElement();
        }
        xml.endElement();
        xml.flush();
    }

    private void serializeDriver(XMLBuilder xml, DriverDescriptor driver, boolean export)
            throws IOException {
        Map<String, String> pathSubstitutions = getPathSubstitutions();

        try (XMLBuilder.Element e0 = xml.startElement(RegistryConstants.TAG_DRIVER)) {
            if (export) {
                xml.addAttribute(RegistryConstants.ATTR_PROVIDER, driver.getProviderDescriptor().getId());
            }
            xml.addAttribute(RegistryConstants.ATTR_ID, driver.getId());
            if (driver.isDisabled()) {
                xml.addAttribute(RegistryConstants.ATTR_DISABLED, true);
            }
            if (!CommonUtils.isEmpty(driver.getCategory())) {
                xml.addAttribute(RegistryConstants.ATTR_CATEGORY, driver.getCategory());
            }
            if (!CommonUtils.isEmpty(driver.getCategories())) {
                xml.addAttribute(RegistryConstants.ATTR_CATEGORIES, String.join(",", driver.getCategories()));
            }

            xml.addAttribute(RegistryConstants.ATTR_NAME, driver.getName());
            if (!CommonUtils.isEmpty(driver.getDriverClassName())) {
                xml.addAttribute(RegistryConstants.ATTR_CLASS, driver.getDriverClassName());
            }
            if (!CommonUtils.isEmpty(driver.getSampleURL())) {
                xml.addAttribute(RegistryConstants.ATTR_URL, driver.getSampleURL());
            }
            if (!CommonUtils.isEmpty(driver.getDefaultPort())) {
                xml.addAttribute(RegistryConstants.ATTR_PORT, driver.getDefaultPort());
            }
            if (!CommonUtils.isEmpty(driver.getDefaultDatabase())) {
                xml.addAttribute(RegistryConstants.ATTR_DEFAULT_DATABASE, driver.getDefaultDatabase());
            }
            if (!CommonUtils.isEmpty(driver.getDefaultServer())) {
                xml.addAttribute(RegistryConstants.ATTR_DEFAULT_SERVER, driver.getDefaultServer());
            }
            if (!CommonUtils.isEmpty(driver.getDefaultUser())) {
                xml.addAttribute(RegistryConstants.ATTR_DEFAULT_USER, driver.getDefaultUser());
            }
            if (!CommonUtils.isEmpty(driver.getDescription())) {
                xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, driver.getDescription());
            }
            if (driver.isCustomDriverLoader()) {
                xml.addAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER, driver.isCustomDriverLoader());
            }
            xml.addAttribute(RegistryConstants.ATTR_CUSTOM, driver.isCustom());
            if (driver.isEmbedded()) {
                xml.addAttribute(RegistryConstants.ATTR_EMBEDDED, driver.isEmbedded());
            }
            if (driver.isPropagateDriverProperties()) {
                xml.addAttribute(RegistryConstants.ATTR_PROPAGATE_DRIVER_PROPERTIES, driver.isPropagateDriverProperties());
            }
            if (driver.isAnonymousAccess()) {
                xml.addAttribute(RegistryConstants.ATTR_ANONYMOUS, driver.isAnonymousAccess());
            }
            if (driver.isAllowsEmptyPassword()) {
                xml.addAttribute("allowsEmptyPassword", driver.isAllowsEmptyPassword());
            }
            if (!driver.isInstantiable()) {
                xml.addAttribute(RegistryConstants.ATTR_INSTANTIABLE, driver.isInstantiable());
            }
            if (!driver.isSupportsDistributedMode()) {
                xml.addAttribute(RegistryConstants.ATTR_SUPPORTS_DISTRIBUTED_MODE, driver.isSupportsDistributedMode());
            }
            if (driver.isThreadSafeDriver() != driver.isOrigThreadSafeDriver()) {
                xml.addAttribute("threadSafe", driver.isThreadSafeDriver());
            }

            // Libraries
            for (DBPDriverLibrary lib : driver.getDriverLibraries()) {
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
                    if (lib instanceof DriverLibraryMavenArtifact) {
                        if (((DriverLibraryMavenArtifact) lib).isIgnoreDependencies()) {
                            xml.addAttribute("ignore-dependencies", true);
                        }
                        if (((DriverLibraryMavenArtifact) lib).isLoadOptionalDependencies()) {
                            xml.addAttribute("load-optional-dependencies", true);
                        }
                    }

                    List<DriverDescriptor.DriverFileInfo> files = driver.getResolvedFiles().get(lib);
                    if (files != null) {
                        for (DriverDescriptor.DriverFileInfo file : files) {
                            try (XMLBuilder.Element e2 = xml.startElement(RegistryConstants.TAG_FILE)) {
                                if (file.getFile() == null) {
                                    log.warn("File missing in " + file.getId());
                                    continue;
                                }
                                xml.addAttribute(RegistryConstants.ATTR_ID, file.getId());
                                // check if we need to store local file in storage

                                if (!CommonUtils.isEmpty(file.getVersion())) {
                                    xml.addAttribute(RegistryConstants.ATTR_VERSION, file.getVersion());
                                }
                                String normalizedFilePath = file.getFile().toString();
                                if (isDistributed) {
                                    normalizedFilePath = normalizedFilePath.replace('\\', '/');
                                }
                                xml.addAttribute(
                                    RegistryConstants.ATTR_PATH,
                                    substitutePathVariables(pathSubstitutions, normalizedFilePath));
                                if (file.getFileCRC() != 0) {
                                    xml.addAttribute("crc", Long.toHexString(file.getFileCRC()));
                                }
                            }
                        }
                    }
                }
            }

            // Client homes
            for (DBPNativeClientLocation location : driver.getNativeClientHomes()) {
                try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_CLIENT_HOME)) {
                    xml.addAttribute(RegistryConstants.ATTR_ID, location.getName());
                    if (location.getPath() != null) {
                        xml.addAttribute(RegistryConstants.ATTR_PATH, location.getPath().getAbsolutePath());
                    }
                }
            }

            // Parameters
            for (Map.Entry<String, Object> paramEntry : driver.getCustomParameters().entrySet()) {
                if (driver.isCustom() || !CommonUtils.equalObjects(paramEntry.getValue(), driver.getDefaultParameters().get(paramEntry.getKey()))) {
                    // Save custom parameters for custom drivers. It can help with PG drivers, as example (we must store serverType for PG-clones).
                    try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PARAMETER)) {
                        xml.addAttribute(RegistryConstants.ATTR_NAME, paramEntry.getKey());
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(paramEntry.getValue()));
                    }
                }
            }

            // Extra icon parameter for the custom driver
            if (driver.isCustom()) {
                try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PARAMETER)) {
                    xml.addAttribute(RegistryConstants.ATTR_ICON, driver.getIcon().getLocation());
                }
            }

            // Properties
            for (Map.Entry<String, Object> propEntry : driver.getConnectionProperties().entrySet()) {
                if (!CommonUtils.equalObjects(propEntry.getValue(), driver.getDefaultConnectionProperties().get(propEntry.getKey()))) {
                    try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PROPERTY)) {
                        xml.addAttribute(RegistryConstants.ATTR_NAME, propEntry.getKey());
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(propEntry.getValue()));
                    }
                }
            }
        }
    }


    public static class DriversParser implements SAXListener {
        private final boolean providedDrivers;
        DataSourceProviderDescriptor curProvider;
        DriverDescriptor curDriver;
        DBPDriverLibrary curLibrary;
        private boolean isLibraryUpgraded = false;

        public DriversParser(boolean provided) {
            this.providedDrivers = provided;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) {
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
                        if (!isDetachedProcess) {
                            log.warn("Datasource provider '" + idAttr + "' not found. Bad provider description.");
                        }
                    }
                    break;
                }
                case RegistryConstants.TAG_DRIVER: {
                    curDriver = null;
                    String idAttr = atts.getValue(RegistryConstants.ATTR_ID);
                    if (curProvider == null) {
                        String providerId = atts.getValue(RegistryConstants.ATTR_PROVIDER);
                        if (!CommonUtils.isEmpty(providerId)) {
                            curProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(providerId);
                            if (curProvider == null && !isDetachedProcess) {
                                log.warn("Datasource provider '" + providerId + "' not found. Bad driver description.");
                            }
                        }
                        if (curProvider == null) {
                            if (!isDetachedProcess) {
                                log.warn("Driver '" + idAttr + "' outside of datasource provider");
                            }
                            return;
                        }
                    }
                    curDriver = curProvider.getDriver(idAttr);
                    if (curDriver == null) {
                        curDriver = new DriverDescriptor(curProvider, idAttr);
                        curProvider.addDriver(curDriver);
                    } else if (DBWorkbench.isDistributed()) {
                        curDriver.resetDriverInstance();
                    }

                    if (providedDrivers || curProvider.isDriversManagable()) {
                        String category = atts.getValue(RegistryConstants.ATTR_CATEGORY);
                        if (!CommonUtils.isEmpty(category)) {
                            curDriver.setCategory(category);
                        }
                        if (providedDrivers || curDriver.isCustom()) {
                            curDriver.setName(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_NAME), curDriver.getName()));
                        }
                        curDriver.setDescription(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_DESCRIPTION), curDriver.getDescription()));
                        curDriver.setDriverClassName(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_CLASS), curDriver.getDriverClassName()));
                        curDriver.setSampleURL(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_URL), curDriver.getSampleURL()));
                        curDriver.setDriverDefaultPort(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_PORT), curDriver.getDefaultPort()));
                        curDriver.setDriverDefaultDatabase(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_DEFAULT_DATABASE), curDriver.getDefaultDatabase()));
                        curDriver.setDriverDefaultServer(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_DEFAULT_SERVER), curDriver.getDefaultServer()));
                        curDriver.setDriverDefaultUser(CommonUtils.toString(atts.getValue(RegistryConstants.ATTR_DEFAULT_USER), curDriver.getDefaultUser()));
                        curDriver.setEmbedded(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_EMBEDDED), curDriver.isEmbedded()));
                        curDriver.setPropagateDriverProperties(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_PROPAGATE_DRIVER_PROPERTIES), curDriver.isPropagateDriverProperties()));
                        curDriver.setAnonymousAccess(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ANONYMOUS), curDriver.isAnonymousAccess()));
                        curDriver.setAllowsEmptyPassword(CommonUtils.getBoolean(atts.getValue("allowsEmptyPassword"), curDriver.isAllowsEmptyPassword()));
                        curDriver.setInstantiable(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_INSTANTIABLE), curDriver.isInstantiable()));
                        curDriver.setThreadSafeDriver(CommonUtils.getBoolean(atts.getValue("threadSafe"), curDriver.isThreadSafeDriver()));
                    }
                    if (atts.getValue(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER) != null) {
                        curDriver.setCustomDriverLoader((
                            CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false)));
                    }
                    if (atts.getValue(RegistryConstants.ATTR_USE_URL_TEMPLATE) != null) {
                        curDriver.setUseURL((
                            CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_USE_URL_TEMPLATE), true)));
                    }
                    if (atts.getValue(RegistryConstants.ATTR_SUPPORTS_DISTRIBUTED_MODE) != null) {
                        curDriver.setSupportsDistributedMode((
                            CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_SUPPORTS_DISTRIBUTED_MODE), true)));
                    }
                    curDriver.setModified(true);
                    String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                    if (CommonUtils.getBoolean(disabledAttr)) {
                        curDriver.setDisabled(true);
                    }
                    break;
                }
                case RegistryConstants.TAG_LIBRARY: {
                    if (curDriver == null) {
                        if (!isDetachedProcess) {
                            log.warn("Library outside of driver (" + atts.getValue(RegistryConstants.ATTR_PATH) + ")");
                        }
                        return;
                    }
                    isLibraryUpgraded = false;

                    DBPDriverLibrary.FileType type;
                    String typeStr = atts.getValue(RegistryConstants.ATTR_TYPE);
                    if (CommonUtils.isEmpty(typeStr)) {
                        type = DBPDriverLibrary.FileType.jar;
                    } else {
                        type = CommonUtils.valueOf(DBPDriverLibrary.FileType.class, typeStr, DBPDriverLibrary.FileType.jar);
                    }
                    String path = normalizeLibraryPath(atts.getValue(RegistryConstants.ATTR_PATH));
                    if (!CommonUtils.isEmpty(path)) {
                        path = replacePathVariables(path);
                    }
                    boolean custom = CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_CUSTOM), true);
                    String version = atts.getValue(RegistryConstants.ATTR_VERSION);
                    DBPDriverLibrary lib = curDriver.getDriverLibrary(path);
                    if (!isDistributed && !providedDrivers && !custom && lib == null) {
                        // Perhaps this library isn't included in driver bundle
                        // Or this is predefined library from some previous version - as it wasn't defined in plugin.xml
                        // so let's just skip it
                        //log.debug("Skip obsolete custom library '" + path + "'");
                        return;
                    }
                    if (providedDrivers && lib == null && !(curDriver.getDriverLibraries().isEmpty())){
                        curDriver.disabledAllDefaultLibraries();
                    }
                    String disabledAttr = atts.getValue(RegistryConstants.ATTR_DISABLED);
                    if (lib != null && CommonUtils.getBoolean(disabledAttr)) {
                        lib.setDisabled(true);
                    } else if (lib == null) {
                        lib = DriverLibraryAbstract.createFromPath(curDriver, type, path, version);
                        curDriver.addDriverLibrary(lib, false);
                    } else if (!CommonUtils.isEmpty(version)) {
                        // Overwrite version only if it is higher than the original one
                        String preferredVersion = CommonUtils.toString(lib.getPreferredVersion(), "0");
                        int versionMatch = VersionUtils.compareVersions(version, preferredVersion);
                        if (versionMatch > 0) {
                            // Version in config higher than in bundles. Probably a manual update - just overwrite it.
                            lib.setPreferredVersion(version);
                        } else if (versionMatch < 0 && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.UI_DRIVERS_VERSION_UPDATE)) {
                            // Version in config is lower than in bundle. Probably it came from product version update - just reset it.
                            lib.resetVersion();
                            isLibraryUpgraded = true;
                        }
                    }
                    if (lib instanceof DriverLibraryMavenArtifact) {
                        ((DriverLibraryMavenArtifact) lib).setIgnoreDependencies(CommonUtils.toBoolean(atts.getValue("ignore-dependencies")));
                        ((DriverLibraryMavenArtifact) lib).setLoadOptionalDependencies(CommonUtils.toBoolean(atts.getValue("load-optional-dependencies")));
                    }
                    curLibrary = lib;
                    break;
                }
                case RegistryConstants.TAG_FILE: {
                    if (curDriver != null && curLibrary != null && !isLibraryUpgraded) {
                        String path = atts.getValue(RegistryConstants.ATTR_PATH);
                        if (path != null) {
                            path = replacePathVariables(path);
                            if (CommonUtils.isEmpty(path)) {
                                log.warn("Empty path for library file");
                            } else {
                                DriverDescriptor.DriverFileInfo info = new DriverDescriptor.DriverFileInfo(
                                        atts.getValue(CommonUtils.notEmpty(RegistryConstants.ATTR_ID)),
                                        atts.getValue(CommonUtils.notEmpty(RegistryConstants.ATTR_VERSION)),
                                        curLibrary.getType(),
                                        Path.of(path));
                                String crcString = atts.getValue("crc");
                                if (!CommonUtils.isEmpty(crcString)) {
                                    long crc = Long.parseLong(crcString, 16);
                                    if (crc != 0) {
                                        info.setFileCRC(crc);
                                    }
                                }
                                curDriver.addLibraryFile(curLibrary, info);
                            }
                        }
                    }
                    break;
                }
                case RegistryConstants.TAG_CLIENT_HOME:
                    if (curDriver != null) {
                        curDriver.addNativeClientLocation(
                            new LocalNativeClientLocation(
                                atts.getValue(RegistryConstants.ATTR_ID),
                                atts.getValue(RegistryConstants.ATTR_PATH)));
                    }
                    break;
                case RegistryConstants.TAG_PARAMETER: {
                    if (curDriver != null) {
                        final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                        final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        if (!CommonUtils.isEmpty(paramName) && !CommonUtils.isEmpty(paramValue)) {
                            curDriver.setDriverParameter(paramName, paramValue, false);
                        }
                        // Read extra icon parameter for custom drivers
                        if (curDriver.isCustom()) {
                            final String iconParam = atts.getValue(RegistryConstants.ATTR_ICON);
                            if (!CommonUtils.isEmpty(iconParam)) {
                                DBPImage icon = curDriver.iconToImage(iconParam);
                                curDriver.setIconPlain(icon);
                                curDriver.makeIconExtensions();
                            }
                        }
                    }
                    break;
                }
                case RegistryConstants.TAG_PROPERTY: {
                    if (curDriver != null) {
                        final String paramName = atts.getValue(RegistryConstants.ATTR_NAME);
                        final String paramValue = atts.getValue(RegistryConstants.ATTR_VALUE);
                        if (!CommonUtils.isEmpty(paramName)) {
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
        public void saxText(SAXReader reader, String data) {
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
            switch (localName) {
                case RegistryConstants.TAG_LIBRARY:
                    curLibrary = null;
                    break;
            }

        }
    }
}
