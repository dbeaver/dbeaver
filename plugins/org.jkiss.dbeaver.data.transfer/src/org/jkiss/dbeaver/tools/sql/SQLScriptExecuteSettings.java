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
package org.jkiss.dbeaver.tools.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLScriptExecuteSettings
 */
public class SQLScriptExecuteSettings {

    private static final Log log = Log.getLog(SQLScriptExecuteSettings.class);

    private List<String> scriptFiles = new ArrayList<>();
    private DBPDataSourceContainer dataSourceContainer;

    private boolean autoCommit;
    private DBPTransactionIsolation transactionIsolation;

    private boolean ignoreErrors;
    private boolean dumpQueryResultsToLog;

    public List<String> getScriptFiles() {
        return scriptFiles;
    }

    public void setScriptFiles(List<String> scriptFiles) {
        this.scriptFiles = scriptFiles;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean isDumpQueryResultsToLog() {
        return dumpQueryResultsToLog;
    }

    public void setDumpQueryResultsToLog(boolean dumpQueryResultsToLog) {
        this.dumpQueryResultsToLog = dumpQueryResultsToLog;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public DBPTransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(DBPTransactionIsolation transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        String projectName = JSONUtils.getString(config, "project");
        DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
        if (project != null) {
            String dataSourceContainerId = JSONUtils.getString(config, "dataSourceContainer");
            if (!CommonUtils.isEmpty(dataSourceContainerId)) {
                dataSourceContainer = project.getDataSourceRegistry().getDataSource(dataSourceContainerId);
            }
        }
        scriptFiles = JSONUtils.deserializeStringList(config, "scriptFiles");

        ignoreErrors = JSONUtils.getBoolean(config, "ignoreErrors");
        dumpQueryResultsToLog = JSONUtils.getBoolean(config, "dumpQueryResultsToLog");

        autoCommit = JSONUtils.getBoolean(config, "autoCommit");
    }

    public void saveConfiguration(Map<String, Object> config) {
        config.put("project", dataSourceContainer == null ? null : dataSourceContainer.getProject().getName());
        config.put("dataSourceContainer", dataSourceContainer == null ? null : dataSourceContainer.getId());
        config.put("scriptFiles", scriptFiles);

        config.put("ignoreErrors", ignoreErrors);
        config.put("dumpQueryResultsToLog", dumpQueryResultsToLog);

        config.put("autoCommit", autoCommit);
    }

    public static IFile getWorkspaceFile(String filePath) {
        return DBWorkbench.getPlatform().getWorkspace().getEclipseWorkspace().getRoot().getFile(new Path(filePath));
    }
}
