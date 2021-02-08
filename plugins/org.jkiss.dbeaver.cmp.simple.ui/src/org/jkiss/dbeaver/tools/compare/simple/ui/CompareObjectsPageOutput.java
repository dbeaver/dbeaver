/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.compare.simple.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.tools.compare.simple.CompareObjectsSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

class CompareObjectsPageOutput extends ActiveWizardPage<CompareObjectsWizard> {

    private Button showOnlyDifference;
    private Combo reportTypeCombo;
    private Text outputFolderText;

    CompareObjectsPageOutput() {
        super("Compare objects");
        setTitle("Compare database objects");
        setDescription("Configuration of output report");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final CompareObjectsSettings settings = getWizard().getSettings();

        {
            Group reportSettings = new Group(composite, SWT.NONE);
            reportSettings.setText("Report settings");
            gl = new GridLayout(1, false);
            reportSettings.setLayout(gl);
            reportSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            showOnlyDifference = UIUtils.createCheckbox(reportSettings, "Show only differences", settings.isShowOnlyDifferences());
            showOnlyDifference.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setShowOnlyDifferences(showOnlyDifference.getSelection());
                }
            });
        }

        {
            Group outputSettings = new Group(composite, SWT.NONE);
            outputSettings.setText("Output");
            gl = new GridLayout(2, false);
            outputSettings.setLayout(gl);
            outputSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(outputSettings, "Output type");
            reportTypeCombo = new Combo(outputSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (CompareObjectsSettings.OutputType outputType : CompareObjectsSettings.OutputType.values()) {
                reportTypeCombo.add(outputType.getTitle());
            }
            reportTypeCombo.select(settings.getOutputType().ordinal());
            reportTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    for (CompareObjectsSettings.OutputType outputType : CompareObjectsSettings.OutputType.values()) {
                        if (outputType.ordinal() == reportTypeCombo.getSelectionIndex()) {
                            settings.setOutputType(outputType);
                            UIUtils.enableWithChildren(outputFolderText.getParent(), outputType == CompareObjectsSettings.OutputType.FILE);
                            break;
                        }
                    }
                }
            });

            outputFolderText = DialogUtils.createOutputFolderChooser(outputSettings, null, null);
            outputFolderText.setText(settings.getOutputFolder());
            UIUtils.enableWithChildren(outputFolderText.getParent(), settings.getOutputType() == CompareObjectsSettings.OutputType.FILE);
            outputFolderText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    settings.setOutputFolder(outputFolderText.getText());
                }
            });
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }
}