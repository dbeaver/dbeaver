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
package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureArgument;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageNavigateHandler extends AbstractHandler //implements IElementUpdater
{
    private static final Log log = Log.getLog(PackageNavigateHandler.class);

    public PackageNavigateHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final OracleProcedurePackaged procedure = getSelectedProcedure(event);
        if (procedure != null) {
            OraclePackage procPackage = procedure.getParentObject();
            IEditorPart entityEditor = NavigatorHandlerObjectOpen.openEntityEditor(procPackage);
            if (entityEditor instanceof EntityEditor) {
                ((EntityEditor) entityEditor).switchFolder("source.definition");
                SQLEditorBase sqlEditor = entityEditor.getAdapter(SQLEditorBase.class);
                if (sqlEditor != null) {
                    new NavigateJob(procedure, sqlEditor).schedule();
                }
            }
        }
        return null;
    }

    static class NavigateJob extends AbstractJob {

        private final OracleProcedurePackaged procedure;
        private final SQLEditorBase sqlEditor;

        public NavigateJob(OracleProcedurePackaged procedure, SQLEditorBase sqlEditor) {
            super("Navigate procedure '" + procedure.getFullyQualifiedName(DBPEvaluationContext.UI));
            this.procedure = procedure;
            this.sqlEditor = sqlEditor;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                navigate(monitor);
            } catch (InterruptedException e) {
                return Status.CANCEL_STATUS;
            } catch (DBException e) {
                return GeneralUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }

        private void navigate(DBRProgressMonitor monitor) throws InterruptedException, DBException {
            if (sqlEditor instanceof SQLEditorNested) {
                int checkAttempts = 0;
                while (!((SQLEditorNested) sqlEditor).isDocumentLoaded() && checkAttempts < 10) {
                    Thread.sleep(500);
                    checkAttempts++;
                }
            }
            final Document document = sqlEditor.getDocument();
            if (document != null) {
                String procRegex = procedure.getProcedureType().name() + "\\s+" + procedure.getName();
                final Collection<OracleProcedureArgument> parameters = procedure.getParameters(monitor);
                if (parameters != null) {
                    List<OracleProcedureArgument> inParams = new ArrayList<>();
                    for (OracleProcedureArgument arg : parameters) {
                        if (arg.getParameterKind() != DBSProcedureParameterKind.OUT && !arg.isResultArgument()) {
                            inParams.add(arg);
                        }
                    }
                    if (!inParams.isEmpty()) {
                        procRegex += "\\s*\\([^\\)]+\\)";
                    }
                }
                final FindReplaceDocumentAdapter findAdapter = new FindReplaceDocumentAdapter(document);
                try {
                    final IRegion procRegion = findAdapter.find(0, procRegex, true, false, false, true);
                    if (procRegion != null) {
                        DBeaverUI.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                sqlEditor.selectAndReveal(procRegion.getOffset(), procRegion.getLength());
                            }
                        });
                    }
                } catch (BadLocationException e) {
                    log.error("Error finding procedure source", e);
                }
            }
        }

    }

    private OracleProcedurePackaged getSelectedProcedure(ExecutionEvent event)
    {
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            Object firstElement = ((IStructuredSelection) currentSelection).getFirstElement();
            return RuntimeUtils.getObjectAdapter(firstElement, OracleProcedurePackaged.class);
        }
        return null;
    }

/*
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
                element.setText("Compile " + objectType*/
/* + " '" + sourceObject.getName() + "'"*//*
);
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
                    action.afterExecute(session, null);
                } catch (DBCException e) {
                    action.afterExecute(session, e);
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
*/

}