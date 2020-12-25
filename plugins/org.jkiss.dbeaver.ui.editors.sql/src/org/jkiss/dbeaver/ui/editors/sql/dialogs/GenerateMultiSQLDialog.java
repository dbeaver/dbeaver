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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Super class for handling dialogs related to
 * 
 * @author Serge Rider
 * 
 */
public abstract class GenerateMultiSQLDialog<T extends DBSObject> extends GenerateSQLDialog {

    private static final Log log = Log.getLog(GenerateMultiSQLDialog.class);

    private static final String DIALOG_ID = "GenerateMultiSQLDialog";

    private final Collection<T> selectedObjects;
    private Table objectsTable;

    public GenerateMultiSQLDialog(
        IWorkbenchPartSite partSite,
        String title,
        Collection<T> objects,
        boolean meta)
    {
        this(
            partSite,
            getContextFromObjects(objects, meta),
            title,
            objects);
    }

    private GenerateMultiSQLDialog(
        IWorkbenchPartSite partSite,
        DBCExecutionContext context,
        String title,
        Collection<T> objects)
    {
        super(
            partSite,
            context,
            title,
            null);
        this.selectedObjects = objects;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        String dialogClassName = getClass().getName();
        int divPos = dialogClassName.lastIndexOf('.');
        dialogClassName = divPos == -1 ? dialogClassName : dialogClassName.substring(divPos + 1);
        return UIUtils.getDialogSettings(DIALOG_ID + "." + dialogClassName);
    }

    protected abstract SQLScriptProgressListener<T> getScriptListener();

    protected String[] generateSQLScript()
    {
        List<T> checkedObjects = getCheckedObjects();
        List<String> lines = new ArrayList<>();
        for (T object : checkedObjects) {
            generateObjectCommand(lines, object);
        }

        return lines.toArray(new String[0]);
    }

    public List<T> getCheckedObjects() {
        List<T> checkedObjects = new ArrayList<>();
        if (objectsTable != null) {
            for (TableItem item : objectsTable.getItems()) {
                if (item.getChecked()) {
                    checkedObjects.add((T) item.getData());
                }
            }
        } else {
            checkedObjects.addAll(selectedObjects);
        }
        return checkedObjects;
    }

    protected void createObjectsSelector(Composite parent) {
        if (selectedObjects.size() < 2) {
            // Don't need it for a single object
            return;
        }
        UIUtils.createControlLabel(parent, "Tables");
        objectsTable = new Table(parent, SWT.BORDER | SWT.CHECK);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 100;
        objectsTable.setLayoutData(gd);
        for (T table : selectedObjects) {
            TableItem item = new TableItem(objectsTable, SWT.NONE);
            item.setText(DBUtils.getObjectFullName(table, DBPEvaluationContext.UI));
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
            item.setChecked(true);
            item.setData(table);
        }
        objectsTable.addSelectionListener(SQL_CHANGE_LISTENER);
        objectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasChecked = !getCheckedObjects().isEmpty();
                getButton(IDialogConstants.OK_ID).setEnabled(hasChecked);
                getButton(IDialogConstants.DETAILS_ID).setEnabled(hasChecked);
            }
        });
    }

    @Override
    protected void executeSQL() {
        final String jobName = getShell().getText();
        final SQLScriptProgressListener<T> scriptListener = getScriptListener();
        final List<T> objects = getCheckedObjects();
        final Map<T, List<String>> objectsSQL = new LinkedHashMap<>();
        for (T object : objects) {
            final List<String> lines = new ArrayList<>();
            generateObjectCommand(lines, object);
            objectsSQL.put(object, lines);
        }
        final DataSourceJob job = new DataSourceJob(jobName, getExecutionContext()) {
            Exception objectProcessingError;

            @Override
            protected IStatus run(final DBRProgressMonitor monitor)
            {
                final DataSourceJob curJob = this;
                UIUtils.asyncExec(() -> scriptListener.beginScriptProcessing(curJob, objects));
                monitor.beginTask(jobName, objects.size());
                try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
                    if (isRunInSeparateTransaction()) {
                        commitChanges(session);
                    }
                    for (int i = 0; i < objects.size(); i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final int objectNumber = i;
                        final T object = objects.get(i);
                        monitor.subTask("Process " + DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
                        objectProcessingError = null;
                        UIUtils.asyncExec(() -> scriptListener.beginObjectProcessing(object, objectNumber));
                        try {
                            final List<String> lines = objectsSQL.get(object);
                            for (String line : lines) {
                                try (final DBCStatement statement = DBUtils.makeStatement(session, line, false)) {
                                    if (statement.executeStatement()) {
                                        try (DBCResultSet resultSet = statement.openResultSet()) {
                                            // Run in sync because we need result set
                                            UIUtils.syncExec(() -> {
                                                try {
                                                    scriptListener.processObjectResults(object, statement, resultSet);
                                                } catch (DBCException e) {
                                                    objectProcessingError = e;
                                                }
                                            });
                                        }
                                        if (objectProcessingError != null) {
                                            break;
                                        }
                                    } else {
                                        UIUtils.syncExec(() -> {
                                            try {
                                                scriptListener.processObjectResults(object, statement, null);
                                            } catch (DBCException e) {
                                                objectProcessingError = e;
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            objectProcessingError = e;
                        } finally {
                            UIUtils.asyncExec(() -> scriptListener.endObjectProcessing(object, objectProcessingError));
                        }
                        monitor.worked(1);
                    }
                    if (isRunInSeparateTransaction()) {
                        commitChanges(session);
                    }

                } finally {
                    monitor.done();
                    UIUtils.asyncExec(scriptListener::endScriptProcessing);
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(false);
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (needsRefreshOnFinish()) {
                    List<T> objectToRefresh = new ArrayList<>(selectedObjects);
                    UIUtils.asyncExec(() -> {
                        try {
                            UIUtils.runInProgressDialog(monitor -> {
                                monitor.beginTask("Refresh objects", objectToRefresh.size());
                                for (T object : objectToRefresh) {
                                    try {
                                        DBNDatabaseNode objectNode = DBNUtils.getNodeByObject(object);
                                        if (objectNode != null) {
                                            objectNode.refreshNode(monitor, DBNEvent.FORCE_REFRESH);
                                        }
                                    } catch (Exception e) {
                                        log.error("Error refreshing object '" + object.getName() + "'", e);
                                    }
                                }
                                monitor.done();
                            });
                        } catch (InvocationTargetException e) {
                            DBWorkbench.getPlatformUI().showError("Objects refresh", "Error refreshing navigator objects", e);
                        }
                    });
                }
            }
        });
        job.schedule();
    }

    protected boolean isRunInSeparateTransaction() {
        return false;
    }

    protected boolean needsRefreshOnFinish() {
        return false;
    }

    protected abstract void generateObjectCommand(List<String> sql, T object);

    private static <T extends DBSObject> DBCExecutionContext getContextFromObjects(@NotNull Collection<T> objects, boolean meta) {
        Iterator<T> iterator = objects.iterator();
        if (iterator.hasNext()) {
            T object = iterator.next();
            if (object != null) {
                return DBUtils.getDefaultContext(object, meta);
            }
        }
        return null;
    }

    private void commitChanges(DBCSession session) {
        try {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
                txnManager.commit(session);
            }
        } catch (Throwable e) {
            log.error("Error committing transactions", e);
        }
    }
}