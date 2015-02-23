/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EditIndexDialog
 *
 * @author Serge Rider
 */
public class EditIndexDialog extends AttributesSelectorDialog {

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
        UIUtils.createControlLabel(panel, CoreMessages.dialog_struct_edit_index_label_type);
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
