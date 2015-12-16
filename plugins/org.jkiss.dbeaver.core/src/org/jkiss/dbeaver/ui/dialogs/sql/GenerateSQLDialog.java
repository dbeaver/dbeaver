/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

public abstract class GenerateSQLDialog extends BaseSQLDialog {

    private final DBCExecutionContext executionContext;
    private Runnable onSuccess;

    protected SelectionListener SQL_CHANGE_LISTENER = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            updateSQL();
        }
    };

    public GenerateSQLDialog(IWorkbenchPartSite parentSite, DBCExecutionContext executionContext, String title, @Nullable DBPImage image)
    {
        super(parentSite, title, image);
        this.executionContext = executionContext;
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
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);
        Composite controlsPanel = UIUtils.createPlaceholder(composite, 1, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.heightHint = 300;
        controlsPanel.setLayoutData(gd);
        createControls(controlsPanel);
        Label divLabel = new Label(controlsPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        divLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createSQLPanel(controlsPanel);

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createCopyButton(parent);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.OK_ID) {
            setReturnCode(IDialogConstants.OK_ID);
            executeSQL();
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected void executeSQL()
    {
        final String jobName = getShell().getText();
        final String[] scriptLines = generateSQLScript();
        DataSourceJob job = new DataSourceJob(jobName, null, executionContext) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
                    for (String line : scriptLines) {
                        DBCStatement statement = DBUtils.prepareStatement(session, line, false);
                        try {
                            statement.executeStatement();
                        } finally {
                            statement.close();
                        }
                    }
                } catch (DBCException e) {
                    return GeneralUtils.makeExceptionStatus(e);
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
    protected DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Override
    protected String getSQLText()
    {
        DBPDataSource dataSource = executionContext.getDataSource();
        if (dataSource instanceof SQLDataSource) {
            String lineSeparator = GeneralUtils.getDefaultLineSeparator();
            String scriptDelimiter = ((SQLDataSource)dataSource).getSQLDialect().getScriptDelimiter() + lineSeparator;
            String[] scriptLines = generateSQLScript();
            StringBuilder sql = new StringBuilder(scriptLines.length * 64);
            for (String line : scriptLines) {
                sql.append(line).append(scriptDelimiter);
            }
            // Cut last line separator
            if (sql.length() > lineSeparator.length()) {
                sql.setLength(sql.length() - lineSeparator.length());
            }
            return sql.toString();
        } else {
            return "-- Not-SQL data source";
        }
    }

    protected abstract void createControls(Composite parent);

    protected abstract String[] generateSQLScript();

}