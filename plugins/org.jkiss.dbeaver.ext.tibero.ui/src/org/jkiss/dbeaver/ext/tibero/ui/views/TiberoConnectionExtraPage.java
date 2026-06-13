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
        setTitle("Tibero Properties");
        setDescription("Tibero connection properties");
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
            "Content",
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );

        readColumnComments = UIUtils.createCheckbox(
            contentGroup,
            "Read column comments",
            "Read column comments from Tibero catalog views while loading table columns.",
            false,
            1);

        Composite navigatorGroup = UIUtils.createTitledComposite(
            cfgGroup,
            "Navigator",
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );
        showOnlyOneSchema = UIUtils.createCheckbox(
            navigatorGroup,
            "Show only connected user schema",
            "Show only the schema that belongs to the connected user in Database Navigator.",
            false,
            1);
        hideEmptySchemas = UIUtils.createCheckbox(
            navigatorGroup,
            "Hide empty schemas",
            "Hide schemas that own no objects in Database Navigator.",
            false,
            1);
        showSchemaTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            "Show table descriptions",
            "Show table/view comments as navigator object descriptions in schema-level folders.",
            true,
            1);
        showSchemaIndexTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            "Show table name for schema indexes",
            "Show the owning table name as navigator object description for indexes in the schema-level Indexes folder.",
            true,
            1);
        showSchemaTriggerTableDescription = UIUtils.createCheckbox(
            navigatorGroup,
            "Show table name for schema triggers",
            "Show the owning table name as navigator object description for triggers in the schema-level Table Triggers folder.",
            true,
            1);

        Composite sourceEditorGroup = UIUtils.createTitledComposite(
            cfgGroup,
            "Source editor",
            1,
            GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING
        );
        compilePackageAfterSave = UIUtils.createCheckbox(
            sourceEditorGroup,
            "Compile package after save",
            "Compile Tibero package specification/body after saving source in package editor.",
            true,
            1);
        recompileBodyOnSpecSave = UIUtils.createCheckbox(
            sourceEditorGroup,
            "Recompile package body when spec saved",
            "When the package specification is saved, also issue ALTER PACKAGE ... COMPILE BODY so the body returns to VALID state (PL/SQL auto-invalidates the body on spec change).",
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
