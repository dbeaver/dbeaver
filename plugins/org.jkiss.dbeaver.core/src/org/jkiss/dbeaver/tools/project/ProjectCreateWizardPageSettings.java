/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.project;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.Workbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class ProjectCreateWizardPageSettings extends WizardPage {

    public static final String DEFAULT_PROJECT_NAME = "Project";
    private ProjectCreateData createData;
    private Text projectNameText;

    protected ProjectCreateWizardPageSettings(ProjectCreateData importData)
    {
        super(CoreMessages.dialog_project_create_settings_name);
        this.createData = importData;

        setTitle(CoreMessages.dialog_project_create_settings_title);
        setDescription(CoreMessages.dialog_project_create_settings_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return !CommonUtils.isEmpty(createData.getName());
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, CoreMessages.dialog_project_create_settings_group_general, 2, GridData.FILL_HORIZONTAL, 0);

        projectNameText = UIUtils.createLabelText(configGroup, CoreMessages.dialog_project_create_settings_label_name, DEFAULT_PROJECT_NAME);
        createData.setName(DEFAULT_PROJECT_NAME);

        final Text projectDescriptionText = UIUtils.createLabelText(configGroup, CoreMessages.dialog_project_create_settings_label_description, null); //$NON-NLS-2$
        projectDescriptionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                createData.setDescription(projectDescriptionText.getText());
            }
        });

        UIUtils.createControlLabel(configGroup, "Project path");
        final TextWithOpenFolder projectPathText = new TextWithOpenFolder(configGroup, "Project path");
        projectPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        final File projectParentDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        final File projectHome = new File(projectParentDir, DEFAULT_PROJECT_NAME);
        createData.setPath(projectHome);
        projectPathText.setText(projectHome.getAbsolutePath());
        projectPathText.getTextControl().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                createData.setPath(new File(projectPathText.getText()));
            }
        });

        projectNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                final String projectName = projectNameText.getText();
                createData.setName(projectName);
                if (!CommonUtils.isEmptyTrimmed(projectName)) {
                    final File oldProjectPath = new File(projectPathText.getText());
                    final File newProjectPath = new File(oldProjectPath.getParent(), createData.getName());
                    createData.setPath(newProjectPath);
                    projectPathText.setText(newProjectPath.getAbsolutePath());
                }
                updateState();
            }
        });

        setControl(placeholder);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

}
