/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
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
public abstract class AbstractToolWizard<D extends DBSEntity, H extends JDBCClientHome> extends Wizard implements DBRRunnableWithProgress {

    static final Log log = LogFactory.getLog(AbstractToolWizard.class);

    private final D dbObject;
    private H serverHome;
    private DBPConnectionInfo connectionInfo;

    protected String task;
    protected final DatabaseWizardPageLog logPage;
    private boolean finished;


    protected AbstractToolWizard(D dbObject, String task)
    {
        this.dbObject = dbObject;
        this.task = task;
        this.logPage = new DatabaseWizardPageLog(task);
    }

    @Override
    public boolean canFinish()
    {
        return !finished && super.canFinish();
    }

    public D getDbObject()
    {
        return dbObject;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public H getServerHome()
    {
        return serverHome;
    }

    public abstract H findServerHome(String clientHomeId);

    @Override
    public void createPageControls(Composite pageContainer)
    {
        super.createPageControls(pageContainer);

        WizardPage currentPage = (WizardPage) getStartingPage();

        DBSDataSourceContainer container = getDbObject().getDataSource().getContainer();
        connectionInfo = container.getConnectionInfo();
        String clientHomeId = connectionInfo.getClientHomeId();
        if (clientHomeId == null) {
            currentPage.setErrorMessage("Server home is not specified for connection");
            getContainer().updateMessage();
            return;
        }
        serverHome = findServerHome(clientHomeId);//MySQLDataSourceProvider.getServerHome(clientHomeId);
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
            UIUtils.showMessageBox(getShell(), task, task + " '" + getDbObject().getName() + "' canceled", SWT.ICON_ERROR);
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

    abstract protected java.util.List<String> getCommandLine();

    public boolean executeProcess(DBRProgressMonitor monitor)
        throws IOException, CoreException, InterruptedException
    {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine());
            processBuilder.redirectErrorStream(this.isMergeProcessStreams());
            Process process = processBuilder.start();
            startProcessHandler(monitor, processBuilder, process);
            logPage.startLogReader(
                processBuilder,
                this.isMergeProcessStreams() ? process.getInputStream() : process.getErrorStream());

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

    public abstract void fillProcessParameters(List<String> cmd);

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process);

}
