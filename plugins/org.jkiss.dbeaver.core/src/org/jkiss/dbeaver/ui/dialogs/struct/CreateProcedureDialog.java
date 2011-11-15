/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;

public class CreateProcedureDialog extends TrayDialog {

    private DBPDataSource dataSource;
    private String name;
    private DBSProcedureType type;

    public CreateProcedureDialog(Shell shell, DBPDataSource dataSource)
    {
        super(shell);
        this.dataSource = dataSource;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_struct_create_procedure_title);
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        Composite propsGroup = new Composite(group, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text nameText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_struct_create_procedure_label_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                name = nameText.getText();
            }
        });
        final Combo typeCombo = UIUtils.createLabelCombo(propsGroup, CoreMessages.dialog_struct_create_procedure_combo_type, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.add(DBSProcedureType.PROCEDURE.name());
        typeCombo.add(DBSProcedureType.FUNCTION.name());
        typeCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                type = typeCombo.getSelectionIndex() == 0 ? DBSProcedureType.PROCEDURE : DBSProcedureType.FUNCTION;
                nameText.setText(type == DBSProcedureType.PROCEDURE ? "NewProcedure" : "NewFunction"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        });
        typeCombo.select(0);
        return group;
    }

    public DBSProcedureType getProcedureType()
    {
        return type;
    }

    public String getProcedureName()
    {
        return DBObjectNameCaseTransformer.transformName(dataSource, name);
    }
}
