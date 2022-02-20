/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.sql;

import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.sql.task.SQLToolRunListener;
import org.jkiss.dbeaver.model.sql.task.SQLToolStatistics;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.sql.internal.TasksSQLUIMessages;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskWizardExecutor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

class SQLToolTaskWizard extends TaskConfigurationWizard<SQLToolExecuteSettings> {
    private static final Log log = Log.getLog(SQLToolTaskWizard.class);

    private SQLToolExecuteSettings settings;
    private SQLToolTaskWizardPageSettings pageSettings;
    private SQLToolTaskWizardPageStatus pageStatus;
    private SQLToolExecuteHandler taskHandler;

    private List<DBSObject> objectList;

    public SQLToolTaskWizard() {
    }

    public SQLToolTaskWizard(@NotNull DBTTask task) {
        super(task);
        try {
            taskHandler = (SQLToolExecuteHandler) task.getType().createHandler();
        } catch (DBException e) {
            throw new IllegalArgumentException("Error instantiating task type handler", e);
        }
        settings = taskHandler.createToolSettings();
        settings.loadConfiguration(UIUtils.getDialogRunnableContext(), task.getProperties());
        objectList = settings.getObjectList();
    }

    public SQLToolExecuteHandler getTaskHandler() {
        return taskHandler;
    }

    @Override
    protected String getDefaultWindowTitle() {
        return getTaskType().getName();
    }

    @Override
    public String getTaskTypeId() {
        return getCurrentTask().getType().getId();
    }

    @Override
    public void addPages() {
        super.addPages();
        pageSettings = new SQLToolTaskWizardPageSettings(this);
        pageStatus = new SQLToolTaskWizardPageStatus(this);
        addPage(pageSettings);
        addPage(pageStatus);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == pageSettings) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        pageSettings.saveSettings();

        settings.saveConfiguration(state);
    }

    @Override
    public SQLToolExecuteSettings getSettings() {
        return settings;
    }

    @Override
    public boolean performFinish() {
        if (isRunTaskOnFinish()) {
            // Only if task is not temporary
            saveConfigurationToTask(getCurrentTask());
            return super.performFinish();
        }

        try {
            // Execute task in wizard
            DBTTask task = getCurrentTask();
            saveConfigurationToTask(task);

            TaskConfigurationWizardDialog container = getContainer();
            container.disableButtonsOnProgress();

            container.showPage(pageStatus);
            pageStatus.clearLog();

            TaskWizardExecutor executor = new SQLTaskExecutor(task);
            executor.executeTask();
            if (taskHandler.needsRefreshOnFinish()) {
                refreshOnFinish();
            }
            container.enableButtonsAfterProgress();
            container.setCompleteMarkAfterProgress();
            return false;
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(e.getMessage(), TasksSQLUIMessages.sql_tool_task_wizard_message_error_running_task, e);
            return false;
        }
    }

    private void refreshOnFinish() throws Exception {
        try {
            getContainer().run(true, true, monitor -> {
                monitor.beginTask("Object refreshing", objectList.size());
                for (DBSObject object : objectList) {
                    try {
                        DBNDatabaseNode objectNode = DBNUtils.getNodeByObject(object);
                        if (objectNode != null) {
                            objectNode.refreshNode(new DefaultProgressMonitor(monitor), DBNEvent.FORCE_REFRESH);
                            if (monitor.isCanceled()) {
                                break;
                            }
                            monitor.worked(1);
                        }
                    } catch (Exception e) {
                        log.error("Error refreshing object '" + object.getName() + "'", e);
                    }
                }
                monitor.done();
            });
        } catch (InvocationTargetException e) {
            throw new DBCException("Refresh error", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private class SQLTaskExecutor extends TaskWizardExecutor implements SQLToolRunListener {
        SQLTaskExecutor(DBTTask task) {
            super(SQLToolTaskWizard.this.getRunnableContext(), task, SQLToolTaskWizard.log, SQLToolTaskWizard.this.pageStatus.getLogWriter());
        }

        @Override
        public void handleActionStatistics(DBPObject object, DBEPersistAction action, DBCSession session, List<? extends SQLToolStatistics> statistics) {
            pageStatus.addStatistics(object, statistics);
        }
    }

}
