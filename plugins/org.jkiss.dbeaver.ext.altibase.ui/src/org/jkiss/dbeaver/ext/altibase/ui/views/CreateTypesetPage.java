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
package org.jkiss.dbeaver.ext.altibase.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTypeset;
import org.jkiss.dbeaver.ext.altibase.ui.internal.AltibaseUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class CreateTypesetPage extends BaseObjectEditPage {

    private DBSProcedure procedure;
    private String name;

    public CreateTypesetPage(AltibaseTypeset procedure) {
        super(AltibaseUIMessages.edit_altibase_typeset_manager_dialog_title);
        this.procedure = procedure;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text containerText = UIUtils.createLabelText(propsGroup, 
                AltibaseUIMessages.edit_altibase_typeset_manager_container, 
                DBUtils.getObjectFullName(this.procedure.getParentObject(), DBPEvaluationContext.UI));
        containerText.setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, 
                AltibaseUIMessages.edit_altibase_typeset_manager_name, null);
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            updatePageState();
        });

        propsGroup.setTabList(ArrayUtils.remove(Control.class, propsGroup.getTabList(), containerText));

        return propsGroup;
    }

    public DBSProcedureType getProcedureType() {
        return getDefaultProcedureType();
    }

    public DBSProcedureType getPredefinedProcedureType() {
        return getDefaultProcedureType();
    }

    public DBSProcedureType getDefaultProcedureType() {
        return DBSProcedureType.UNKNOWN;
    }

    public String getProcedureName() {
        return DBObjectNameCaseTransformer.transformName(procedure.getDataSource(), name);
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmpty(name);
    }

    @Override
    public DBSObject getObject() {
        return procedure;
    }
}
