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
package org.jkiss.dbeaver.model.cli.command;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "auth-models", description = "List available database authentication models")
public class ListAuthenticationModelParameterHandler extends AbstractRootCommandLineParameterHandler {

    @CommandLine.Mixin
    protected ProjectOption projectOption;

    @CommandLine.Option(names = {"--provider"}, description = "Filter by provider ID")
    protected String providerId;

    @CommandLine.Option(names = {"--driver"}, description = "Filter by driver ID")
    protected String driverId;

    @CommandLine.Option(names = {"--datasource"}, description = "Filter by datasource ID or name")
    protected String datasourceId;

    @Override
    public void run() throws CLIException {
        DBPDataSourceProviderRegistry dataSourceProviderRegistry = DBWorkbench.getPlatform().getDataSourceProviderRegistry();
        List<? extends DBPAuthModelDescriptor> authModels = dataSourceProviderRegistry.getAllAuthModels();
        List<DBPDriver> applicableDrivers = new ArrayList<>();

        if (CommonUtils.isNotEmpty(datasourceId)) {
            DBPDataSourceContainer dataSource = CLIUtils.findDataSource(
                CLIUtils.findProject(projectOption.getProjectIdOrName(), context()),
                datasourceId
            );
            DBPDriver driver = dataSource.getDriver();
            applicableDrivers.add(driver);
            authModels = authModels.stream()
                .filter(am -> am.isApplicableTo(driver))
                .toList();
        } else if (CommonUtils.isNotEmpty(driverId)) {
            DBPDriver driver = dataSourceProviderRegistry.findDriver(driverId);
            if (driver != null) {
                applicableDrivers.add(driver);
                authModels = authModels.stream()
                    .filter(am -> am.isApplicableTo(driver))
                    .toList();
            } else {
                throw new CLIException("Can't find driver '" + driverId + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
            }
        } else if (CommonUtils.isNotEmpty(providerId)) {
            DBPDataSourceProviderDescriptor provider = dataSourceProviderRegistry.getDataSourceProvider(providerId);
            if (provider != null) {
                applicableDrivers.addAll(provider.getEnabledDrivers());
                authModels = authModels.stream()
                    .filter(am ->
                        applicableDrivers.stream()
                            .anyMatch(am::isApplicableTo)
                    )
                    .toList();
            } else {
                throw new CLIException("Can't find provider '" + providerId + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
            }
        }

        context().addResult(getConsoleOutput(authModels, applicableDrivers));
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);

    }

    @NotNull
    private static String getConsoleOutput(List<? extends DBPAuthModelDescriptor> authModels, List<DBPDriver> applicableDrivers) {
        StringBuilder outBuilder = new StringBuilder();
        for (DBPAuthModelDescriptor authModel : authModels) {
            outBuilder.append(String.format(
                "Auth Model ID: %s, Name: %s, Description: %s%n",
                authModel.getId(),
                authModel.getName(),
                authModel.getDescription()
            ));

            String drivers = applicableDrivers.stream()
                .filter(authModel::isApplicableTo)
                .map(DBPDriver::getFullName)
                .collect(Collectors.joining(", "));
            if (!CommonUtils.isEmpty(drivers)) {
                outBuilder.append("Applicable Drivers: ").append(drivers).append("\n");
            }

            outBuilder.append("Parameters:\n");
            DBAAuthModel<? extends DBAAuthCredentials> authModelInstance = authModel.getInstance();
            DBAAuthCredentials credentials = authModelInstance.createCredentials();
            PropertyCollector propertyCollector = new PropertyCollector(credentials, true);
            propertyCollector.collectProperties();
            for (DBPPropertyDescriptor property : propertyCollector.getProperties()) {
                String helpText = getHelpText(property);
                outBuilder.append(helpText);
            }
            outBuilder.append("\n");
        }
        return outBuilder.toString();
    }

    private static @NotNull String getHelpText(DBPPropertyDescriptor property) {
        String displayName = property.getDisplayName();
        String description = property.getDescription();
        String helpText;
        if (CommonUtils.equalObjects(displayName, description) || CommonUtils.isEmpty(description)) {
            helpText = "  - %s = %s%n".formatted(
                property.getId(),
                CommonUtils.notEmpty(displayName)
            );
        } else {
            helpText = "  - %s (%s) = %s%n".formatted(
                property.getId(),
                property.getDisplayName(),
                property.getDescription()
            );
        }
        return helpText;
    }
}