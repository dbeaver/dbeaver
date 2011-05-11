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
        UIUtils.createControlLabel(panel, "Type");
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
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

    public DBSIndexType getIndexType()
    {
        return selectedIndexType;
    }

}
