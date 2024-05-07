/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.browser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class BrowserChartConfigurationEditor implements IObjectPropertyConfigurator<DashboardItemConfiguration, DashboardItemConfiguration> {

    private Text urlText;
    private Button resolveVariablesCheck;

    @Override
    public void createControl(@NotNull Composite composite, DashboardItemConfiguration itemDescriptor, @NotNull Runnable propertyChangeListener) {
        boolean readOnly = !itemDescriptor.isCustom();
        int baseStyle = !readOnly ? SWT.NONE : SWT.READ_ONLY;
        urlText = UIUtils.createLabelText(composite, "URL", CommonUtils.notEmpty(itemDescriptor.getDescription()), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | baseStyle);
        urlText.addModifyListener(e -> propertyChangeListener.run());
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(urlText) * 8;
        gd.widthHint = UIUtils.getFontHeight(urlText) * 50;
        urlText.setLayoutData(gd);

        resolveVariablesCheck = UIUtils.createCheckbox(
            composite,
            "Resolve variables",
            "Resolve environment and database-specific variables in URL",
            itemDescriptor.isResolveVariables(),
            2);
    }

    @Override
    public void loadSettings(@NotNull DashboardItemConfiguration itemConfiguration) {
        urlText.setText(CommonUtils.notEmpty(itemConfiguration.getDashboardURL()));
        resolveVariablesCheck.setSelection(itemConfiguration.isResolveVariables());
    }

    @Override
    public void saveSettings(@NotNull DashboardItemConfiguration itemDescriptor) {
        itemDescriptor.setDashboardURL(urlText.getText());
        itemDescriptor.setResolveVariables(resolveVariablesCheck.getSelection());
    }

    @Override
    public void resetSettings(@NotNull DashboardItemConfiguration itemConfiguration) {

    }

    @Override
    public boolean isComplete() {
        return !urlText.getText().isEmpty();
    }
}
