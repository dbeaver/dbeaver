/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class DatabaseImportWizardPageSettings extends AbstractToolWizardPage<DatabaseImportWizard> {

    private Text inputFileText;

    protected DatabaseImportWizardPageSettings(DatabaseImportWizard wizard)
    {
        super(wizard, "Import configuration");
        setTitle("Import configuration");
        setDescription(("Set database import  settings"));
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.inputFile != null;
    }

    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group outputGroup = UIUtils.createControlGroup(composite, "Input", 3, GridData.FILL_HORIZONTAL, 0);
        inputFileText = UIUtils.createLabelText(outputGroup, "Input File", "");
        Button browseButton = new Button(outputGroup, SWT.PUSH);
        browseButton.setText("Browse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                File file = ContentUtils.openFile(getShell(), new String[]{"*.sql", "*.txt", "*.*"});
                if (file != null) {
                    inputFileText.setText(file.getAbsolutePath());
                }
                updateState();
            }
        });
        if (wizard.inputFile != null) {
            inputFileText.setText(wizard.inputFile.getName());
        }

        setControl(composite);

        //updateState();
    }

    private void updateState()
    {
        String fileName = inputFileText.getText();
        wizard.inputFile = CommonUtils.isEmpty(fileName) ? null : new File(fileName);

        getContainer().updateButtons();
    }

}
