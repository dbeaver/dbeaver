/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.navigator;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;


class DashboardCreateWizardPage extends WizardPage {

    @Nullable
    private DBPProject project;
    private String dashboardName;

    protected DashboardCreateWizardPage(@Nullable DBPProject project) {
        super("New dashboard");
        setTitle("Create new dashboard");
        setDescription("Specify dashboard properties");

        this.project = project;
    }

    public String getDashboardName() {
        return dashboardName;
    }

    @Override
    public boolean isPageComplete() {
        if (project == null || !project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
            setErrorMessage("The user needs more permissions to create a new diagram.");
            return false;
        }
        boolean hasName = !CommonUtils.isEmpty(dashboardName);
        if (!hasName) {
            setErrorMessage("Set dashboard name");
        } else {
            setErrorMessage(null);
        }
        if (getErrorMessage() != null) {
            return false;
        }
        return hasName;
    }

    @Override
    public void createControl(Composite parent) {
        Composite configGroup = UIUtils.createControlGroup(parent, "Settings", 2, GridData.FILL_BOTH, 0);

        final Text dashboardNameText = UIUtils.createLabelText(configGroup, "Name", null); //$NON-NLS-1$
        dashboardNameText.addModifyListener(e -> {
            dashboardName = dashboardNameText.getText();
            updateState();
        });

        setControl(configGroup);
    }

    private void updateState() {
        getContainer().updateButtons();
    }

}
