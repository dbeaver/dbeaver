/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
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
    public Object initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, AuthModelPgPassCredentials credentials, DBPConnectionConfiguration configuration, @NotNull Properties connectProps) throws DBException {
        if (credentials.getParseError() != null) {
            throw new DBCException("Couldn't get password from PGPASS file", credentials.getParseError());
        }
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connectProps);
    }

    private void loadPasswordFromPgPass(AuthModelPgPassCredentials credentials, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) throws DBException {
        String conHostName = configuration.getHostName();
        String conHostPort = configuration.getHostPort();
        String conDatabaseName = configuration.getDatabaseName();
        String conUserName = configuration.getUserName();

        if (CommonUtils.isEmpty(conHostPort)) {
            conHostPort = dataSource.getDriver().getDefaultPort();
        }

        String pgPassPath = System.getenv(PGPASSFILE_ENV_VARIABLE);
        if (CommonUtils.isEmpty(pgPassPath)) {
            if (RuntimeUtils.isPlatformWindows()) {
                String appData = System.getenv("AppData");
                if (appData == null) {
                    appData = System.getProperty("user.home");
                }
                pgPassPath = appData + "/postgresql/pgpass.conf";
            } else {
                pgPassPath = System.getProperty("user.home") + "/.pgpass";
            }
        }
        File pgPassFile = new File(pgPassPath);
        if (!pgPassFile.exists()) {
            throw new DBException("PgPass file '" + pgPassFile.getAbsolutePath() + "' not found");
        }

        try (Reader r = new InputStreamReader(new FileInputStream(pgPassFile), GeneralUtils.UTF8_CHARSET)) {
            String passString = IOUtils.readToString(r);
            String[] lines = passString.split("\n");
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

                if (matchParam(conHostName, host) &&
                    matchParam(conHostPort, port) &&
                    matchParam(conDatabaseName, database) &&
                    matchParam(conUserName, user))
                {
                    if (!user.equals("*")) {
                        configuration.setUserName(user);
                    }
                    credentials.setUserPassword(password);
                    return;
                }
            }
        } catch (IOException e) {
            throw new DBException("Error reading pgpass", e);
        }

        throw new DBException("No matches in pgpass");
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
