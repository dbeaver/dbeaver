/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.postgresql.tools;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreServerHome;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PostgreScriptExecuteWizard extends AbstractScriptExecuteWizard<PostgreSchema, PostgreSchema> {

    enum LogLevel {
        Normal,
        Verbose,
        Debug
    }

    private LogLevel logLevel;
    private boolean noBeep;

    private boolean isImport;
    private PostgreScriptExecuteWizardPageSettings mainPage;

    public PostgreScriptExecuteWizard(PostgreSchema catalog, boolean isImport)
    {
        super(Collections.singleton(catalog), isImport ? "Import database" : "Execute script");
        this.isImport = isImport;
        this.logLevel = LogLevel.Normal;
        this.noBeep = true;
        this.mainPage = new PostgreScriptExecuteWizardPageSettings(this);
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
    public void fillProcessParameters(List<String> cmd, PostgreSchema arg) throws IOException
    {
        String dumpPath = RuntimeUtils.getHomeBinary(getClientHome(), PostgreConstants.BIN_FOLDER, "psql").getAbsolutePath(); //$NON-NLS-1$
        cmd.add(dumpPath);
        if (logLevel == LogLevel.Debug) {
            cmd.add("--debug-info"); //$NON-NLS-1$
        }
        if (noBeep) {
            cmd.add("--no-beep"); //$NON-NLS-1$
        }
    }

    @Override
    public PostgreServerHome findServerHome(String clientHomeId)
    {
        return PostgreDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public Collection<PostgreSchema> getRunInfo() {
        return getDatabaseObjects();
    }

    @Override
    protected List<String> getCommandLine(PostgreSchema arg) throws IOException
    {
        List<String> cmd = PostgreToolScript.getPostgreToolCommandLine(this, arg);
        cmd.add(arg.getName());
        return cmd;
    }
}
