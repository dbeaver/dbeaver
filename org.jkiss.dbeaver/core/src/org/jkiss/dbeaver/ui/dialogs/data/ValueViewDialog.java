/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import net.sf.jkiss.utils.CommonUtils;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog implements DBDValueEditor {

    private static int dialogCount = 0;

    private DBDValueController valueController;
    private DBSTableColumn refTableColumn;
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
        createButton(parent, IDialogConstants.IGNORE_ID, "Set &NULL", false).setEnabled(!valueController.isReadOnly() && !DBSUtils.isNullValue(valueController.getValue()));
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
            applyChanges();

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

    protected abstract void applyChanges();

    protected void createEditorSelector(Composite parent, Text control)
    {
        if (getValueController().isReadOnly()) {
            return;
        }
        refTableColumn = DBSUtils.getUniqueReferenceColumn(valueController.getColumnMetaData());
        if (refTableColumn == null) {
            return;
        }

        this.editor = control;

        Label label = new Label(parent, SWT.NONE);
        label.setText("Dictionary: ");

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
                    new SelectorLoaderJob(editor.getText()).schedule();
                }
            }
        });
        handleEditorChange = true;

        new SelectorLoaderJob().schedule();
    }

    private class SelectorLoaderJob extends DataSourceJob {

        private String autoComplete;

        private SelectorLoaderJob(String autoComplete)
        {
            this();
            this.autoComplete = autoComplete.trim();
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
            final Map<String, Object> keyValues = new TreeMap<String, Object>();
            DBCExecutionContext context = valueController.getDataSource().openContext(monitor, getName());
            try {
                String query = "SELECT " + refTableColumn.getName() + " FROM " + refTableColumn.getTable().getFullQualifiedName();
                if (!CommonUtils.isEmpty(autoComplete)) {
                    autoComplete = autoComplete.replace('\'', '"');
                    query += " WHERE " + refTableColumn.getName() + " LIKE '" + autoComplete + "%'";
                }
                DBCStatement dbStat = context.prepareStatement(
                    query,
                    false,
                    false);
                try {
                    dbStat.setLimit(0, 100);
                    if (dbStat.executeStatement()) {
                        DBCResultSet dbResult = dbStat.openResultSet();
                        try {
                            while (dbResult.nextRow()) {
                                Object keyValue = dbResult.getColumnValue(1);
                                if (keyValue != null) {
                                    keyValues.put(keyValue.toString(), keyValue);
                                }
                            }
                        }
                        finally {
                            dbResult.close();
                        }
                    }
                }
                finally {
                    dbStat.close();
                }
            }
            catch (DBCException e) {
                // do nothing
            }
            finally {
                context.close();
            }
            valueController.getValueSite().getShell().getDisplay().syncExec(new Runnable() {
                public void run()
                {
                    if (editorSelector != null && !editorSelector.isDisposed()) {
                        editorSelector.removeAll();
                        for (String key : keyValues.keySet()) {
                            editorSelector.add(key);
                        }

                        if (editor != null && !editor.isDisposed()) {
                            String curValue = editor.getText();
                            int selIndex = editorSelector.indexOf(curValue);
                            if (selIndex >= 0) {
                                editorSelector.select(selIndex);
                            }
                        }
                    }
                }
            });
            return Status.OK_STATUS;
        }
    }
}
