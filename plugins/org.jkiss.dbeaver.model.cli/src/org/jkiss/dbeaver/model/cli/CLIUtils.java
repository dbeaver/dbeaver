/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceOptions;
import org.jkiss.dbeaver.model.cli.model.option.InputFileOption;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.fs.DBFPath;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.DataSourceUtils;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CLIUtils {
    private static final Log log = Log.getLog(CLIUtils.class);

    @Nullable
    public static String readValueFromFileOrSystemIn(@Nullable InputFileOption filesOptions) throws CLIException {

        if (filesOptions == null) {
            return tryReadFromSystemIn();
        }

        DBFPath inputFile = filesOptions.getInputFile();
        if (inputFile == null) {
            return tryReadFromSystemIn();
        }

        try (inputFile) {
            Path path = inputFile.path();
            if (Files.notExists(path)) {
                throw new CLIException(
                    "Input file does not exist: " + inputFile,
                    CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
                );
            }

            return Files.readString(path);

        } catch (IOException e) {
            throw new CLIException(
                "Error reading GQL from input file: " + inputFile,
                e,
                CLIConstants.EXIT_CODE_ERROR
            );
        }
    }

    @Nullable
    private static String tryReadFromSystemIn() {
        try {
            if (System.in.available() > 0) {
                return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Error reading from system in", e);
            return null;
        }
        return null;
    }


    @NotNull
    public static DBPProject findProject(@Nullable String projectIdOrName, @NotNull CommandLineContext context) throws CLIException {
        DBPProject project;
        DBPWorkspace workspace = context.getContextParameter(DBPWorkspace.class.getName());
        if (workspace == null) {
            workspace = DBWorkbench.getPlatform().getWorkspace();
        }
        if (CommonUtils.isEmpty(projectIdOrName)) {
            project = workspace.getActiveProject();
        } else {
            project = workspace.getProject(projectIdOrName);
            if (project == null) {
                project = workspace.getProjectById(projectIdOrName);
            }
        }
        if (project == null) {
            throw new CLIException("Can't find project '" + projectIdOrName + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        return project;
    }

    @NotNull
    public static DBPDataSourceContainer findDataSource(
        @NotNull DBPProject project,
        @NotNull String existConnectionIdOrName
    ) throws CLIException {
        var registry = project.getDataSourceRegistry();
        DBPDataSourceContainer container = registry.getDataSource(existConnectionIdOrName);

        if (container == null) {
            container = registry.findDataSourceByName(existConnectionIdOrName);
        }
        if (container == null) {
            throw new CLIException("Can't find connection '" + existConnectionIdOrName + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        return container;
    }

    @NotNull
    public static DBPDataSourceContainer createTempDataSource(
        @NotNull DBPProject project,
        @NotNull String driverId,
        @NotNull DataSourceOptions dataSourceOptions,
        @NotNull DataSourceAuthOptions authOptions
    ) throws CLIException {
        DBPDataSourceContainer tempDatasource = createDataSource(
            project,
            driverId,
            dataSourceOptions,
            authOptions,
            true
        );

        processDataSourceAuthOptions(tempDatasource, authOptions);
        return tempDatasource;
    }

    @NotNull
    public static DBPDataSourceContainer createDataSource(
        @NotNull DBPProject project,
        @NotNull String driverId,
        @NotNull DataSourceOptions dataSourceOptions,
        @NotNull DataSourceAuthOptions authOptions,
        boolean temporary
    ) throws CLIException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(driverId);
        if (driver == null) {
            throw new CLIException("Can't find driver '" + driverId + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        DBPConnectionConfiguration connectionConfiguration = updateConnectionConfiguration(
            dataSourceOptions,
            new DBPConnectionConfiguration()
        );

        var registry = project.getDataSourceRegistry();
        DBPDataSourceContainer dataSource = registry.createDataSource(driver, connectionConfiguration);
        updateDataSource(dataSourceOptions, authOptions, dataSource);
        dataSource.setTemporary(temporary);
        try {
            registry.addDataSource(dataSource);
        } catch (Exception e) {
            log.error("Error adding datasource", e);
        }
        return dataSource;
    }

    public static void updateDataSource(
        @NotNull DataSourceOptions dataSourceOptions,
        @NotNull DataSourceAuthOptions authOptions,
        @NotNull DBPDataSourceContainer dataSource
    ) throws CLIException {
        String dsName = dataSourceOptions.getDatasourceName();
        if (CommonUtils.isEmpty(dsName)) {
            dsName = "Ext: " + dataSource.getDriver().getName();
            if (CommonUtils.isNotEmpty(dataSourceOptions.getDbName())) {
                dsName += " - " + dataSourceOptions.getDbName();
            } else if (CommonUtils.isNotEmpty(dataSourceOptions.getServer())) {
                dsName += " - " + dataSourceOptions.getServer();
            }
        }
        if (CommonUtils.isNotEmpty(dataSourceOptions.getDatasourceName())) {
            dataSource.setName(dsName);
        }
        if (CommonUtils.isNotEmpty(dataSourceOptions.getFolder())) {
            DBPDataSourceFolder folder = dataSource.getRegistry().getFolder(dataSourceOptions.getFolder());
            dataSource.setFolder(folder);
        }
        dataSource.setSavePassword(dataSourceOptions.isSavePassword());
        processDataSourceAuthOptions(dataSource, authOptions);
    }


    @NotNull
    public static DBPConnectionConfiguration updateConnectionConfiguration(
        @NotNull DataSourceOptions dataSourceOptions,
        @NotNull DBPConnectionConfiguration connectionConfiguration
    ) {
        if (CommonUtils.isNotEmpty(dataSourceOptions.getUrl())) {
            connectionConfiguration.setUrl(dataSourceOptions.getUrl());
        }
        if (CommonUtils.isNotEmpty(dataSourceOptions.getHost())) {
            connectionConfiguration.setHostName(dataSourceOptions.getHost());
        }
        if (dataSourceOptions.getPort() != null) {
            connectionConfiguration.setHostPort(dataSourceOptions.getPort().toString());
        }
        if (CommonUtils.isNotEmpty(dataSourceOptions.getServer())) {
            connectionConfiguration.setServerName(dataSourceOptions.getServer());
        }
        if (CommonUtils.isNotEmpty(dataSourceOptions.getDbName())) {
            connectionConfiguration.setDatabaseName(dataSourceOptions.getDbName());
        }

        if (!CommonUtils.isEmpty(dataSourceOptions.getAuthModel())) {
            connectionConfiguration.setAuthModelId(dataSourceOptions.getAuthModel());
        }
        return connectionConfiguration;
    }


    @NotNull
    public static Map<String, String> prepareKeyValueParams(
        @Nullable Map<String, String> parentParams,
        @NotNull List<String> cliParams
    ) throws CLIException {
        Map<String, String> properties = parentParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parentParams);
        for (String param : cliParams) {
            String[] paramParts = param.split("=", 2);
            if (paramParts.length == 2) {
                String paramName = paramParts[0].trim();
                String paramValue = paramParts[1].trim();
                if (CommonUtils.isNotEmpty(paramName) && CommonUtils.isNotEmpty(paramValue)) {
                    properties.put(paramName, paramValue);
                }
            } else {
                throw new CLIException("Invalid param format: " + param, CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
            }
        }
        return properties;
    }


    public static void processDataSourceAuthOptions(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DataSourceAuthOptions authOptions
    ) throws CLIException {
        var connectionConfiguration = dataSource.getConnectionConfiguration();

        if (CommonUtils.isNotEmpty(authOptions.getDbUser())) {
            connectionConfiguration.setUserName(authOptions.getDbUser());
        }
        if (CommonUtils.isNotEmpty(authOptions.getDbPassword())) {
            connectionConfiguration.setUserPassword(authOptions.getDbPassword());
        }

        if (!CommonUtils.isEmpty(authOptions.getConnectionParams())) {
            connectionConfiguration.setProperties(
                prepareKeyValueParams(connectionConfiguration.getProperties(), authOptions.getConnectionParams())
            );
        }

        if (!CommonUtils.isEmpty(authOptions.getProviderParams())) {
            connectionConfiguration.setProviderProperties(
                prepareKeyValueParams(connectionConfiguration.getProviderProperties(), authOptions.getProviderParams())
            );
        }

        if (!CommonUtils.isEmpty(authOptions.getAuthParams())) {
            Map<String, String> authProperties = CLIUtils.prepareKeyValueParams(
                connectionConfiguration.getAuthProperties(),
                authOptions.getAuthParams()
            );
            if (!CommonUtils.isEmpty(authProperties)) {
                DBAAuthCredentials credentialsInstance = connectionConfiguration.getAuthModel()
                    .loadCredentials(dataSource, connectionConfiguration);
                PropertySerializationUtils.updateCredentialsFromProperties(
                    new LoggingProgressMonitor(),
                    credentialsInstance,
                    authProperties
                );
                dataSource.getConnectionConfiguration().getAuthModel()
                    .provideCredentials(dataSource, dataSource.getConnectionConfiguration(), credentialsInstance);
            }
        }

        if (authOptions.getNetworkHandlerOptions() != null
            && !CommonUtils.isEmpty(authOptions.getNetworkHandlerOptions().getHandlerParams())
        ) {
            Map<String, String> handlerParams = prepareKeyValueParams(
                new HashMap<>(),
                authOptions.getNetworkHandlerOptions().getHandlerParams()
            );
            try {
                DataSourceUtils.processNetworkHandlerProperties(
                    dataSource,
                    authOptions.getNetworkHandlerOptions().isSavePassword(),
                    handlerParams
                );
            } catch (Exception e) {
                throw new CLIException(
                    "Error processing network handler properties: " + e.getMessage(),
                    e,
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }
    }


    public static String getPropertyHelpText(@NotNull DBPPropertyDescriptor property) {
        return getPropertyHelpText(property, null);
    }

    @NotNull
    public static String getPropertyHelpText(
        @NotNull DBPPropertyDescriptor property,
        @Nullable String namePrefix
    ) {
        String displayName = property.getDisplayName();
        String description = property.getDescription();
        var helpText = new StringBuilder();


        helpText.append("  - ");
        if (CommonUtils.isNotEmpty(namePrefix) && !property.getId().startsWith(namePrefix)) {
            helpText.append(namePrefix);
        }
        helpText.append(property.getId());
        if (!CommonUtils.equalObjects(displayName, description)) {
            helpText.append(" (").append(displayName).append(")");
        }
        if (CommonUtils.isNotEmpty(description)) {
            helpText.append(" = ").append(description);
        }
        if (property instanceof IPropertyValueListProvider<?> valueListProvider) {
            Object[] possibleValues = valueListProvider.getPossibleValues(null);
            if (!ArrayUtils.isEmpty(possibleValues)) {
                helpText.append(", possible values: ");
                for (int i = 0; i < possibleValues.length; i++) {
                    helpText.append(possibleValues[i]);
                    if (i < possibleValues.length - 1) {
                        helpText.append(", ");
                    }
                }
            }
        }
        helpText.append("\n");

        return helpText.toString();
    }

    @NotNull
    public static String formatAsTable(@NotNull List<Map<String, String>> data) {
        if (data.isEmpty()) {
            return "";
        }
        Map<String, Integer> columnWidths = new LinkedHashMap<>();
        for (String key : data.getFirst().keySet()) {
            columnWidths.put(key, key.length());
        }
        for (Map<String, String> row : data) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                columnWidths.put(entry.getKey(), Math.max(columnWidths.get(entry.getKey()), entry.getValue().length()));
            }
        }

        StringBuilder sb = new StringBuilder();
        // header
        for (Map.Entry<String, Integer> entry : columnWidths.entrySet()) {
            sb.append(String.format("%-" + (entry.getValue() + 3) + "s", entry.getKey()));
        }
        sb.append("\n");
        // rows
        for (Map<String, String> row : data) {
            for (Map.Entry<String, Integer> entry : columnWidths.entrySet()) {
                sb.append(String.format("%-" + (entry.getValue() + 3) + "s", row.get(entry.getKey())));
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
