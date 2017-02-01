/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * EditMavenArtifactDialog
 */
class EditMavenArtifactDialog extends Dialog
{
    private MavenArtifactReference artifact;
    private Text groupText;
    private Text artifactText;
    private Text classifierText;
    private Combo versionText;

    public EditMavenArtifactDialog(Shell shell, MavenArtifactReference artifact)
    {
        super(shell);
        this.artifact = artifact == null ? new MavenArtifactReference("", "", null, MavenArtifactReference.VERSION_PATTERN_RELEASE) : artifact;
    }

    public MavenArtifactReference getArtifact() {
        return artifact;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Edit Maven Artifact");

        Composite composite = (Composite) super.createDialogArea(parent);
        ((GridLayout)composite.getLayout()).numColumns = 2;

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;

        groupText = UIUtils.createLabelText(composite, "Group Id", artifact.getGroupId());
        groupText.setLayoutData(gd);
        artifactText = UIUtils.createLabelText(composite, "Artifact Id", artifact.getArtifactId());
        artifactText.setLayoutData(gd);
        classifierText = UIUtils.createLabelText(composite, "Classifier", CommonUtils.notEmpty(artifact.getClassifier()));
        classifierText.setLayoutData(gd);

        versionText = UIUtils.createLabelCombo(composite, "Version", SWT.DROP_DOWN | SWT.BORDER);
        versionText.setLayoutData(gd);

        versionText.setText(artifact.getVersion());
        versionText.add(MavenArtifactReference.VERSION_PATTERN_RELEASE);
        versionText.add(MavenArtifactReference.VERSION_PATTERN_LATEST);

        ModifyListener ml = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        };
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
        artifact = new MavenArtifactReference(
            groupText.getText(),
            artifactText.getText(),
            CommonUtils.isEmpty(classifier) ? null : classifier,
            versionText.getText());
        super.okPressed();
    }

}