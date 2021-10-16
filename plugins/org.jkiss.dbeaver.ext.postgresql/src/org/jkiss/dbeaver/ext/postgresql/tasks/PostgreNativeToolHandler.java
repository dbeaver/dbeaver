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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class PostgreNativeToolHandler<SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG>
    extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {

    public static final boolean USE_STREAM_MONITOR = false;

    @Override
    protected void setupProcessParameters(DBRProgressMonitor monitor, SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
        String userPassword = settings.getToolUserPassword();
        if (CommonUtils.isEmpty(userPassword)) {
            userPassword = getDataSourcePassword(monitor, settings);
        }
        if (!CommonUtils.isEmpty(userPassword)) {
            process.environment().put("PGPASSWORD", userPassword);
        }
    }

    @Override
    public void fillProcessParameters(SETTINGS settings, PROCESS_ARG process_arg, List<String> cmd) throws IOException {
        File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), PostgreConstants.BIN_FOLDER,
            this instanceof PostgreDatabaseBackupHandler ? "pg_dump" :
                this instanceof PostgreDatabaseRestoreHandler ? "pg_restore" : "psql"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);

        if (isVerbose()) {
            cmd.add("--verbose");
        }
        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }
        String toolUserName = settings.getToolUserName();
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = connectionInfo.getUserName();
        }
        cmd.add("--username=" + toolUserName);

        settings.addExtraCommandArgs(cmd);
    }

    public boolean isVerbose() {
        return false;
    }

    protected abstract boolean isExportWizard();

}
