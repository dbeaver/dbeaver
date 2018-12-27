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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.generator.GenerateSQLContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

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

        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(
            currentSelection);
        List<DBSEntity> entities = new ArrayList<>();
        for (DBSObject object : selectedObjects) {
            ds = object.getDataSource().getContainer();
            if (object instanceof DBSEntity) {
                entities.add((DBSEntity) object);
            }
        }
        DBRRunnableWithResult<String> generator = GenerateSQLContributor.SELECT_GENERATOR(entities, true);
        String title = "Query";
        if (entities.size() == 1) {
            title = DBUtils.getObjectFullName(entities.get(0), DBPEvaluationContext.DML);
        }
        try {
            openConsole(workbenchWindow, generator, ds, title, !entities.isEmpty(), currentSelection);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Open console", "Can open SQL editor", e);
        }
        return null;
    }


    protected void openConsole(IWorkbenchWindow workbenchWindow, DBRRunnableWithResult<String> generator,
                               DBPDataSourceContainer ds, String title, boolean doRun, ISelection currentSelection) throws Exception {
        UIUtils.runInUI(workbenchWindow, generator);
        String sql = CommonUtils.notEmpty(generator.getResult());

        IProject project = ds != null ? ds.getRegistry().getProject() : DBWorkbench.getPlatform().getProjectManager().getActiveProject();
        OpenHandler.checkProjectIsOpen(project);
        IFolder folder = OpenHandler.getCurrentScriptFolder(currentSelection);
        IFile scriptFile = ResourceUtils.createNewScript(project, folder, ds);
        //InputStream is = new ByteArrayInputStream(sql.getBytes(GeneralUtils.getDefaultFileEncoding()));
        //scriptFile.setContents(is, true, false, new NullProgressMonitor());

        FileEditorInput sqlInput = new FileEditorInput(scriptFile);
        SQLEditor editor = (SQLEditor) workbenchWindow.getActivePage().openEditor(sqlInput, SQLEditor.class.getName());

        if (editor != null) {
            editor.getDocument().set(sql);
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
            if (doRun) {
                execJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        UIUtils.syncExec(() -> editor.processSQL(false, false));
                    }
                });
            }
            execJob.schedule();
        }
    }
}
