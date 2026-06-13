/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.tibero.model.TiberoSchema;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourcePersistAction;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.ext.tibero.ui.editors.TiberoPackageSourceDeclarationEditor;
import org.jkiss.dbeaver.ext.tibero.ui.editors.TiberoPackageSourceDefinitionEditor;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompileHandler extends AbstractHandler {

    private static final Log log = Log.getLog(CompileHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
        List<TiberoSourceObject> objects = getSelectedObjects(event);
        if (objects.isEmpty()) {
            return null;
        }
        Shell activeShell = HandlerUtil.getActiveShell(event);

        if (objects.size() == 1) {
            compileSingle(activePart, activeShell, objects.get(0));
        } else {
            compileMultiple(activeShell, objects);
        }
        return null;
    }

    private void compileSingle(@org.jkiss.code.Nullable IWorkbenchPart activePart, Shell shell, TiberoSourceObject unit) {
        DBCSourceHost sourceHost = null;
        if (activePart != null) {
            sourceHost = RuntimeUtils.getObjectAdapter(activePart, DBCSourceHost.class);
            if (sourceHost == null) {
                sourceHost = activePart.getAdapter(DBCSourceHost.class);
            }
        }
        if (sourceHost != null && sourceHost.getSourceObject() != unit) {
            sourceHost = null;
        }

        TiberoSourceType[] objectTypes = getCompileObjectTypes(activePart, sourceHost);

        DBCCompileLog compileLog = sourceHost == null ? new DBCCompileLogBase() : sourceHost.getCompileLog();
        compileLog.clearLog();

        Throwable error = null;
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask("Compile " + unit.getName(), 1);
                try {
                    compileUnit(monitor, compileLog, unit, objectTypes);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            if (compileLog.getError() != null) {
                error = compileLog.getError();
            }
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            return;
        }

        if (error != null) {
            DBWorkbench.getPlatformUI().showError("Unexpected compilation error", null, error);
        } else if (!CommonUtils.isEmpty(compileLog.getErrorStack())) {
            int line = -1;
            int position = -1;
            StringBuilder fullMessage = new StringBuilder();
            for (DBCCompileError ce : compileLog.getErrorStack()) {
                fullMessage.append(ce).append(GeneralUtils.getDefaultLineSeparator());
                if (line < 0) {
                    line = ce.getLine();
                    position = ce.getPosition();
                }
            }
            String errorTitle = unit.getName() + " compilation failed";
            if (sourceHost != null) {
                if (line > 0 && position >= 0) {
                    sourceHost.positionSource(line, position);
                }
                sourceHost.setCompileInfo(errorTitle, true);
                sourceHost.showCompileLog();
            }
            refreshPropertiesPart(activePart, unit);
            DBWorkbench.getPlatformUI().showError(errorTitle, fullMessage.toString());
        } else {
            String message = unit.getName() + " compiled successfully";
            if (sourceHost != null) {
                sourceHost.setCompileInfo(message, false);
            }
            refreshPropertiesPart(activePart, unit);
            UIUtils.showMessageBox(shell, "Done", message, SWT.ICON_INFORMATION);
        }
    }

    private void refreshPropertiesPart(@org.jkiss.code.Nullable IWorkbenchPart activePart, TiberoSourceObject unit) {
        if (activePart instanceof EntityEditor entityEditor) {
            IEditorPart propertiesEditor = entityEditor.getPageEditor(EntityEditorDescriptor.DEFAULT_OBJECT_EDITOR_ID);
            if (propertiesEditor instanceof IRefreshablePart refreshablePart) {
                UIUtils.syncExec(() -> refreshablePart.refreshPart(unit, true));
            }
        }
    }

    private void compileMultiple(Shell shell, List<TiberoSourceObject> units) {
        int[] okRef = {0};
        int[] failRef = {0};
        Throwable[] errorRef = new Throwable[1];
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask("Compile " + units.size() + " objects", units.size());
                try {
                    for (TiberoSourceObject unit : units) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        monitor.subTask(unit.getName());
                        DBCCompileLog log = new DBCCompileLogBase();
                        try {
                            compileUnit(monitor, log, unit);
                            if (CommonUtils.isEmpty(log.getErrorStack())) {
                                okRef[0]++;
                            } else {
                                failRef[0]++;
                            }
                        } catch (DBException e) {
                            failRef[0]++;
                            log.error(new DBCCompileError(true, e.getMessage(), 0, 0));
                        }
                        monitor.worked(1);
                    }
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            errorRef[0] = e.getTargetException();
        } catch (InterruptedException e) {
            return;
        }
        if (errorRef[0] != null) {
            DBWorkbench.getPlatformUI().showError("Compile error", null, errorRef[0]);
        } else {
            UIUtils.showMessageBox(
                shell,
                "Done",
                "Compiled " + okRef[0] + " object(s), " + failRef[0] + " failed.",
                failRef[0] == 0 ? SWT.ICON_INFORMATION : SWT.ICON_WARNING);
        }
    }

    public static boolean compileUnit(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCCompileLog compileLog,
        @NotNull TiberoSourceObject unit
    ) throws DBException {
        return compileUnit(monitor, compileLog, unit, (TiberoSourceType[]) null);
    }

    public static boolean compileUnit(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCCompileLog compileLog,
        @NotNull TiberoSourceObject unit,
        @org.jkiss.code.Nullable TiberoSourceType... objectTypes
    ) throws DBException {
        DBEPersistAction[] actions = unit.getCompileActions(monitor);
        if (actions == null || actions.length == 0) {
            throw new DBCException("No compile actions associated with " + unit.getSourceType().name());
        }

        try (JDBCSession session = DBUtils.openUtilSession(monitor, unit, "Compile " + unit.getName())) {
            boolean success = true;
            for (DBEPersistAction action : actions) {
                if (!matchesObjectType(action, objectTypes)) {
                    continue;
                }
                String script = action.getScript();
                if (script == null || script.isBlank()) {
                    continue;
                }
                compileLog.trace(script);
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    try (DBCStatement dbStat = session.prepareStatement(
                        DBCStatementType.QUERY, script, false, false, false
                    )) {
                        action.beforeExecute(session);
                        dbStat.executeStatement();
                    }
                    action.afterExecute(session, null);
                } catch (DBCException e) {
                    action.afterExecute(session, e);
                    throw e;
                }
                String objectType = unit.getSourceType().name();
                if (action instanceof TiberoSourcePersistAction sourceAction) {
                    objectType = sourceAction.getObjectType();
                }
                if (!logCompileErrors(session, compileLog, unit, objectType)) {
                    success = false;
                }
            }
            refreshObjectState(monitor, unit);
            DBUtils.fireObjectUpdate(unit);
            return success;
        }
    }

    private static boolean matchesObjectType(
        @NotNull DBEPersistAction action,
        @org.jkiss.code.Nullable TiberoSourceType[] objectTypes
    ) {
        if (objectTypes == null || objectTypes.length == 0) {
            return true;
        }
        if (!(action instanceof TiberoSourcePersistAction sourceAction)) {
            return false;
        }
        for (TiberoSourceType objectType : objectTypes) {
            if (sourceAction.getObjectType().equals(objectType.name())) {
                return true;
            }
        }
        return false;
    }

    private static TiberoSourceType[] getCompileObjectTypes(
        @org.jkiss.code.Nullable IWorkbenchPart activePart,
        @org.jkiss.code.Nullable DBCSourceHost sourceHost
    ) {
        if (activePart instanceof TiberoPackageSourceDeclarationEditor || sourceHost instanceof TiberoPackageSourceDeclarationEditor) {
            return new TiberoSourceType[] {TiberoSourceType.PACKAGE};
        }
        if (activePart instanceof TiberoPackageSourceDefinitionEditor || sourceHost instanceof TiberoPackageSourceDefinitionEditor) {
            return new TiberoSourceType[] {TiberoSourceType.PACKAGE_BODY};
        }
        return null;
    }

    private static void refreshObjectState(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TiberoSourceObject unit
    ) throws DBCException {
        if (monitor.isCanceled()) {
            return;
        }
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        unit.refreshObjectState(monitor);
    }

    private static boolean logCompileErrors(
        @NotNull JDBCSession session,
        @NotNull DBCCompileLog compileLog,
        @NotNull TiberoSourceObject unit,
        @NotNull String objectType
    ) {
        TiberoSchema schema = unit.getSchema();
        if (schema == null) {
            return true;
        }
        String catalogType = objectType.replace('_', ' ');
        String[] sources = {
            "SELECT LINE, POSITION, TEXT, ATTRIBUTE FROM ALL_ERRORS "
                + "WHERE OWNER = ? AND NAME = ? AND TYPE = ? ORDER BY SEQUENCE",
            "SELECT LINE, POSITION, TEXT, ATTRIBUTE FROM USER_ERRORS "
                + "WHERE NAME = ? AND TYPE = ? ORDER BY SEQUENCE"
        };
        Exception lastError = null;
        for (int idx = 0; idx < sources.length; idx++) {
            String sql = sources[idx];
            boolean isUserView = idx == 1;
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                if (isUserView) {
                    dbStat.setString(1, unit.getName());
                    dbStat.setString(2, catalogType);
                } else {
                    dbStat.setString(1, schema.getName());
                    dbStat.setString(2, unit.getName());
                    dbStat.setString(3, catalogType);
                }
                try (ResultSet rs = dbStat.executeQuery()) {
                    boolean hasErrors = false;
                    while (rs.next()) {
                        int line = rs.getInt("LINE");
                        int position = rs.getInt("POSITION");
                        String message = rs.getString("TEXT");
                        String attribute = rs.getString("ATTRIBUTE");
                        if (line <= 0) {
                            message = objectType + ": " + message + " (source line is not reported by Tibero)";
                        }
                        DBCCompileError ce = new DBCCompileError("ERROR".equals(attribute), message, line, position);
                        hasErrors = true;
                        if (ce.isError()) {
                            compileLog.error(ce);
                        } else {
                            compileLog.warn(ce);
                        }
                    }
                    return !hasErrors;
                }
            } catch (Exception e) {
                lastError = e;
                // Fall through to next view (USER_ERRORS) on permission error
            }
        }
        log.error("Can't read Tibero compile errors", lastError);
        return false;
    }

    private static List<TiberoSourceObject> getSelectedObjects(ExecutionEvent event) {
        List<TiberoSourceObject> result = new ArrayList<>();
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection structured && !selection.isEmpty()) {
            Iterator<?> iter = structured.iterator();
            while (iter.hasNext()) {
                Object element = iter.next();
                TiberoSourceObject src = RuntimeUtils.getObjectAdapter(element, TiberoSourceObject.class);
                if (src != null) {
                    result.add(src);
                }
            }
        }
        if (result.isEmpty()) {
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            if (activePart instanceof IEditorPart editorPart
                && editorPart.getEditorInput() instanceof IDatabaseEditorInput input) {
                DBSObject dbsObject = input.getDatabaseObject();
                if (dbsObject instanceof TiberoSourceObject src) {
                    result.add(src);
                }
            }
        }
        return result;
    }
}
