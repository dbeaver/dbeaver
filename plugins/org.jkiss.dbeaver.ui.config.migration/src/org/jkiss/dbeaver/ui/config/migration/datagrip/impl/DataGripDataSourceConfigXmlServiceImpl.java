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
package org.jkiss.dbeaver.ui.config.migration.datagrip.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.config.migration.datagrip.DataGripConfigXMLConstant;
import org.jkiss.dbeaver.ui.config.migration.datagrip.api.DataGripDataSourceConfigXmlService;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConfigurationException;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportDriverInfo;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DataGripDataSourceConfigXmlServiceImpl implements DataGripDataSourceConfigXmlService {

    public static final DataGripDataSourceConfigXmlServiceImpl INSTANCE = new DataGripDataSourceConfigXmlServiceImpl();

    private static final Log log = Log.getLog(DataGripDataSourceConfigXmlServiceImpl.class);

    private DataGripDataSourceConfigXmlServiceImpl() {
    }

    @Override
    public @NotNull Map<String, Map<String, String>> buildIdeaConfigProps(@NotNull String pathToIdeaFolder) throws Exception {
        Map<String, Map<String, String>> uuidToDataSourceProps = new HashMap<>();
        Path pathToDataSource = Path.of(pathToIdeaFolder, DataGripConfigXMLConstant.IDEA_FOLDER, DataGripConfigXMLConstant.DATASOURCE_XML_FILENAME);
        Path pathToDataSourceLocal = Path.of(pathToIdeaFolder, DataGripConfigXMLConstant.IDEA_FOLDER, DataGripConfigXMLConstant.DATASOURCE_LOCAL_XML_FILENAME);

        uuidToDataSourceProps.putAll(importXML(pathToDataSource));
        Map<String, Map<String, String>> uuidToDataSourceFromDifferentXml = importXML(pathToDataSourceLocal);
        uuidToDataSourceProps = mergeTwoMapProps(uuidToDataSourceProps, uuidToDataSourceFromDifferentXml);

        Map<String, Map<String, String>> sshIdToSshConfigMap = tryReadIdeaSshConfig(getJetBrainsDirectory());
        uuidToDataSourceProps = mergeSshConfigToIdeaConfigMap(uuidToDataSourceProps, sshIdToSshConfigMap);
        return uuidToDataSourceProps;
    }

    @NotNull
    @Override
    public List<Path> tryExtractRecentProjectPath() {
        try {
            Path pathSuffixToFile = Path.of(DataGripConfigXMLConstant.IDEA_OPTIONS_FOLDER, DataGripConfigXMLConstant.RECENT_PROJECT_XML_FILENAME);
            List<Path> pathToRecentProjectXmlList = getAllExistingPathsToFileFromFolder(getJetBrainsDirectory(), pathSuffixToFile);
            List<Path> result = new ArrayList<>();
            for (Path projectFile : pathToRecentProjectXmlList) {
                String projectConfigPath = getRecentProjectPathFromXml(projectFile);
                if (!CommonUtils.isEmpty(projectConfigPath)) {
                    Path projectPath = Path.of(replaceUserHomePath(projectConfigPath));
                    if (Files.exists(projectPath)) {
                        result.add(projectPath);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Can't extract recent project path for DataGrip", e);
        }
        return List.of();
    }

    private String replaceUserHomePath(String path) {
        String[] split = path.split("\\$/");
        if (split.length < 2) {
            return path;
        }
        String pathFromUserHome = split[1];
        String osDependencePath = CommonUtils.makeOsDependencePath(pathFromUserHome);
        return System.getProperty("user.home") + File.separator + osDependencePath;
    }

    @Override
    public @NotNull ImportConnectionInfo buildIdeaConnectionFromProps(@NotNull Map<String, String> conProps) {

        ImportDriverInfo driverInfo = buildDriverInfo(conProps);
        String url = conProps.get(DataGripConfigXMLConstant.JDBC_URL_TAG);
        ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
            driverInfo,
            conProps.get(DataGripConfigXMLConstant.DATASOURCE_UUID_PATH),
            conProps.get(DataGripConfigXMLConstant.DATASOURCE_NAME_PATH),
            url,
            //host, port and database will be determinate by url
            null,
            null,
            "",
            conProps.get(DataGripConfigXMLConstant.USERNAME_PATH),
            ""
        );
        configureDriverProperties(connectionInfo, conProps);
        configureSshConfig(connectionInfo, conProps);
        log.debug("load connection: " + connectionInfo);
        return connectionInfo;
    }

    private void configureDriverProperties(ImportConnectionInfo connectionInfo, Map<String, String> conProps) {

        for (Map.Entry<String, String> conPropEntry : conProps.entrySet()) {
            if (conPropEntry.getKey().startsWith(DataGripConfigXMLConstant.PROPERTIES_TAG + "$")) {
                connectionInfo.setProperty(conPropEntry.getKey().substring(DataGripConfigXMLConstant.PROPERTIES_TAG.length() + 1), conPropEntry.getValue());
            }
        }
    }

    private List<Path> getAllExistingPathsToFileFromFolder(String pathToIdeaFolder, Path pathSuffixToFile) throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(pathToIdeaFolder))) {
            List<Path> ideaFolders = paths
                .filter(Files::isDirectory)
                .toList();

            List<Path> result = new ArrayList<>();
            for (Path ideaFolder : ideaFolders) {
                Path pathToFile = ideaFolder.resolve(pathSuffixToFile);
                if (Files.exists(pathToFile)) {
                    result.add(pathToFile);
                }
            }
            return result;
        }
    }

    private String getJetBrainsDirectory() {
        Path defaultWorkingDirectory = Path.of(DataGripConfigXMLConstant.JET_BRAINS_HOME_FOLDER);
        if (RuntimeUtils.isMacOS()) {
            defaultWorkingDirectory = Path.of("Application Support", DataGripConfigXMLConstant.JET_BRAINS_HOME_FOLDER);
        } else if (RuntimeUtils.isLinux()){
            Path path = Path.of(System.getProperty("user.home"),".config", DataGripConfigXMLConstant.JET_BRAINS_HOME_FOLDER);
            log.info("Using linux path for import config home directory: " + path);
            return path.toString();
        }
        return RuntimeUtils.getWorkingDirectory(defaultWorkingDirectory.toString());
    }

    private Path getMostRecentFile(List<Path> pathToRecentProjectXmlList) throws IOException {
        Path lastModifiedPath = null;
        long maxMilis = Long.MIN_VALUE;
        for (Path path : pathToRecentProjectXmlList) {
            long lastModifiedMilis = Files.getLastModifiedTime(path).toMillis();
            if (lastModifiedMilis > maxMilis) {
                maxMilis = lastModifiedMilis;
                lastModifiedPath = path;
            }
        }
        return lastModifiedPath;
    }

    private String getRecentProjectPathFromXml(Path filePath) throws IOException, XMLException {
        Document document = XMLUtils.parseDocument(filePath.toAbsolutePath().toString());
        NodeList optionList = document.getElementsByTagName(DataGripConfigXMLConstant.OPTION_TAG);
        if (optionList.getLength() == 0) {
            return "";
        }
        for (int i = 0; i < optionList.getLength(); i++) {
            Node item = optionList.item(i);
            NamedNodeMap attributes = item.getAttributes();
            Node nameAttr = attributes.getNamedItem("name");
            if (nameAttr != null && DataGripConfigXMLConstant.LAST_OPENED_PROJECT_TAG.equals(nameAttr.getNodeValue())) {
                return attributes.getNamedItem("value").getNodeValue();
            }
        }
        return "";
    }

    private void configureSshConfig(ImportConnectionInfo connectionInfo, Map<String, String> conProps) {

        NetworkHandlerDescriptor sslHD = NetworkHandlerRegistry.getInstance().getDescriptor(DBWUtils.SSH_TUNNEL);
        DBWHandlerConfiguration sshHandler = new DBWHandlerConfiguration(sslHD, null);
        sshHandler.setUserName(conProps.get(DataGripConfigXMLConstant.SSH_USERNAME_PATH));
        sshHandler.setSavePassword(true);
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_HOST, conProps.get(DataGripConfigXMLConstant.SSH_HOST_PATH));
        sshHandler.setProperty(DBWHandlerConfiguration.PROP_PORT, conProps.get(DataGripConfigXMLConstant.SSH_PORT_PATH));

        if (!CommonUtils.isEmpty(conProps.get(DataGripConfigXMLConstant.SSH_KEY_FILE_PATH))) {
            sshHandler.setProperty("authType", "PUBLIC_KEY");
            sshHandler.setProperty("keyPath", conProps.get(DataGripConfigXMLConstant.SSH_KEY_FILE_PATH));
        } else if ("OPEN_SSH".equals(conProps.get(DataGripConfigXMLConstant.SSH_CONFIG_AUTH_TYPE))) {
            sshHandler.setProperty("authType", "AGENT");
        } else {
            sshHandler.setProperty("authType", "PASSWORD");
        }
        sshHandler.setProperty("implementation", "sshj");
        sshHandler.setEnabled(true);
        connectionInfo.addNetworkHandler(sshHandler);
    }

    private Map<String, Map<String, String>> mergeSshConfigToIdeaConfigMap(
        Map<String, Map<String, String>> uuidToDataSourceProps,
        Map<String, Map<String, String>> sshIdToSshConfigMap
    ) {
        for (Map.Entry<String, Map<String, String>> configEntry : uuidToDataSourceProps.entrySet()) {

            Map<String, String> config = configEntry.getValue();
            String sshUuid = config.get(DataGripConfigXMLConstant.SSH_PROPERTIES_UUID_PATH);
            Map<String, String> sshConfig = sshIdToSshConfigMap.get(sshUuid);
            if (sshConfig == null) {
                continue;
            }
            config.putAll(sshConfig);
        }
        return uuidToDataSourceProps;
    }


    private Map<String, Map<String, String>> tryReadIdeaSshConfig(String pathToJetBrainsHomeDirectory) {
        try {
            return readIdeaSshConfig(pathToJetBrainsHomeDirectory);
        } catch (Exception e) {
            //can't read ssh config, move on
            log.warn("Could not read Idea ssh config", e);
        }
        return new HashMap<>();
    }

    private Map<String, Map<String, String>> mergeTwoMapProps(
        Map<String, Map<String, String>> uuidToDataSourceProps,
        Map<String, Map<String, String>> uuidToDataSourceFromDifferentXml
    ) {
        for (Map.Entry<String, Map<String, String>> uuidToDataSourceEntry : uuidToDataSourceProps.entrySet()) {
            Map<String, String> dataSourceProps = uuidToDataSourceProps.get(uuidToDataSourceEntry.getKey());
            if (dataSourceProps == null) {
                log.warn("Unexpectedly haven't found data source properties for " + uuidToDataSourceEntry.getKey());
                dataSourceProps = new HashMap<>();
            }
            Map<String, String> mergeValue = uuidToDataSourceFromDifferentXml.get(uuidToDataSourceEntry.getKey());
            if (mergeValue != null) {
                dataSourceProps.putAll(mergeValue);
            }
        }
        return uuidToDataSourceProps;
    }

    private Map<String, Map<String, String>> readIdeaSshConfig(String pathToIdeaFolder) throws Exception {

        Path pathToSshFile = Path.of(DataGripConfigXMLConstant.IDEA_OPTIONS_FOLDER, DataGripConfigXMLConstant.SSH_CONFIG_XML_FILENAME);
        Map<String, Map<String, String>> sshConfig = new HashMap<>();
        List<Path> allPossiblePathToFileFromFolder = getAllExistingPathsToFileFromFolder(pathToIdeaFolder, pathToSshFile);
        for (Path path : allPossiblePathToFileFromFolder) {
            sshConfig.putAll(importXML(path));
        }
        return sshConfig;
    }

    private Map<String, Map<String, String>> importXML(Path filePath) throws XMLException {
        Document document = XMLUtils.parseDocument(filePath.toAbsolutePath().toString());
        Map<String, String> conProps = new HashMap<>();
        Map<String, Map<String, String>> uuidToDatasourceProps = new HashMap<>();
        // * - for getting all element
        NodeList allElements = document.getElementsByTagName("*");
        if (allElements.getLength() == 0) {
            throw new ImportConfigurationException("No elements found");
        }

        String uuid = null;
        for (int i = 0; i < allElements.getLength(); i++) {
            Node element = allElements.item(i);
            NamedNodeMap attrs = element.getAttributes();
            if (DataGripConfigXMLConstant.DATASOURCE_TAG.equals(element.getNodeName())) {
                if (uuid != null) {
                    uuidToDatasourceProps.put(uuid, conProps);
                }
                String uuidOfNewDataSource = attrs.getNamedItem(DataGripConfigXMLConstant.UUID_ATTRIBUTE).getNodeValue();
                conProps = uuidToDatasourceProps.getOrDefault(uuidOfNewDataSource, new HashMap<>());
                uuid = uuidOfNewDataSource;
            }
            if (DataGripConfigXMLConstant.PROPERTIES_TAG.equals(element.getNodeName())) {
                Node value = attrs.getNamedItem("value");
                String name = attrs.getNamedItem("name").getNodeValue();
                if (name.startsWith(DataGripConfigXMLConstant.INTELIJ_CUSTOM_VALUE)) continue;
                conProps.put(DataGripConfigXMLConstant.PROPERTIES_TAG + "$" + name, value == null ? "" : value.getNodeValue());
            }
            //SSH_CONFIG_TAG - tag from sshConfig.xml
            if (DataGripConfigXMLConstant.SSH_CONFIG_TAG.equals(element.getNodeName())) {
                uuid = attrs.getNamedItem(DataGripConfigXMLConstant.ID_ATTRIBUTE).getNodeValue();
                conProps = uuidToDatasourceProps.computeIfAbsent(uuid, key -> new HashMap<>());
            }
            //SSH_PROPERTIES_TAG - tag from dataSourceLocal.xml
            if (DataGripConfigXMLConstant.SSH_PROPERTIES_TAG.equals(element.getNodeName())) {
                Node sshEnabled = allElements.item(++i);
                conProps.put(DataGripConfigXMLConstant.SSH_PROPERTIES_ENABLE_PATH, sshEnabled.getFirstChild().getNodeValue());
                Node sshUuid = allElements.item(++i);
                conProps.put(DataGripConfigXMLConstant.SSH_PROPERTIES_UUID_PATH, sshUuid.getFirstChild().getNodeValue());
                continue;
            }

            for (int j = 0; j < attrs.getLength(); j++) {
                Attr attr = (Attr) attrs.item(j);
                if (attr == null) continue;
                String key = String.format("%s.%s", element.getNodeName(), attr.getName());
                conProps.put(key, attr.getValue());
            }
            if (isNodeHasTextValue(element)) {
                conProps.put(element.getNodeName(), element.getFirstChild().getNodeValue());
            }
        }
        uuidToDatasourceProps.put(uuid, conProps);
        return uuidToDatasourceProps;
    }

    private URI parseURL(String url) {
        String jdbcString = "jdbc:";
        int indexOf = url.indexOf(jdbcString);
        String cleanURI;
        if (indexOf == -1) {
            cleanURI = url;
        } else {
            cleanURI = url.substring(indexOf + jdbcString.length());
        }

        return URI.create(cleanURI);
    }

    private static boolean isNodeHasTextValue(Node element) {
        return element.hasChildNodes() && element.getChildNodes().getLength() > 0 &&
            !element.getFirstChild().getNodeValue().isBlank();
    }

    private ImportDriverInfo buildDriverInfo(Map<String, String> conProps) {

        String name = conProps.get(DataGripConfigXMLConstant.DATABASE_NAME_PATH);
        String refDriverName = conProps.get(DataGripConfigXMLConstant.DRIVER_REF_TAG);

        //todo to think about predefined map from idea name to our driver for exceptional case
        DBPDriver driver = findDriver(name, refDriverName);
        if (driver == null) {
            driver = tryFindDriverByToken(name);
            if (driver == null) {
                driver = tryExtractDriverByUrl(conProps.get(DataGripConfigXMLConstant.JDBC_URL_TAG));
            }
        }
        return new ImportDriverInfo(driver);
    }

    private DBPDriver tryExtractDriverByUrl(String url) {

        URI uri = parseURL(url);
        String scheme = uri.getScheme();
        return findDriver(scheme, null);
    }

    private DBPDriver tryFindDriverByToken(String name) {
        if (name == null) {
            return null;
        }
        DBPDriver driver;
        List<String> nameTokens = Arrays.stream(name.split("_")).toList();
        if (nameTokens.size() > 1) {
            for (String nameToken : nameTokens) {
                driver = findDriver(nameToken, null);
                if (driver != null) {
                    return driver;
                }
            }
        }
        return null;
    }

    private DBPDriver findDriver(@NotNull String name, @Nullable String refDriverName) {
        DataSourceProviderRegistry dataSourceProviderRegistry = DataSourceProviderRegistry.getInstance();
        List<DataSourceProviderDescriptor> dataSourceProviders = dataSourceProviderRegistry.getDataSourceProviders();
        for (DataSourceProviderDescriptor dataSourceProvider : dataSourceProviders) {
            List<DriverDescriptor> drivers = dataSourceProvider.getDrivers();
            for (DriverDescriptor driver : drivers) {
                if (driver.getName().equalsIgnoreCase(name) || driver.getId().equalsIgnoreCase(name)
                    || driver.getName().equalsIgnoreCase(refDriverName)
                    || driver.getId().equalsIgnoreCase(refDriverName)) {
                    while (driver.getReplacedBy() != null) {
                        driver = driver.getReplacedBy();
                    }
                    return driver;
                }
            }
            if (dataSourceProvider.getId().equalsIgnoreCase(name)
                || dataSourceProvider.getName().equalsIgnoreCase(name)
                || dataSourceProvider.getId().equalsIgnoreCase(refDriverName)
                || dataSourceProvider.getName().equalsIgnoreCase(refDriverName)) {
                if (!drivers.isEmpty()) {
                    DriverDescriptor driverDescriptor = drivers.get(0);
                    while (driverDescriptor.getReplacedBy() != null) {
                        driverDescriptor = driverDescriptor.getReplacedBy();
                    }
                    return driverDescriptor;
                }
            }
        }
        return null;
    }
}
