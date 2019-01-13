/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EditIndexPage
 *
 * @author Serge Rider
 */
public class EditIndexPage extends AttributesSelectorPage {

    public static final String PROP_DESC = "desc";

    private List<DBSIndexType> indexTypes;
    private DBSIndexType selectedIndexType;
    private boolean unique;

    private int descColumnIndex;

    public EditIndexPage(
        String title,
        DBSTable table,
        Collection<DBSIndexType> indexTypes)
    {
        super(title, table);
        this.indexTypes = new ArrayList<>(indexTypes);
        Assert.isTrue(!CommonUtils.isEmpty(this.indexTypes));
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_index_label_type);
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

        final Button uniqueButton = UIUtils.createLabelCheckbox(panel, "Unique", false);
        uniqueButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                unique = uniqueButton.getSelection();
            }
        });
    }

    public DBSIndexType getIndexType()
    {
        return selectedIndexType;
    }

    public boolean isUnique() {
        return unique;
    }

    @Override
    protected void createAttributeColumns(Table columnsTable) {
        super.createAttributeColumns(columnsTable);

        TableColumn colDesc = UIUtils.createTableColumn(columnsTable, SWT.NONE, "Order");
        colDesc.setToolTipText("Ascending/descending");
    }

    @Override
    protected int fillAttributeColumns(DBSEntityAttribute attribute, AttributeInfo attributeInfo, TableItem columnItem) {
        descColumnIndex = super.fillAttributeColumns(attribute, attributeInfo, columnItem) + 1;

        boolean isDesc = Boolean.TRUE.equals(attributeInfo.getProperty(PROP_DESC));
        columnItem.setText(descColumnIndex, isDesc ? "DESC" : "ASC");

        return descColumnIndex;
    }

    protected Control createCellEditor(Table table, int index, TableItem item, AttributeInfo attributeInfo) {
        if (index == descColumnIndex) {
            boolean isDesc = Boolean.TRUE.equals(attributeInfo.getProperty(PROP_DESC));
            CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
            combo.add("ASC");
            combo.add("DESC");
            combo.select(isDesc ? 1 : 0);
            return combo;
        }
        return super.createCellEditor(table, index, item, attributeInfo);
    }

    protected void saveCellValue(Control control, int index, TableItem item, AttributeInfo attributeInfo) {
        if (index == descColumnIndex) {
            CCombo combo = (CCombo) control;
            boolean isDesc = combo.getSelectionIndex() == 1;
            item.setText(index, isDesc ? "DESC" : "ASC");
            attributeInfo.setProperty(PROP_DESC, isDesc);
        } else {
            super.saveCellValue(control, index, item, attributeInfo);
        }
    }

}
