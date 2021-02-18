/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public abstract class MySQLNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
        extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {

    private File config;

    @Override
    protected boolean doExecute(DBRProgressMonitor monitor, DBTTask task, SETTINGS settings, Log log) throws DBException, InterruptedException {
        try {
            return super.doExecute(monitor, task, settings, log);
        } finally {
            if (config != null && !config.delete()) {
                log.debug("Failed to delete configuration file");
            }
        }
    }

    @Override
    protected void setupProcessParameters(DBRProgressMonitor monitor, SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
        if (!isOverrideCredentials(settings)) {
            String toolUserPassword = settings.getToolUserPassword();
            if (CommonUtils.isEmpty(toolUserPassword)) {
                toolUserPassword = getDataSourcePassword(monitor, settings);
            }

            if (CommonUtils.isNotEmpty(toolUserPassword)) {
                process.environment().put(MySQLConstants.ENV_VAR_MYSQL_PWD, toolUserPassword);
            }
        }
    }

    @Override
    protected List<String> getCommandLine(SETTINGS settings, PROCESS_ARG arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);

        String toolUserName = settings.getToolUserName();
        String toolUserPassword = settings.getToolUserPassword();

        /*
         * Use credentials derived from connection configuration
         * if no username was specified by export configuration itself.
         * This is needed to avoid overriding empty password.
         */
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserName();
            toolUserPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        }

        if (isOverrideCredentials(settings)) {
            config = createCredentialsFile(toolUserName, toolUserPassword);
            cmd.add(1, "--defaults-file=" + config.getAbsolutePath());
        } else {
            cmd.add("-u");
            cmd.add(toolUserName);
        }

        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }

        return cmd;
    }

    private static File createCredentialsFile(String username, String password) throws IOException {
        File dir = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "mysql-native-handler"); //$NON-NLS-1$
        File cnf = new File(dir, ".my.cnf"); //$NON-NLS-1$
        cnf.deleteOnExit();

        try (Writer writer = new FileWriter(cnf)) {
            writer.write("[client]"); //$NON-NLS-1$
            writer.write("\nuser=" + CommonUtils.notEmpty(username)); //$NON-NLS-1$
            writer.write("\npassword=" + CommonUtils.notEmpty(password)); //$NON-NLS-1$
        }

        return cnf;
    }

    private boolean isOverrideCredentials(SETTINGS settings) {
        if (settings instanceof MySQLNativeCredentialsSettings) {
            return ((MySQLNativeCredentialsSettings) settings).isOverrideCredentials();
        }
        return false;
    }
}
