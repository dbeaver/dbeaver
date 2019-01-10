/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.ext.mysql.tools;

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class MySQLScriptExecuteWizard extends AbstractScriptExecuteWizard<MySQLCatalog, MySQLCatalog> {

    enum LogLevel {
        Normal,
        Verbose,
        Debug
    }

    private LogLevel logLevel;
    private boolean noBeep;

    private boolean isImport;
    private MySQLScriptExecuteWizardPageSettings mainPage;

    public MySQLScriptExecuteWizard(MySQLCatalog catalog, boolean isImport)
    {
        super(Collections.singleton(catalog), isImport ? MySQLMessages.tools_script_execute_wizard_db_import : MySQLMessages.tools_script_execute_wizard_execute_script);
        this.isImport = isImport;
        this.logLevel = LogLevel.Normal;
        this.noBeep = true;
        this.mainPage = new MySQLScriptExecuteWizardPageSettings(this);
    }

    public LogLevel getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel)
    {
        this.logLevel = logLevel;
    }

    public boolean isImport()
    {
        return isImport;
    }

    @Override
    public boolean isVerbose()
    {
        return logLevel == LogLevel.Verbose || logLevel == LogLevel.Debug;
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd, MySQLCatalog arg) throws IOException
    {
        String dumpPath = RuntimeUtils.getNativeClientBinary(getClientHome(), MySQLConstants.BIN_FOLDER, "mysql").getAbsolutePath(); //$NON-NLS-1$
        cmd.add(dumpPath);
        if (logLevel == LogLevel.Debug) {
            cmd.add("--debug-info"); //$NON-NLS-1$
        }
        if (noBeep) {
            cmd.add("--no-beep"); //$NON-NLS-1$
        }
        addExtraCommandArgs(cmd);
    }

    @Override
    protected void setupProcessParameters(ProcessBuilder process) {
        if (!CommonUtils.isEmpty(getToolUserPassword())) {
            process.environment().put(MySQLConstants.ENV_VARIABLE_MYSQL_PWD, getToolUserPassword());
        }
    }

    @Override
    public MySQLServerHome findNativeClientHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public Collection<MySQLCatalog> getRunInfo() {
        return getDatabaseObjects();
    }

    @Override
    protected List<String> getCommandLine(MySQLCatalog arg) throws IOException
    {
        List<String> cmd = MySQLToolScript.getMySQLToolCommandLine(this, arg);
        cmd.add(arg.getName());
        return cmd;
    }

    /**
     * Use binary file transform job (#2863)
     */
    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final MySQLCatalog arg, ProcessBuilder processBuilder, Process process) {
        if (isImport) {
            logPage.startLogReader(
                processBuilder,
                process.getInputStream());
            new BinaryFileTransformerJob(monitor, getInputFile(), process.getOutputStream()).start();
        } else {
            super.startProcessHandler(monitor, arg, processBuilder, process);
        }
    }

}
