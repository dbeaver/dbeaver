/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.tools;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Abstract wizard
 */
public abstract class AbstractToolWizard<BASE_OBJECT extends DBSObject>
        extends Wizard implements DBRRunnableWithProgress {

    static final Log log = Log.getLog(AbstractToolWizard.class);

    private final BASE_OBJECT databaseObject;
    private DBPClientHome clientHome;
    private DBPConnectionConfiguration connectionInfo;
    private String toolUserName;
    private String toolUserPassword;

    protected String task;
    protected final DatabaseWizardPageLog logPage;
    private boolean finished;

    protected AbstractToolWizard(BASE_OBJECT databaseObject, String task)
    {
        this.databaseObject = databaseObject;
        this.task = task;
        this.logPage = new DatabaseWizardPageLog(task);
    }

    @Override
    public boolean canFinish()
    {
        return !finished && super.canFinish();
    }

    public BASE_OBJECT getDatabaseObject()
    {
        return databaseObject;
    }

    public DBPConnectionConfiguration getConnectionInfo()
    {
        return connectionInfo;
    }

    public DBPClientHome getClientHome()
    {
        return clientHome;
    }

    public String getToolUserName()
    {
        return toolUserName;
    }

    public void setToolUserName(String toolUserName)
    {
        this.toolUserName = toolUserName;
    }

    public String getToolUserPassword()
    {
        return toolUserPassword;
    }

    public void setToolUserPassword(String toolUserPassword)
    {
        this.toolUserPassword = toolUserPassword;
    }

    public abstract DBPClientHome findServerHome(String clientHomeId);

    @Override
    public void createPageControls(Composite pageContainer)
    {
        DBPDataSourceContainer container = databaseObject.getDataSource().getContainer();
        connectionInfo = container.getActualConnectionConfiguration();

        super.createPageControls(pageContainer);

        WizardPage currentPage = (WizardPage) getStartingPage();

        String clientHomeId = connectionInfo.getClientHomeId();
        if (clientHomeId == null) {
            currentPage.setErrorMessage(CoreMessages.tools_wizard_message_no_client_home);
            getContainer().updateMessage();
            return;
        }
        clientHome = findServerHome(clientHomeId);//MySQLDataSourceProvider.getServerHome(clientHomeId);
        if (clientHome == null) {
            currentPage.setErrorMessage(NLS.bind(CoreMessages.tools_wizard_message_client_home_not_found, clientHomeId));
            getContainer().updateMessage();
        }
    }

    @Override
    public boolean performFinish() {
        if (getContainer().getCurrentPage() != logPage) {
            getContainer().showPage(logPage);
        }
        try {
            RuntimeUtils.run(getContainer(), true, true, this);
        }
        catch (InterruptedException ex) {
            UIUtils.showMessageBox(getShell(), task, NLS.bind(CoreMessages.tools_wizard_error_task_canceled, task, databaseObject.getName()), SWT.ICON_ERROR);
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                NLS.bind(CoreMessages.tools_wizard_error_task_error_title, task),
                CoreMessages.tools_wizard_error_task_error_message + task,
                ex.getTargetException());
            return false;
        }
        finally {
            getContainer().updateButtons();

        }
        onSuccess();
        return false;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        try {
            finished = executeProcess(monitor);
            // Refresh navigator node (script execution can change everything inside)
            final DBNDatabaseNode node = databaseObject.getDataSource().getContainer().getApplication().getNavigatorModel().findNode(databaseObject);
            if (node != null) {
                node.refreshNode(monitor, AbstractToolWizard.this);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    public boolean executeProcess(DBRProgressMonitor monitor)
        throws IOException, CoreException, InterruptedException
    {
        try {
            final List<String> commandLine = getCommandLine();
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            Process process = processBuilder.start();

            startProcessHandler(monitor, processBuilder, process);
            Thread.sleep(100);

            for (;;) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        logPage.appendLog(NLS.bind(CoreMessages.tools_wizard_log_process_exit_code, exitCode));
                        return false;
                    }
                } catch (Exception e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            log.error(e);
            logPage.appendLog(NLS.bind(CoreMessages.tools_wizard_log_io_error, e.getMessage()));
            return false;
        }

        return true;
    }

    protected boolean isMergeProcessStreams()
    {
        return false;
    }

    public boolean isVerbose()
    {
        return false;
    }

    protected void onSuccess()
    {

    }

    abstract protected java.util.List<String> getCommandLine() throws IOException;

    public abstract void fillProcessParameters(List<String> cmd) throws IOException;

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process);

}
