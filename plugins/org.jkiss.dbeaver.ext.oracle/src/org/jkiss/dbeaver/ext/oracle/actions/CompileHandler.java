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
package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.views.OracleCompilerDialog;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.compile.DBCSourceHost;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompileHandler extends AbstractHandler implements IElementUpdater
{
    private static final Log log = Log.getLog(CompileHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final List<OracleSourceObject> objects = getSelectedObjects(event);
        if (!objects.isEmpty()) {
            final Shell activeShell = HandlerUtil.getActiveShell(event);
            if (objects.size() == 1) {
                final OracleSourceObject unit = objects.get(0);

                DBCSourceHost sourceHost = null;
                final IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
                if (activePart != null) {
                    sourceHost = RuntimeUtils.getObjectAdapter(activePart, DBCSourceHost.class);
                    if (sourceHost == null) {
                        sourceHost = activePart.getAdapter(DBCSourceHost.class);
                    }
                }
                if (sourceHost != null && sourceHost.getSourceObject() != unit) {
                    sourceHost = null;
                }

                final DBCCompileLog compileLog = sourceHost == null ? new DBCCompileLogBase() : sourceHost.getCompileLog();
                compileLog.clearLog();
                Throwable error = null;
                try {
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            try {
                                compileUnit(monitor, compileLog, unit);
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            }
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
                    UIUtils.showErrorDialog(activeShell, "Unexpected compilation error", null, error);
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

                    // If compiled object is currently open in editor - try to position on error line
                    if (sourceHost != null && sourceHost.getSourceObject() == unit && line > 0 && position > 0) {
                        sourceHost.positionSource(line, position);
                        activePart.getSite().getPage().activate(activePart);
                    }

                    String errorTitle = unit.getName() + " compilation failed";
                    if (sourceHost != null) {
                        sourceHost.setCompileInfo(errorTitle, true);
                        sourceHost.showCompileLog();
                    }
                    UIUtils.showErrorDialog(activeShell, errorTitle, fullMessage.toString());
                } else {
                    String message = unit.getName() + " compiled successfully";
                    if (sourceHost != null) {
                        sourceHost.setCompileInfo(message, true);
                    }
                    UIUtils.showMessageBox(activeShell, "Done", message, SWT.ICON_INFORMATION);
                }
            } else {
                OracleCompilerDialog dialog = new OracleCompilerDialog(activeShell, objects);
                dialog.open();
            }
        }
        return null;
    }

    private List<OracleSourceObject> getSelectedObjects(ExecutionEvent event)
    {
        List<OracleSourceObject> objects = new ArrayList<>();
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext(); ) {
                final Object element = iter.next();
                final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(element, OracleSourceObject.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (objects.isEmpty()) {
            final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleSourceObject.class);
            if (sourceObject != null) {
                objects.add(sourceObject);
            }
        }
        return objects;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        List<OracleSourceObject> objects = new ArrayList<>();
        IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        final Object item = iter.next();
                        final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, OracleSourceObject.class);
                        if (sourceObject != null) {
                            objects.add(sourceObject);
                        }
                    }
                }
            }
            if (objects.isEmpty()) {
                final IWorkbenchPart activePart = partSite.getPart();
                final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleSourceObject.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (!objects.isEmpty()) {
            if (objects.size() > 1) {
                element.setText("Compile " + objects.size() + " objects");
            } else {
                final OracleSourceObject sourceObject = objects.get(0);
                String objectType = TextUtils.formatWord(sourceObject.getSourceType().name());
                element.setText("Compile " + objectType/* + " '" + sourceObject.getName() + "'"*/);
            }
        }
    }

    public static boolean compileUnit(DBRProgressMonitor monitor, DBCCompileLog compileLog, OracleSourceObject unit) throws DBCException
    {
        final DBEPersistAction[] compileActions = unit.getCompileActions();
        if (ArrayUtils.isEmpty(compileActions)) {
            return true;
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, unit.getDataSource(), "Compile '" + unit.getName() + "'")) {
            boolean success = true;
            for (DBEPersistAction action : compileActions) {
                final String script = action.getScript();
                compileLog.trace(script);

                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    try (DBCStatement dbStat = session.prepareStatement(
                        DBCStatementType.QUERY,
                        script,
                        false, false, false))
                    {
                        dbStat.executeStatement();
                    }
                    action.handleExecute(session, null);
                } catch (DBCException e) {
                    action.handleExecute(session, e);
                    throw e;
                }
                if (action instanceof OracleObjectPersistAction) {
                    if (!logObjectErrors(session, compileLog, unit, ((OracleObjectPersistAction) action).getObjectType())) {
                        success = false;
                    }
                }
            }
            final DBSObjectState oldState = unit.getObjectState();
            unit.refreshObjectState(monitor);
            if (unit.getObjectState() != oldState) {
                unit.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, unit));
            }

            return success;
        }
    }

    public static boolean logObjectErrors(
        JDBCSession session,
        DBCCompileLog compileLog,
        OracleSourceObject unit,
        OracleObjectType objectType)
    {
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_ERRORS WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY SEQUENCE")) {
                dbStat.setString(1, unit.getSchema().getName());
                dbStat.setString(2, unit.getName());
                dbStat.setString(3, objectType.getTypeName());
                try (ResultSet dbResult = dbStat.executeQuery()) {
                    boolean hasErrors = false;
                    while (dbResult.next()) {
                        DBCCompileError error = new DBCCompileError(
                            "ERROR".equals(dbResult.getString("ATTRIBUTE")),
                            dbResult.getString("TEXT"),
                            dbResult.getInt("LINE"),
                            dbResult.getInt("POSITION"));
                        hasErrors = true;
                        if (error.isError()) {
                            compileLog.error(error);
                        } else {
                            compileLog.warn(error);
                        }
                    }
                    return !hasErrors;
                }
            }
        } catch (Exception e) {
            log.error("Can't read user errors", e);
            return false;
        }
    }

}