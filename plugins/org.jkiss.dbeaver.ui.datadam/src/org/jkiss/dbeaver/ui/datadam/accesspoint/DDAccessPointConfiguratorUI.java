/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam.accesspoint;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.accesspoint.DDAccessPointTunnel;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.AbstractObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class DDAccessPointConfiguratorUI extends AbstractObjectPropertyConfigurator<Object, DBWHandlerConfiguration> {

    private Text apIdText;

    @Override
    public void createControl(@NotNull Composite parent, @NotNull Object object, @NotNull Runnable propertyChangeListener) {
        Composite composite = UIUtils.createTitledComposite(parent, "Access Point", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        composite.setLayout(new GridLayout(2, false));
        apIdText = UIUtils.createLabelText(composite, "Access Point name", "");
        apIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        apIdText.addModifyListener(e -> propertyChangeListener.run());
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
        apIdText.setText(CommonUtils.notEmpty(configuration.getStringProperty(DDAccessPointTunnel.PROP_AP_ID)));
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
        configuration.setProperty(DDAccessPointTunnel.PROP_AP_ID, apIdText.getText().trim());
    }

    @Override
    public void resetSettings(@NotNull DBWHandlerConfiguration configuration) {
    }

    @Override
    public boolean isComplete() {
        return !CommonUtils.isEmpty(apIdText.getText().trim());
    }
}
