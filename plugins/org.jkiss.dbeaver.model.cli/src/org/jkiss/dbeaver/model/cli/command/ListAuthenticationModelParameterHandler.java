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

import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.cli.AbstractRootCommandLineParameterHandler;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "auth-models", description = "List available database authentication models")
public class ListAuthenticationModelParameterHandler extends AbstractRootCommandLineParameterHandler {
    // provider/driver/connection filter
    @Override
    public void run() {
        List<? extends DBPAuthModelDescriptor> authModels =

            DBWorkbench.getPlatform().getDataSourceProviderRegistry().getAllAuthModels();

        StringBuilder outBuilder = new StringBuilder();
        for (DBPAuthModelDescriptor authModel : authModels) {
            DBAAuthModel<?> modelInstance = authModel.getInstance();
            outBuilder.append(String.format(
                "Auth Model ID: %s, Name: %s, Description: %s, Parameters:\n",
                authModel.getId(),
                authModel.getName(),
                authModel.getDescription()
            ));
            DBAAuthCredentials credentials = modelInstance.createCredentials();
            PropertyCollector propertyCollector = new PropertyCollector(credentials, true);
            propertyCollector.collectProperties();
            for (DBPPropertyDescriptor property : propertyCollector.getProperties()) {
                String helpText = CLIUtils.getPropertyHelpText(property);
                outBuilder.append(helpText);
            }
        }
        context().addResult(outBuilder.toString());
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }

}
