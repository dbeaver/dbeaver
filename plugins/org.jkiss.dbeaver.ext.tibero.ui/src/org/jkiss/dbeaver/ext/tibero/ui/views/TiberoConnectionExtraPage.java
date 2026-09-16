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

package org.jkiss.dbeaver.ext.tibero.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.dbeaver.ext.tibero.ui.internal.TiberoUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class TiberoConnectionExtraPage extends ConnectionPageAbstract {

    private Button showOnlyOneSchema;
    private Button hideEmptySchemas;
    private Button readColumnComments;
    private Button showSchemaTableDescription;
    private Button showSchemaIndexTableDescription;
    private Button showSchemaTriggerTableDescription;
    private Button compilePackageAfterSave;
    private Button recompileBodyOnSpecSave;

    public TiberoConnectionExtraPage() {
        setTitle(TiberoUIMessages.dialog_connection_tibero_properties);
        setDescription(TiberoUIMessages.dialog_connection_tibero_properties_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        cfgGroup.setLayout(layout);
        cfgGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite contentGroup = UIUtils.createTitledComposite(
            cfgGroup,
            TiberoUIMessages.dialog_controlgroup_content,
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );

        readColumnComments = UIUtils.createCheckbox(
            contentGroup,
            TiberoUIMessages.edit_checkbox_read_column_comments,
            TiberoUIMessages.edit_checkbox_read_column_comments_description,
            false,
            1);

        Composite navigatorGroup = UIUtils.createTitledComposite(
            cfgGroup,
            TiberoUIMessages.dialog_controlgroup_navigator,
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );
        showOnlyOneSchema = UIUtils.createCheckbox(
            navigatorGroup,
            TiberoUIMessages.edit_checkbox_show_only_one_schema,
            TiberoUIMessages.edit_checkbox_show_only_one_schema_description,
            false,
            1);
        hideEmptySchemas = UIUtils.createCheckbox(
            navigatorGroup,
            TiberoUIMessages.edit_checkbox_hide_empty_schemas,
            TiberoUIMessages.edit_checkbox_hide_empty_schemas_description,
            false,
            1);
        showSchemaTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            TiberoUIMessages.edit_checkbox_show_schema_table_description,
            TiberoUIMessages.edit_checkbox_show_schema_table_description_description,
            true,
            1);
        showSchemaIndexTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            TiberoUIMessages.edit_checkbox_show_schema_index_table_description,
            TiberoUIMessages.edit_checkbox_show_schema_index_table_description_description,
            true,
            1);
        showSchemaTriggerTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            TiberoUIMessages.edit_checkbox_show_schema_trigger_table_description,
            TiberoUIMessages.edit_checkbox_show_schema_trigger_table_description_description,
            true,
            1);

        Composite sourceEditorGroup = UIUtils.createTitledComposite(
            cfgGroup,
            TiberoUIMessages.dialog_controlgroup_source_editor,
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );
        compilePackageAfterSave = UIUtils.createCheckbox(
            sourceEditorGroup,
            TiberoUIMessages.edit_checkbox_compile_package_after_save,
            TiberoUIMessages.edit_checkbox_compile_package_after_save_description,
            true,
            1);
        recompileBodyOnSpecSave = UIUtils.createCheckbox(
            sourceEditorGroup,
            TiberoUIMessages.edit_checkbox_recompile_body_on_spec_save,
            TiberoUIMessages.edit_checkbox_recompile_body_on_spec_save_description,
            true,
            1);

        setControl(cfgGroup);
        loadSettings();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        Map<String, String> providerProperties = connectionInfo.getProviderProperties();

        showOnlyOneSchema.setSelection(CommonUtils.toBoolean(providerProperties.get(TiberoConstants.PROP_SHOW_ONLY_ONE_SCHEMA)));
        hideEmptySchemas.setSelection(CommonUtils.toBoolean(providerProperties.get(TiberoConstants.PROP_HIDE_EMPTY_SCHEMAS)));
        readColumnComments.setSelection(CommonUtils.toBoolean(providerProperties.get(TiberoConstants.PROP_READ_COLUMN_COMMENTS)));
        String showTableDescription = providerProperties.get(TiberoConstants.PROP_SHOW_SCHEMA_TABLE_DESCRIPTION);
        showSchemaTableDescription.setSelection(showTableDescription == null || CommonUtils.toBoolean(showTableDescription));
        String showIndexDescription = providerProperties.get(TiberoConstants.PROP_SHOW_SCHEMA_INDEX_TABLE_DESCRIPTION);
        showSchemaIndexTableDescription.setSelection(showIndexDescription == null || CommonUtils.toBoolean(showIndexDescription));
        String showTriggerDescription = providerProperties.get(TiberoConstants.PROP_SHOW_SCHEMA_TRIGGER_TABLE_DESCRIPTION);
        showSchemaTriggerTableDescription.setSelection(showTriggerDescription == null || CommonUtils.toBoolean(showTriggerDescription));
        String compileAfterSave = providerProperties.get(TiberoConstants.PROP_COMPILE_AFTER_SAVE);
        compilePackageAfterSave.setSelection(compileAfterSave == null || CommonUtils.toBoolean(compileAfterSave));
        String recompileBody = providerProperties.get(TiberoConstants.PROP_RECOMPILE_BODY_ON_SPEC_SAVE);
        recompileBodyOnSpecSave.setSelection(recompileBody == null || CommonUtils.toBoolean(recompileBody));
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        Map<String, String> providerProperties = dataSource.getConnectionConfiguration().getProviderProperties();

        providerProperties.put(TiberoConstants.PROP_SHOW_ONLY_ONE_SCHEMA, String.valueOf(showOnlyOneSchema.getSelection()));
        providerProperties.put(TiberoConstants.PROP_HIDE_EMPTY_SCHEMAS, String.valueOf(hideEmptySchemas.getSelection()));
        providerProperties.put(TiberoConstants.PROP_READ_COLUMN_COMMENTS, String.valueOf(readColumnComments.getSelection()));
        providerProperties.put(TiberoConstants.PROP_SHOW_SCHEMA_TABLE_DESCRIPTION, String.valueOf(showSchemaTableDescription.getSelection()));
        providerProperties.put(TiberoConstants.PROP_SHOW_SCHEMA_INDEX_TABLE_DESCRIPTION, String.valueOf(showSchemaIndexTableDescription.getSelection()));
        providerProperties.put(TiberoConstants.PROP_SHOW_SCHEMA_TRIGGER_TABLE_DESCRIPTION, String.valueOf(showSchemaTriggerTableDescription.getSelection()));
        providerProperties.put(TiberoConstants.PROP_COMPILE_AFTER_SAVE, String.valueOf(compilePackageAfterSave.getSelection()));
        providerProperties.put(TiberoConstants.PROP_RECOMPILE_BODY_ON_SPEC_SAVE, String.valueOf(recompileBodyOnSpecSave.getSelection()));
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }
}
