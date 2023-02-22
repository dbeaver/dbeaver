/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

/**
 * Create data
 */
class ProjectCreateRemotePage extends ActiveWizardPage<ProjectCreateWizard> {

    private String projectName;
    private String projectDescription;

    protected ProjectCreateRemotePage(String pageName) {
        super(pageName);
        setDescription("Project settings");
    }

    @Override
    public void createControl(Composite parent) {
        Composite panel = UIUtils.createControlGroup(parent, "Project", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Text nameText = UIUtils.createLabelText(panel, "Project name", null);
        nameText.addModifyListener(e -> {
            projectName = nameText.getText();
            updatePageCompletion();
        });
        Text descriptionText = UIUtils.createLabelText(panel, "Description", null, SWT.SINGLE | SWT.BORDER);
        descriptionText.addModifyListener(e -> {
            projectDescription = descriptionText.getText();
            updatePageCompletion();
        });

        setControl(panel);
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(projectName);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }
}
