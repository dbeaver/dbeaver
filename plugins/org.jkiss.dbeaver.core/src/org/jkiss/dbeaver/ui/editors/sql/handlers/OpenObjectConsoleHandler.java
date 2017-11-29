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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.generator.GenerateSQLContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;

public class OpenObjectConsoleHandler extends AbstractHandler {

    public OpenObjectConsoleHandler()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        DBPDataSourceContainer ds = null;

        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(
            HandlerUtil.getCurrentSelection(event));
        List<DBSEntity> entities = new ArrayList<>();
        for (DBSObject object : selectedObjects) {
            if (object instanceof DBSEntity) {
                entities.add((DBSEntity) object);
                ds = object.getDataSource().getContainer();
            }
        }
        DBRRunnableWithResult<String> generator = GenerateSQLContributor.SELECT_GENERATOR(entities, true);
        DBeaverUI.runInUI(workbenchWindow, generator);
        String sql = generator.getResult();
        SQLEditor editor = OpenHandler.openSQLConsole(workbenchWindow, ds, "Query", sql);
        if (editor != null) {
            AbstractJob execJob = new AbstractJob("Execute SQL in console") {

                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    // If we open new connection for each editor it may take some time
                    // So let's give it a chance and wait for 10 seconds
                    for (int i = 0; i < 100; i++) {
                        if (editor.getExecutionContext() != null) {
                            break;
                        }
                        RuntimeUtils.pause(100);
                    }
                    return Status.OK_STATUS;
                }
            };
            execJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            editor.processSQL(false, false);
                        }
                    });
                }
            });
            execJob.schedule();
        }
        return null;
    }

}
