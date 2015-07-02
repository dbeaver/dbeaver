/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;
import org.jkiss.dbeaver.ui.data.*;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.struct.EditDictionaryDialog;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog implements IValueEditorStandalone {

    static final Log log = Log.getLog(ValueViewDialog.class);

    private static int dialogCount = 0;
    public static final String SETTINGS_SECTION_DI = "ValueViewDialog";

    private IValueController valueController;
    private DBSEntityReferrer refConstraint;
    private Table editorSelector;
    private boolean handleEditorChange;
    private SelectorLoaderJob loaderJob = null;
    private Object editedValue;
    private boolean columnInfoVisible = true;
    private ColumnInfoPanel columnPanel;
    private final IDialogSettings dialogSettings;
    private boolean opened;

    protected ValueViewDialog(IValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
        dialogSettings = UIUtils.getDialogSettings(SETTINGS_SECTION_DI);
        if (dialogSettings.get(getInfoVisiblePrefId()) != null) {
            columnInfoVisible = dialogSettings.getBoolean(getInfoVisiblePrefId());
        }
        dialogCount++;
    }

    @Override
    public void createControl() {

    }

    protected IDialogSettings getDialogSettings()
    {
        return dialogSettings;
    }

    protected boolean isForeignKey()
    {
        return getEnumerableConstraint() != null;
    }

    @Nullable
    protected IValueEditor createPanelEditor(final Composite placeholder)
        throws DBException
    {
        IValueEditor editor = valueController.getValueManager().createEditor(new IValueController() {
            @NotNull
            @Override
            public DBCExecutionContext getExecutionContext() {
                return valueController.getExecutionContext();
            }

            @Override
            public String getValueName()
            {
                return valueController.getValueName();
            }

            @Override
            public DBSTypedObject getValueType()
            {
                return valueController.getValueType();
            }

            @Override
            public Object getValue()
            {
                return valueController.getValue();
            }

            @Override
            public void updateValue(Object value)
            {
                valueController.updateValue(value);
            }

            @Override
            public DBDValueHandler getValueHandler()
            {
                return valueController.getValueHandler();
            }

            @Override
            public IValueManager getValueManager() {
                return valueController.getValueManager();
            }

            @Override
            public EditType getEditType()
            {
                return EditType.PANEL;
            }

            @Override
            public boolean isReadOnly()
            {
                return valueController.isReadOnly();
            }

            @Override
            public IWorkbenchPartSite getValueSite()
            {
                return valueController.getValueSite();
            }

            @Override
            public Composite getEditPlaceholder()
            {
                return placeholder;
            }

            @Override
            public IContributionManager getEditBar()
            {
                return null;
            }

            @Override
            public void closeInlineEditor()
            {
            }

            @Override
            public void nextInlineEditor(boolean next)
            {
            }

            @Override
            public void unregisterEditor(IValueEditorStandalone editor)
            {
            }

            @Override
            public void showMessage(String message, boolean error)
            {
            }
        });
        if (editor != null) {
            editor.createControl();
        }
        return editor;
    }

    public IValueController getValueController() {
        return valueController;
    }

    @Override
    public void showValueEditor() {
        if (!opened) {
            open();
        } else {
            getShell().setFocus();
        }
    }

    @Override
    public void closeValueEditor() {
        if (this.valueController != null) {
            this.valueController.unregisterEditor(this);
            this.valueController = null;
        }
        this.setReturnCode(CANCEL);
        this.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
/*
        SashForm sash = new SashForm(parent, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite dialogGroup = (Composite)super.createDialogArea(sash);
        dialogGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new ColumnInfoPanel(dialogGroup, SWT.BORDER, getValueController());
        Composite editorGroup = (Composite) super.createDialogArea(sash);
        editorGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        //editorGroup.setLayout(new GridLayout(1, false));
        return editorGroup;

*/
        Composite dialogGroup = (Composite)super.createDialogArea(parent);
        if (valueController instanceof IAttributeController) {
            final Link columnHideLink = new Link(dialogGroup, SWT.NONE);
            columnHideLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    columnInfoVisible = !columnInfoVisible;
                    dialogSettings.put(getInfoVisiblePrefId(), columnInfoVisible);
                    initColumnInfoVisibility(columnHideLink);
                    getShell().layout();
                    int width = getShell().getSize().x;
                    getShell().setSize(width, getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
                }
            });

            columnPanel = new ColumnInfoPanel(dialogGroup, SWT.BORDER, valueController);
            columnPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            initColumnInfoVisibility(columnHideLink);
        }

        return dialogGroup;
    }

    private void initColumnInfoVisibility(Link columnHideLink)
    {
        columnPanel.setVisible(columnInfoVisible);
        ((GridData)columnPanel.getLayoutData()).exclude = !columnInfoVisible;
        columnHideLink.setText("Column Info: (<a>" + (columnInfoVisible ? "hide" : "show") + "</a>)");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, CoreMessages.dialog_value_view_button_save, true)
            .setEnabled(!valueController.isReadOnly());
        boolean required = false;//valueController.getValueType() instanceof DBSAttributeBase && ((DBSAttributeBase) valueController.getValueType()).isRequired();
        createButton(parent, IDialogConstants.IGNORE_ID, CoreMessages.dialog_value_view_button_sat_null, false)
            .setEnabled(!valueController.isReadOnly() && !DBUtils.isNullValue(valueController.getValue()) && !required);
        createButton(parent, IDialogConstants.CANCEL_ID, CoreMessages.dialog_value_view_button_cancel, false);
    }

    @Override
    protected void initializeBounds()
    {
        super.initializeBounds();

        Shell shell = getShell();

        String sizeString = dialogSettings.get(getDialogSizePrefId());
        if (!CommonUtils.isEmpty(sizeString) && sizeString.contains(":")) {
            int divPos = sizeString.indexOf(':');
            shell.setSize(new Point(
                Integer.parseInt(sizeString.substring(0, divPos)),
                Integer.parseInt(sizeString.substring(divPos + 1))));
            shell.layout();
        }

        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 3;
        x += dialogCount * 20;
        y += dialogCount * 20;
        shell.setLocation(x, y);
    }

    private String getInfoVisiblePrefId()
    {
        return getClass().getSimpleName() + "-" +
            CommonUtils.escapeIdentifier(getValueController().getValueType().getTypeName()) +
            "-columnInfoVisible";
    }

    private String getDialogSizePrefId()
    {
        return getClass().getSimpleName() + "-" +
            CommonUtils.escapeIdentifier(getValueController().getValueType().getTypeName()) +
            "-dialogSize";
    }

    @Override
    public final int open()
    {
        try {
            opened = true;
            int result = super.open();
            if (result == IDialogConstants.OK_ID) {
                getValueController().updateValue(editedValue);
            }
            return result;
        } finally {
            dialogCount--;
            if (this.valueController != null) {
                this.valueController.unregisterEditor(this);
                this.valueController = null;
            }
        }
    }

    @Override
    protected void okPressed()
    {
        try {
            editedValue = extractEditorValue();

            super.okPressed();
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_value_view_dialog_error_updating_title, CoreMessages.dialog_value_view_dialog_error_updating_message, e);
            super.cancelPressed();
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        Point size = getShell().getSize();
        String sizeString = size.x + ":" + size.y;
        dialogSettings.put(getDialogSizePrefId(), sizeString);

        if (buttonId == IDialogConstants.IGNORE_ID) {
            if (!valueController.isReadOnly()) {
                editedValue = BaseValueManager.makeNullValue(valueController);
            }
            super.okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (valueController instanceof IAttributeController) {
            DBSAttributeBase meta = ((IAttributeController)valueController).getBinding();
            shell.setText(meta.getName());
        }
    }

    @Nullable
    private DBSEntityReferrer getEnumerableConstraint()
    {
        if (valueController instanceof IAttributeController) {
            try {
                DBSEntityAttribute entityAttribute = ((IAttributeController) valueController).getBinding().getEntityAttribute();
                if (entityAttribute != null) {
                    java.util.List<DBSEntityReferrer> refs = DBUtils.getAttributeReferrers(VoidProgressMonitor.INSTANCE, entityAttribute);
                    DBSEntityReferrer constraint = refs.isEmpty() ? null : refs.get(0);
                    if (constraint instanceof DBSEntityAssociation &&
                        ((DBSEntityAssociation)constraint).getReferencedConstraint() instanceof DBSConstraintEnumerable &&
                        ((DBSConstraintEnumerable)((DBSEntityAssociation)constraint).getReferencedConstraint()).supportsEnumeration())
                    {
                        return constraint;
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
        return null;
    }

    protected void createEditorSelector(Composite parent)
    {
        if (!(valueController instanceof IAttributeController) || valueController.isReadOnly()) {
            return;
        }
        refConstraint = getEnumerableConstraint();
        if (refConstraint == null) {
            return;
        }

        if (refConstraint instanceof DBSEntityAssociation) {
            final DBSEntityAssociation association = (DBSEntityAssociation)refConstraint;
            final DBSEntity refTable = association.getReferencedConstraint().getParentObject();
            Composite labelGroup = UIUtils.createPlaceholder(parent, 2);
            labelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_HORIZONTAL));
            Link dictLabel = new Link(labelGroup, SWT.NONE);
            dictLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            dictLabel.setText(NLS.bind(CoreMessages.dialog_value_view_label_dictionary, refTable.getName()));
            dictLabel.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // Open
                    final IWorkbenchWindow window = valueController.getValueSite().getWorkbenchWindow();
                    DBeaverUI.runInUI(window, new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            DBNDatabaseNode tableNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(
                                monitor,
                                refTable,
                                true
                            );
                            if (tableNode != null) {
                                NavigatorHandlerObjectOpen.openEntityEditor(tableNode, DatabaseDataEditor.class.getName(), window);
                            }
                        }
                    });
                }
            });

            Link hintLabel = new Link(labelGroup, SWT.NONE);
            hintLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
            hintLabel.setText("(<a>Define Description</a>)");
            hintLabel.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    EditDictionaryDialog dialog = new EditDictionaryDialog(getShell(), "Dictionary structure", refTable);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        loaderJob.schedule();
                    }
                }
            });
        }

        editorSelector = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        editorSelector.setLinesVisible(true);
        editorSelector.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        //gd.widthHint = 300;
        //gd.grabExcessVerticalSpace = true;
        //gd.grabExcessHorizontalSpace = true;
        editorSelector.setLayoutData(gd);

        UIUtils.createTableColumn(editorSelector, SWT.LEFT, CoreMessages.dialog_value_view_column_value);
        UIUtils.createTableColumn(editorSelector, SWT.LEFT, CoreMessages.dialog_value_view_column_description);
        UIUtils.packColumns(editorSelector);

        editorSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = editorSelector.getSelection();
                if (selection != null && selection.length > 0) {
                    handleEditorChange = false;
                    Object value = selection[0].getData();
                    //editorControl.setText(selection[0].getText());
                    try {
                        primeEditorValue(value);
                    } catch (DBException e1) {
                        log.error(e1);
                    }
                    handleEditorChange = true;
                }
            }
        });

        Control control = getControl();
        ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (handleEditorChange) {
                    if (loaderJob.getState() == Job.RUNNING) {
                        // Cancel it and create new one
                        loaderJob.cancel();
                        loaderJob = new SelectorLoaderJob();
                    }
                    try {
                        loaderJob.setPattern(extractEditorValue());
                    } catch (DBException e1) {
                        log.error(e1);
                    }
                    if (loaderJob.getState() != Job.WAITING) {
                        loaderJob.schedule(500);
                    }
                }
            }
        };
        if (control instanceof Text) {
            ((Text)control).addModifyListener(modifyListener);
        } else if (control instanceof StyledText) {
            ((StyledText)control).addModifyListener(modifyListener);
        }
        handleEditorChange = true;

        loaderJob = new SelectorLoaderJob();
        loaderJob.schedule(500);
    }

    private class SelectorLoaderJob extends DataSourceJob {

        private Object pattern;

        private SelectorLoaderJob()
        {
            super(
                CoreMessages.dialog_value_view_job_selector_name + valueController.getValueName() + " possible values",
                DBeaverIcons.getImageDescriptor(UIIcon.SQL_EXECUTE),
                valueController.getExecutionContext());
            setUser(false);
        }

        void setPattern(@Nullable Object pattern)
        {
            this.pattern = pattern;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            final Map<Object, String> keyValues = new TreeMap<Object, String>();
            try {
                IAttributeController attributeController = (IAttributeController)valueController;
                final DBSEntityAttribute tableColumn = attributeController.getBinding().getEntityAttribute();
                if (tableColumn == null) {
                    return Status.OK_STATUS;
                }
                final DBSEntityAttributeRef fkColumn = DBUtils.getConstraintAttribute(monitor, refConstraint, tableColumn);
                if (fkColumn == null) {
                    return Status.OK_STATUS;
                }
                DBSEntityAssociation association;
                if (refConstraint instanceof DBSEntityAssociation) {
                    association = (DBSEntityAssociation)refConstraint;
                } else {
                    return Status.OK_STATUS;
                }
                final DBSEntityAttribute refColumn = DBUtils.getReferenceAttribute(monitor, association, tableColumn);
                if (refColumn == null) {
                    return Status.OK_STATUS;
                }
                java.util.List<DBDAttributeValue> precedingKeys = null;
                java.util.List<? extends DBSEntityAttributeRef> allColumns = CommonUtils.safeList(refConstraint.getAttributeReferences(monitor));
                if (allColumns.size() > 1 && allColumns.get(0) != fkColumn) {
                    // Our column is not a first on in foreign key.
                    // So, fill uo preceeding keys
                    List<DBDAttributeBinding> rowAttributes = attributeController.getRowController().getRowAttributes();
                    precedingKeys = new ArrayList<DBDAttributeValue>();
                    for (DBSEntityAttributeRef precColumn : allColumns) {
                        if (precColumn == fkColumn) {
                            // Enough
                            break;
                        }
                        DBSEntityAttribute precAttribute = precColumn.getAttribute();
                        DBDAttributeBinding rowAttr = DBUtils.findBinding(rowAttributes, precAttribute);
                        if (rowAttr != null) {
                            Object precValue = attributeController.getRowController().getAttributeValue(rowAttr);
                            precedingKeys.add(new DBDAttributeValue(precAttribute, precValue));
                        }
                    }
                }
                final DBCSession session = getExecutionContext().openSession(
                    monitor,
                    DBCExecutionPurpose.UTIL,
                    NLS.bind(CoreMessages.dialog_value_view_context_name, fkColumn.getAttribute().getName()));
                try {
                    final DBSEntityConstraint refConstraint = association.getReferencedConstraint();
                    DBSConstraintEnumerable enumConstraint = (DBSConstraintEnumerable) refConstraint;
                    Collection<DBDLabelValuePair> enumValues = enumConstraint.getKeyEnumeration(
                        session,
                        refColumn,
                        pattern,
                        precedingKeys,
                        100);
                    for (DBDLabelValuePair pair : enumValues) {
                        keyValues.put(pair.getValue(), pair.getLabel());
                    }
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    UIUtils.runInUI(getShell(), new Runnable() {
                        @Override
                        public void run()
                        {
                            DBDValueHandler colHandler = DBUtils.findValueHandler(session, fkColumn.getAttribute());

                            if (editorSelector != null && !editorSelector.isDisposed()) {
                                editorSelector.setRedraw(false);
                                try {
                                    editorSelector.removeAll();
                                    for (Map.Entry<Object, String> entry : keyValues.entrySet()) {
                                        TableItem discItem = new TableItem(editorSelector, SWT.NONE);
                                        discItem.setText(0,
                                            colHandler.getValueDisplayString(
                                                fkColumn.getAttribute(),
                                                entry.getKey(),
                                                DBDDisplayFormat.UI));
                                        discItem.setText(1, entry.getValue());
                                        discItem.setData(entry.getKey());
                                    }

                                    Control editorControl = getControl();
                                    if (editorControl != null && !editorControl.isDisposed()) {
                                        try {
                                            Object curValue = extractEditorValue();
                                            TableItem curItem = null;
                                            for (TableItem item : editorSelector.getItems()) {
                                                if (item.getData() == curValue || (item.getData() != null && curValue != null && item.getData().equals(curValue))) {
                                                    curItem = item;
                                                    break;
                                                }
                                            }
                                            if (curItem != null) {
                                                editorSelector.select(editorSelector.indexOf(curItem));
                                                editorSelector.showSelection();
                                            }
                                        } catch (DBException e) {
                                            log.error(e);
                                        }
                                    }

                                    UIUtils.maxTableColumnsWidth(editorSelector);
                                } finally {
                                    editorSelector.setRedraw(true);
                                }
                            }
                        }
                    });
                }
                finally {
                    session.close();
                }

            } catch (DBException e) {
                // error
                // just ignore
                log.warn(e);
            }
            return Status.OK_STATUS;
        }
    }
}
