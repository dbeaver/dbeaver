/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-log.2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.format.sqlworkbenchj;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.editors.sql.format.BaseFormatterConfigurationPage;
import org.jkiss.utils.CommonUtils;

/**
 * External SQL formatter
 */
public class SQLWorkbenchJFormatterSettingsPage extends BaseFormatterConfigurationPage {

    private TextWithOpenFolder pathEdit;

    @Override
    protected Composite createFormatSettings(Composite parent) {
        Group settings = UIUtils.createControlGroup(parent, "Settings", 2, GridData.FILL_HORIZONTAL, 0);

        pathEdit = new TextWithOpenFolder(settings, "SQL Workbench/J path");
        pathEdit.setText(CommonUtils.toString(getConfiguration().getProperty(SQLWorkbenchJConstants.PROP_WORKBENCH_PATH)));

        return parent;
    }

    @Override
    public void loadSettings(DBPPreferenceStore preferenceStore) {
        super.loadSettings(preferenceStore);
        pathEdit.setText(CommonUtils.toString(preferenceStore.getString(SQLWorkbenchJConstants.PROP_WORKBENCH_PATH)));
    }

    @Override
    public void saveSettings(DBPPreferenceStore preferenceStore) {
        super.saveSettings(preferenceStore);
        // Save formatter settings
        preferenceStore.setValue(SQLWorkbenchJConstants.PROP_WORKBENCH_PATH, pathEdit.getText());
    }


}
