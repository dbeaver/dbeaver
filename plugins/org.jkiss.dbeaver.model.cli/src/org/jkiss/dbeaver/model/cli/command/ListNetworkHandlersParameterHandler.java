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
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWHandlerRegistry;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "network-handlers", description = "List available database network handlers.")

public class ListNetworkHandlersParameterHandler extends AbstractRootCommandLineParameterHandler {
    @NotNull
    @CommandLine.Option(names = {"--driver"}, arity = "1", description = "List supported handlers for the specified driver.")
    private String driverId;

    @Override
    public void run() throws CLIException {
        DBPDataSourceProviderRegistry driverRegistry = DBWorkbench.getPlatform().getDataSourceProviderRegistry();
        DBWHandlerRegistry networkHandlerRegistry = DBWorkbench.getPlatform().getNetworkHandlerRegistry();
        StringBuilder output = new StringBuilder();

        if (CommonUtils.isNotEmpty(driverId)) {
            var driver = driverRegistry.findDriver(driverId);
            if (driver == null) {
                throw new CLIException("Driver '" + driverId + "' not found", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
            }
            oneDriverHelp(driver, networkHandlerRegistry, output);
        } else {
            allDriversHelp(driverRegistry, networkHandlerRegistry, output);
        }

        context().addResult(output.toString());
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }

    private void oneDriverHelp(
        @NotNull DBPDriver driver,
        @NotNull DBWHandlerRegistry networkHandlerRegistry,
        @NotNull StringBuilder output
    ) {
        List<? extends DBWHandlerDescriptor> networkHandlers = networkHandlerRegistry.getDescriptors(driver);
        output.append("Network handlers for driver '").append(driver.getName()).append("'\n");

        for (var handler : networkHandlers) {
            output.append("Network Handler ID: ").append(handler.getId())
                .append(", Name: ").append(handler.getCodeName())
                .append(", Description: ").append(handler.getDescription());
            if (!ArrayUtils.isEmpty(handler.getHandlerProperties())) {
                output.append(", Parameters:");
            }
            String prefix = handler.getPrefix() + ".";
            for (DBPPropertyDescriptor property : handler.getHandlerProperties()) {
                String helpText = CLIUtils.getPropertyHelpText(property, prefix);
                output.append(helpText);
            }
            output.append("\n");
        }
        output.replace(output.length() - 2, output.length(), "");
    }

    private void allDriversHelp(
        @NotNull DBPDataSourceProviderRegistry driverRegistry,
        @NotNull DBWHandlerRegistry networkHandlerRegistry,
        @NotNull StringBuilder output
    ) {
        Map<String, Set<String>> driversByHandler = new HashMap<>();
        List<? extends DBPDataSourceProviderDescriptor> providers = driverRegistry.getDataSourceProviders();
        for (DBPDataSourceProviderDescriptor provider : providers) {
            for (DBPDriver driver : provider.getDrivers()) {
                List<? extends DBWHandlerDescriptor> networkHandlers = networkHandlerRegistry.getDescriptors(driver);
                for (var networkHandler : networkHandlers) {
                    driversByHandler
                        .computeIfAbsent(networkHandler.getId(), k -> new HashSet<>())
                        .add(driver.getId());
                }
            }
        }

        output.append("Available network handlers:\n");
        for (var entry : driversByHandler.entrySet()) {
            String handlerId = entry.getKey();
            DBWHandlerDescriptor handler = networkHandlerRegistry.getDescriptor(handlerId);
            String prefix = handler.getPrefix() + ".";
            Set<String> driverIds = entry.getValue();
            output.append("Network Handler ID: ").append(handlerId)
                .append(", Name: ").append(handler.getCodeName())
                .append(", Description: ").append(handler.getDescription());
            if (!ArrayUtils.isEmpty(handler.getHandlerProperties())) {
                output.append(", Parameters:");
            }
            output.append("\n")
                .append("Supported by drivers: ").append(String.join(", ", driverIds)).append("\n");

            for (DBPPropertyDescriptor property : handler.getHandlerProperties()) {
                String helpText = CLIUtils.getPropertyHelpText(property, prefix);
                output.append(helpText);
            }
            output.append("\n");
        }
        output.replace(output.length() - 2, output.length(), "");
    }
}
