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

package org.jkiss.dbeaver.ext.gaussdb.ui.views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBProcedure;
import org.jkiss.dbeaver.ext.gaussdb.ui.internal.GaussDBMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class CreateFunctionOrProcedurePage extends BaseObjectEditPage {

    protected static final Log log = Log.getLog(CreateFunctionOrProcedurePage.class);

    private String name;
    private DBSProcedureType type;

    private final GaussDBProcedure parent;
    private final DBRProgressMonitor monitor;
    private PostgreLanguage language;
    private PostgreDataType returnType;
    private Combo returnTypeCombo;

    private boolean isFunction;

    public CreateFunctionOrProcedurePage(DBRProgressMonitor monitor, GaussDBProcedure parent, boolean isFunction) {
        super(isFunction ? GaussDBMessages.dialog_struct_create_function_title : GaussDBMessages.dialog_struct_create_procedure_title);
        this.parent = parent;
        this.monitor = monitor;
        this.isFunction = isFunction;
    }

    public PostgreLanguage getLanguage() {
        return language;
    }

    public PostgreDataType getReturnType() {
        return returnType;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text containerText = UIUtils.createLabelText(propsGroup, GaussDBMessages.dialog_struct_create_procedure_container,
            DBUtils.getObjectFullName(this.parent.getParentObject(), DBPEvaluationContext.UI));
        containerText.setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, GaussDBMessages.dialog_struct_create_procedure_label_name, null);
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            updatePageState();
        });
        Combo typeCombo;
        if (getPredefinedProcedureType() == null) {
            typeCombo = UIUtils.createLabelCombo(propsGroup, GaussDBMessages.dialog_struct_create_procedure_combo_type,
                SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.add(DBSProcedureType.PROCEDURE.name());
            typeCombo.add(DBSProcedureType.FUNCTION.name());

        } else {
            typeCombo = null;
        }
        typeCombo.setEnabled(false);
        propsGroup.setTabList(ArrayUtils.remove(Control.class, propsGroup.getTabList(), containerText));
        if (isFunction) {
            createExtraControls(propsGroup);
        }

        if (typeCombo != null) {
            typeCombo.select(getDefaultProcedureType() == DBSProcedureType.FUNCTION ? 1 : 0);
        }

        return propsGroup;
    }

    protected void updateProcedureType(DBSProcedureType type) {
        returnTypeCombo.setEnabled(type.hasReturnValue());
    }

    protected void createExtraControls(Composite group) {
        {
            List<PostgreLanguage> languages = new ArrayList<>();
            try {
                languages.addAll(parent.getDatabase().getLanguages(monitor));
            } catch (DBException e) {
                log.error(e);
            }
            final Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN | SWT.READ_ONLY);
            for (PostgreLanguage lang : languages) {
                languageCombo.add(lang.getName());
            }

            languageCombo.addModifyListener(e -> {
                language = languages.get(languageCombo.getSelectionIndex());
            });
            languageCombo.setText("sql");
        }
        {
            List<PostgreDataType> dataTypes = new ArrayList<>(parent.getDatabase().getLocalDataTypes());
            dataTypes.sort(Comparator.comparing(PostgreDataType::getName));
            returnTypeCombo = UIUtils.createLabelCombo(group, "Return type", SWT.DROP_DOWN);
            for (PostgreDataType dt : dataTypes) {
                returnTypeCombo.add(dt.getName());
            }

            returnTypeCombo.addModifyListener(e -> {
                String dtName = returnTypeCombo.getText();
                if (!CommonUtils.isEmpty(dtName)) {
                    returnType = parent.getDatabase().getLocalDataType(dtName);
                } else {
                    returnType = null;
                }
            });
            returnTypeCombo.setText("int4");
        }
    }

    public DBSProcedureType getProcedureType() {
        DBSProcedureType procedureType = getPredefinedProcedureType();
        return procedureType == null ? type : procedureType;
    }

    public DBSProcedureType getPredefinedProcedureType() {
        return null;
    }

    public DBSProcedureType getDefaultProcedureType() {
        return isFunction ? DBSProcedureType.FUNCTION : DBSProcedureType.PROCEDURE;
    }

    public String getProcedureName() {
        return DBObjectNameCaseTransformer.transformName(parent.getDataSource(), name);
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmpty(name);
    }

    @Override
    public DBSObject getObject() {
        // TODO Auto-generated method stub
        return null;
    }
}
