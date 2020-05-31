/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractNativeToolSettings<BASE_OBJECT extends DBSObject> implements DBTTaskSettings<BASE_OBJECT> {

    private static final Log log = Log.getLog(AbstractNativeToolSettings.class);

    private final String PROP_NAME_EXTRA_ARGS = "tools.wizard." + getClass().getSimpleName() + ".extraArgs";

    private DBPNativeClientLocation clientHome;
    private Writer logWriter = new PrintWriter(System.out, true);

    private String clientHomeName;

    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    private DBPDataSourceContainer dataSourceContainer;
    private final List<BASE_OBJECT> databaseObjects = new ArrayList<>();

    public List<BASE_OBJECT> getDatabaseObjects() {
        return databaseObjects;
    }

    public DBPProject getProject() {
        return dataSourceContainer == null ? null : dataSourceContainer.getProject();
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

    public Writer getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(Writer logWriter) {
        this.logWriter = logWriter;
    }

    public String getClientHomeName() {
        return clientHomeName;
    }

    public DBPNativeClientLocation getClientHome() {
        return clientHome;
    }

    public void setClientHome(DBPNativeClientLocation clientHome) {
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
                String projectName = preferenceStore.getString("project");
                DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                if (project == null) {
                    if (!CommonUtils.isEmpty(projectName)) {
                        log.error("Can't find project '" + projectName + "' for tool configuration");
                    }
                    project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                }
                dataSourceContainer = project.getDataSourceRegistry().getDataSource(dsID);
                if (dataSourceContainer == null) {
                    log.error("Can't find datasource '" + dsID+ "' in project '" + project.getName() + "' for tool configuration");
                }
            }
        }

        if (preferenceStore instanceof DBPPreferenceMap && dataSourceContainer != null) {
            List<String> databaseObjectList = ((DBPPreferenceMap) preferenceStore).getObject("databaseObjects");
            if (!CommonUtils.isEmpty(databaseObjectList)) {
                DBPProject finalProject = dataSourceContainer.getProject();
                try {
                    runnableContext.run(true, true, monitor -> {
                        try {
                            monitor.beginTask("Load database object list", databaseObjectList.size());
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
                            monitor.done();
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
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
        toolUserName  = preferenceStore.getString("tool.user");
        toolUserPassword = preferenceStore.getString("tool.password");

        try {
            final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();
            if (!CommonUtils.isEmpty(toolUserName)) toolUserName = encrypter.decrypt(toolUserName);
            if (!CommonUtils.isEmpty(toolUserPassword)) toolUserPassword = encrypter.decrypt(toolUserPassword);
        } catch (Exception e) {
            throw new DBException("Error decrypting user credentials", e);
        }

    }

    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        preferenceStore.setValue("project", getProject().getName());
        if (dataSourceContainer != null) {
            preferenceStore.setValue("dataSource", dataSourceContainer.getId());
        }

        if (preferenceStore instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            List<String> objectList = new ArrayList<>();
            for (BASE_OBJECT object : databaseObjects) {
                objectList.add(DBUtils.getObjectFullId(object));
            }

            ((DBPPreferenceMap) preferenceStore).getPropertyMap().put("databaseObjects", objectList);
        }

        preferenceStore.setValue(PROP_NAME_EXTRA_ARGS, extraCommandArgs);
        if (clientHomeName != null) {
            preferenceStore.setValue("clientHomeName", clientHomeName);
        }

        try {
            final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();

            if (!CommonUtils.isEmpty(toolUserName)) {
                preferenceStore.setValue("tool.user", encrypter.encrypt(toolUserName));
            } else {
                preferenceStore.setToDefault("tool.user");
            }
            if (!CommonUtils.isEmpty(toolUserPassword)) {
                preferenceStore.setValue("tool.password", encrypter.encrypt(toolUserPassword));
            } else {
                preferenceStore.setToDefault("tool.password");
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

}
