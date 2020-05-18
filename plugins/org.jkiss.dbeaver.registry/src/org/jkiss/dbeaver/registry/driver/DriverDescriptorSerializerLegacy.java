/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.LocalNativeClientLocation;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DriverDescriptorSerializerLegacy
 */
@Deprecated
public class DriverDescriptorSerializerLegacy extends DriverDescriptorSerializer {

    private static final Log log = Log.getLog(DriverDescriptorSerializerLegacy.class);

    private DriverDescriptor driver;

    DriverDescriptorSerializerLegacy(DriverDescriptor driver) {
        this.driver = driver;
    }

    public void serialize(XMLBuilder xml, boolean export)
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
            xml.addAttribute(RegistryConstants.ATTR_CATEGORIES, String.join(",", driver.getCategories()));

            xml.addAttribute(RegistryConstants.ATTR_NAME, driver.getName());
            xml.addAttribute(RegistryConstants.ATTR_CLASS, driver.getDriverClassName());
            if (!CommonUtils.isEmpty(driver.getSampleURL())) {
                xml.addAttribute(RegistryConstants.ATTR_URL, driver.getSampleURL());
            }
            if (driver.getDefaultPort() != null) {
                xml.addAttribute(RegistryConstants.ATTR_PORT, driver.getDefaultPort());
            }
            xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, CommonUtils.notEmpty(driver.getDescription()));
            if (driver.isCustomDriverLoader()) {
                xml.addAttribute(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER, driver.isCustomDriverLoader());
            }
            xml.addAttribute(RegistryConstants.ATTR_CUSTOM, driver.isCustom());
            if (driver.isEmbedded()) {
                xml.addAttribute(RegistryConstants.ATTR_EMBEDDED, driver.isEmbedded());
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
                    //xml.addAttribute(RegistryConstants.ATTR_CUSTOM, lib.isCustom());
                    List<DriverDescriptor.DriverFileInfo> files = driver.getResolvedFiles().get(lib);
                    if (files != null) {
                        for (DriverDescriptor.DriverFileInfo file : files) {
                            try (XMLBuilder.Element e2 = xml.startElement(RegistryConstants.TAG_FILE)) {
                                if (file.getFile() == null) {
                                    log.warn("File missing in " + file.getId());
                                    continue;
                                }
                                xml.addAttribute(RegistryConstants.ATTR_ID, file.getId());
                                if (!CommonUtils.isEmpty(file.getVersion())) {
                                    xml.addAttribute(RegistryConstants.ATTR_VERSION, file.getVersion());
                                }
                                xml.addAttribute(RegistryConstants.ATTR_PATH, substitutePathVariables(pathSubstitutions, file.getFile().getAbsolutePath()));
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
                if (!CommonUtils.equalObjects(paramEntry.getValue(), driver.getDefaultParameters().get(paramEntry.getKey()))) {
                    try (XMLBuilder.Element e1 = xml.startElement(RegistryConstants.TAG_PARAMETER)) {
                        xml.addAttribute(RegistryConstants.ATTR_NAME, paramEntry.getKey());
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, CommonUtils.toString(paramEntry.getValue()));
                    }
                }
            }

            // Properties
            for (Map.Entry<String, Object> propEntry : driver.getCustomConnectionProperties().entrySet()) {
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
                        curDriver.setEmbedded(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_EMBEDDED), curDriver.isEmbedded()));
                        curDriver.setAnonymousAccess(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_ANONYMOUS), curDriver.isAnonymousAccess()));
                        curDriver.setAllowsEmptyPassword(CommonUtils.getBoolean(atts.getValue("allowsEmptyPassword"), curDriver.isAllowsEmptyPassword()));
                        curDriver.setInstantiable(CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_INSTANTIABLE), curDriver.isInstantiable()));
                    }
                    if (atts.getValue(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER) != null) {
                        curDriver.setCustomDriverLoader((
                            CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_CUSTOM_DRIVER_LOADER), false)));
                    }
                    if (atts.getValue(RegistryConstants.ATTR_USE_URL_TEMPLATE) != null) {
                        curDriver.setUseURL((
                            CommonUtils.getBoolean(atts.getValue(RegistryConstants.ATTR_USE_URL_TEMPLATE), true)));
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
                        log.warn("Library outside of driver");
                        return;
                    }
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
                    if (!providedDrivers && !custom && lib == null) {
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
                        curDriver.addDriverLibrary(lib, false);
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
                                DriverDescriptor.DriverFileInfo info = new DriverDescriptor.DriverFileInfo(
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
