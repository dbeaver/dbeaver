/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.valuefilter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class FilterValueEditDialog extends BaseDialog{
	
	private static final String DIALOG_ID = "DBeaver.FilterValueEditDialog";//$NON-NLS-1$
    private GenericFilterValueEdit handler;
    private Object value;
    private static final Log log = Log.getLog(FilterValueEditDialog.class);

    public FilterValueEditDialog(
        @NotNull ResultSetViewer viewer,
        @NotNull DBDAttributeBinding attr,
        @NotNull ResultSetRow[] rows,
        @NotNull DBCLogicalOperator operator
    ) {
		super(viewer.getControl().getShell(), ResultSetMessages.dialog_filter_value_edit_dialog_title, null);
		setShellStyle(getShellStyle() & ~(SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL));
		handler = new GenericFilterValueEdit(viewer, attr, rows, operator);
	}

	@Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID + "." + handler.getOperator().name());
    }

    @NotNull
    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        ResultSetUtils.closeOnResultSetRefresh(handler.getViewer(), getShell());
        return contents;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        Label label = UIUtils.createControlLabel(composite, handler.getAttribute().getName() + " " + handler.getOperator().getExpression());
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int argumentCount = handler.getOperator().getArgumentCount();
        if (argumentCount == 1) {
            createSingleValueEditor(composite);
        } else if (argumentCount < 0) {
            createMultiValueSelector(composite);
        }

        return parent;
    }

    private void createSingleValueEditor(@NotNull Composite composite) {
        Composite editorGroup = UIUtils.createPlaceholder(composite, 2, 5);
        editorGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite editorPlaceholder = UIUtils.createPlaceholder(editorGroup, 1);

        editorPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorPlaceholder.setLayout(new FillLayout());

        ResultSetRow singleRow = handler.getRows()[0];
        final ResultSetValueController valueController = new ResultSetValueController(
            handler.getViewer(),
            new ResultSetCellLocation(handler.getAttribute(), singleRow, null, null),
            IValueController.EditType.INLINE,
            editorPlaceholder) {
            @Override
            public boolean isReadOnly() {
                // Filter value is never read-only
                return false;
            }
        };

        IValueEditor editor = null;
        try {
            editor = valueController.getValueManager().createEditor(valueController);
        } catch (Exception e) {
            log.error("Can't create inline value editor", e);
        }
        if (editor == null) {
            editor = new StringInlineEditor(valueController);
        }
        handler.setEditor(editor);
        editor.createControl();
        editor.getControl().addTraverseListener(event -> {
            if (event.detail == SWT.TRAVERSE_RETURN) {
                event.doit = false;
                event.detail = SWT.TRAVERSE_NONE;
                okPressed();
            }
        });
        try {
            editor.primeEditorValue(valueController.getValue());
        } catch (DBException e) {
            log.error("Error populating filter value", e);
        }

        ToolBar toolbar = new ToolBar(editorGroup, SWT.VERTICAL | SWT.FLAT);
        toolbar.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
        UIUtils.createToolItem(
            toolbar,
            ResultSetMessages.sql_editor_resultset_filter_panel_btn_apply.replace("&", ""),
            UIIcon.FILTER_APPLY,
            SelectionListener.widgetSelectedAdapter(event -> okPressed()));
        new ToolItem(toolbar, SWT.SEPARATOR);
        UIUtils.createToolItem(
            toolbar,
            ResultSetMessages.controls_resultset_viewer_action_filter_clipboard,
            UIIcon.FILTER_CLIPBOARD,
            SelectionListener.widgetSelectedAdapter(event -> pasteFromClipboard()));
    }

	
	private void createMultiValueSelector(Composite composite) {
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.widthHint = 400;
		layoutData.heightHint = 300;
		handler.setupTable(composite, SWT.BORDER | SWT.MULTI | SWT.CHECK | SWT.FULL_SELECTION, true, true, layoutData);


        ViewerColumnController columnController = new ViewerColumnController(getClass().getName(), handler.getTableViewer());
        columnController.addColumn("Value", "Value", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return handler.getAttribute().getValueHandler().getValueDisplayString(handler.getAttribute(), ((DBDLabelValuePair)element).getValue(), DBDDisplayFormat.UI);
            }
        });
        columnController.addColumn("Description", "Row description (composed from dictionary columns)", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DBDLabelValuePair)element).getLabel();
            }
        });
        columnController.createColumns(true);

        Action[] elements = new Action[] {
	        new Action(WorkbenchMessages.Workbench_selectAll) {
	            @Override
	            public void run() {
                    for (TableItem item : handler.getTableViewer().getTable().getItems()) {
                        item.setChecked(true);
                    }
	            }
	        },
	        new Action("Select &None") {
	            @Override
	            public void run() {
                    for (TableItem item : handler.getTableViewer().getTable().getItems()) {
                        item.setChecked(false);
                    }
	            }
	        }
        };
        handler.addContextMenu(elements);

        handler.addFilterText(composite);

        handler.setFilterPattern(null);
        handler.loadValues(null);

        columnController.createColumns(true);

        ToolBar toolbar = new ToolBar(composite, SWT.FLAT | SWT.RIGHT);
        toolbar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        UIUtils.createToolItem(
            toolbar,
            UIStandardLabel.APPLY.getLabel(),
            ResultSetMessages.sql_editor_resultset_filter_panel_btn_apply.replace("&", ""),
            UIIcon.FILTER_APPLY,
            SelectionListener.widgetSelectedAdapter(event -> okPressed()));
    }

	public Object getValue() {
        return value;
    }

    @NotNull
    @Override
    protected Control createButtonBar(@NotNull Composite parent) {
        return parent;
    }

    private void pasteFromClipboard() {
        try {
            Object clipboardValue = ResultSetUtils.getAttributeValueFromClipboard(handler.getAttribute());
            handler.getEditor().primeEditorValue(clipboardValue);
            handler.getEditor().getControl().setFocus();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Copy from clipboard", "Can't copy value", e);
        }
    }

    @Override
    protected void okPressed()
    {
        value = handler.getFilterValue();
        super.okPressed();
    }

}
