/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * TaskHandlerNativeToolBase
 */
public abstract class TaskHandlerNativeToolBase<BASE_OBJECT extends DBSObject, PROCESS_ARG> implements DBTTaskHandler {

    private List<BASE_OBJECT> databaseObjects;
    private DBPNativeClientLocation clientHome;
    private DBPDataSourceContainer dataSourceContainer;
    private DBPConnectionConfiguration connectionInfo;
    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    private boolean refreshObjects;
    private boolean isSuccess;

    ////////////////////////////////////
    // Methods to implement in real handlers

    public abstract Collection<PROCESS_ARG> getRunInfo(DBTTask task);

    protected abstract List<String> getCommandLine(PROCESS_ARG arg) throws IOException;

    protected void setupProcessParameters(ProcessBuilder process) {
    }

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process);

    protected boolean isNativeClientHomeRequired() {
        return true;
    }

    protected boolean isMergeProcessStreams() {
        return false;
    }

    public boolean isVerbose() {
        return false;
    }

    protected boolean needsModelRefresh() {
        return true;
    }

    protected void onSuccess(long workTime) {

    }

    ////////////////////////////////////
    // Helpers

    public List<BASE_OBJECT> getDatabaseObjects() {
        return databaseObjects;
    }

    public DBPConnectionConfiguration getConnectionInfo() {
        return connectionInfo;
    }

    public DBPNativeClientLocation getClientHome() {
        return clientHome;
    }

    public String getToolUserName() {
        return toolUserName;
    }

    public void setToolUserName(String toolUserName) {
        this.toolUserName = toolUserName;
    }

    public String getToolUserPassword() {
        return toolUserPassword;
    }

    public void setToolUserPassword(String toolUserPassword) {
        this.toolUserPassword = toolUserPassword;
    }

    public String getExtraCommandArgs() {
        return extraCommandArgs;
    }

    public void setExtraCommandArgs(String extraCommandArgs) {
        this.extraCommandArgs = extraCommandArgs;
    }

    protected void addExtraCommandArgs(List<String> cmd) {
        if (!CommonUtils.isEmptyTrimmed(extraCommandArgs)) {
            Collections.addAll(cmd, extraCommandArgs.split(" "));
        }
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public DBPNativeClientLocation findNativeClientHome(String clientHomeId) {
        return null;
    }


    ////////////////////////////////////
    // Native tool executor

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        log.debug(task.getType().getName() + " initiated");

        try {
            runnableContext.run(true, true, monitor -> {
                loadToolSettings(monitor, task, log);

                if (!prepareTaskRun(monitor, task, log)) {
                    isSuccess = false;
                    return;
                }

                Collection<PROCESS_ARG> runList = getRunInfo(task);
                try {
                    isSuccess = true;
                    for (PROCESS_ARG arg : runList) {
                        if (monitor.isCanceled()) break;
                        if (!executeProcess(monitor, task, log, arg)) {
                            isSuccess = false;
                        }
                    }
                } catch (Exception e){
                    throw new InvocationTargetException(e);
                }
                refreshObjects = isSuccess && !monitor.isCanceled();

                if (refreshObjects && !CommonUtils.isEmpty(databaseObjects) && needsModelRefresh()) {
                    // Refresh navigator node (script execution can change everything inside)
                    for (BASE_OBJECT object : databaseObjects) {
                        final DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode(object);
                        if (node != null) {
                            try {
                                node.refreshNode(monitor, TaskHandlerNativeToolBase.this);
                            } catch (DBException e) {
                                log.debug("Error refreshing node '" + node.getNodeItemPath() + "' after native tool execution", e);
                            }
                        }
                    }
                }
            });
        } catch (InvocationTargetException e) {
            throw new DBException("Error executing native tool", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
            log.debug("Canceled");
        }

        log.debug(task.getType().getName() + " completed");
    }

    protected boolean prepareTaskRun(DBRProgressMonitor monitor, DBTTask task, Log log) {
        return true;
    }

    private boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, Log log, PROCESS_ARG arg) throws IOException, DBException, InterruptedException {
        monitor.beginTask(task.getName(), 1);
        try {
            final List<String> commandLine = getCommandLine(arg);
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(processBuilder);
            Process process = processBuilder.start();

            startProcessHandler(monitor, arg, processBuilder, process);

            Thread.sleep(100);

            for (; ; ) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        log.error("Process exit code: " + exitCode);
                        return false;
                    }
                } catch (IllegalThreadStateException e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            monitor.done();
            log.error(e);
            //logPage.appendLog(NLS.bind(TaskNativeUIMessages.tools_wizard_log_io_error, e.getMessage()) + "\n", true);
            return false;
        }

        return true;
    }

    protected void loadToolSettings(@NotNull DBRProgressMonitor monitor, @NotNull DBTTask task, Log log) {
        String projectName = CommonUtils.toString(task.getProperties().get("project"));
        DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
        if (project == null) {
            if (!CommonUtils.isEmpty(projectName)) {
                log.error("Can't find project '" + projectName + "' for tool configuration");
            }
            project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        }
        String dsID = CommonUtils.toString(task.getProperties().get("dataSource"));
        if (!CommonUtils.isEmpty(dsID)) {
            dataSourceContainer = project.getDataSourceRegistry().getDataSource(dsID);
            if (dataSourceContainer == null) {
                log.error("Can't find datasource '" + dsID+ "' in project '" + project.getName() + "' for tool configuration");
            }
        }
        {
            List<String> databaseObjectList = (List<String>) task.getProperties().get("databaseObjects");
            if (!CommonUtils.isEmpty(databaseObjectList)) {
                DBPProject finalProject = project;
                for (String objectId : databaseObjectList) {
                    try {
                        DBSObject object = DBUtils.findObjectById(monitor, finalProject, objectId);
                        if (object != null) {
                            databaseObjects.add((BASE_OBJECT) object);
                        }
                    } catch (Throwable e) {
                        log.error("Can't find database object '" + objectId + "' in project '" + finalProject.getName() + "' for task configuration");
                    }
                }
            }
        }
        connectionInfo = dataSourceContainer == null ? null : dataSourceContainer.getActualConnectionConfiguration();

        toolUserName = CommonUtils.toString(task.getProperties().get("toolUserName"));
        toolUserPassword = CommonUtils.toString(task.getProperties().get("toolUserPassword"));
        extraCommandArgs = CommonUtils.toString(task.getProperties().get("extraArgs"));
    }

}
