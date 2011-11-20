/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;

/**
 * Database dump
 */
public class MySQLToolDump extends MySQLToolAbstract {

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    private MySQLCatalog catalog;
    private File outputFile;
    private DumpMethod method;
    private boolean noCreateStatements;
    private boolean addDropStatements;
    private boolean disableKeys;
    private boolean noExtendedInserts;
    private boolean dumpEvents;
    private boolean comments;

    private DumpJob dumpJob;
    private final Object dumpSync = new Object();

    @Override
    public void execute(IWorkbenchWindow window, DBPObject object) throws DBException
    {
        if (object instanceof MySQLCatalog) {
            dumpCatalog(window, (MySQLCatalog)object);
        }
    }

    private void dumpCatalog(IWorkbenchWindow window, MySQLCatalog object)
    {
        this.catalog = object;
        method = DumpMethod.ONLINE;
        outputFile = new File(catalog.getName() + "-" + RuntimeUtils.getCurrentTimeStamp() + ".sql");

        DumpSettingsDialog settingsDialog = new DumpSettingsDialog(window.getShell());
        if (settingsDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        DumpProgressDialog progressDialog = new DumpProgressDialog(window.getShell());
        progressDialog.open();
    }

    /**
     * Dump settings
     */
    class DumpSettingsDialog extends HelpEnabledDialog {

        private Text outputFileText;
        private Combo methodCombo;
        private Button noCreateStatementsCheck;
        private Button addDropStatementsCheck;
        private Button disableKeysCheck;
        private Button noExtendedInsertsCheck;
        private Button dumpEventsCheck;
        private Button commentsCheck;

        protected DumpSettingsDialog(Shell shell)
        {
            super(shell, "");
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Dump settings");

            Composite composite = (Composite) super.createDialogArea(parent);

            Group methodGroup = UIUtils.createControlGroup(composite, "Execution Method", 1, GridData.FILL_HORIZONTAL, 0);
            methodCombo = new Combo(methodGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            methodCombo.add("Online backup in single transaction");
            methodCombo.add("Lock all tables");
            methodCombo.add("Normal (no locks)");
            methodCombo.select(method.ordinal());

            Group settingsGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
            noCreateStatementsCheck = UIUtils.createCheckbox(settingsGroup, "No CREATE statements", noCreateStatements);
            addDropStatementsCheck = UIUtils.createCheckbox(settingsGroup, "Add DROP statements", addDropStatements);
            disableKeysCheck = UIUtils.createCheckbox(settingsGroup, "Disable keys", disableKeys);
            noExtendedInsertsCheck = UIUtils.createCheckbox(settingsGroup, "No extended inserts", noExtendedInserts);
            dumpEventsCheck = UIUtils.createCheckbox(settingsGroup, "Dump events", dumpEvents);
            commentsCheck = UIUtils.createCheckbox(settingsGroup, "Additional comments", comments);

            Group outputGroup = UIUtils.createControlGroup(composite, "Output", 3, GridData.FILL_HORIZONTAL, 0);
            outputFileText = UIUtils.createLabelText(outputGroup, "Output File", "");
            Button browseButton = new Button(outputGroup, SWT.PUSH);
            browseButton.setText("Browse");
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    File file = ContentUtils.selectFileForSave(getShell(), "Choose output file", new String[]{"*.sql", "*.txt", "*.*"}, outputFileText.getText());
                    outputFileText.setText(file.getAbsolutePath());
                }
            });
            outputFileText.setText(outputFile.getName());

            return parent;
        }

        @Override
        protected void okPressed()
        {
            outputFile = new File(outputFileText.getText());
            switch (methodCombo.getSelectionIndex()) {
                case 0: method = DumpMethod.ONLINE; break;
                case 1: method = DumpMethod.LOCK_ALL_TABLES; break;
                default: method = DumpMethod.NORMAL; break;
            }
            noCreateStatements = noCreateStatementsCheck.getSelection();
            addDropStatements = addDropStatementsCheck.getSelection();
            disableKeys = disableKeysCheck.getSelection();
            noExtendedInserts = noExtendedInsertsCheck.getSelection();
            dumpEvents = dumpEventsCheck.getSelection();
            comments = commentsCheck.getSelection();

            super.okPressed();
        }
    }

    class DumpProgressDialog extends org.eclipse.jface.dialogs.Dialog {

        private StyledText dumpLogText;
        private Label statusLabel;

        public DumpProgressDialog(Shell parent)
        {
            super(parent);
            setShellStyle(SWT.SHELL_TRIM);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Dump progress");

            Composite composite = (Composite) super.createDialogArea(parent);

            dumpLogText = new StyledText(composite, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 600;
            gd.heightHint = 400;
            dumpLogText.setLayoutData(gd);

            dumpJob = new DumpJob(this);
            dumpJob.schedule(1000);

            return composite;
        }

        protected boolean canHandleShellCloseEvent()
        {
            return false;
        }

        protected Control createButtonBar(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            layout.makeColumnsEqualWidth = false;
            layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
            layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
            layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
            composite.setLayout(layout);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(data);
            composite.setFont(parent.getFont());

            // Add the buttons to the button bar.
            createButtonsForButtonBar(composite);
            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            statusLabel = new Label(parent, SWT.NONE);
            statusLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
            createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        }

        @Override
        protected void buttonPressed(int buttonId)
        {
            if (buttonId == IDialogConstants.CANCEL_ID) {
                synchronized (dumpSync) {
                    if (dumpJob != null && dumpJob.getState() == Job.RUNNING) {
                        if (UIUtils.confirmAction(getShell(), "Cancel database dump", "Are you sure you want to cancel database dump? Dump file may be corrupted")) {
                            dumpJob.stop();
                        }
                    }
                }
            }
            super.buttonPressed(buttonId);
        }

        public void finishDump()
        {
            UIUtils.runInUI(getShell(), new Runnable() {
                public void run()
                {
                    Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
                    if (cancelButton != null && !cancelButton.isDisposed()) {
                        cancelButton.setText(IDialogConstants.CLOSE_LABEL);
                    }
                }
            });
        }
    }

    class DumpJob extends AbstractJob {
        private DumpProgressDialog progressDialog;
        protected DumpJob(DumpProgressDialog progressDialog)
        {
            super("Dump database " + catalog.getName());
            this.progressDialog = progressDialog;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            progressDialog.finishDump();
            synchronized (dumpSync) {
                dumpJob = null;
            }
            return Status.OK_STATUS;
        }

        public void stop()
        {

        }
    }

}
