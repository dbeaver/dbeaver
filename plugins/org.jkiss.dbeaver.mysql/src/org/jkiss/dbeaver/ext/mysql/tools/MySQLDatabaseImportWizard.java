/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractDatabaseImportWizard;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

class MySQLDatabaseImportWizard extends AbstractDatabaseImportWizard<MySQLCatalog, MySQLServerHome>
{
    public MySQLDatabaseImportWizard(MySQLCatalog catalog, boolean isImport) {
        super(catalog, isImport);
	}

    @Override
    public void fillProcessParameters(List<String> cmd)
    {
        String dumpPath = new File(getServerHome().getHomePath(), "bin/mysql").getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    public MySQLServerHome findServerHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine()
    {
        return MySQLToolScript.getMySQLToolCommandLine(this);
    }
}
