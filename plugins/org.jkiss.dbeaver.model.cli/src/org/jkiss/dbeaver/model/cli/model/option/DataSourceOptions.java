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

import org.jkiss.code.Nullable;
import picocli.CommandLine;

public class DataSourceOptions {
    @Nullable
    @CommandLine.Option(names = {"--host"}, arity = "1", description = "Database host")
    private String host;

    @Nullable
    @CommandLine.Option(names = {"--database"}, arity = "1", description = "Database name")
    private String dbName;
    @Nullable
    @CommandLine.Option(names = {"--server"}, arity = "1", description = "Database server")
    private String server;

    @Nullable
    @CommandLine.Option(names = {"--url"}, arity = "1", description = "Database url(e.g. JDBC url)")
    private String url;

    @Nullable
    @CommandLine.Option(names = {"--auth-model"}, arity = "1", description = "Database auth model")
    private String authModel;

    @Nullable
    @CommandLine.Option(names = {"--port"}, arity = "1", description = "Database port")
    private Integer port;

    @Nullable
    @CommandLine.Option(names = {"--folder"}, arity = "1", description = "Connection folder")
    private String folder;

    @Nullable
    @CommandLine.Option(names = {"--name"}, arity = "1", description = "Connection name")
    private String dataSourceName;

    @CommandLine.Option(
        names = {"--save-password"},
        arity = "1",
        defaultValue = "true",
        description = "Save password"
    )
    private boolean savePassword;

    @Nullable
    public String getAuthModel() {
        return authModel;
    }

    @Nullable
    public String getFolder() {
        return folder;
    }

    @Nullable
    public String getHost() {
        return host;
    }

    @Nullable
    public Integer getPort() {
        return port;
    }

    @Nullable
    public String getServer() {
        return server;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getDbName() {
        return dbName;
    }

    @Nullable
    public String getDatasourceName() {
        return dataSourceName;
    }

    public boolean isSavePassword() {
        return savePassword;
    }
}
