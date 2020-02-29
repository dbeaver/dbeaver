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
package org.jkiss.dbeaver.model.virtual;

import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

    private DBPDataSourceContainer dataSourceContainer;
    @NotNull
    private String id;

    public DBVModel(@NotNull String id, @NotNull Map<String, Object> map) {
        super(null, id, map);
        this.id = id;
    }

    public DBVModel(@NotNull DBPDataSourceContainer dataSourceContainer) {
        super(null, dataSourceContainer.getId());
        this.dataSourceContainer = dataSourceContainer;
        this.id = dataSourceContainer.getId();
    }

    // Copy constructor
    public DBVModel(@NotNull DBPDataSourceContainer dataSourceContainer, @NotNull DBVModel source) {
        this(dataSourceContainer);
        copyFrom(source);
    }

    public void dispose() {
        super.dispose();
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBSObjectContainer getRealContainer(DBRProgressMonitor monitor) throws DBException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            dataSourceContainer.connect(monitor, true, true);
            dataSource = dataSourceContainer.getDataSource();
        }
        if (dataSource instanceof DBSObjectContainer) {
            return (DBSObjectContainer) dataSource;
        }
        log.warn("Datasource '" + dataSource + "' is not an object container");
        return null;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return dataSourceContainer == null ? null : dataSourceContainer.getDataSource();
    }

    /**
     * Search for virtual entity descriptor
     *
     * @param entity    entity
     * @param createNew create new entity if missing
     * @return entity virtual entity
     */
    public DBVEntity findEntity(DBSEntity entity, boolean createNew) {
        return findEntity(entity, entity.getName(), createNew);
    }

    // Pass explicit entity name - it is needed to handle entity rename (we will use old entity name here)
    DBVEntity findEntity(DBSEntity entity, String entityName, boolean createNew) {
        DBSObject[] path = DBUtils.getObjectPath(entity, false);
        if (path.length == 0) {
            log.warn("Empty entity path");
            return null;
        }
        if (path[0] != dataSourceContainer) {
            log.warn("Entity's root must be datasource container '" + dataSourceContainer.getName() + "'");
            return null;
        }
        DBVContainer container = this;
        for (int i = 1; i < path.length; i++) {
            DBSObject item = path[i];
            container = container.getContainer(item.getName(), createNew);
            if (container == null) {
                return null;
            }
        }
        return container.getEntity(entityName, createNew);
    }

    public DBVObject findObject(DBSObject source, boolean create) {
        DBSObject[] path = DBUtils.getObjectPath(source, true);
        if (path.length == 0) {
            log.warn("Empty entity path");
            return null;
        }
        if (path[0] != dataSourceContainer) {
            log.warn("Entity's root must be datasource container '" + dataSourceContainer.getName() + "'");
            return null;
        }
        DBVContainer container = this;
        for (int i = 1; i < path.length; i++) {
            DBSObject item = path[i];
            DBVContainer childContainer = container.getContainer(item.getName(), create);
            if (childContainer == null) {
                if (i == path.length - 1) {
                    return container.getEntity(item.getName(), create);
                }
                return null;
            } else {
                container = childContainer;
            }
        }
        return container;
    }

    public void serialize(DBRProgressMonitor monitor, JsonWriter json) throws IOException, DBException {
        DBVModelSerializerModern.serializeContainer(monitor, json, this);
    }

    @Deprecated
    public void serialize(XMLBuilder xml) throws IOException {
        DBVModelSerializerLegacy.serializeContainer(xml, this);
    }

    @Deprecated
    public SAXListener getModelParser() {
        return new DBVModelSerializerLegacy.ModelParser(this);
    }

    public void copyFrom(DBVModel model) {
        super.copyFrom(model, this);
    }

    private static final Map<String, List<DBVEntityForeignKey>> globalReferenceCache = new HashMap<>();

    public static Map<String, List<DBVEntityForeignKey>> getGlobalReferenceCache() {
        synchronized (globalReferenceCache) {
            return new HashMap<>(globalReferenceCache);
        }
    }

    @Nullable
    public static List<DBVEntityForeignKey> getGlobalReferences(DBNDatabaseNode databaseNode) {
        synchronized (globalReferenceCache) {
            return globalReferenceCache.get(databaseNode.getNodeItemPath());
        }
    }

    static void addToCache(@NotNull DBVEntityForeignKey foreignKey) {
        synchronized (globalReferenceCache) {
            List<DBVEntityForeignKey> fkList = globalReferenceCache.computeIfAbsent(foreignKey.getRefEntityId(), s -> new ArrayList<>());
            fkList.add(foreignKey);
        }
    }

    static void removeFromCache(@NotNull DBVEntityForeignKey foreignKey) {
        synchronized (globalReferenceCache) {
            String refEntityId = foreignKey.getRefEntityId();
            List<DBVEntityForeignKey> fkList = globalReferenceCache.get(refEntityId);
            if (fkList != null) {
                fkList.remove(foreignKey);
                if (fkList.isEmpty()) {
                    globalReferenceCache.remove(refEntityId);
                }
            }
        }
    }

    private static void renameEntityInGlobalCache(String newRefEntityId, String oldName, String newName) {
        String oldRefEntityId = newRefEntityId.replace("/" + newName, "/" + oldName);
        synchronized (globalReferenceCache) {
            List<DBVEntityForeignKey> fkList = globalReferenceCache.get(oldRefEntityId);
            if (fkList != null) {
                globalReferenceCache.remove(oldRefEntityId);
                globalReferenceCache.put(newRefEntityId, fkList);
                for (DBVEntityForeignKey fk : fkList) {
                    fk.setRefEntityId(newRefEntityId);
                    fk.getEntity().persistConfiguration();
                }
            }
        }
    }

    public static void checkGlobalCacheIsEmpty() {
        synchronized (globalReferenceCache) {
            if (!globalReferenceCache.isEmpty()) {
                log.error("Virtual references cache is not empty. Possible memory leak: " + globalReferenceCache);
            }
        }
    }

    public static class ModelChangeListener implements DBPEventListener {
        @Override
        public void handleDataSourceEvent(DBPEvent event) {
            DBSObject object = event.getObject();
            if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && object instanceof DBSEntity) {
                // Handle table renames
                Map<String, Object> options = event.getOptions();
                if (options != null) {
                    String oldName = (String)options.get(DBEObjectRenamer.PROP_OLD_NAME);
                    String newName = (String)options.get(DBEObjectRenamer.PROP_NEW_NAME);
                    if (oldName != null && newName != null) {
                        handleEntityRename((DBSEntity) object, oldName, newName);
                    }
                }
            }
        }

    }

    private static void handleEntityRename(DBSEntity object, String oldName, String newName) {
        DBNDatabaseNode objectNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(object);
        if (objectNode != null) {
            String objectNodePath = objectNode.getNodeItemPath();
            renameEntityInGlobalCache(objectNodePath, oldName, newName);
        }
        if (object.getDataSource() != null) {
            DBVModel vModel = object.getDataSource().getContainer().getVirtualModel();

            DBVEntity vEntity = vModel.findEntity(object, oldName, false);
            if (vEntity != null) {
                vEntity.handleRename(oldName, newName);
                vEntity.persistConfiguration();
            }
        }
    }
}
