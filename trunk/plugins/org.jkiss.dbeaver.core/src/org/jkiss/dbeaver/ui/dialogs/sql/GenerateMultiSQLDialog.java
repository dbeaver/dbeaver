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
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * Super class for handling dialogs related to
 * 
 * @author Serge Rieder
 * 
 */
public abstract class GenerateMultiSQLDialog<T extends DBSObject> extends GenerateSQLDialog {

    protected final Collection<T> selectedObjects;
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

    public GenerateMultiSQLDialog(
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

    protected abstract SQLScriptProgressListener<T> getScriptListener();

    protected String[] generateSQLScript()
    {
        List<T> checkedObjects = getCheckedObjects();
        List<String> lines = new ArrayList<String>();
        for (T object : checkedObjects) {
            generateObjectCommand(lines, object);
        }

        return lines.toArray(new String[lines.size()]);
    }

    public List<T> getCheckedObjects() {
        List<T> checkedObjects = new ArrayList<T>();
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
            item.setText(DBUtils.getObjectFullName(table));
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
        final Map<T, List<String>> objectsSQL = new LinkedHashMap<T, List<String>>();
        for (T object : objects) {
            final List<String> lines = new ArrayList<String>();
            generateObjectCommand(lines, object);
            objectsSQL.put(object, lines);
        }
        final DataSourceJob job = new DataSourceJob(jobName, null, getExecutionContext()) {
            public Exception objectProcessingError;

            @Override
            protected IStatus run(final DBRProgressMonitor monitor)
            {
                final DataSourceJob curJob = this;
                UIUtils.runInDetachedUI(getShell(), new Runnable() {
                    @Override
                    public void run() {
                        scriptListener.beginScriptProcessing(curJob, objects);
                    }
                });
                monitor.beginTask(jobName, objects.size());
                DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, jobName);
                try {
                    for (int i = 0; i < objects.size(); i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final int objectNumber = i;
                        final T object = objects.get(i);
                        monitor.subTask("Process " + DBUtils.getObjectFullName(object));
                        objectProcessingError = null;
                        UIUtils.runInDetachedUI(getShell(), new Runnable() {
                            @Override
                            public void run() {
                                scriptListener.beginObjectProcessing(object, objectNumber);
                            }
                        });
                        try {
                            final List<String> lines = objectsSQL.get(object);
                            for (String line : lines) {
                                DBCStatement statement = DBUtils.prepareStatement(session, line, false);
                                try {
                                    if (statement.executeStatement()) {
                                        final DBCResultSet resultSet = statement.openResultSet();
                                        try {
                                            // Run in sync because we need result set
                                            UIUtils.runInUI(getShell(), new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        scriptListener.processObjectResults(object, resultSet);
                                                    } catch (DBCException e) {
                                                        objectProcessingError = e;
                                                    }
                                                }
                                            });
                                        } finally {
                                            resultSet.close();
                                        }
                                        if (objectProcessingError != null) {
                                            break;
                                        }
                                    }
                                } finally {
                                    statement.close();
                                }
                            }
                        } catch (Exception e) {
                            objectProcessingError = e;
                        } finally {
                            UIUtils.runInDetachedUI(getShell(), new Runnable() {
                                @Override
                                public void run() {
                                    scriptListener.endObjectProcessing(object, objectProcessingError);
                                }
                            });
                        }
                        monitor.worked(1);
                    }
                } finally {
                    session.close();
                    monitor.done();
                    UIUtils.runInDetachedUI(getShell(), new Runnable() {
                        @Override
                        public void run() {
                            scriptListener.endScriptProcessing();
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(false);
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
            }
        });
        job.schedule();
    }

    protected abstract void generateObjectCommand(List<String> sql, T object);

    protected static <T extends DBSObject> DBCExecutionContext getContextFromObjects(@NotNull Collection<T> objects, boolean meta) {
        Iterator<T> iterator = objects.iterator();
        if (iterator.hasNext()) {
            T object = iterator.next();
            DBPDataSource dataSource = object.getDataSource();
            return dataSource == null ? null : dataSource.getDefaultContext(meta);
        }
        return null;
    }

}