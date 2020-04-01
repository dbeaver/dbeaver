/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class EditConnectionNavigatorSettingsDialog extends BaseDialog {
    private DataSourceNavigatorSettings navigatorSettings;

    private Button showSystemObjects;
    private Button showUtilityObjects;
    private Button showOnlyEntities;
    private Button hideFolders;

    public EditConnectionNavigatorSettingsDialog(Shell shell, DBNBrowseSettings navigatorSettings) {
        super(shell, CoreMessages.dialog_connection_wizard_final_group_navigator, null);
        this.navigatorSettings = new DataSourceNavigatorSettings(navigatorSettings);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        {
            Group miscGroup = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_ui_general_group_general,
                1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            miscGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            showSystemObjects = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects_tip,
                navigatorSettings.isShowSystemObjects(),
                1);

            showUtilityObjects = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_util_objects,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_util_objects_tip,
                navigatorSettings.isShowUtilityObjects(),
                1);

            showOnlyEntities = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_only_entities,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_only_entities_tip,
                navigatorSettings.isShowOnlyEntities(),
                1);

            hideFolders = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_hide_folders,
                CoreMessages.dialog_connection_wizard_final_checkbox_hide_folders_tip,
                navigatorSettings.isHideFolders(),
                1);
        }

        return composite;
    }

    @Override
    protected void okPressed() {
        navigatorSettings.setShowSystemObjects(showSystemObjects.getSelection());
        navigatorSettings.setShowUtilityObjects(showUtilityObjects.getSelection());
        navigatorSettings.setShowOnlyEntities(showOnlyEntities.getSelection());
        navigatorSettings.setHideFolders(hideFolders.getSelection());
        super.okPressed();
    }

    public DBNBrowseSettings getNavigatorSettings() {
        return navigatorSettings;
    }
}
