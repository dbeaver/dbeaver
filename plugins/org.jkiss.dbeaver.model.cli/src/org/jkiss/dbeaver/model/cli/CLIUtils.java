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
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceOptions;
import org.jkiss.dbeaver.model.cli.model.option.InputFileOption;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CLIUtils {
    private static final Log log = Log.getLog(CLIUtils.class);

    @Nullable
    public static String readValueFromFileOrSystemIn(@Nullable InputFileOption filesOptions) throws CLIException {
        String value = null;
        if (filesOptions == null || filesOptions.getInputFile() == null) {
            value = tryReadFromSystemIn();
        } else if (filesOptions.getInputFile() != null) {
            if (Files.notExists(filesOptions.getInputFile())) {
                throw new CLIException(
                    "Input file does not exist: " + filesOptions.getInputFile(),
                    CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
                );
            }
            try {
                value = Files.readString(filesOptions.getInputFile());
            } catch (IOException e) {
                throw new CLIException(
                    "Error reading GQL from input file: " + filesOptions.getInputFile(),
                    e,
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }
        return value;
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
            var folder = dataSource.getRegistry().getFolder(dataSourceOptions.getFolder());
            if (folder != null) {
                dataSource.setFolder(folder);
            }
        }
        dataSource.setSavePassword(dataSourceOptions.isSavePassword());
        processDataSourceAuthOptions(dataSource, authOptions);
    }


    @NotNull
    public static DBPConnectionConfiguration updateConnectionConfiguration(
        @NotNull DataSourceOptions dataSourceOptions,
        @NotNull DBPConnectionConfiguration connectionConfiguration
    ) {
        connectionConfiguration.setUrl(dataSourceOptions.getUrl());
        connectionConfiguration.setHostName(dataSourceOptions.getHost());
        connectionConfiguration.setHostPort(dataSourceOptions.getPort() == null ? null : dataSourceOptions.getPort().toString());
        connectionConfiguration.setServerName(dataSourceOptions.getServer());
        connectionConfiguration.setDatabaseName(dataSourceOptions.getDbName());

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
        for (String authParam : cliParams) {
            String[] paramParts = authParam.split("=", 2);
            if (paramParts.length == 2) {
                String paramName = paramParts[0].trim();
                String paramValue = paramParts[1].trim();
                if (CommonUtils.isNotEmpty(paramName) && CommonUtils.isNotEmpty(paramValue)) {
                    properties.put(paramName, paramValue);
                }
            } else {
                throw new CLIException("Invalid auth-param format: " + authParam, CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
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
    }
}
