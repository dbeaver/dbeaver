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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EditConstraintDialog
 *
 * @author Serge Rider
 */
public class EditIndexDialog extends ColumnsSelectorDialog {

    private List<DBSIndexType> indexTypes;
    private DBSIndexType selectedIndexType;

    public EditIndexDialog(
        Shell shell,
        String title,
        DBSTable table,
        Collection<DBSIndexType> indexTypes) {
        super(shell, title, table);
        this.indexTypes = new ArrayList<DBSIndexType>(indexTypes);
        Assert.isTrue(!CommonUtils.isEmpty(this.indexTypes));
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        final Composite typeGroup = new Composite(panel, SWT.NONE);
        typeGroup.setLayout(new GridLayout(2, false));
        typeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(typeGroup, "Type");
        final Combo typeCombo = new Combo(typeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (DBSIndexType indexType : indexTypes) {
            typeCombo.add(indexType.getName());
            if (selectedIndexType == null) {
                selectedIndexType = indexType;
            }
        }
        typeCombo.select(0);
        typeCombo.setEnabled(indexTypes.size() > 1);
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                selectedIndexType = indexTypes.get(typeCombo.getSelectionIndex());
            }
        });
    }

    public DBSIndexType getConstraintType()
    {
        return selectedIndexType;
    }

}
