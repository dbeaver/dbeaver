/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.ExportSettingsExtension;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostgreBackupAllSettings extends AbstractImportExportSettings<DBSObject>
    implements ExportSettingsExtension<PostgreDatabaseBackupAllInfo> {

    private static final Log log = Log.getLog(PostgreBackupAllSettings.class);

    private List<PostgreDatabaseBackupAllInfo> exportObjects = new ArrayList<>();

    private String encoding;
    private boolean exportOnlyMetadata;
    private boolean exportOnlyGlobals;
    private boolean exportOnlyRoles;
    private boolean exportOnlyTablespaces;
    private boolean noPrivileges;
    private boolean noOwner;
    private boolean addRolesPasswords;
    private File outputFolder;

    @NotNull
    public File getOutputFile(@NotNull PostgreDatabaseBackupAllInfo info) {
        String outputFileName = resolveVars(info.getDataSource(), null, null, getOutputFilePattern());
        return new File(getOutputFolder(info), outputFileName);
    }

    @NotNull
    @Override
    public List<PostgreDatabaseBackupAllInfo> getExportObjects() {
        return exportObjects;
    }

    @NotNull
    @Override
    public File getOutputFolder(@NotNull PostgreDatabaseBackupAllInfo info) {
        if (outputFolder == null) {
            outputFolder = new File(resolveVars(info.getDataSource(), null, null, getOutputFolderPattern()));
        }
        return outputFolder;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isExportOnlyMetadata() {
        return exportOnlyMetadata;
    }

    public void setExportOnlyMetadata(boolean exportOnlyMetadata) {
        this.exportOnlyMetadata = exportOnlyMetadata;
    }

    public boolean isExportOnlyGlobals() {
        return exportOnlyGlobals;
    }

    public void setExportOnlyGlobals(boolean exportOnlyGlobals) {
        this.exportOnlyGlobals = exportOnlyGlobals;
    }

    public boolean isExportOnlyRoles() {
        return exportOnlyRoles;
    }

    public void setExportOnlyRoles(boolean exportOnlyRoles) {
        this.exportOnlyRoles = exportOnlyRoles;
    }

    public boolean isExportOnlyTablespaces() {
        return exportOnlyTablespaces;
    }

    public void setExportOnlyTablespaces(boolean exportOnlyTablespaces) {
        this.exportOnlyTablespaces = exportOnlyTablespaces;
    }

    public boolean isNoPrivileges() {
        return noPrivileges;
    }

    public void setNoPrivileges(boolean noPrivileges) {
        this.noPrivileges = noPrivileges;
    }

    public boolean isNoOwner() {
        return noOwner;
    }

    public void setNoOwner(boolean noOwner) {
        this.noOwner = noOwner;
    }

    public boolean isAddRolesPasswords() {
        return addRolesPasswords;
    }

    public void setAddRolesPasswords(boolean addRolesPasswords) {
        this.addRolesPasswords = addRolesPasswords;
    }

    public void fillExportObjectsFromInput() {
        PostgreDataSource dataSource = null;
        List<PostgreDatabase> databases = new ArrayList<>();
        for (DBSObject object : getDatabaseObjects()) {
            if (object instanceof PostgreDatabase) {
                PostgreDatabase database = (PostgreDatabase) object;
                dataSource = database.getDataSource();
                databases.add(database);
            }
        }
        if (dataSource != null) {
            exportObjects.add(new PostgreDatabaseBackupAllInfo(dataSource, databases));
            updateDataSourceContainer();
        }
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);
        encoding = store.getString("pg.export.all.encoding");
        exportOnlyMetadata = store.getBoolean("pg.export.all.exportOnlyMetadata");
        exportOnlyGlobals = store.getBoolean("pg.export.all.exportOnlyGlobals");
        exportOnlyRoles = store.getBoolean("pg.export.all.exportOnlyRoles");
        exportOnlyTablespaces = store.getBoolean("pg.export.all.exportOnlyTablespaces");
        noPrivileges = store.getBoolean("pg.export.all.noPrivileges");
        noOwner = store.getBoolean("pg.export.all.noOwner");
        addRolesPasswords = store.getBoolean("pg.export.all.addRolesPasswords");

        if (store instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject("exportObjects.all");
            if (!CommonUtils.isEmpty(objectList)) {
                for (Map<String, Object> object : objectList) {
                    String catalogId = CommonUtils.toString(object.get("datasource"));
                    if (!CommonUtils.isEmpty(catalogId)) {
                        List<String> databaseNames = (List<String>) object.get("databases");
                        PostgreDatabaseBackupAllInfo exportInfo = loadDatabaseExportInfo(runnableContext, catalogId, databaseNames);
                        if (exportInfo != null) {
                            exportObjects.add(exportInfo);
                        }
                    }
                }
            }
        }
    }

    private PostgreDatabaseBackupAllInfo loadDatabaseExportInfo(
        DBRRunnableContext runnableContext,
        @NotNull String catalogId,
        List<String> databaseNames)
    {
        PostgreDatabaseBackupAllInfo[] exportInfo = new PostgreDatabaseBackupAllInfo[1];
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    PostgreDataSource dataSource = null;
                    DBSObject object = DBUtils.findObjectById(monitor, getProject(), catalogId);
                    if (object instanceof PostgreDataSource) {
                        dataSource = (PostgreDataSource) object;
                    } else if (object instanceof DBPDataSourceContainer) {
                        dataSource = (PostgreDataSource) object.getDataSource();
                    }
                    if (dataSource == null) {
                        throw new DBException("Datasource " + catalogId + " not found");
                    }
                    List<PostgreDatabase> databases = new ArrayList<>();
                    for (String databaseName : CommonUtils.safeCollection(databaseNames)) {
                        PostgreDatabase database = dataSource.getDatabase(databaseName);
                        if (database != null) {
                            databases.add(database);
                        } else {
                            log.debug("Database '" + databaseName + "' not found in dataSource '" + dataSource.getName() + "'");
                        }
                    }
                    exportInfo[0] = new PostgreDatabaseBackupAllInfo(dataSource, databases);
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Error loading objects configuration", e);
        } catch (InterruptedException e) {
            // Ignore
        }
        return exportInfo[0];
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
        store.setValue("pg.export.all.encoding", encoding);
        store.setValue("pg.export.all.exportOnlyMetadata", exportOnlyMetadata);
        store.setValue("pg.export.all.exportOnlyGlobals", exportOnlyGlobals);
        store.setValue("pg.export.all.exportOnlyRoles", exportOnlyRoles);
        store.setValue("pg.export.all.exportOnlyTablespaces", exportOnlyTablespaces);
        store.setValue("pg.export.all.noPrivileges", noPrivileges);
        store.setValue("pg.export.all.noOwner", noOwner);
        store.setValue("pg.export.all.addRolesPasswords", addRolesPasswords);

        if (store instanceof DBPPreferenceMap && !CommonUtils.isEmpty(exportObjects)) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = new ArrayList<>();
            for (PostgreDatabaseBackupAllInfo object : exportObjects) {
                Map<String, Object> objInfo = new LinkedHashMap<>();
                objInfo.put("datasource", DBUtils.getObjectFullId(object.getDataSource()));
                if (!CommonUtils.isEmpty(object.getDatabases())) {
                    List<String> tableList = new ArrayList<>();
                    for (PostgreDatabase database : object.getDatabases()) {
                        tableList.add(database.getName());
                    }
                    objInfo.put("databases", tableList);
                }
                objectList.add(objInfo);
            }
            ((DBPPreferenceMap) store).getPropertyMap().put("exportObjects.all", objectList);
        }
    }
}
