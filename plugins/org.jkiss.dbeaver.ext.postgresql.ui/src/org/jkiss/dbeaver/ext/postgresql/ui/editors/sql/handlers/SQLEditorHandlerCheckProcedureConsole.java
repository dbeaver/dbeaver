/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.sql.generator.SQLGeneratorProcedureCheck;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenObjectConsole;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SQLEditorHandlerCheckProcedureConsole extends SQLEditorHandlerOpenObjectConsole {

    private static final Log log = Log.getLog(SQLEditorHandlerCheckProcedureConsole.class);
    
    void openConsoleCheck(IWorkbenchWindow workbenchWindow, DBRRunnableWithResult<String> generator,
            SQLNavigatorContext navigatorContext, String title, boolean doRun, ISelection currentSelection) throws Exception {
        UIUtils.runInUI(workbenchWindow, generator);
        String sql = CommonUtils.notEmpty(generator.getResult());
        openAndExecuteSQLScript(workbenchWindow, navigatorContext, title, true, currentSelection, sql, true);
    }

    /**
     * Generate PostgreSQL procedure check SQL - via https://github.com/okbob/plpgsql_check
     */        
    @NotNull
    public static SQLGenerator<DBSProcedure> checkGenerator(final List<DBSProcedure> entities) {
        SQLGeneratorProcedureCheck procedureCheck = new SQLGeneratorProcedureCheck();
        procedureCheck.initGenerator(entities);
        return procedureCheck;
    }


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        SQLNavigatorContext navContext = null;
        String procName = null;

        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(currentSelection);
        List<DBSProcedure> entities = new ArrayList<>();
        for (DBSObject object : selectedObjects) {
            if (object instanceof DBSProcedure) {
                DBSProcedure proc = (DBSProcedure) object;
                procName = proc.getName();
                entities.add(proc);
                if (navContext == null) {
                    navContext = new SQLNavigatorContext(object);
                }
            }
        }
        if (navContext == null || navContext.getDataSourceContainer() == null) {
            log.debug("No active datasource");
            return null;
        }

        DBRRunnableWithResult<String> generator = checkGenerator(entities);

        String title = PostgreMessages.procedure_check_label_ext;
        if (entities.size() == 1 && !CommonUtils.isEmpty(procName)) {
            title = NLS.bind(PostgreMessages.procedure_check_label2, procName); 
        }

        try {
            UIUtils.runInUI(workbenchWindow, generator);
            String sql = CommonUtils.notEmpty(generator.getResult());
            openAndExecuteSQLScript(workbenchWindow, navContext, title, true, currentSelection, sql, true);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(PostgreMessages.message_open_console, 
                PostgreMessages.error_cant_open_sql_editor, e);
        }
        return null;
    }
}
