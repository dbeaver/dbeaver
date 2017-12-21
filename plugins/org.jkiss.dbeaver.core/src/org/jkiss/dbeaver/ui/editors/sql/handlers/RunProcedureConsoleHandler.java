/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.ui.editors.sql.generator.GenerateSQLContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class RunProcedureConsoleHandler extends OpenObjectConsoleHandler {

    public RunProcedureConsoleHandler()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        DBPDataSourceContainer ds = null;
        String procName = null;

        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event));
        List<DBSProcedure> entities = new ArrayList<>();
        for (DBSObject object : selectedObjects) {
            if (object instanceof DBSProcedure) {
                DBSProcedure proc = (DBSProcedure) object;
                procName = proc.getName();
                entities.add(proc);
                ds = object.getDataSource().getContainer();
            }
        }

        DBRRunnableWithResult<String> generator = GenerateSQLContributor.CALL_GENERATOR(entities);

        String title = "Stored procedures call";
        if (entities.size() == 1 && !CommonUtils.isEmpty(procName)) {
            title = procName + " call";
        }

        openConsole(workbenchWindow, generator, ds, title, false);
        return null;
    }
}
