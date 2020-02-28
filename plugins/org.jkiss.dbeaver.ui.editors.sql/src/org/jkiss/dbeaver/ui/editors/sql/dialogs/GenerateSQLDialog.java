/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

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
        DataSourceJob job = new DataSourceJob(jobName, executionContext) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
                    for (String line : scriptLines) {
                        DBCStatement statement = DBUtils.makeStatement(session, line, false);
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
        {
            String lineSeparator = GeneralUtils.getDefaultLineSeparator();
            String scriptDelimiter = dataSource.getSQLDialect().getScriptDelimiter() + lineSeparator;
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
        }
    }

    protected abstract void createControls(Composite parent);

    protected abstract String[] generateSQLScript();

}