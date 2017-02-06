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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.RuntimeUtils;

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

                }

            }
        }
        return null;
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
*/

}