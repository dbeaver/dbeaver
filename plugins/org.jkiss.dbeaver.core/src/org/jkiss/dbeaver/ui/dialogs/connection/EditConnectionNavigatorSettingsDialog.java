/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class EditConnectionNavigatorSettingsDialog extends BaseDialog {
    private final DataSourceNavigatorSettings navigatorSettings;
    @Nullable
    private final DBPDataSourceContainer dataSourceDescriptor;

    private Button showSystemObjects;
    private Button showUtilityObjects;
    private Button showOnlyEntities;
    private Button mergeEntities;
    private Button hideFolders;

    public EditConnectionNavigatorSettingsDialog(
        @NotNull Shell shell,
        @NotNull DBNBrowseSettings navigatorSettings,
        @Nullable DBPDataSourceContainer dataSourceDescriptor) {
        super(shell, CoreMessages.dialog_connection_wizard_final_group_navigator, null);
        this.navigatorSettings = new DataSourceNavigatorSettings(navigatorSettings);
        this.dataSourceDescriptor = dataSourceDescriptor;
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

            mergeEntities = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_merge_entities,
                CoreMessages.dialog_connection_wizard_final_checkbox_merge_entities_tip,
                navigatorSettings.isMergeEntities(),
                1);

            if (dataSourceDescriptor != null) {
                mergeEntities.setEnabled(
                    dataSourceDescriptor.getDriver().getProviderDescriptor().getTreeDescriptor().supportsEntityMerge());
            }

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
        navigatorSettings.setMergeEntities(mergeEntities.getSelection());
        navigatorSettings.setHideFolders(hideFolders.getSelection());
        super.okPressed();
    }

    public DBNBrowseSettings getNavigatorSettings() {
        return navigatorSettings;
    }
}
