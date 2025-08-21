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

package org.jkiss.dbeaver.ui.config.migration.wizards.datagrip;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportDriverInfo;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * DataGrip (JetBrains) connection importer
 * 
 * This importer reads DataGrip connection settings from the exported settings.
 * It supports the dataSources.xml file format that is part of DataGrip's exported IDE settings.
 * 
 * Supported connection types:
 * - PostgreSQL
 * - MySQL
 * - Oracle  
 * - SQL Server
 * - SQLite
 * - MariaDB
 * - And other JDBC-based connections
 * 
 * Usage: Export IDE Settings from DataGrip, extract the ZIP, and point the importer
 * to the extracted folder containing the settings/options/dataSources.xml file.
 */
public class ConfigImportWizardPageDataGrip extends ConfigImportWizardPage {

    private static final Log log = Log.getLog(ConfigImportWizardPageDataGrip.class);

    // DataGrip settings file structure
    public static final String DATAGRIP_SETTINGS_FOLDER = "settings";
    public static final String DATAGRIP_OPTIONS_FOLDER = "options";
    public static final String DATAGRIP_DATASOURCES_FILE = "dataSources.xml";

    // Map DataGrip driver references to DBeaver driver IDs
    private static final Map<String, String> DRIVER_MAPPING = new HashMap<>();
    
    static {
        // PostgreSQL
        DRIVER_MAPPING.put("postgresql", "postgresql");
        
        // MySQL and MariaDB
        DRIVER_MAPPING.put("mysql", "mysql8");
        DRIVER_MAPPING.put("mariadb", "mariaDB");
        
        // Oracle
        DRIVER_MAPPING.put("oracle", "oracle_thin");
        
        // SQL Server
        DRIVER_MAPPING.put("sqlserver", "mssql");
        DRIVER_MAPPING.put("sqlserver_2012", "mssql");
        DRIVER_MAPPING.put("sqlserver_2014", "mssql");
        DRIVER_MAPPING.put("sqlserver_2016", "mssql");
        DRIVER_MAPPING.put("sqlserver_2017", "mssql");
        DRIVER_MAPPING.put("sqlserver_2019", "mssql");
        DRIVER_MAPPING.put("sqlserver_2022", "mssql");
        
        // SQLite
        DRIVER_MAPPING.put("sqlite", "sqlite_jdbc");
        
        // Other common databases
        DRIVER_MAPPING.put("h2", "h2_embedded");
        DRIVER_MAPPING.put("derby", "derby_embedded");
        DRIVER_MAPPING.put("hsqldb", "hsqldb_embedded");
    }

    protected ConfigImportWizardPageDataGrip() {
        super("DataGrip");
        setTitle("DataGrip");
        setDescription("Import DataGrip (JetBrains) connections from exported IDE settings");
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {
        setErrorMessage(null);
        
        ConfigImportWizardDataGrip wizard = (ConfigImportWizardDataGrip) getWizard();
        File inputFile = wizard.getPageSettings().getInputFile();
        
        if (inputFile == null) {
            throw new DBException("No input file specified");
        }

        // Check if the input is a directory (extracted settings folder) or a specific file
        File dataSourcesFile;
        if (inputFile.isDirectory()) {
            // Look for settings/options/dataSources.xml
            dataSourcesFile = findDataSourcesFile(inputFile);
        } else if (inputFile.getName().equals(DATAGRIP_DATASOURCES_FILE)) {
            // Direct dataSources.xml file
            dataSourcesFile = inputFile;
        } else {
            throw new DBException("Invalid DataGrip settings file. Expected a folder with DataGrip exported settings or a dataSources.xml file.");
        }

        if (!dataSourcesFile.exists()) {
            throw new DBException("DataGrip dataSources.xml file not found. Please make sure you selected the correct exported settings folder.");
        }

        try {
            parseDataSources(dataSourcesFile, importData);
        } catch (XMLException e) {
            throw new DBException("Error parsing DataGrip dataSources.xml: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error importing DataGrip connections", e);
            throw new DBException("Unexpected error importing DataGrip connections: " + e.getMessage(), e);
        }
    }

    private File findDataSourcesFile(File rootFolder) throws DBException {
        // Look for settings/options/dataSources.xml
        File settingsFolder = new File(rootFolder, DATAGRIP_SETTINGS_FOLDER);
        if (!settingsFolder.exists()) {
            // Maybe the root folder is already the settings folder
            settingsFolder = rootFolder;
        }
        
        File optionsFolder = new File(settingsFolder, DATAGRIP_OPTIONS_FOLDER);
        if (!optionsFolder.exists()) {
            throw new DBException("DataGrip options folder not found. Expected structure: settings/options/dataSources.xml");
        }
        
        return new File(optionsFolder, DATAGRIP_DATASOURCES_FILE);
    }

    private void parseDataSources(File dataSourcesFile, ImportData importData) throws XMLException {
        Document document = XMLUtils.parseDocument(dataSourcesFile);
        Element rootElement = document.getDocumentElement();
        
        // Find the dataSourceStorage component
        Element dataSourceStorage = findDataSourceStorageComponent(rootElement);
        if (dataSourceStorage == null) {
            log.warn("No dataSourceStorage component found in DataGrip dataSources.xml");
            return;
        }

        // Keep track of added drivers to avoid duplicates
        Map<String, ImportDriverInfo> addedDrivers = new HashMap<>();

        // Parse each data-source element
        for (Element dataSourceElement : XMLUtils.getChildElementList(dataSourceStorage, "data-source")) {
            try {
                ImportConnectionInfo connectionInfo = parseDataSource(dataSourceElement, importData, addedDrivers);
                if (connectionInfo != null) {
                    importData.addConnection(connectionInfo);
                }
            } catch (Exception e) {
                log.warn("Error parsing DataGrip data source: " + e.getMessage(), e);
                // Continue with other connections
            }
        }
    }

    private Element findDataSourceStorageComponent(Element rootElement) {
        for (Element component : XMLUtils.getChildElementList(rootElement, "component")) {
            String componentName = component.getAttribute("name");
            if ("dataSourceStorage".equals(componentName)) {
                return component;
            }
        }
        return null;
    }

    private ImportConnectionInfo parseDataSource(Element dataSourceElement, ImportData importData, Map<String, ImportDriverInfo> addedDrivers) {
        String name = dataSourceElement.getAttribute("name");
        String uuid = dataSourceElement.getAttribute("uuid");
        String source = dataSourceElement.getAttribute("source");
        String readOnly = dataSourceElement.getAttribute("read-only");
        
        if (CommonUtils.isEmpty(name)) {
            log.warn("DataGrip data source has no name, skipping");
            return null;
        }

        // Extract connection details
        String driverRef = getChildElementText(dataSourceElement, "driver-ref");
        String jdbcUrl = getChildElementText(dataSourceElement, "jdbc-url");
        String jdbcDriver = getChildElementText(dataSourceElement, "jdbc-driver");
        String remarks = getChildElementText(dataSourceElement, "remarks");
        
        if (CommonUtils.isEmpty(jdbcUrl)) {
            log.warn("DataGrip data source '" + name + "' has no JDBC URL, skipping");
            return null;
        }

        // Map DataGrip driver to DBeaver driver
        ImportDriverInfo driverInfo = mapDriver(driverRef, jdbcDriver, jdbcUrl, importData, addedDrivers);
        if (driverInfo == null) {
            log.warn("Unknown driver type for DataGrip data source '" + name + "': " + driverRef);
            return null;
        }

        // Parse connection details from JDBC URL
        ConnectionDetails details = parseJdbcUrl(jdbcUrl);
        
        ImportConnectionInfo connectionInfo = new ImportConnectionInfo(
            driverInfo,
            uuid,
            name,
            jdbcUrl,
            details.host,
            details.port,
            details.database,
            null, // username not stored in dataSources.xml for security
            null  // password not stored in dataSources.xml for security
        );

        // Set additional properties
        if (!CommonUtils.isEmpty(remarks)) {
            connectionInfo.setProperty("description", remarks);
        }
        
        if (CommonUtils.toBoolean(readOnly)) {
            connectionInfo.setProperty("readOnly", "true");
        }

        return connectionInfo;
    }

    private String getChildElementText(Element parent, String tagName) {
        Element child = XMLUtils.getChildElement(parent, tagName);
        return child != null ? XMLUtils.getElementBody(child) : null;
    }

    private ImportDriverInfo mapDriver(String driverRef, String jdbcDriver, String jdbcUrl, ImportData importData, Map<String, ImportDriverInfo> addedDrivers) {
        if (CommonUtils.isEmpty(driverRef)) {
            // Try to guess from JDBC URL
            driverRef = guessDriverFromUrl(jdbcUrl);
        }
        
        String dbeaverDriverId = DRIVER_MAPPING.get(driverRef);
        ImportDriverInfo driverInfo;
        
        if (dbeaverDriverId == null) {
            // Create a generic driver info if we can't map it
            driverInfo = createGenericDriverInfo(driverRef, jdbcDriver, jdbcUrl);
        } else {
            // Create driver info based on mapping
            driverInfo = createDriverInfo(dbeaverDriverId, driverRef, jdbcUrl, jdbcDriver);
        }
        
        // Add driver to ImportData if not already added
        String driverKey = driverInfo.getId() != null ? driverInfo.getId() : driverInfo.getName();
        if (!addedDrivers.containsKey(driverKey)) {
            addedDrivers.put(driverKey, driverInfo);
            importData.addDriver(driverInfo);
        } else {
            // Return the already added driver to maintain reference consistency
            driverInfo = addedDrivers.get(driverKey);
        }
        
        return driverInfo;
    }

    private String guessDriverFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        
        String lowerUrl = jdbcUrl.toLowerCase();
        if (lowerUrl.startsWith("jdbc:postgresql:")) return "postgresql";
        if (lowerUrl.startsWith("jdbc:mysql:")) return "mysql";
        if (lowerUrl.startsWith("jdbc:mariadb:")) return "mariadb";
        if (lowerUrl.startsWith("jdbc:oracle:")) return "oracle";
        if (lowerUrl.startsWith("jdbc:sqlserver:")) return "sqlserver";
        if (lowerUrl.startsWith("jdbc:sqlite:")) return "sqlite";
        if (lowerUrl.startsWith("jdbc:h2:")) return "h2";
        if (lowerUrl.startsWith("jdbc:derby:")) return "derby";
        if (lowerUrl.startsWith("jdbc:hsqldb:")) return "hsqldb";
        
        return null;
    }

    private ImportDriverInfo createDriverInfo(String dbeaverDriverId, String dataGripDriverRef, String jdbcUrl, String jdbcDriver) {
        // Create standard driver info based on known DBeaver drivers
        switch (dbeaverDriverId) {
            case "postgresql":
                return new ImportDriverInfo(dbeaverDriverId, "PostgreSQL", "jdbc:postgresql://{host}[:{port}]/[{database}]", "org.postgresql.Driver");
            case "mysql8":
                return new ImportDriverInfo(dbeaverDriverId, "MySQL", "jdbc:mysql://{host}[:{port}]/[{database}]", "com.mysql.cj.jdbc.Driver");
            case "mariaDB":
                return new ImportDriverInfo(dbeaverDriverId, "MariaDB", "jdbc:mariadb://{host}[:{port}]/[{database}]", "org.mariadb.jdbc.Driver");
            case "oracle_thin":
                return new ImportDriverInfo(dbeaverDriverId, "Oracle", "jdbc:oracle:thin:@{host}[:{port}]/{database}", "oracle.jdbc.OracleDriver");
            case "mssql":
                return new ImportDriverInfo(dbeaverDriverId, "SQL Server", "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
            case "sqlite_jdbc":
                return new ImportDriverInfo(dbeaverDriverId, "SQLite", "jdbc:sqlite:{file}", "org.sqlite.JDBC");
            case "h2_embedded":
                return new ImportDriverInfo(dbeaverDriverId, "H2", "jdbc:h2:{file}", "org.h2.Driver");
            case "derby_embedded":
                return new ImportDriverInfo(dbeaverDriverId, "Derby", "jdbc:derby:{database}", "org.apache.derby.jdbc.EmbeddedDriver");
            case "hsqldb_embedded":
                return new ImportDriverInfo(dbeaverDriverId, "HSQLDB", "jdbc:hsqldb:{file}", "org.hsqldb.jdbc.JDBCDriver");
            default:
                return createGenericDriverInfo(dataGripDriverRef, jdbcDriver, jdbcUrl);
        }
    }

    private ImportDriverInfo createGenericDriverInfo(String driverRef, String jdbcDriver, String jdbcUrl) {
        String driverName = driverRef != null ? driverRef.toUpperCase() : "Unknown";
        String driverClass = jdbcDriver != null ? jdbcDriver : "unknown.driver.Class";
        String urlTemplate = jdbcUrl != null ? jdbcUrl : "jdbc:unknown://host:port/database";
        
        return new ImportDriverInfo(null, driverName, urlTemplate, driverClass);
    }

    private ConnectionDetails parseJdbcUrl(String jdbcUrl) {
        ConnectionDetails details = new ConnectionDetails();
        
        if (CommonUtils.isEmpty(jdbcUrl)) {
            return details;
        }

        try {
            URI uri = new URI(jdbcUrl.substring(5)); // Remove "jdbc:" prefix
            
            // Extract basic components
            String scheme = uri.getScheme();
            details.host = uri.getHost();
            details.port = uri.getPort() > 0 ? String.valueOf(uri.getPort()) : null;
            details.database = extractDatabaseFromPath(uri.getPath(), scheme);
            
            // Handle special URL formats
            if (details.host == null) {
                parseSpecialUrls(jdbcUrl, details);
            }
            
        } catch (URISyntaxException e) {
            // For URLs that don't parse as URI, try manual parsing
            parseManually(jdbcUrl, details);
        }

        return details;
    }

    private String extractDatabaseFromPath(String path, String scheme) {
        if (CommonUtils.isEmpty(path) || "/".equals(path)) {
            return null;
        }
        
        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // For SQL Server, database might be in parameters
        if ("sqlserver".equals(scheme)) {
            return null; // Database name is usually in connection parameters
        }
        
        return path;
    }

    private void parseSpecialUrls(String jdbcUrl, ConnectionDetails details) {
        // Handle special JDBC URL formats that don't parse as standard URIs
        
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            // SQLite file path
            details.database = jdbcUrl.substring("jdbc:sqlite:".length());
            details.host = "localhost";
        } else if (jdbcUrl.startsWith("jdbc:h2:")) {
            // H2 database
            String dbPath = jdbcUrl.substring("jdbc:h2:".length());
            details.database = dbPath;
            details.host = "localhost";
        } else if (jdbcUrl.contains("//")) {
            // Try to extract host:port from URLs with //
            int startIdx = jdbcUrl.indexOf("//") + 2;
            int endIdx = jdbcUrl.indexOf("/", startIdx);
            if (endIdx == -1) {
                endIdx = jdbcUrl.indexOf("?", startIdx);
            }
            if (endIdx == -1) {
                endIdx = jdbcUrl.length();
            }
            
            String hostPort = jdbcUrl.substring(startIdx, endIdx);
            int colonIdx = hostPort.lastIndexOf(":");
            if (colonIdx > 0) {
                details.host = hostPort.substring(0, colonIdx);
                try {
                    details.port = hostPort.substring(colonIdx + 1);
                    Integer.parseInt(details.port); // Validate it's a number
                } catch (NumberFormatException e) {
                    details.port = null;
                }
            } else {
                details.host = hostPort;
            }
        }
    }

    private void parseManually(String jdbcUrl, ConnectionDetails details) {
        // Manual parsing for complex JDBC URLs
        
        // This is a basic implementation - could be extended for more complex URL formats
        // based on actual DataGrip exports encountered
        
        if (jdbcUrl.contains("://")) {
            String[] parts = jdbcUrl.split("://");
            if (parts.length > 1) {
                String remaining = parts[1];
                String[] hostParts = remaining.split("/");
                if (hostParts.length > 0) {
                    String hostPort = hostParts[0];
                    if (hostPort.contains(":")) {
                        String[] hp = hostPort.split(":");
                        details.host = hp[0];
                        if (hp.length > 1) {
                            details.port = hp[1];
                        }
                    } else {
                        details.host = hostPort;
                    }
                }
                
                if (hostParts.length > 1) {
                    details.database = hostParts[1].split("\\?")[0]; // Remove parameters
                }
            }
        }
    }

    private static class ConnectionDetails {
        String host;
        String port;
        String database;
    }
}
