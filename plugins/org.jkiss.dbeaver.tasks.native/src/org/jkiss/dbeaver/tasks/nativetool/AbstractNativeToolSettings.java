/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import org.jkiss.dbeaver.model.task.DBTTaskSettingsInput;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
import org.jkiss.utils.CommonUtils;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractNativeToolSettings<BASE_OBJECT extends DBSObject>
    implements DBTTaskSettings<BASE_OBJECT>, DBTTaskSettingsInput<BASE_OBJECT> {

    private static final Log log = Log.getLog(AbstractNativeToolSettings.class);

    private final String PROP_NAME_EXTRA_ARGS = "tools.wizard." + getClass().getSimpleName() + ".extraArgs";

    @Nullable
    private DBPNativeClientLocation clientHome;
    private PrintStream logWriter = System.out;

    private String clientHomeName;

    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    private DBPDataSourceContainer dataSourceContainer;
    private final List<BASE_OBJECT> databaseObjects = new ArrayList<>();

    @Nullable
    private DBPProject project;

    protected AbstractNativeToolSettings() {
    }

    protected AbstractNativeToolSettings(@NotNull DBPProject project) {
        this.project = project;
    }

    public List<BASE_OBJECT> getDatabaseObjects() {
        return databaseObjects;
    }

    @Nullable
    public DBPProject getProject() {
        if (project == null) {
            setProject(dataSourceContainer == null ? null : dataSourceContainer.getProject());
        }
        return project;
    }

    private void setProject(@Nullable DBPProject project) {
        this.project = project;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    protected void updateDataSourceContainer() {
        if (databaseObjects.isEmpty()) {
            dataSourceContainer = null;
        } else {
            dataSourceContainer = databaseObjects.get(0).getDataSource().getContainer();
        }
    }

    public DBPNativeClientLocation findNativeClientHome(String clientHomeId) {
        return null;
    }

    public PrintStream getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(PrintStream logWriter) {
        this.logWriter = logWriter;
    }

    public String getClientHomeName() {
        return clientHomeName;
    }

    @Nullable
    public DBPNativeClientLocation getClientHome() {
        return clientHome;
    }

    public void setClientHome(@Nullable DBPNativeClientLocation clientHome) {
        this.clientHome = clientHome;
        this.clientHomeName = clientHome == null ? null : clientHome.getName();
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

    public void addExtraCommandArgs(List<String> cmd) {
        if (!CommonUtils.isEmptyTrimmed(extraCommandArgs)) {
            Collections.addAll(cmd, extraCommandArgs.split(" "));
        }
    }

    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) throws DBException {
        if (dataSourceContainer == null && !CommonUtils.isEmpty(databaseObjects)) {
            BASE_OBJECT baseObject = databaseObjects.get(0);
            dataSourceContainer = baseObject instanceof DBPDataSourceContainer ?
                (DBPDataSourceContainer) baseObject : baseObject.getDataSource().getContainer();
        }

        if (dataSourceContainer == null) {
            String dsID = preferenceStore.getString("dataSource");
            if (!CommonUtils.isEmpty(dsID)) {
                if (getProject() == null) {
                    String projectName = preferenceStore.getString("project");
                    DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform()
                        .getWorkspace()
                        .getProject(projectName);
                    if (project == null) {
                        if (!CommonUtils.isEmpty(projectName)) {
                            log.error("Can't find project '" + projectName + "' for tool configuration");
                        }
                        project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                    }
                    setProject(project);
                }
                dataSourceContainer = getProject().getDataSourceRegistry().getDataSource(dsID);
            }
        }

        if (preferenceStore instanceof DBPPreferenceMap && dataSourceContainer != null) {
            List<String> databaseObjectList = ((DBPPreferenceMap) preferenceStore).getObject("databaseObjects");
            if (!CommonUtils.isEmpty(databaseObjectList)) {
                DBPProject finalProject = dataSourceContainer.getProject();
                try {
                    runnableContext.run(true, true, monitor -> {
                        monitor.beginTask("Load database object list", databaseObjectList.size());
                        try {
                            for (String objectId : databaseObjectList) {
                                monitor.subTask("Load " + objectId);
                                try {
                                    DBSObject object = DBUtils.findObjectById(monitor, finalProject, objectId);
                                    if (object != null) {
                                        databaseObjects.add((BASE_OBJECT) object);
                                        dataSourceContainer = object instanceof DBPDataSourceContainer ? (DBPDataSourceContainer) object : object.getDataSource().getContainer();
                                    }
                                } catch (Throwable e) {
                                    throw new DBException("Can't find database object '" + objectId + "' in project '" + finalProject.getName() + "' for task configuration", e);
                                }
                                monitor.worked(1);
                            }
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            monitor.done();
                        }
                    });
                } catch (InvocationTargetException e) {
                    throw new DBException("Error loading objects configuration", e.getTargetException());
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        extraCommandArgs = preferenceStore.getString(PROP_NAME_EXTRA_ARGS);
        clientHomeName = preferenceStore.getString("clientHomeName");

        if (preferenceStore instanceof DBPPreferenceMap) {
            toolUserName = preferenceStore.getString("tool.user");
            toolUserPassword = preferenceStore.getString("tool.password");

            try {
                // Backward compatibility
                final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();
                if (!CommonUtils.isEmpty(toolUserName)) toolUserName = encrypter.decrypt(toolUserName);
                if (!CommonUtils.isEmpty(toolUserPassword)) toolUserPassword = encrypter.decrypt(toolUserPassword);
            } catch (Exception ignored) {
                DBSValueEncryptor encryptor = getProject().getValueEncryptor();
                if (!CommonUtils.isEmpty(toolUserName)) toolUserName = encryptor.decryptString(toolUserName);
                if (!CommonUtils.isEmpty(toolUserPassword)) toolUserPassword = encryptor.decryptString(toolUserPassword);
            }
        }
    }

    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        preferenceStore.setValue("project", getProject().getName());
        if (dataSourceContainer != null) {
            preferenceStore.setValue("dataSource", dataSourceContainer.getId());
        }

        if (preferenceStore instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            Map<String, Object> propertyMap = ((DBPPreferenceMap) preferenceStore).getPropertyMap();

            List<String> objectList = new ArrayList<>();
            for (BASE_OBJECT object : databaseObjects) {
                objectList.add(DBUtils.getObjectFullId(object));
            }
            propertyMap.put("databaseObjects", objectList);

            try {
                DBSValueEncryptor encryptor = getProject().getValueEncryptor();
                if (!CommonUtils.isEmpty(toolUserName)) {
                    propertyMap.put("tool.user", encryptor.encryptString(toolUserName));
                } else {
                    propertyMap.put("tool.user", "");
                }
                if (!CommonUtils.isEmpty(toolUserPassword)) {
                    propertyMap.put("tool.password", encryptor.encryptString(toolUserPassword));
                } else {
                    propertyMap.put("tool.password", "");
                }
            } catch (Exception e) {
                log.debug(e);
            }
        }

        preferenceStore.setValue(PROP_NAME_EXTRA_ARGS, extraCommandArgs);
        if (clientHomeName != null) {
            preferenceStore.setValue("clientHomeName", clientHomeName);
        }
    }
    
    @Override
    public void loadSettingsFromInput(List<BASE_OBJECT> inputObjects) {
        databaseObjects.addAll(inputObjects);
    }

    public boolean isMutatingTask() {
        return false;
    }

    @NotNull
    protected String makeOutFilePath(String outputFolder, String outputFileName) {
        // Check URI query
        String query = null;
        int queryStartPos = outputFolder.lastIndexOf("?");
        if (queryStartPos != -1) {
            query = outputFolder.substring(queryStartPos);
            outputFolder = outputFolder.substring(0, queryStartPos - 1);
        }
        if (!outputFolder.endsWith("/")) outputFolder += "/";
        String outFile = outputFolder + outputFileName;
        if (query != null) {
            outFile += query;
        }
        return outFile;
    }

}
