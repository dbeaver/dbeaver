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
package org.jkiss.dbeaver.model.cli.model.option;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.command.ListAuthenticationModelParameterHandler;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import picocli.CommandLine;

import java.util.List;

public class DataSourceAuthOptions implements DataSourceUpdater {
    @Nullable
    @CommandLine.Option(names = {"-u", "--user"}, arity = "1", description = "Database user name for username/password authentication")
    private String dbUser;

    @Nullable
    @CommandLine.Option(names = {"-p", "--password"}, arity = "1", description = "Database password for username/password authentication")
    private String dbPassword;

    @Nullable
    @CommandLine.Option(
        names = {"-auth", "--auth-property"},
        arity = "1",
        description = "Authentication property in the form 'name=value'. May be specified multiple times. "
            + "See '" + ListAuthenticationModelParameterHandler.COMMAND_NAME + "' command for details"
    )
    private List<String> authParams;

    @Nullable
    @CommandLine.Option(
        names = {"-ext", "--extended-property"},
        arity = "1",
        description = "Database provider parameter in the form 'name=value'. May be specified multiple times"
    )
    private List<String> providerParams;

    @Nullable
    @CommandLine.Option(
        names = {"-prop", "--property"},
        arity = "1",
        description = "Database connection parameter in the form 'name=value'. May be specified multiple times"
    )
    private List<String> connectionParams;

    @CommandLine.ArgGroup(
        exclusive = false
    )
    private NetworkHandlerOptions networkHandlerOptions;

    @Nullable
    public List<String> getAuthParams() {
        return authParams;
    }

    @Nullable
    public String getDbPassword() {
        return dbPassword;
    }

    @Nullable
    public String getDbUser() {
        return dbUser;
    }

    @Nullable
    public List<String> getProviderParams() {
        return providerParams;
    }

    @Nullable
    public List<String> getConnectionParams() {
        return connectionParams;
    }

    @Nullable
    public NetworkHandlerOptions getNetworkHandlerOptions() {
        return networkHandlerOptions;
    }

    @Override
    public void updateDataSource(@NotNull DBPDataSourceContainer dataSource) throws CLIException {
        CLIUtils.processDataSourceAuthOptions(dataSource, this);
    }
}
