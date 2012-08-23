/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * EditDictionaryDialog
 *
 * @author Serge Rider
 */
public class EditDictionaryDialog extends ColumnsSelectorDialog {

    private String customDescription;
    private Text criteriaText;

    public EditDictionaryDialog(
        Shell shell,
        String title,
        DBSEntity table)
    {
        super(shell, title, table);
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        Label label = UIUtils.createControlLabel(panel,
            "Choose dictionary description columns or set custom criteria");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
    }

    @Override
    protected void createContentsAfterColumns(Composite panel)
    {
        Group group = UIUtils.createControlGroup(panel, "Custom criteria", 1, GridData.FILL_HORIZONTAL, 0);
        criteriaText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 50;
        criteriaText.setLayoutData(gd);
    }

    @Override
    protected void handleColumnsChange() {
        StringBuilder custom = new StringBuilder();
        for (DBSEntityAttribute column : getSelectedColumns()) {
            if (custom.length() > 0) {
                custom.append(",");
            }
            custom.append(DBUtils.getQuotedIdentifier(column));
        }
        criteriaText.setText(custom.toString());
    }
}
