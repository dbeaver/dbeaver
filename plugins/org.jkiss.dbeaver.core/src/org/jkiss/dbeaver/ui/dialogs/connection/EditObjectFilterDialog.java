/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;

import java.util.List;

/**
 * Object filter edit dialog
 */
public class EditObjectFilterDialog extends HelpEnabledDialog {

    static final Log log = LogFactory.getLog(EditObjectFilterDialog.class);

    private String objectTitle;
    private DBSObjectFilter filter;
    private Composite blockControl;
    private ControlEnableState blockEnableState;

    protected EditObjectFilterDialog(Shell shell, String objectTitle, DBSObjectFilter filter)
    {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.objectTitle = objectTitle;
        this.filter = new DBSObjectFilter(filter);
    }

    public DBSObjectFilter getFilter()
    {
        return filter;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(objectTitle + " Filter");
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        final Button enableButton = UIUtils.createCheckbox(composite, "Enable", false);
        enableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                filter.setEnabled(enableButton.getSelection());
                enableFiltersContent();
            }
        });
        enableButton.setSelection(filter.isEnabled());
        blockControl = UIUtils.createPlaceholder(composite, 1);

        createEditableList("Include", filter.getInclude());
        createEditableList("Exclude", filter.getExclude());

        return composite;
    }

    private void createEditableList(String name, List<String> values)
    {
        Group group = UIUtils.createControlGroup(blockControl, name, 2, GridData.FILL_HORIZONTAL, 0);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Table paramTable = new Table(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        paramTable.setLayoutData(gd);
        //paramTable.setHeaderVisible(true);
        paramTable.setLinesVisible(true);

        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Value");
        valueColumn.setWidth(300);

        for (String value : values) {
            new TableItem(paramTable, SWT.LEFT).setText(value);
        }

        final TableEditor tableEditor = new TableEditor(paramTable);
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.grabHorizontal = true;
        tableEditor.grabVertical = true;

        final EditorMouseAdapter mouseAdapter = new EditorMouseAdapter(paramTable, tableEditor);
        paramTable.addMouseListener(mouseAdapter);

        paramTable.addTraverseListener(
            new UIUtils.ColumnTextEditorTraverseListener(paramTable, tableEditor, 0, mouseAdapter));

        Composite buttonsGroup = UIUtils.createPlaceholder(group, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText("Add");
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem newItem = new TableItem(paramTable, SWT.LEFT);
                paramTable.setSelection(newItem);
                mouseAdapter.closeEditor(tableEditor);
                mouseAdapter.showEditor(newItem);
            }
        });

        final Button removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                int selectionIndex = paramTable.getSelectionIndex();
                if (selectionIndex >= 0) {
                    mouseAdapter.closeEditor(tableEditor);
                    paramTable.remove(selectionIndex);
                }
            }
        });
        
        paramTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                int selectionIndex = paramTable.getSelectionIndex();
                removeButton.setEnabled(selectionIndex >= 0);
            }
        });
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
