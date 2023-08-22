package org.jkiss.dbeaver.ext.yashandb.ui.actions;

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
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBObjectPersistAction;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchedulerJob;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUtils;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
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
 * @Description:
 * @Author dengqh
 * @Date 2023/7/5 17:32
 */
public class JobRunHandler extends YashanDBTaskHandler{
    private static final Log log = Log.getLog(JobRunHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
        final List<YashanDBSchedulerJob> objects = getSelectedJobs(event);
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
                final YashanDBSchedulerJob job = objects.get(0);

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

    private List<YashanDBSchedulerJob> getSelectedJobs(ExecutionEvent event)
    {
        List<YashanDBSchedulerJob> objects = new ArrayList<>();
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext(); ) {
                final Object element = iter.next();
                final YashanDBSchedulerJob sourceJob = RuntimeUtils.getObjectAdapter(element, YashanDBSchedulerJob.class);
                if (sourceJob != null) {
                    objects.add(sourceJob);
                }
            }
        }
        if (objects.isEmpty()) {
            final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            final YashanDBSchedulerJob sourceJob = RuntimeUtils.getObjectAdapter(activePart, YashanDBSchedulerJob.class);
            if (sourceJob != null) {
                objects.add(sourceJob);
            }
        }
        return objects;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        List<YashanDBSourceObject> objects = getYashanDBSourceObjects(element);
        if (!objects.isEmpty()) {
            if (objects.size() > 1) {
                element.setText("Run " + objects.size() + " jobs");
            } else {
                final YashanDBSourceObject sourceObject = objects.get(0);
                String objectType = YashanDBUtils.formatWord(sourceObject.getSourceType().name());
                element.setText("Run " + objectType/* + " '" + sourceObject.getName() + "'"*/);
            }
        }
    }

    public static boolean runJob(DBRProgressMonitor monitor, DBCCompileLog compileLog, YashanDBSchedulerJob job) throws DBCException
    {
        final DBEPersistAction[] compileActions = job.getRunActions();
        if (ArrayUtils.isEmpty(compileActions)) {
            return true;
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, job, "Run '" + job.getName() + "'")) {
            boolean success = true;
            for (DBEPersistAction action : compileActions) {
                final String script = action.getScript();
                compileLog.info(script);

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
//                if (action instanceof YashanDBObjectPersistAction) {
//                    if (!logObjectErrors(session, compileLog, job, ((YashanDBObjectPersistAction) action).getObjectType())) {
//                        success = false;
//                    }
//                }
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
