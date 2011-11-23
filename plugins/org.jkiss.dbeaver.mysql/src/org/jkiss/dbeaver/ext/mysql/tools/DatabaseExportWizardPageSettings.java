/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class DatabaseExportWizardPageSettings extends AbstractToolWizardPage<DatabaseExportWizard> {

    private Text outputFileText;
    private Combo methodCombo;
    private Button noCreateStatementsCheck;
    private Button addDropStatementsCheck;
    private Button disableKeysCheck;
    private Button extendedInsertsCheck;
    private Button dumpEventsCheck;
    private Button commentsCheck;

    protected DatabaseExportWizardPageSettings(DatabaseExportWizard wizard)
    {
        super(wizard, "Export configuration");
        setTitle("Export configuration");
        setDescription(("Set database export settings"));
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.outputFile != null;
    }

    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group methodGroup = UIUtils.createControlGroup(composite, "Execution Method", 1, GridData.FILL_HORIZONTAL, 0);
        methodCombo = new Combo(methodGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        methodCombo.add("Online backup in single transaction");
        methodCombo.add("Lock all tables");
        methodCombo.add("Normal (no locks)");
        methodCombo.select(wizard.method.ordinal());

        Group settingsGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
        noCreateStatementsCheck = UIUtils.createCheckbox(settingsGroup, "No CREATE statements", wizard.noCreateStatements);
        addDropStatementsCheck = UIUtils.createCheckbox(settingsGroup, "Add DROP statements", wizard.addDropStatements);
        disableKeysCheck = UIUtils.createCheckbox(settingsGroup, "Disable keys", wizard.disableKeys);
        extendedInsertsCheck = UIUtils.createCheckbox(settingsGroup, "Extended inserts", wizard.extendedInserts);
        dumpEventsCheck = UIUtils.createCheckbox(settingsGroup, "Dump events", wizard.dumpEvents);
        commentsCheck = UIUtils.createCheckbox(settingsGroup, "Additional comments", wizard.comments);

        Group outputGroup = UIUtils.createControlGroup(composite, "Output", 3, GridData.FILL_HORIZONTAL, 0);
        outputFileText = UIUtils.createLabelText(outputGroup, "Output File", "");
        Button browseButton = new Button(outputGroup, SWT.PUSH);
        browseButton.setText("Browse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                File file = ContentUtils.selectFileForSave(getShell(), "Choose output file", new String[]{"*.sql", "*.txt", "*.*"}, outputFileText.getText());
                if (file != null) {
                    outputFileText.setText(file.getAbsolutePath());
                }
                updateState();
            }
        });
        if (wizard.outputFile != null) {
            outputFileText.setText(wizard.outputFile.getName());
        }

        setControl(composite);

        //updateState();
    }

    private void updateState()
    {
        String fileName = outputFileText.getText();
        wizard.outputFile = CommonUtils.isEmpty(fileName) ? null : new File(fileName);
        switch (methodCombo.getSelectionIndex()) {
            case 0: wizard.method = DatabaseExportWizard.DumpMethod.ONLINE; break;
            case 1: wizard.method = DatabaseExportWizard.DumpMethod.LOCK_ALL_TABLES; break;
            default: wizard.method = DatabaseExportWizard.DumpMethod.NORMAL; break;
        }
        wizard.noCreateStatements = noCreateStatementsCheck.getSelection();
        wizard.addDropStatements = addDropStatementsCheck.getSelection();
        wizard.disableKeys = disableKeysCheck.getSelection();
        wizard.extendedInserts = extendedInsertsCheck.getSelection();
        wizard.dumpEvents = dumpEventsCheck.getSelection();
        wizard.comments = commentsCheck.getSelection();

        getContainer().updateButtons();
    }

}
