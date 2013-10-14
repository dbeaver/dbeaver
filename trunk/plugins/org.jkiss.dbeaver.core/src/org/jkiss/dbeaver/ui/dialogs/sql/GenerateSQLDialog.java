/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

public abstract class GenerateSQLDialog extends BaseSQLDialog {

    private DBPDataSource dataSource;
    private Runnable onSuccess;

    public GenerateSQLDialog(IWorkbenchPartSite parentSite, DBPDataSource dataSource, String title, Image image)
    {
        super(parentSite, title, image);
        this.dataSource = dataSource;
    }

    public void setOnSuccess(Runnable onSuccess)
    {
        this.onSuccess = onSuccess;
    }

    @Override
    protected boolean isWordWrap()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        SashForm sashForm = new SashForm(composite, SWT.VERTICAL | SWT.SMOOTH);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        sashForm.setSashWidth(5);
        Composite controlsPanel = UIUtils.createPlaceholder(sashForm, 1);
        controlsPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        createControls(controlsPanel);
        createSQLPanel(sashForm);
        //sashForm.setWeights(new int[] {800, 200});

        return sashForm;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.PROCEED_ID, CoreMessages.dialog_view_sql_button_persist, true);
        createCopyButton(parent);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.PROCEED_ID) {
            setReturnCode(IDialogConstants.PROCEED_ID);
            executeSQL();
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void executeSQL()
    {
        final String jobName = getShell().getText();
        final String[] scriptLines = generateSQLScript();
        DataSourceJob job = new DataSourceJob(getShell().getText(), null, dataSource) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                DBCSession session = getDataSource().openSession(monitor, DBCExecutionPurpose.UTIL, jobName);
                try {
                    for (String line : scriptLines) {
                        DBCStatement statement = DBUtils.prepareStatement(session, line);
                        try {
                            statement.executeStatement();
                        } finally {
                            statement.close();
                        }
                    }
                } catch (DBCException e) {
                    return RuntimeUtils.makeExceptionStatus(e);
                } finally {
                    session.close();
                }
                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                if (event.getResult().isOK()) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }
            }
        });
        job.schedule();
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    protected String getSQLText()
    {
        String scriptDelimiter = dataSource.getInfo().getScriptDelimiter() + ContentUtils.getDefaultLineSeparator();
        String[] scriptLines = generateSQLScript();
        StringBuilder sql = new StringBuilder(scriptLines.length * 64);
        for (String line : scriptLines) {
            sql.append(line).append(scriptDelimiter);
        }
        return sql.toString();
    }

    protected abstract void createControls(Composite parent);

    protected abstract String[] generateSQLScript();

}