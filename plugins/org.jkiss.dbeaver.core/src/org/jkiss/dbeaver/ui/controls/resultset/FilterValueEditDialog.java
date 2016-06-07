/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

class FilterValueEditDialog extends BaseDialog {

    private static final Log log = Log.getLog(FilterValueEditDialog.class);
    public static final int MAX_MULTI_VALUES = 1000;

    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final DBDAttributeBinding attr;
    @NotNull
    private final ResultSetRow[] rows;
    @NotNull
    private final DBCLogicalOperator operator;

    private Object value;
    private IValueEditor editor;
    private Text textControl;
    private Table table;

    public FilterValueEditDialog(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows, @NotNull DBCLogicalOperator operator) {
        super(viewer.getControl().getShell(), "Edit value", null);
        this.viewer = viewer;
        this.attr = attr;
        this.rows = rows;
        this.operator = operator;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        Label label = new Label(composite, SWT.NONE);
        label.setText(attr.getName() + " " + operator.getStringValue() + " :");
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int argumentCount = operator.getArgumentCount();
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

        ResultSetRow singleRow = rows[0];
        final ResultSetValueController valueController = new ResultSetValueController(
            viewer,
            attr,
            singleRow,
            IValueController.EditType.INLINE,
            editorPlaceholder) {
            @Override
            public boolean isReadOnly() {
                // Filter value is never read-only
                return false;
            }
        };

        try {
            editor = valueController.getValueManager().createEditor(valueController);
            if (editor != null) {
                editor.createControl();
                editor.primeEditorValue(valueController.getValue());
            }
        } catch (DBException e) {
            log.error("Can't create inline value editor", e);
        }
        if (editor == null) {
            textControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            textControl.setText("");
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.heightHint = 300;
            gd.minimumHeight = 100;
            gd.minimumWidth = 100;
            textControl.setLayoutData(gd);
        }
    }

    private void createMultiValueSelector(Composite composite) {
        table = new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
        table.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        table.setLayoutData(gd);

        if (attr.getEntityAttribute() instanceof DBSAttributeEnumerable) {
            loadAttributeEnum((DBSAttributeEnumerable) attr.getEntityAttribute());
        } else {
            java.util.Map<String, DBDLabelValuePair> enumList = new TreeMap<>();
            for (ResultSetRow row : viewer.getModel().getAllRows()) {
                Object cellValue = viewer.getModel().getCellValue(attr, row);
                String itemString = attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);
                enumList.put(itemString, new DBDLabelValuePair(itemString, cellValue));
            }
            loadMultiValueList(enumList.values());
        }
    }

    private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable) {
        final AbstractJob loadJob = new AbstractJob("Load '" + attr.getName() + "' values") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                final DBCExecutionContext executionContext = viewer.getExecutionContext();
                if (executionContext == null) {
                    return Status.OK_STATUS;
                }
                try (DBCSession session = DBUtils.openUtilSession(monitor, executionContext.getDataSource(), "Read value enumeration")) {
                    final Collection<DBDLabelValuePair> valueEnumeration = attributeEnumerable.getValueEnumeration(session, null, MAX_MULTI_VALUES);
                    UIUtils.runInDetachedUI(null, new Runnable() {
                        @Override
                        public void run() {
                            loadMultiValueList(valueEnumeration);
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        };
        loadJob.schedule();
    }

    private void loadMultiValueList(Collection<DBDLabelValuePair> values) {
        for (DBDLabelValuePair row : values) {
            Object cellValue = row.getValue();
            String itemString = attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);

            TableItem item = new TableItem(table, SWT.LEFT);
            item.setData(cellValue);
            item.setText(itemString);
            if (ArrayUtils.contains(rows, row)) {
                item.setChecked(true);
            }
        }
        UIUtils.packColumns(table);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (operator.getArgumentCount() == 1) {
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
                Object value = ResultSetUtils.getAttributeValueFromClipboard(attr);
                editor.primeEditorValue(value);
            } catch (DBException e) {
                UIUtils.showErrorDialog(getShell(), "Copy from clipboard", "Can't copy value", e);
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed()
    {
        if (table != null) {
            java.util.List<Object> values = new ArrayList<>();
            for (TableItem item : table.getItems()) {
                if (item.getChecked()) {
                    values.add(item.getData());
                }
            }
            value = values.toArray();
        } else if (editor != null) {
            try {
                value = editor.extractEditorValue();
            } catch (DBException e) {
                log.error("Can't get editor value", e);
            }
        } else {
            value = textControl.getText();
        }
        super.okPressed();
    }

    public Object getValue() {
        return value;
    }
}
