/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;

public class CreateProcedurePage extends BaseObjectEditPage {

    private DBSObjectContainer container;
    private String name;
    private DBSProcedureType type;

    public CreateProcedurePage(DBSObjectContainer container)
    {
        super(CoreMessages.dialog_struct_create_procedure_title);
        this.container = container;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        UIUtils.createLabelText(propsGroup, "Container", DBUtils.getObjectFullName(container, DBPEvaluationContext.UI)).setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_struct_create_procedure_label_name, null);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                name = nameText.getText();
            }
        });
        final Combo typeCombo = UIUtils.createLabelCombo(propsGroup, CoreMessages.dialog_struct_create_procedure_combo_type, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.add(DBSProcedureType.PROCEDURE.name());
        typeCombo.add(DBSProcedureType.FUNCTION.name());
        typeCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                type = typeCombo.getSelectionIndex() == 0 ? DBSProcedureType.PROCEDURE : DBSProcedureType.FUNCTION;
                nameText.setText(type == DBSProcedureType.PROCEDURE ? "NewProcedure" : "NewFunction"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        });
        typeCombo.select(0);
        propsGroup.setTabList(new Control[] { nameText, typeCombo} );
        return propsGroup;
    }

    public DBSProcedureType getProcedureType()
    {
        return type;
    }

    public String getProcedureName()
    {
        return DBObjectNameCaseTransformer.transformName(container.getDataSource(), name);
    }
}
