/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.OpenObjectEditorAction;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;
import org.jkiss.dbeaver.ui.editors.sql.SQLTableDataEditor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog implements DBDValueEditor {

    private static int dialogCount = 0;

    private DBDValueController valueController;
    private DBSForeignKey refConstraint;
    private Text editor;
    private org.eclipse.swt.widgets.List editorSelector;
    private boolean handleEditorChange;

    protected ValueViewDialog(DBDValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
        this.valueController.registerEditor(this);
        dialogCount++;
    }

    protected boolean isForeignKey()
    {
        return getEnumerableConstraint() != null;
    }

    public DBDValueController getValueController() {
        return valueController;
    }

    public void showValueEditor() {
        getShell().setFocus();
    }

    public void closeValueEditor() {
        this.setReturnCode(CANCEL);
        this.close();
    }

    @Override
    public boolean close() {
        dialogCount--;
        if (this.valueController != null) {
            this.valueController.unregisterEditor(this);
            this.valueController = null;
        }
        return super.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        new ColumnInfoPanel(dialogGroup, SWT.BORDER, getValueController());

        return dialogGroup;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, "&Save", true).setEnabled(!valueController.isReadOnly());
        createButton(parent, IDialogConstants.IGNORE_ID, "Set &NULL", false).setEnabled(!valueController.isReadOnly() && !DBUtils.isNullValue(valueController.getValue()));
        createButton(parent, IDialogConstants.CANCEL_ID, "&Cancel", false);
    }

    protected void initializeBounds()
    {
        super.initializeBounds();

        Shell shell = getShell();
        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 3;
        x += dialogCount * 20;
        y += dialogCount * 20;
        shell.setLocation (x, y);
    }

    @Override
    protected void okPressed()
    {
        try {
            Object value = getEditorValue();
            getValueController().updateValue(value);

            super.okPressed();
        }
        catch (Exception e) {
            DBeaverUtils.showErrorDialog(getShell(), "Error updating column", "Could not update column value", e);
            super.cancelPressed();
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.IGNORE_ID) {
            if (!valueController.isReadOnly()) {
                getValueController().updateValue(null);
            }
            super.okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        DBCColumnMetaData meta = valueController.getColumnMetaData();
        shell.setText(meta.getTableName() + "." + meta.getColumnName());
    }

    protected abstract Object getEditorValue();

    private DBSForeignKey getEnumerableConstraint()
    {
        DBSForeignKey constraint = DBUtils.getUniqueForeignConstraint(valueController.getColumnMetaData());
        if (constraint != null &&
            constraint.getReferencedKey() instanceof DBSConstraintEnumerable &&
            ((DBSConstraintEnumerable)constraint.getReferencedKey()).supportsEnumeration())
        {
            return constraint;
        } else {
            return null;
        }
    }

    protected void createEditorSelector(Composite parent, Text control)
    {
        if (getValueController().isReadOnly()) {
            return;
        }
        refConstraint = getEnumerableConstraint();
        if (refConstraint == null) {
            return;
        }

        this.editor = control;

        Link label = new Link(parent, SWT.NONE);
        label.setText("Dictionary  (<a>" + refConstraint.getReferencedKey().getTable().getName() + "</a>): ");
        label.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // Open
                final IWorkbenchWindow window = valueController.getValueSite().getWorkbenchWindow();
                DBeaverCore.getInstance().runInUI(window, new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        DBMNode tableNode = DBeaverCore.getInstance().getMetaModel().getNodeByObject(monitor, refConstraint.getReferencedKey().getTable(), true);
                        if (tableNode != null) {
                            OpenObjectEditorAction.openEntityEditor(tableNode, SQLTableDataEditor.class.getName(), window);
                        }
                    }
                });
            }
        });
        

        editorSelector = new org.eclipse.swt.widgets.List(parent, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        gd.widthHint = 300;
        gd.grabExcessVerticalSpace = true;
        editorSelector.setLayoutData(gd);

        editorSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String[] selection = editorSelector.getSelection();
                if (selection.length == 1) {
                    handleEditorChange = false;
                    editor.setText(selection[0]);
                    handleEditorChange = true;
                }
            }
        });
        editor.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                if (handleEditorChange) {
                    new SelectorLoaderJob(getEditorValue()).schedule();
                }
            }
        });
        handleEditorChange = true;

        new SelectorLoaderJob().schedule();
    }

    private class SelectorLoaderJob extends DataSourceJob {

        private Object pattern;

        private SelectorLoaderJob(Object pattern)
        {
            this();
            this.pattern = pattern;
        }

        private SelectorLoaderJob()
        {
            super(
                "Select " + valueController.getColumnMetaData().getColumnName() + " possible values",
                DBIcon.SQL_EXECUTE.getImageDescriptor(),
                valueController.getDataSource());
            setUser(false);
        }

        protected IStatus run(DBRProgressMonitor monitor)
        {
            final Map<Object, String> keyValues = new TreeMap<Object, String>();
            try {
                DBSForeignKeyColumn fkColumn = refConstraint.getColumn(monitor, valueController.getColumnMetaData().getTableColumn(monitor));
                if (fkColumn == null) {
                    return Status.OK_STATUS;
                }
                java.util.List<DBDColumnValue> preceedingKeys = null;
                Collection<? extends DBSForeignKeyColumn> allColumns = refConstraint.getColumns(monitor);
                if (allColumns.size() > 1) {
                    if (allColumns.iterator().next() != fkColumn) {
                        // Our column is not a first on in foreign key.
                        // So, fill uo preceeding keys
                        preceedingKeys = new ArrayList<DBDColumnValue>();
                        for (DBSForeignKeyColumn precColumn : allColumns) {
                            if (precColumn == fkColumn) {
                                // Enough
                                break;
                            }
                            DBCColumnMetaData precMeta = valueController.getRow().getColumnMetaData(
                                valueController.getColumnMetaData().getTable(), precColumn.getTableColumn().getName());
                            if (precMeta != null) {
                                Object precValue = valueController.getRow().getColumnValue(precMeta);
                                preceedingKeys.add(new DBDColumnValue(precColumn.getTableColumn(), precValue));
                            }
                        }
                    }
                }
                DBSConstraintEnumerable enumConstraint = (DBSConstraintEnumerable)refConstraint.getReferencedKey();
                Collection<DBDLabelValuePair> enumValues = enumConstraint.getKeyEnumeration(
                    monitor,
                    fkColumn.getReferencedColumn(),
                    pattern,
                    preceedingKeys,
                    100);
                for (DBDLabelValuePair pair : enumValues) {
                    keyValues.put(pair.getValue(), pair.getLabel());
                }
            } catch (DBException e) {
                // error
                // just ignore
            }
            valueController.getValueSite().getShell().getDisplay().syncExec(new Runnable() {
                public void run()
                {
                    if (editorSelector != null && !editorSelector.isDisposed()) {
                        editorSelector.removeAll();
                        for (Map.Entry<Object, String> entry : keyValues.entrySet()) {
                            editorSelector.add(entry.getValue());
                        }

                        if (editor != null && !editor.isDisposed()) {
                            String curValue = editor.getText();
                            int selIndex = editorSelector.indexOf(curValue);
                            if (selIndex >= 0) {
                                editorSelector.select(selIndex);
                                editorSelector.showSelection();
                            }
                        }
                    }
                }
            });
            return Status.OK_STATUS;
        }
    }
}
