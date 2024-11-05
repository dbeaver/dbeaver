/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AuthModelPgPass extends AuthModelDatabaseNative<AuthModelPgPassCredentials> {
    private static final Log log = Log.getLog(AuthModelPgPass.class);

    public static final String PGPASSFILE_ENV_VARIABLE = "PGPASSFILE";

    @NotNull
    @Override
    public AuthModelPgPassCredentials createCredentials() {
        return new AuthModelPgPassCredentials();
    }

    @NotNull
    @Override
    public AuthModelPgPassCredentials loadCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration) {
        AuthModelPgPassCredentials credentials = super.loadCredentials(dataSource, configuration);
        try {
            loadPasswordFromPgPass(credentials, dataSource, configuration);
            credentials.setParseError(null);
        } catch (DBException e) {
            credentials.setParseError(e);
        }
        return credentials;
    }

    @Override
    public Object initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull AuthModelPgPassCredentials credentials, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connectProps) throws DBException {
        if (credentials.getParseError() != null) {
            throw new DBCException("Couldn't get password from PGPASS file", credentials.getParseError());
        }
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connectProps);
    }

    private String getSSHHost(@NotNull DBPDataSourceContainer dataSourceContainer) {
        DBWHandlerConfiguration sshHandler =
            dataSourceContainer.getActualConnectionConfiguration().getHandler("ssh_tunnel");
        if (sshHandler != null) {
            Object host = sshHandler.getProperty(DBPConnectionConfiguration.VARIABLE_HOST);
            if (host != null) {
                return (String) host;
            }
        }
        return null;
    }


    private void loadPasswordFromPgPass(AuthModelPgPassCredentials credentials, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) throws DBException {
        // Take database name from original config. Because it may change when user switch between databases.
        DBPConnectionConfiguration originalConfiguration = dataSource.getConnectionConfiguration();
        String conHostName = originalConfiguration.getHostName();
        String sshHost = null;
        if (CommonUtils.isEmpty(conHostName) || conHostName.equals(DBConstants.HOST_LOCALHOST) || conHostName.equals(DBConstants.HOST_LOCALHOST_IP)) {
            sshHost = getSSHHost(dataSource);
        }
        final String providerProperty = dataSource.getConnectionConfiguration()
            .getProviderProperty(PostgreConstants.PG_PASS_HOSTNAME);
        if (!CommonUtils.isEmpty(providerProperty)) {
            sshHost = providerProperty;
        }
        String pgPassPath = System.getenv(PGPASSFILE_ENV_VARIABLE);
        if (CommonUtils.isEmpty(pgPassPath)) {
            if (RuntimeUtils.isWindows()) {
                String appData = System.getenv("AppData");
                if (appData == null) {
                    appData = System.getProperty("user.home");
                }
                pgPassPath = appData + "/postgresql/pgpass.conf";
            } else {
                pgPassPath = System.getProperty("user.home") + "/.pgpass";
            }
        }
        Path pgPassFile = Path.of(pgPassPath);
        if (!Files.exists(pgPassFile)) {
            throw new DBException("PgPass file '" + pgPassFile + "' not found");
        }

        try (Reader r = Files.newBufferedReader(pgPassFile, GeneralUtils.UTF8_CHARSET)) {
            String passString = IOUtils.readToString(r);
            String[] lines = passString.split("\n");
            if (findHostCredentials(credentials, configuration, dataSource, sshHost, lines)) {
                return;
            } else if (findHostCredentials(credentials, configuration, dataSource, conHostName, lines)) {
                return;
            }
        } catch (IOException e) {
            throw new DBException("Error reading pgpass at '" + pgPassFile + "'", e);
        }

        throw new DBException("No matches in pgpass");
    }

    private boolean findHostCredentials(
        @NotNull AuthModelPgPassCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @Nullable String hostName,
        @NotNull String[] lines) {
        if (hostName == null) {
            return false;
        }
        DBPConnectionConfiguration originalConfiguration = dataSourceContainer.getConnectionConfiguration();
        String conHostPort = originalConfiguration.getHostPort();
        String conDatabaseName = PostgreUtils.getDatabaseNameFromConfiguration(originalConfiguration);
        String conUserName = originalConfiguration.getUserName();
        if (CommonUtils.isEmpty(conHostPort)) {
            conHostPort = dataSourceContainer.getDriver().getDefaultPort();
        }
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // Escape colons
            String[] params = splitPassLine(line);
            if (params == null) {
                continue;
            }
            String host = params[0];
            String port = params[1];
            String database = params[2];
            String user = params[3];
            String password = params[4];

            if (matchParam(hostName, host)
                && matchParam(conHostPort, port)
                && matchParam(conDatabaseName, database)) {
                if (CommonUtils.isEmpty(conUserName)) {
                    // No user name specified. Get the first matched params
                    //configuration.setUserName(user);
                    //configuration.setUserPassword(password);
                    credentials.setUserName(user);
                    credentials.setUserPassword(password);
                    return true;
                } else if (matchParam(conUserName, user)) {
                    if (!user.equals("*")) {
                        configuration.setUserName(user);
                    }
                    credentials.setUserPassword(password);
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static String[] splitPassLine(String line) {
        line = line.replace("\\\\", "@BSESC@").replace("\\:", "@CESC@");
        String[] params = line.split(":");
        if (params.length < 5) {
            return null;
        }
        // Unescape colons
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].replace("@CESC@", ":").replace("@BSESC@", "\\");
        }
        return params;
    }

    private static boolean matchParam(String cfgParam, String passParam) {
        return passParam.equals("*") || passParam.equalsIgnoreCase(cfgParam);
    }
}
