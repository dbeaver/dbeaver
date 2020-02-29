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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostgreDatabaseBackupSettings extends PostgreBackupRestoreSettings {

    private static final Log log = Log.getLog(PostgreDatabaseBackupSettings.class);

    private List<PostgreDatabaseBackupInfo> exportObjects = new ArrayList<>();

    private String compression;
    private String encoding;
    private boolean showViews;
    private boolean useInserts;
    private boolean noPrivileges;
    private boolean noOwner;

    public List<PostgreDatabaseBackupInfo> getExportObjects() {
        return exportObjects;
    }

    public void setExportObjects(List<PostgreDatabaseBackupInfo> exportObjects) {
        this.exportObjects = exportObjects;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isShowViews() {
        return showViews;
    }

    public void setShowViews(boolean showViews) {
        this.showViews = showViews;
    }

    public boolean isUseInserts() {
        return useInserts;
    }

    public void setUseInserts(boolean useInserts) {
        this.useInserts = useInserts;
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

    public void fillExportObjectsFromInput() {
        Map<PostgreDatabase, PostgreDatabaseBackupInfo> objMap = new LinkedHashMap<>();
        for (DBSObject object : getDatabaseObjects()) {
            PostgreDatabase database = null;
            PostgreSchema schema = null;
            if (object instanceof PostgreDatabase) {
                database = (PostgreDatabase) object;
            } else if (object instanceof PostgreSchema) {
                database = ((PostgreSchema) object).getDatabase();
                schema = (PostgreSchema) object;
            } else if (object instanceof PostgreTableBase) {
                database = ((PostgreTableBase) object).getDatabase();
                schema = ((PostgreTableBase) object).getSchema();
            }
            if (database == null) {
                continue;
            }
            PostgreDatabaseBackupInfo info = objMap.computeIfAbsent(database, db -> new PostgreDatabaseBackupInfo(db, null, null));
            if (schema != null) {
                List<PostgreSchema> schemas = info.getSchemas();
                if (schemas == null) {
                    schemas = new ArrayList<>();
                    info.setSchemas(schemas);
                }
                if (!schemas.contains(schema)) {
                    schemas.add(schema);
                }
            }
            if (object instanceof PostgreTableBase) {
                List<PostgreTableBase> tables = info.getTables();
                if (tables == null) {
                    tables = new ArrayList<>();
                    info.setTables(tables);
                }
                tables.add((PostgreTableBase) object);
            }
        }
        getExportObjects().addAll(objMap.values());

        updateDataSourceContainer();
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);

        compression = store.getString("pg.export.compression");
        encoding = store.getString("pg.export.encoding");
        showViews = store.getBoolean("pg.export.showViews");
        useInserts = store.getBoolean("pg.export.useInserts");
        noPrivileges = store.getBoolean("pg.export.noPrivileges");
        noOwner = store.getBoolean("pg.export.noOwner");

        if (store instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject("exportObjects");
            if (!CommonUtils.isEmpty(objectList)) {
                for (Map<String, Object> object : objectList) {
                    String catalogId = CommonUtils.toString(object.get("database"));
                    if (!CommonUtils.isEmpty(catalogId)) {
                        List<String> schemaNames = (List<String>) object.get("schemas");
                        List<String> tableNames = (List<String>) object.get("tables");
                        PostgreDatabaseBackupInfo exportInfo = loadDatabaseExportInfo(runnableContext, catalogId, schemaNames, tableNames);
                        if (exportInfo != null) {
                            exportObjects.add(exportInfo);
                        }
                    }
                }
            }
        }
    }


    private PostgreDatabaseBackupInfo loadDatabaseExportInfo(DBRRunnableContext runnableContext, String catalogId, List<String> schemaNames, List<String> tableNames) {
        PostgreDatabaseBackupInfo[] exportInfo = new PostgreDatabaseBackupInfo[1];
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    PostgreDatabase database = (PostgreDatabase) DBUtils.findObjectById(monitor, getProject(), catalogId);
                    if (database == null) {
                        throw new DBException("Database " + catalogId + " not found");
                    }
                    List<PostgreSchema> schemas = null;
                    List<PostgreTableBase> tables = null;
                    if (!CommonUtils.isEmpty(schemaNames)) {
                        schemas = new ArrayList<>();
                        for (String schemaName : schemaNames) {
                            PostgreSchema schema = database.getSchema(monitor, schemaName);
                            if (schema != null) {
                                schemas.add(schema);
                            } else {
                                log.debug("Schema '" + schemaName + "' not found in database '" + database.getName() + "'");
                            }
                        }
                    }
                    if (!CommonUtils.isEmpty(tableNames) && !CommonUtils.isEmpty(schemas)) {
                        PostgreSchema schema = schemas.get(0);
                        tables = new ArrayList<>();
                        for (String tableName : tableNames) {
                            PostgreTableBase table = schema.getTableCache().getObject(monitor, schema, tableName);
                            if (table != null) {
                                tables.add(table);
                            } else {
                                log.debug("Table '" + tableName + "' not found in schema '" + schema.getName() + "'");
                            }
                        }
                    }
                    exportInfo[0] = new PostgreDatabaseBackupInfo(database, schemas, tables);
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

        store.setValue("pg.export.compression", compression);
        store.setValue("pg.export.encoding", encoding);
        store.setValue("pg.export.showViews", showViews);
        store.setValue("pg.export.useInserts", useInserts);
        store.setValue("pg.export.noPrivileges", noPrivileges);
        store.setValue("pg.export.noOwner", noOwner);

        if (store instanceof DBPPreferenceMap && !CommonUtils.isEmpty(exportObjects)) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = new ArrayList<>();
            for (PostgreDatabaseBackupInfo object : exportObjects) {
                Map<String, Object> objInfo = new LinkedHashMap<>();
                objInfo.put("database", DBUtils.getObjectFullId(object.getDatabase()));
                if (!CommonUtils.isEmpty(object.getSchemas())) {
                    List<String> tableList = new ArrayList<>();
                    for (PostgreSchema schema : object.getSchemas()) {
                        tableList.add(schema.getName());
                    }
                    objInfo.put("schemas", tableList);
                }
                if (!CommonUtils.isEmpty(object.getTables())) {
                    List<String> tableList = new ArrayList<>();
                    for (PostgreTableBase table : object.getTables()) {
                        tableList.add(table.getName());
                    }
                    objInfo.put("tables", tableList);
                }
                objectList.add(objInfo);
            }

            ((DBPPreferenceMap) store).getPropertyMap().put("exportObjects", objectList);
        }
    }
}
