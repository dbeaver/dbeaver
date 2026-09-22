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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.mimer.model.MimerSynonym;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Synonym" dialog: Name, Target Schema (combo of the datasource's schemas), Target
 * Name (a combo listing that schema's own tables/views, reloaded whenever Target Schema
 * changes - stays typeable, since {@code EXT_SYNONYMS} visibility can lag what the picker
 * itself can see). All attributes are collected before the object is created - see
 * {@link MimerSynonymConfigurator}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateSynonymPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateSynonymPage.class);

    private final MimerSynonym synonym;

    private String name = "";
    private String targetSchema = "";
    private String targetName = "";
    private Combo targetNameCombo;

    public MimerCreateSynonymPage(@NotNull MimerSynonym synonym) {
        super("Create synonym");
        this.synonym = synonym;
    }

    @Override
    public DBSObject getObject() {
        return synonym;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(synonym.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Combo targetSchemaCombo = UIUtils.createLabelCombo(group, "Target schema", SWT.DROP_DOWN);
        for (GenericSchema schema : synonym.getDataSource().getSchemas()) {
            targetSchemaCombo.add(schema.getName());
        }
        targetSchemaCombo.addModifyListener(e -> {
            targetSchema = targetSchemaCombo.getText();
            loadTargetNames(targetSchema);
            updatePageState();
        });

        targetNameCombo = UIUtils.createLabelCombo(group, "Target name", SWT.DROP_DOWN);
        targetNameCombo.setToolTipText(MimerUIMessages.page_create_synonym_target_name_tooltip);
        targetNameCombo.addModifyListener(e -> {
            targetName = targetNameCombo.getText();
            updatePageState();
        });

        return group;
    }

    /**
     * Repopulates {@link #targetNameCombo} with the chosen schema's own tables/views - doesn't
     * touch whatever the user already typed, only the dropdown list.
     */
    private void loadTargetNames(@NotNull String schemaName) {
        targetNameCombo.removeAll();
        GenericSchema schema = synonym.getDataSource().getSchema(schemaName);
        if (schema == null) {
            return;
        }
        try {
            for (GenericTableBase table : schema.getTables(new VoidProgressMonitor())) {
                targetNameCombo.add(table.getName());
            }
        } catch (Exception e) {
            log.debug("Can't load table list for the create-synonym dialog", e);
        }
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(targetSchema) && !CommonUtils.isEmptyTrimmed(targetName);
    }

    /**
     * Applies the collected values to the synonym. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        synonym.setName(name.trim());
        synonym.setTargetSchema(targetSchema.trim());
        synonym.setTargetName(targetName.trim());
    }
}
