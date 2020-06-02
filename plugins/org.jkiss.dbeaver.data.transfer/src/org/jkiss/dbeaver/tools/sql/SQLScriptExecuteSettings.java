/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLScriptExecuteSettings
 */
public class SQLScriptExecuteSettings implements DBTTaskSettings<IResource> {

    private static final Log log = Log.getLog(SQLScriptExecuteSettings.class);

    private List<DBPDataSourceContainer> dataSources = new ArrayList<>();
    private List<String> scriptFiles = new ArrayList<>();

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

    public List<DBPDataSourceContainer> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DBPDataSourceContainer> dataSources) {
        this.dataSources = dataSources;
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
        // Legacy config support (single datasource
        String projectName = JSONUtils.getString(config, "project");
        DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
        if (project != null) {
            String dataSourceContainerId = JSONUtils.getString(config, "dataSourceContainer");
            if (!CommonUtils.isEmpty(dataSourceContainerId)) {
                DBPDataSourceContainer dataSource = project.getDataSourceRegistry().getDataSource(dataSourceContainerId);
                if (dataSource != null) {
                    dataSources.add(dataSource);
                }
            }
        } else {
            // Modern config (datasource list)
            List<Map<String, Object>> dsConfig = JSONUtils.getObjectList(config, "dataSources");
            for (Map<String, Object> dsInfo : dsConfig) {
                projectName = JSONUtils.getString(dsInfo, "project");
                project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                if (project != null) {
                    String dataSourceContainerId = JSONUtils.getString(dsInfo, "dataSource");
                    if (!CommonUtils.isEmpty(dataSourceContainerId)) {
                        DBPDataSourceContainer dataSource = project.getDataSourceRegistry().getDataSource(dataSourceContainerId);
                        if (dataSource != null) {
                            dataSources.add(dataSource);
                        }
                    }
                }
            }
        }
        scriptFiles = JSONUtils.deserializeStringList(config, "scriptFiles");

        ignoreErrors = JSONUtils.getBoolean(config, "ignoreErrors");
        dumpQueryResultsToLog = JSONUtils.getBoolean(config, "dumpQueryResultsToLog");

        autoCommit = JSONUtils.getBoolean(config, "autoCommit");
    }

    public void saveConfiguration(Map<String, Object> config) {
        config.put("scriptFiles", scriptFiles);
        List<Map<String, Object>> dsConfig = new ArrayList<>();
        config.put("dataSources", dsConfig);
        for (DBPDataSourceContainer ds : dataSources) {
            Map<String, Object> dsInfo = new LinkedHashMap<>();
            dsInfo.put("project", ds.getProject().getName());
            dsInfo.put("dataSource", ds.getId());
            dsConfig.add(dsInfo);
        }

        config.put("ignoreErrors", ignoreErrors);
        config.put("dumpQueryResultsToLog", dumpQueryResultsToLog);

        config.put("autoCommit", autoCommit);
    }

    public static IFile getWorkspaceFile(String filePath) {
        return DBWorkbench.getPlatform().getWorkspace().getEclipseWorkspace().getRoot().getFile(new Path(filePath));
    }

}
