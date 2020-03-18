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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.Properties;

public class AuthModelPgPass implements DBAAuthModel {

    private static final Log log = Log.getLog(AuthModelPgPass.class);
    public static final String PGPASSFILE_ENV_VARIABLE = "PGPASSFILE";

    @Override
    public void initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) throws DBException {
        loadPasswordFromPgPass(configuration);
    }

    @Override
    public void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) {

    }

    private void loadPasswordFromPgPass(DBPConnectionConfiguration configuration) throws DBException {
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
                String[] params = line.split(":");
                if (params.length < 5) {
                    continue;
                }
                String host = params[0];
                String port = params[1];
                String database = params[2];
                String user = params[3];
                String password = params[4];

                if (matchParam(configuration.getHostName(), host) &&
                    matchParam(configuration.getHostPort(), port) &&
                    matchParam(configuration.getDatabaseName(), database) &&
                    matchParam(configuration.getUserName(), user))
                {
                    if (!user.equals("*")) {
                        configuration.setUserName(user);
                    }
                    configuration.setUserPassword(password);
                    return;
                }
            }
        } catch (IOException e) {
            throw new DBException("Error reading pgpass", e);
        }

        throw new DBException("No matches in pgpass");
    }

    private static boolean matchParam(String cfgParam, String passParam) {
        return passParam.equals("*") || passParam.equalsIgnoreCase(cfgParam);
    }


}
