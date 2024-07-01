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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class CreateProcedurePage extends BaseObjectEditPage {

    private DBSProcedure procedure;
    private String name;
    private DBSProcedureType type;

    public CreateProcedurePage(DBSProcedure procedure) {
        super(ObjectEditorMessages.dialog_struct_create_procedure_title);
        this.procedure = procedure;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text containerText = UIUtils.createLabelText(propsGroup, ObjectEditorMessages.dialog_struct_create_procedure_container, DBUtils.getObjectFullName(this.procedure.getParentObject(), DBPEvaluationContext.UI));
        containerText.setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, ObjectEditorMessages.dialog_struct_create_procedure_label_name, null);
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            updatePageState();
        });
        Combo typeCombo;
        if (getPredefinedProcedureType() == null) {
            typeCombo = UIUtils.createLabelCombo(propsGroup, ObjectEditorMessages.dialog_struct_create_procedure_combo_type, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.add(DBSProcedureType.PROCEDURE.name());
            typeCombo.add(DBSProcedureType.FUNCTION.name());
            typeCombo.addModifyListener(e -> {
                type = typeCombo.getSelectionIndex() == 0 ? DBSProcedureType.PROCEDURE : DBSProcedureType.FUNCTION;
                updateProcedureType(type);

            });
        } else {
            typeCombo = null;
        }
        propsGroup.setTabList(ArrayUtils.remove(Control.class, propsGroup.getTabList(), containerText));

        createExtraControls(propsGroup);

        if (typeCombo != null) {
            typeCombo.select(getDefaultProcedureType() == DBSProcedureType.FUNCTION ? 1 : 0);
        }

        return propsGroup;
    }

    protected void updateProcedureType(DBSProcedureType type) {

    }

    protected void createExtraControls(Composite group) {

    }

    public DBSProcedureType getProcedureType() {
        DBSProcedureType procedureType = getPredefinedProcedureType();
        return procedureType == null ? type : procedureType;
    }

    public DBSProcedureType getPredefinedProcedureType() {
        return null;
    }

    public DBSProcedureType getDefaultProcedureType() {
        return DBSProcedureType.PROCEDURE;
    }

    public String getProcedureName() {
        return DBObjectNameCaseTransformer.transformName(procedure.getDataSource(), name);
    }

    @Override
    public DBSObject getObject() {
        return procedure;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmpty(name);
    }
}
