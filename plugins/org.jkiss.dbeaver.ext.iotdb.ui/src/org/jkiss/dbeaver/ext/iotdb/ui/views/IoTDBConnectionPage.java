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

package org.jkiss.dbeaver.ext.iotdb.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class IoTDBConnectionPage extends GenericConnectionPage {

    private Combo sqlDialectCombo;
    private static final String GROUP_SQL_DIALECT = "sqlDialect";
    private boolean activated;

    @Override
    public void createAdvancedSettingsGroup(Composite composite) {
        ModifyListener textListener = e -> {
            if (activated) {
                saveAndUpdate();
            }
        };
        Composite additionalSettingsGroup = UIUtils.createTitledComposite(composite, "Additional Settings", 4, GridData.FILL_HORIZONTAL);
        additionalSettingsGroup.setLayout(new GridLayout(4, false));
        sqlDialectCombo = UIUtils.createLabelCombo(additionalSettingsGroup, "Sql Dialect", SWT.DROP_DOWN | SWT.READ_ONLY);
        sqlDialectCombo.addModifyListener(textListener);
        Control emptyLabel = UIUtils.createEmptyLabel(additionalSettingsGroup, 2, 1);
        addControlToGroup(GROUP_SQL_DIALECT, sqlDialectCombo);
        addControlToGroup(GROUP_SQL_DIALECT, emptyLabel);
    }

    private void saveAndUpdate() {
        saveSettings(site.getActiveDataSource());
        site.updateButtons();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        final DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        if (sqlDialectCombo != null) {
            sqlDialectCombo.removeAll();
            sqlDialectCombo.add("tree");
            sqlDialectCombo.add("table");

            if (site.isNew() && CommonUtils.isEmpty(connectionInfo.getServerName())) {
                sqlDialectCombo.setText(CommonUtils.notEmpty(site.getDriver().getDefaultServer()));
            } else {
                sqlDialectCombo.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
            }
        }

        activated = true;

        UIUtils.asyncExec(() -> {
            if (sqlDialectCombo != null && sqlDialectCombo.isVisible()) {
                sqlDialectCombo.setFocus();
            }
        });
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (sqlDialectCombo != null) {
            connectionInfo.setServerName(sqlDialectCombo.getText().trim());
        }
        super.saveSettings(dataSource);
    }
}
