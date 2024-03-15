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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.ExportSettingsExtension;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PostgreBackupAllSettings extends AbstractImportExportSettings<DBSObject>
    implements ExportSettingsExtension<PostgreDatabaseBackupAllInfo> {

    private static final Log log = Log.getLog(PostgreBackupAllSettings.class);
    
    private static final String PROP_EXPORT_ALL_ENCODING = "pg.export.all.encoding";
    private static final String PROP_EXPORT_ALL_EXPORT_ONLY_METADATA = "pg.export.all.exportOnlyMetadata";
    private static final String PROP_EXPORT_ALL_ONLY_GLOBALS = "pg.export.all.exportOnlyGlobals";
    private static final String PROP_EXPORT_ALL_ONLY_ROLES = "pg.export.all.exportOnlyRoles";
    private static final String PROP_EXPORT_ALL_ONLY_TABLESPACES = "pg.export.all.exportOnlyTablespaces";
    private static final String PROP_EXPORT_ALL_NO_PRIVILEGES = "pg.export.all.noPrivileges";
    private static final String PROP_EXPORT_ALL_NO_OWNER = "pg.export.all.noOwner";
    private static final String PROP_EXPORT_ALL_ADD_ROLES_PASSWORDS = "pg.export.all.addRolesPasswords";
    private static final String PROP_EXPORT_OBJECTS_ALL = "exportObjects.all";
    private static final String PROP_DATASOURCE = "datasource";
    private static final String PROP_DATABASES = "databases";

    private final List<PostgreDatabaseBackupAllInfo> exportObjects = new ArrayList<>();

    private String encoding;
    private boolean exportOnlyMetadata;
    private boolean exportOnlyGlobals;
    private boolean exportOnlyRoles;
    private boolean exportOnlyTablespaces;
    private boolean noPrivileges;
    private boolean noOwner;
    private boolean addRolesPasswords;

    public PostgreBackupAllSettings() {
    }
    public PostgreBackupAllSettings(@NotNull DBPProject project) {
        super(project);
    }

    @NotNull
    @Override
    public String getOutputFile(@NotNull PostgreDatabaseBackupAllInfo info) {
        DBSObjectContainer container = getContainerObject(info.getDatabases());
        String outputFileName = resolveVars(
            container != null ? container : info.getDataSource(),
            null,
            null,
            getOutputFilePattern());
        String outputFolder = getOutputFolder(info);
        return makeOutFilePath(outputFolder, outputFileName);
    }

    @NotNull
    @Override
    public final List<PostgreDatabaseBackupAllInfo> getExportObjects() {
        return exportObjects;
    }

    @NotNull
    @Override
    public String getOutputFolder(@NotNull PostgreDatabaseBackupAllInfo info) {
        DBSObjectContainer container = getContainerObject(info.getDatabases());
        return resolveVars(
            container != null ? container : info.getDataSource(), null, null, getOutputFolderPattern());
    }

    @Nullable
    private DBSObjectContainer getContainerObject(@Nullable List<PostgreDatabase> databases) {
        final Iterator<? extends PostgreDatabase> iterator = databases == null ? null : databases.iterator();
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
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

    @Override
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
        encoding = store.getString(PROP_EXPORT_ALL_ENCODING);
        exportOnlyMetadata = store.getBoolean(PROP_EXPORT_ALL_EXPORT_ONLY_METADATA);
        exportOnlyGlobals = store.getBoolean(PROP_EXPORT_ALL_ONLY_GLOBALS);
        exportOnlyRoles = store.getBoolean(PROP_EXPORT_ALL_ONLY_ROLES);
        exportOnlyTablespaces = store.getBoolean(PROP_EXPORT_ALL_ONLY_TABLESPACES);
        noPrivileges = store.getBoolean(PROP_EXPORT_ALL_NO_PRIVILEGES);
        noOwner = store.getBoolean(PROP_EXPORT_ALL_NO_OWNER);
        addRolesPasswords = store.getBoolean(PROP_EXPORT_ALL_ADD_ROLES_PASSWORDS);

        super.loadSettings(runnableContext, store);
        if (store instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject(PROP_EXPORT_OBJECTS_ALL);
            if (!CommonUtils.isEmpty(objectList)) {
                for (Map<String, Object> object : objectList) {
                    String catalogId = CommonUtils.toString(object.get(PROP_DATASOURCE));
                    if (!CommonUtils.isEmpty(catalogId)) {
                        List<String> databaseNames = (List<String>) object.get(PROP_DATABASES);
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
        @NotNull DBRRunnableContext runnableContext,
        @NotNull String catalogId,
        @Nullable List<String> databaseNames)
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
        store.setValue(PROP_EXPORT_ALL_ENCODING, encoding);
        store.setValue(PROP_EXPORT_ALL_EXPORT_ONLY_METADATA, exportOnlyMetadata);
        store.setValue(PROP_EXPORT_ALL_ONLY_GLOBALS, exportOnlyGlobals);
        store.setValue(PROP_EXPORT_ALL_ONLY_ROLES, exportOnlyRoles);
        store.setValue(PROP_EXPORT_ALL_ONLY_TABLESPACES, exportOnlyTablespaces);
        store.setValue(PROP_EXPORT_ALL_NO_PRIVILEGES, noPrivileges);
        store.setValue(PROP_EXPORT_ALL_NO_OWNER, noOwner);
        store.setValue(PROP_EXPORT_ALL_ADD_ROLES_PASSWORDS, addRolesPasswords);

        if (store instanceof DBPPreferenceMap && !CommonUtils.isEmpty(exportObjects)) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = new ArrayList<>();
            for (PostgreDatabaseBackupAllInfo object : exportObjects) {
                Map<String, Object> objInfo = new LinkedHashMap<>();
                objInfo.put(PROP_DATASOURCE, DBUtils.getObjectFullId(object.getDataSource()));
                if (!CommonUtils.isEmpty(object.getDatabases())) {
                    List<String> tableList = new ArrayList<>();
                    for (PostgreDatabase database : object.getDatabases()) {
                        tableList.add(database.getName());
                    }
                    objInfo.put(PROP_DATABASES, tableList);
                }
                objectList.add(objInfo);
            }
            ((DBPPreferenceMap) store).getPropertyMap().put(PROP_EXPORT_OBJECTS_ALL, objectList);
        }
    }
}
