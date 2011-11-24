/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract wizard
 */
public abstract class AbstractToolWizard extends Wizard implements DBRRunnableWithProgress {

    static final Log log = LogFactory.getLog(AbstractToolWizard.class);

    private final MySQLCatalog catalog;
    private MySQLServerHome serverHome;
    private DBPConnectionInfo connectionInfo;

    protected String task;
    protected final DatabaseWizardPageLog logPage;
    private boolean finished;


    protected AbstractToolWizard(MySQLCatalog catalog, String task)
    {
        this.catalog = catalog;
        this.task = task;
        this.logPage = new DatabaseWizardPageLog(task);
    }

    @Override
    public boolean canFinish()
    {
        return !finished && super.canFinish();
    }

    public MySQLCatalog getCatalog()
    {
        return catalog;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public MySQLServerHome getServerHome()
    {
        return serverHome;
    }

    @Override
    public void createPageControls(Composite pageContainer)
    {
        super.createPageControls(pageContainer);

        WizardPage currentPage = (WizardPage) getStartingPage();

        DBSDataSourceContainer container = getCatalog().getDataSource().getContainer();
        connectionInfo = container.getConnectionInfo();
        String clientHomeId = connectionInfo.getClientHomeId();
        if (clientHomeId == null) {
            currentPage.setErrorMessage("Server home is not specified for connection");
            getContainer().updateMessage();
            return;
        }
        serverHome = MySQLDataSourceProvider.getServerHome(clientHomeId);
        if (serverHome == null) {
            currentPage.setErrorMessage("Server home '" + clientHomeId + "' not found");
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
            UIUtils.showMessageBox(getShell(), task, task + " '" + getCatalog().getName() + "' canceled", SWT.ICON_ERROR);
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                task + " error",
                "Cannot perform " + task,
                ex.getTargetException());
            return false;
        }
        finally {
            getContainer().updateButtons();
        }
        onSuccess();
        return false;
    }

    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        try {
            executeProcess(monitor);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            finished = true;
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    public boolean executeProcess(DBRProgressMonitor monitor)
        throws IOException, CoreException, InterruptedException
    {
        java.util.List<String> cmd = new ArrayList<String>();
        fillProcessParameters(cmd);

        cmd.add("-v");
        cmd.add("-q");
        cmd.add("--host=" + getConnectionInfo().getHostName());
        if (!CommonUtils.isEmpty(getConnectionInfo().getHostPort())) {
            cmd.add("--port=" + getConnectionInfo().getHostPort());
        }
        cmd.add("-u");
        cmd.add(getConnectionInfo().getUserName());
        cmd.add("--password=" + getConnectionInfo().getUserPassword());

        cmd.add(getCatalog().getName());

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();
            startProcessHandler(monitor, processBuilder, process);
            logPage.startLogReader(processBuilder, process);

            for (;;) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    process.exitValue();
                } catch (Exception e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            log.error(e);
            logPage.setErrorMessage(e.getMessage());
            return false;
        }

        return true;
    }

    protected void onSuccess()
    {

    }

    protected abstract void fillProcessParameters(List<String> cmd);

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process);

}
