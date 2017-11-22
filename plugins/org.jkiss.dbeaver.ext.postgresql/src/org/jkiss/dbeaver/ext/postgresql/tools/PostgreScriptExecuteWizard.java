/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.tools;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreServerHome;
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PostgreScriptExecuteWizard extends AbstractScriptExecuteWizard<PostgreDatabase, PostgreDatabase> {

    private boolean isImport;
    private PostgreScriptExecuteWizardPageSettings mainPage;

    PostgreScriptExecuteWizard(PostgreDatabase catalog, boolean isImport)
    {
        super(Collections.singleton(catalog), isImport ? PostgresMessages.wizard_script_title_import_db : PostgresMessages.wizard_script_title_execute_script);
        this.isImport = isImport;
        this.mainPage = new PostgreScriptExecuteWizardPageSettings(this);
    }

    public boolean isImport()
    {
        return isImport;
    }

    @Override
    public boolean isVerbose()
    {
        return false;
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabase arg) throws IOException
    {
        String dumpPath = RuntimeUtils.getHomeBinary(getClientHome(), PostgreConstants.BIN_FOLDER, "psql").getAbsolutePath(); //$NON-NLS-1$
        cmd.add(dumpPath);
        cmd.add("--echo-errors"); //$NON-NLS-1$
    }

    @Override
    protected void setupProcessParameters(ProcessBuilder process) {
        super.setupProcessParameters(process);
        if (!CommonUtils.isEmpty(getToolUserPassword())) {
            process.environment().put("PGPASSWORD", getToolUserPassword());
        }
    }

    @Override
    public PostgreServerHome findServerHome(String clientHomeId)
    {
        return PostgreDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public Collection<PostgreDatabase> getRunInfo() {
        return getDatabaseObjects();
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabase arg) throws IOException
    {
        List<String> cmd = PostgreToolScript.getPostgreToolCommandLine(this, arg);
        cmd.add(arg.getName());
        return cmd;
    }
}
