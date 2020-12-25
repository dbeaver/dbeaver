/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * EditMavenArtifactDialog
 */
class EditMavenArtifactDialog extends Dialog
{
    private DriverLibraryMavenArtifact library;
    private Text groupText;
    private Text artifactText;
    private Text classifierText;
    private Combo versionText;
    private boolean ignoreDependencies;

    public EditMavenArtifactDialog(Shell shell, DriverDescriptor driver, DriverLibraryMavenArtifact library)
    {
        super(shell);
        this.library = library == null ?
            new DriverLibraryMavenArtifact(driver, DBPDriverLibrary.FileType.jar, "", MavenArtifactReference.VERSION_PATTERN_RELEASE) : library;
    }

    public DriverLibraryMavenArtifact getLibrary() {
        return library;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(UIConnectionMessages.dialog_edit_driver_edit_maven_title);

        Composite composite = (Composite) super.createDialogArea(parent);
        ((GridLayout)composite.getLayout()).numColumns = 2;

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;

        groupText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_edit_driver_edit_maven_group_id_label, library.getReference().getGroupId());
        groupText.setLayoutData(gd);
        artifactText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_edit_driver_edit_maven_artifact_id_label, library.getReference().getArtifactId());
        artifactText.setLayoutData(gd);
        classifierText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_edit_driver_edit_maven_classfier_label, CommonUtils.notEmpty(library.getReference().getClassifier()));
        classifierText.setLayoutData(gd);

        versionText = UIUtils.createLabelCombo(composite, UIConnectionMessages.dialog_edit_driver_edit_maven_version_label, SWT.DROP_DOWN | SWT.BORDER);
        versionText.setLayoutData(gd);

        versionText.setText(library.getVersion());
        versionText.add(MavenArtifactReference.VERSION_PATTERN_RELEASE);
        versionText.add(MavenArtifactReference.VERSION_PATTERN_LATEST);

        Button ignoreDependenciesCheckbox = UIUtils.createCheckbox(composite, "Ignore transient dependencies", "Do not include library dependencies", library.isIgnoreDependencies(), 2);
        ignoreDependenciesCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ignoreDependencies = ignoreDependenciesCheckbox.getSelection();
            }
        });

        ModifyListener ml = e -> updateButtons();
        groupText.addModifyListener(ml);
        artifactText.addModifyListener(ml);
        classifierText.addModifyListener(ml);
        versionText.addModifyListener(ml);

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(
            !CommonUtils.isEmpty(groupText.getText()) &&
                !CommonUtils.isEmpty(artifactText.getText()) &&
                !CommonUtils.isEmpty(versionText.getText())
        );
    }

    @Override
    protected void okPressed() {
        String classifier = classifierText.getText();
        library.setReference(
            new MavenArtifactReference(
                groupText.getText(),
                artifactText.getText(),
                CommonUtils.isEmpty(classifier) ? null : classifier,
                versionText.getText()));
        library.setIgnoreDependencies(ignoreDependencies);
        super.okPressed();
    }

}