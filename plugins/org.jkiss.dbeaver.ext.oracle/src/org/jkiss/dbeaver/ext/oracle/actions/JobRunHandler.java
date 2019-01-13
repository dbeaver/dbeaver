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
package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJob;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This handler provides the capability to run scheduled jobs.
 * The structure is copied from CompileHandler
 * @author crowne
 */
public class JobRunHandler extends OracleTaskHandler
{
    private static final Log log = Log.getLog(JobRunHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
        final List<OracleSchedulerJob> objects = getSelectedJobs(event);
        if (!objects.isEmpty()) {
            if (activePart instanceof EntityEditor) {
                // Save editor before run
                // Use null monitor as entity editor has its own detached job for save
                EntityEditor entityEditor = (EntityEditor) activePart;
                if (entityEditor.isDirty()) {
                    NullProgressMonitor monitor = new NullProgressMonitor();
                    entityEditor.doSave(monitor);
                    if (monitor.isCanceled()) {
                        // Save failed - doesn't make sense to compile
                        return null;
                    }
                }
            }
            final Shell activeShell = HandlerUtil.getActiveShell(event);
            if (objects.size() == 1) {
                final OracleSchedulerJob job = objects.get(0);

                final DBCCompileLog compileLog = new DBCCompileLogBase();
                compileLog.clearLog();
                Throwable error = null;
                try {
                    UIUtils.runInProgressService(monitor -> {
                        try {
                            runJob(monitor, compileLog, job);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                    if (compileLog.getError() != null) {
                        error = compileLog.getError();
                    }
                } catch (InvocationTargetException e) {
                    error = e.getTargetException();
                } catch (InterruptedException e) {
                    return null;
                }
                if (error != null) {
                    DBWorkbench.getPlatformUI().showError("Unexpected run schedule error", null, error);
                } else if (!CommonUtils.isEmpty(compileLog.getErrorStack())) {
                    // Show compile errors
                    int line = -1, position = -1;
                    StringBuilder fullMessage = new StringBuilder();
                    for (DBCCompileError oce : compileLog.getErrorStack()) {
                        fullMessage.append(oce.toString()).append(GeneralUtils.getDefaultLineSeparator());
                        if (line < 0) {
                            line = oce.getLine();
                            position = oce.getPosition();
                        }
                    }

                    String errorTitle = job.getName() + " run schedule failed";
                    DBWorkbench.getPlatformUI().showError(errorTitle, fullMessage.toString());
                } else {
                    String message = job.getName() + " successfully scheduled to run";
                    UIUtils.showMessageBox(activeShell, "Done", message, SWT.ICON_INFORMATION);
                }
            }
        }
        return null;
    }

    private List<OracleSchedulerJob> getSelectedJobs(ExecutionEvent event)
    {
        List<OracleSchedulerJob> objects = new ArrayList<>();
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext(); ) {
                final Object element = iter.next();
                final OracleSchedulerJob sourceJob = RuntimeUtils.getObjectAdapter(element, OracleSchedulerJob.class);
                if (sourceJob != null) {
                    objects.add(sourceJob);
                }
            }
        }
        if (objects.isEmpty()) {
            final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            final OracleSchedulerJob sourceJob = RuntimeUtils.getObjectAdapter(activePart, OracleSchedulerJob.class);
            if (sourceJob != null) {
                objects.add(sourceJob);
            }
        }
        return objects;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        List<OracleSourceObject> objects = getOracleSourceObjects(element);
        if (!objects.isEmpty()) {
            if (objects.size() > 1) {
                element.setText("Run " + objects.size() + " jobs");
            } else {
                final OracleSourceObject sourceObject = objects.get(0);
                String objectType = TextUtils.formatWord(sourceObject.getSourceType().name());
                element.setText("Run " + objectType/* + " '" + sourceObject.getName() + "'"*/);
            }
        }
    }

    public static boolean runJob(DBRProgressMonitor monitor, DBCCompileLog compileLog, OracleSchedulerJob job) throws DBCException
    {
        final DBEPersistAction[] compileActions = job.getRunActions();
        if (ArrayUtils.isEmpty(compileActions)) {
            return true;
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, job, "Run '" + job.getName() + "'")) {
            boolean success = true;
            for (DBEPersistAction action : compileActions) {
                final String script = action.getScript();
                compileLog.trace(script);

                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    try (DBCStatement dbStat = session.prepareStatement(
                        DBCStatementType.SCRIPT,
                        script,
                        false, false, false))
                    {
                        action.beforeExecute(session);
                        dbStat.executeStatement();
                    }
                    action.afterExecute(session, null);
                } catch (DBCException e) {
                    action.afterExecute(session, e);
                    throw e;
                }
                if (action instanceof OracleObjectPersistAction) {
                    if (!logObjectErrors(session, compileLog, job, ((OracleObjectPersistAction) action).getObjectType())) {
                        success = false;
                    }
                }
            }
            final DBSObjectState oldState = job.getObjectState();
            job.refreshObjectState(monitor);
            if (job.getObjectState() != oldState) {
                job.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, job));
            }

            return success;
        }
    }

}