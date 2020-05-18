/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.generator.SQLGeneratorContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SQLEditorHandlerRunProcedureConsole extends SQLEditorHandlerOpenObjectConsole {

    private static final Log log = Log.getLog(SQLEditorHandlerRunProcedureConsole.class);

    public SQLEditorHandlerRunProcedureConsole()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
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

        DBRRunnableWithResult<String> generator = SQLGeneratorContributor.CALL_GENERATOR(entities);

        String title = "Stored procedures call";
        if (entities.size() == 1 && !CommonUtils.isEmpty(procName)) {
            title = procName + " call";
        }

        try {
            openConsole(workbenchWindow, generator, navContext, title, false, currentSelection);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Open console", "Can open SQL editor", e);
        }
        return null;
    }
}
