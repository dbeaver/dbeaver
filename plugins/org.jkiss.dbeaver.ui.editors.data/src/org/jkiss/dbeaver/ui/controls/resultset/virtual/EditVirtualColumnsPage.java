/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.virtual;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIdProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * Custom virtual attributes edit dialog
 */
public class EditVirtualColumnsPage extends BaseObjectEditPage implements IHelpContextIdProvider {

    private ResultSetViewer viewer;
    private DBVEntity vEntity;
    private boolean structChanged;
    private Table attrTable;

    public EditVirtualColumnsPage(ResultSetViewer viewer, DBVEntity vEntity) {
        super(ResultSetMessages.virtual_edit_columns_page_add, DBIcon.TREE_COLUMN);
        this.viewer = viewer;
        this.vEntity = vEntity;
    }

    public boolean isStructChanged() {
        return structChanged;
    }

    @Override
    protected Composite createPageContents(Composite parent) {
        Composite panel = UIUtils.createComposite(parent, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        attrTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
        attrTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        attrTable.setHeaderVisible(true);
        UIUtils.executeOnResize(attrTable, () -> UIUtils.packColumns(attrTable, true));

        UIUtils.createTableColumn(attrTable, SWT.LEFT, ResultSetMessages.virtual_edit_columns_page_table_column_name);
        UIUtils.createTableColumn(attrTable, SWT.LEFT, ResultSetMessages.virtual_edit_columns_page_table_column_data_type);
        UIUtils.createTableColumn(attrTable, SWT.LEFT, ResultSetMessages.virtual_edit_columns_page_table_column_expression);

        {
            Composite buttonsPanel = UIUtils.createComposite(panel, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Button btnAdd = UIUtils.createDialogButton(buttonsPanel, ResultSetMessages.virtual_edit_columns_page_dialog_button_add, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityAttribute vAttr = new DBVEntityAttribute(vEntity, null, "vcolumn");
                    EditVirtualAttributePage editAttrPage = new EditVirtualAttributePage(viewer, vAttr);
                    if (editAttrPage.edit(parent.getShell())) {
                        vAttr.setCustom(true);
                        vEntity.addVirtualAttribute(vAttr);
                        structChanged = true;
                        createAttributeItem(attrTable, vAttr);
                    }
                }
            });
            Button btnEdit = UIUtils.createDialogButton(buttonsPanel, ResultSetMessages.virtual_edit_columns_page_dialog_button_edit, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editSelectedAttribute(attrTable);
                }
            });
            btnEdit.setEnabled(false);

            Button btnRemove = UIUtils.createDialogButton(buttonsPanel, ResultSetMessages.virtual_edit_columns_page_dialog_button_remove, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBVEntityAttribute virtualAttr = (DBVEntityAttribute) attrTable.getSelection()[0].getData();
                    if (!UIUtils.confirmAction(parent.getShell(),
                    	ResultSetMessages.virtual_edit_columns_page_confirm_action_delete,
                        NLS.bind(ResultSetMessages.virtual_edit_columns_page_confirm_action_question_delete_column, virtualAttr.getName()))) {
                        return;
                    }
                    vEntity.removeVirtualAttribute(virtualAttr);
                    attrTable.remove(attrTable.getSelectionIndices());
                    structChanged = true;
                }
            });
            btnRemove.setEnabled(false);

            attrTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean attrSelected = attrTable.getSelectionIndex() >= 0;
                    btnEdit.setEnabled(attrSelected);
                    btnRemove.setEnabled(attrSelected);
                }
            });
            attrTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    editSelectedAttribute(attrTable);
                }
            });
        }

        refreshAttributes();

        return panel;
    }

    private void editSelectedAttribute(Table attrTable) {
        TableItem[] selection = attrTable.getSelection();
        if (selection.length <= 0) {
            return;
        }
        TableItem tableItem = selection[0];
        DBVEntityAttribute vAttr = (DBVEntityAttribute) tableItem.getData();
        EditVirtualAttributePage editAttrPage = new EditVirtualAttributePage(viewer, vAttr);
        if (editAttrPage.edit(attrTable.getShell())) {
            tableItem.setText(0, vAttr.getName());
            tableItem.setText(1, vAttr.getTypeName());
            tableItem.setText(2, CommonUtils.notEmpty(vAttr.getExpression()));
        }
    }

    private void createAttributeItem(Table attrTable, DBVEntityAttribute attribute) {
        TableItem item = new TableItem(attrTable, SWT.NONE);
        item.setImage(0, DBeaverIcons.getImage(DBValueFormatting.getObjectImage(attribute)));
        item.setText(0, attribute.getName());
        item.setText(1, attribute.getTypeName());
        if (attribute.getExpression() != null) {
            item.setText(2, attribute.getExpression());
        }
        item.setData(attribute);
    }


    public void refreshAttributes() {
        attrTable.removeAll();
        for (DBVEntityAttribute attr : vEntity.getCustomAttributes()) {
            createAttributeItem(attrTable, attr);
        }
    }

    @Override
    public String getHelpContextId() {
        return "virtual-column-expressions";
    }
}
