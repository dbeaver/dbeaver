/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * EditConstraintDialog
 *
 * @author Serge Rider
 */
public class EditConstraintDialog extends ColumnsSelectorDialog {

    private DBSConstraintType[] constraintTypes;
    private DBSConstraintType selectedConstraintType;

    public EditConstraintDialog(
        Shell shell,
        String title,
        DBSTable table,
        DBSConstraintType[] constraintTypes) {
        super(shell, title, table);
        this.constraintTypes = constraintTypes;
        Assert.isTrue(!CommonUtils.isEmpty(this.constraintTypes));
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        UIUtils.createControlLabel(panel, "Type");
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (DBSConstraintType constraintType : constraintTypes) {
            typeCombo.add(constraintType.getName());
            if (selectedConstraintType == null) {
                selectedConstraintType = constraintType;
            }
        }
        typeCombo.select(0);
        typeCombo.setEnabled(constraintTypes.length > 1);
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                selectedConstraintType = constraintTypes[typeCombo.getSelectionIndex()];
            }
        });
    }

    public DBSConstraintType getConstraintType()
    {
        return selectedConstraintType;
    }

}
