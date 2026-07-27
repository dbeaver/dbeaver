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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.tools.sql.SQLTaskConstants;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.SimpleDatabaseEditorContext;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLEditorHandlerExecuteScriptFile extends AbstractHandler {

    private static final Log log = Log.getLog(SQLEditorHandlerExecuteScriptFile.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell activeShell = HandlerUtil.getActiveShell(event);
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        List<IFile> scripts = getSelectedScripts(HandlerUtil.getCurrentSelection(event));
        for (IFile script : scripts) {
            executeScriptFile(activeShell, workbenchWindow, script);
        }
        return null;
    }

    @NotNull
    private static List<IFile> getSelectedScripts(@Nullable ISelection selection) {
        List<IFile> scripts = new ArrayList<>();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            for (Object element : ((IStructuredSelection) selection).toList()) {
                DBNNode node = RuntimeUtils.getObjectAdapter(element, DBNNode.class);
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        scripts.add((IFile) resource);
                    }
                }
            }
        }
        return scripts;
    }

    private static void executeScriptFile(@NotNull Shell shell, @NotNull IWorkbenchWindow workbenchWindow, @NotNull IFile script) {
        DBPDataSourceContainer resolvedDataSource = EditorUtils.getFileDataSource(script);
        if (resolvedDataSource == null) {
            resolvedDataSource = chooseDataSource(shell, script);
            if (resolvedDataSource == null) {
                return;
            }
        }
        final DBPDataSourceContainer dataSourceContainer = resolvedDataSource;

        DBPProject project = DBPPlatformDesktop.getInstance().getWorkspace().getProject(script.getProject());
        if (!(project instanceof RCPProject rcpProject)) {
            DBWorkbench.getPlatformUI().showError(
                "Execute SQL script",
                "Can't determine project of script '" + script.getName() + "'");
            return;
        }

        DBTTaskManager taskManager = project.getTaskManager();
        DBTTaskType taskType = taskManager.getRegistry().getTaskType(SQLTaskConstants.TASK_SCRIPT_EXECUTE);
        if (taskType == null) {
            DBWorkbench.getPlatformUI().showError(
                "Execute SQL script",
                "SQL script execution task type is not available");
            return;
        }
        if (!TaskUIRegistry.getInstance().supportsConfigurator(taskType)) {
            DBWorkbench.getPlatformUI().showError(
                "Execute SQL script",
                "Task '" + taskType.getName() + "' has no configuration UI");
            return;
        }

        SQLScriptExecuteSettings settings = new SQLScriptExecuteSettings();
        settings.setScriptFiles(List.of(rcpProject.getResourcePath(script)));
        settings.setDataSources(List.of(dataSourceContainer));
        settings.setAutoCommit(true);
        settings.setIgnoreErrors(false);

        Map<String, Object> config = new LinkedHashMap<>();
        settings.saveConfiguration(config);

        try {

            DBTTask task = taskManager.createTemporaryTask(taskType, "Execute SQL script '" + script.getName() + "'");
            task.setProperties(config);
            DBTTaskConfigurator configurator = TaskUIRegistry.getInstance().createConfigurator(taskType);
            TaskConfigurationWizard<?> wizard = configurator.createTaskConfigWizard(task);
            TaskConfigurationWizardDialog dialog = new TaskConfigurationWizardDialog(workbenchWindow, wizard);
            dialog.open();
        } catch (Exception e) {
            log.debug("Error opening execute script task for '" + script.getName() + "'", e);
            DBWorkbench.getPlatformUI().showError(
                "Execute SQL script",
                "Error opening execute script task for '" + script.getName() + "'",
                e);
        }
    }

    @Nullable
    private static DBPDataSourceContainer chooseDataSource(@NotNull Shell shell, @NotNull IFile script) {
        SelectDataSourceDialog dialog = new SelectDataSourceDialog(
            shell,
            DBPPlatformDesktop.getInstance().getWorkspace().getProject(script.getProject()),
            null);
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return null;
        }
        DBPDataSourceContainer dataSource = dialog.getDataSource();
        if (dataSource != null) {
            EditorUtils.setFileDataSource(script, new SimpleDatabaseEditorContext(dataSource));
        }
        return dataSource;
    }
}
