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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Object filter edit dialog
 */
public class EditObjectFilterDialog extends HelpEnabledDialog {

    public static final int SHOW_GLOBAL_FILTERS_ID = 1000;
    
    private String objectTitle;
    private DBSObjectFilter filter;
    private boolean globalFilter;
    private Composite blockControl;
    private ControlEnableState blockEnableState;
    private Table includeTable;
    private Table excludeTable;

    public EditObjectFilterDialog(Shell shell, String objectTitle, DBSObjectFilter filter, boolean globalFilter)
    {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.objectTitle = objectTitle;
        this.filter = new DBSObjectFilter(filter);
        this.globalFilter = globalFilter;
    }

    public DBSObjectFilter getFilter()
    {
        return filter;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(NLS.bind(CoreMessages.dialog_filter_title, objectTitle));
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        Composite topPanel = UIUtils.createPlaceholder(composite, globalFilter ? 1 : 2);
        topPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        final Button enableButton = UIUtils.createCheckbox(topPanel, CoreMessages.dialog_filter_button_enable, false);
        enableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                filter.setEnabled(enableButton.getSelection());
                enableFiltersContent();
            }
        });
        enableButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        enableButton.setSelection(filter.isEnabled());
        if (!globalFilter) {
            Link globalLink = new Link(topPanel, SWT.NONE);
            globalLink.setText(CoreMessages.dialog_filter_global_link);
            globalLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    setReturnCode(SHOW_GLOBAL_FILTERS_ID);
                    close();
                }
            });
            globalLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }
        blockControl = UIUtils.createPlaceholder(composite, 1);
        blockControl.setLayoutData(new GridData(GridData.FILL_BOTH));

        includeTable = createEditableList(CoreMessages.dialog_filter_list_include, filter.getInclude());
        excludeTable = createEditableList(CoreMessages.dialog_filter_list_exclude, filter.getExclude());

        enableFiltersContent();

        return composite;
    }

    private Table createEditableList(String name, List<String> values)
    {
        Group group = UIUtils.createControlGroup(blockControl, name, 2, GridData.FILL_BOTH, 0);

        final Table valueTable = new Table(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
         valueTable.setLayoutData(gd);
        // valueTable.setHeaderVisible(true);
         valueTable.setLinesVisible(true);

        final TableColumn valueColumn = UIUtils.createTableColumn( valueTable, SWT.LEFT, CoreMessages.dialog_filter_table_column_value);
        valueColumn.setWidth(300);

        if (CommonUtils.isEmpty(values)) {
            new TableItem( valueTable, SWT.LEFT).setText(""); //$NON-NLS-1$
        } else {
            for (String value : values) {
                new TableItem( valueTable, SWT.LEFT).setText(value);
            }
        }

        final TableEditor tableEditor = new TableEditor( valueTable);
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.grabHorizontal = true;
        tableEditor.grabVertical = true;

        final EditorMouseAdapter mouseAdapter = new EditorMouseAdapter( valueTable, tableEditor);
         valueTable.addMouseListener(mouseAdapter);

         valueTable.addTraverseListener(
            new UIUtils.ColumnTextEditorTraverseListener( valueTable, tableEditor, 0, mouseAdapter));

        Composite buttonsGroup = UIUtils.createPlaceholder(group, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(CoreMessages.dialog_filter_button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (tableEditor.getItem() != null && !tableEditor.getItem().isDisposed() && tableEditor.getEditor() != null && !tableEditor.getEditor().isDisposed()) {
                    tableEditor.getItem().setText(((Text)tableEditor.getEditor()).getText());
                }

                TableItem newItem = new TableItem( valueTable, SWT.LEFT);
                valueTable.setSelection(newItem);
                mouseAdapter.closeEditor(tableEditor);
                mouseAdapter.showEditor(newItem);
            }
        });

        final Button removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(CoreMessages.dialog_filter_button_remove);
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                int selectionIndex =  valueTable.getSelectionIndex();
                if (selectionIndex >= 0) {
                    mouseAdapter.closeEditor(tableEditor);
                    valueTable.remove(selectionIndex);
                    removeButton.setEnabled(valueTable.getSelectionIndex() >= 0);
                }
            }
        });
        removeButton.setEnabled(false);

         valueTable.addSelectionListener(new SelectionAdapter() {
             @Override
             public void widgetSelected(SelectionEvent e)
             {
                 int selectionIndex =  valueTable.getSelectionIndex();
                 removeButton.setEnabled(selectionIndex >= 0);
             }
         });
        return valueTable;
    }

    protected void enableFiltersContent()
    {
        if (filter.isEnabled()) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else if (blockEnableState == null) {
            blockEnableState = ControlEnableState.disable(blockControl);
        }
    }

    private void saveConfigurations()
    {
        filter.setInclude(collectValues(includeTable));
        filter.setExclude(collectValues(excludeTable));
    }

    private List<String> collectValues(Table table)
    {
        List<String> values = new ArrayList<String>();
        for (TableItem item : table.getItems()) {
            String value = item.getText().trim();
            if (value.isEmpty() || value.equals("%")) { //$NON-NLS-1$
                continue;
            }
            values.add(value);
        }
        return values;
    }

    @Override
    protected void okPressed()
    {
        saveConfigurations();
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }

    private static class EditorMouseAdapter extends MouseAdapter implements UIUtils.TableEditorController {
        private final Table paramTable;
        private final TableEditor tableEditor;

        public EditorMouseAdapter(Table paramTable, TableEditor tableEditor)
        {
            this.paramTable = paramTable;
            this.tableEditor = tableEditor;
        }

        @Override
        public void mouseUp(MouseEvent e)
        {
            Text editor = (Text)tableEditor.getEditor();
            if (editor != null && !editor.isDisposed()) {
                tableEditor.getItem().setText(editor.getText());
            }
            // Clean up any previous editor control
            closeEditor(tableEditor);

            TableItem item = paramTable.getItem(new Point(e.x, e.y));
            if (item == null) {
                return;
            }
            int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            if (columnIndex != 0) {
                return;
            }
            showEditor(item);
        }

        @Override
        public void showEditor(final TableItem item)
        {
            Text editor = new Text(paramTable, SWT.BORDER);
            editor.setText(item.getText());
            tableEditor.setEditor(editor, item, 0);
            editor.setFocus();
        }

        @Override
        public void closeEditor(TableEditor tableEditor)
        {
            if (tableEditor.getEditor() != null) tableEditor.getEditor().dispose();
        }
    }
}
