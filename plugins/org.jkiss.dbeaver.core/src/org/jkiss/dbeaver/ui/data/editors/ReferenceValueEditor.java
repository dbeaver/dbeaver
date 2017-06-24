/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ReferenceValueEditor
 *
 * @author Serge Rider
 */
public class ReferenceValueEditor {

    private static final Log log = Log.getLog(ReferenceValueEditor.class);

    private IValueController valueController;
    private IValueEditor valueEditor;
    private DBSEntityReferrer refConstraint;
    private Table editorSelector;
    private SelectorLoaderJob loaderJob = null;

    public ReferenceValueEditor(IValueController valueController, IValueEditor valueEditor) {
        this.valueController = valueController;
        this.valueEditor = valueEditor;
    }

    public boolean isReferenceValue()
    {
        return getEnumerableConstraint() != null;
    }

    @Nullable
    private DBSEntityReferrer getEnumerableConstraint()
    {
        if (valueController instanceof IAttributeController) {
            return getEnumerableConstraint(((IAttributeController) valueController).getBinding());
        }
        return null;
    }

    public static DBSEntityReferrer getEnumerableConstraint(DBDAttributeBinding binding) {
        try {
            DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
            if (entityAttribute != null) {
                List<DBSEntityReferrer> refs = DBUtils.getAttributeReferrers(new VoidProgressMonitor(), entityAttribute);
                DBSEntityReferrer constraint = refs.isEmpty() ? null : refs.get(0);
                if (constraint instanceof DBSEntityAssociation &&
                    ((DBSEntityAssociation)constraint).getReferencedConstraint() instanceof DBSConstraintEnumerable)
                {
                    final DBSConstraintEnumerable refConstraint = (DBSConstraintEnumerable) ((DBSEntityAssociation) constraint).getReferencedConstraint();
                    if (refConstraint != null && refConstraint.supportsEnumeration()) {
                        return constraint;
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    public boolean createEditorSelector(final Composite parent)
    {
        if (!(valueController instanceof IAttributeController) || valueController.isReadOnly()) {
            return false;
        }
        refConstraint = getEnumerableConstraint();
        if (refConstraint == null) {
            return false;
        }

        if (refConstraint instanceof DBSEntityAssociation) {
            final DBSEntityAssociation association = (DBSEntityAssociation)refConstraint;
            if (association.getReferencedConstraint() != null) {
                final DBSEntity refTable = association.getReferencedConstraint().getParentObject();
                Composite labelGroup = UIUtils.createPlaceholder(parent, 2);
                labelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                Link dictLabel = UIUtils.createLink(
                    labelGroup,
                    NLS.bind(CoreMessages.dialog_value_view_label_dictionary, refTable.getName()), new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            // Open
                            final IWorkbenchWindow window = valueController.getValueSite().getWorkbenchWindow();
                            DBeaverUI.runInUI(window, new DBRRunnableWithProgress() {
                                @Override
                                public void run(DBRProgressMonitor monitor)
                                    throws InvocationTargetException, InterruptedException {
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
                dictLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                Link hintLabel = UIUtils.createLink(labelGroup, "(<a>Define Description</a>)", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditDictionaryPage editDictionaryPage = new EditDictionaryPage("Dictionary structure", refTable);
                        if (editDictionaryPage.edit(parent.getShell())) {
                            loaderJob.schedule();
                        }
                    }
                });
                hintLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
            }
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
                    Object value = selection[0].getData();
                    //editorControl.setText(selection[0].getText());
                    try {
                        valueEditor.primeEditorValue(value);
                    } catch (DBException e1) {
                        log.error(e1);
                    }
                }
            }
        });

        Control control = valueEditor.getControl();
        ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                Object curEditorValue;
                try {
                    curEditorValue = valueEditor.extractEditorValue();
                } catch (DBException e1) {
                    log.error(e1);
                    return;
                }
                // Try to select current value in the table
                final String curTextValue = valueController.getValueHandler().getValueDisplayString(
                    ((IAttributeController) valueController).getBinding(),
                    curEditorValue,
                    DBDDisplayFormat.UI);
                boolean valueFound = false;
                for (TableItem item : editorSelector.getItems()) {
                    if (item.getText(0).equals(curTextValue)) {
                        editorSelector.select(editorSelector.indexOf(item));
                        editorSelector.showItem(item);
                        valueFound = true;
                        break;
                    }
                }

                if (!valueFound) {
                    // Read dictionary
                    if (loaderJob.getState() == Job.RUNNING) {
                        // Cancel it and create new one
                        loaderJob.cancel();
                        loaderJob = new SelectorLoaderJob();
                    }
                    loaderJob.setPattern(curEditorValue);
                    if (loaderJob.getState() != Job.WAITING) {
                        loaderJob.schedule(100);
                    }
                }
            }
        };
        if (control instanceof Text) {
            ((Text)control).addModifyListener(modifyListener);
        } else if (control instanceof StyledText) {
            ((StyledText)control).addModifyListener(modifyListener);
        }

        loaderJob = new SelectorLoaderJob();
        final Object curValue = valueController.getValue();
        if (curValue instanceof Number) {
            loaderJob.setPattern(curValue);
        }
        loaderJob.schedule(500);

        return true;
    }

    private void updateDictionarySelector(Map<Object, String> keyValues, DBSEntityAttributeRef keyColumn, DBDValueHandler keyHandler) {
        if (editorSelector == null || editorSelector.isDisposed()) {
            return;
        }
        editorSelector.setRedraw(false);
        try {
            editorSelector.removeAll();
            for (Map.Entry<Object, String> entry : keyValues.entrySet()) {
                TableItem discItem = new TableItem(editorSelector, SWT.NONE);
                discItem.setText(0,
                    keyHandler.getValueDisplayString(
                        keyColumn.getAttribute(),
                        entry.getKey(),
                        DBDDisplayFormat.UI));
                discItem.setText(1, entry.getValue());
                discItem.setData(entry.getKey());
            }

            Control editorControl = valueEditor.getControl();
            if (editorControl != null && !editorControl.isDisposed()) {
                try {
                    Object curValue = valueEditor.extractEditorValue();
                    final String curTextValue = valueController.getValueHandler().getValueDisplayString(
                            ((IAttributeController) valueController).getBinding(),
                            curValue,
                            DBDDisplayFormat.UI);

                    TableItem curItem = null;
                    for (TableItem item : editorSelector.getItems()) {
                        if (item.getText(0).equals(curTextValue)) {
                            curItem = item;
                            break;
                        }
                    }
                    if (curItem != null) {
                        editorSelector.setSelection(curItem);
                        editorSelector.showItem(curItem);
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

    private class SelectorLoaderJob extends DataSourceJob {

        private Object pattern;

        private SelectorLoaderJob()
        {
            super(
                CoreMessages.dialog_value_view_job_selector_name + valueController.getValueName() + " possible values",
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
            if (editorSelector.isDisposed()) {
                return Status.OK_STATUS;
            }
            final Map<Object, String> keyValues = new TreeMap<>();
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
                List<DBDAttributeValue> precedingKeys = null;
                List<? extends DBSEntityAttributeRef> allColumns = CommonUtils.safeList(refConstraint.getAttributeReferences(monitor));
                if (allColumns.size() > 1 && allColumns.get(0) != fkColumn) {
                    // Our column is not a first on in foreign key.
                    // So, fill uo preceeding keys
                    List<DBDAttributeBinding> rowAttributes = attributeController.getRowController().getRowAttributes();
                    precedingKeys = new ArrayList<>();
                    for (DBSEntityAttributeRef precColumn : allColumns) {
                        if (precColumn == fkColumn) {
                            // Enough
                            break;
                        }
                        DBSEntityAttribute precAttribute = precColumn.getAttribute();
                        if (precAttribute != null) {
                            DBDAttributeBinding rowAttr = DBUtils.findBinding(rowAttributes, precAttribute);
                            if (rowAttr != null) {
                                Object precValue = attributeController.getRowController().getAttributeValue(rowAttr);
                                precedingKeys.add(new DBDAttributeValue(precAttribute, precValue));
                            }
                        }
                    }
                }
                final DBSEntityAttribute fkAttribute = fkColumn.getAttribute();
                final DBSEntityConstraint refConstraint = association.getReferencedConstraint();
                final DBSConstraintEnumerable enumConstraint = (DBSConstraintEnumerable) refConstraint;
                if (fkAttribute != null && enumConstraint != null) {
                    try (DBCSession session = getExecutionContext().openSession(
                        monitor,
                        DBCExecutionPurpose.UTIL,
                        NLS.bind(CoreMessages.dialog_value_view_context_name, fkAttribute.getName()))) {
                        Collection<DBDLabelValuePair> enumValues = enumConstraint.getKeyEnumeration(
                            session,
                            refColumn,
                            pattern,
                            precedingKeys,
                            200);
                        for (DBDLabelValuePair pair : enumValues) {
                            keyValues.put(pair.getValue(), pair.getLabel());
                        }
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        final DBDValueHandler colHandler = DBUtils.findValueHandler(session, fkAttribute);
                        DBeaverUI.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                updateDictionarySelector(keyValues, fkColumn, colHandler);
                            }
                        });
                    }
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
