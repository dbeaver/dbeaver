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
package org.jkiss.dbeaver.ui.controls.resultset.valuefilter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetValueController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class FilterValueEditDialog extends BaseDialog{
	
	private static final String DIALOG_ID = "DBeaver.FilterValueEditDialog";//$NON-NLS-1$
    private GenericFilterValueEdit handler;
    private Object value;
    private static final Log log = Log.getLog(FilterValueEditDialog.class);

    public FilterValueEditDialog(ResultSetViewer viewer, DBDAttributeBinding attr, ResultSetRow[] rows, DBCLogicalOperator operator) {
		super(viewer.getControl().getShell(), "Edit value", null);
		handler = new GenericFilterValueEdit(viewer, attr, rows, operator);
	}

	@Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID + "." + handler.getOperator().name());
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        Label label = UIUtils.createControlLabel(composite, handler.getAttribute().getName() + " " + handler.getOperator().getStringValue());
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int argumentCount = handler.getOperator().getArgumentCount();
        if (argumentCount == 1) {
            createSingleValueEditor(composite);
        } else if (argumentCount < 0) {
            createMultiValueSelector(composite);
        }

        return parent;
    }

    private void createSingleValueEditor(Composite composite) {
        Composite editorPlaceholder = UIUtils.createPlaceholder(composite, 1);

        editorPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorPlaceholder.setLayout(new FillLayout());

        ResultSetRow singleRow = handler.getRows()[0];
        final ResultSetValueController valueController = new ResultSetValueController(
            handler.getViewer(),
            handler.getAttribute(),
            singleRow,
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
        try {
            editor.primeEditorValue(valueController.getValue());
        } catch (DBException e) {
            log.error("Error populating filter value", e);
        }
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
	        new Action("Select &All") {
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

        handler.addFilterTextbox(composite);

        handler.setFilterPattern(null);
        handler.loadValues(null);

        columnController.createColumns(true);
    }

	public Object getValue() {
        return value;
    }

	@Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (handler.getOperator().getArgumentCount() == 1) {
            Button copyButton = createButton(parent, IDialogConstants.DETAILS_ID, "Clipboard", false);
            copyButton.setImage(DBeaverIcons.getImage(UIIcon.FILTER_CLIPBOARD));
        }

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            try {
                Object value = ResultSetUtils.getAttributeValueFromClipboard(handler.getAttribute());
                handler.getEditor().primeEditorValue(value);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Copy from clipboard", "Can't copy value", e);
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed()
    {
        value = handler.getFilterValue();
        super.okPressed();
    }

}
