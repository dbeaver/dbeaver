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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual container
 */
public class DBVContainer extends DBVObject implements DBSObjectContainer {

    static final String ENTITY_PREFIX = ":";
    static final String CONFIG_PREFIX = "@";

    private final DBVContainer parent;
    private String name;
    private String type = "container";
    private String description;
    private Map<String, DBVContainer> containers = new LinkedHashMap<>();
    private Map<String, DBVEntity> entities = new LinkedHashMap<>();

    public DBVContainer(DBVContainer parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    DBVContainer(DBVContainer parent, String name, Map<String, Object> map) {
        this.parent = parent;
        this.name = name;
        for (Map.Entry<String, Object> element : map.entrySet()) {
            String id = element.getKey();
            if (id.startsWith(ENTITY_PREFIX)) {
                DBVEntity entity = new DBVEntity(this, id.substring(ENTITY_PREFIX.length()), (Map<String, Object>) element.getValue());
                entities.put(entity.getName(), entity);
            } else if (id.startsWith(CONFIG_PREFIX)) {
                String configMap = id.substring(CONFIG_PREFIX.length());
                if (configMap.equals("properties")) {
                    loadPropertiesFrom(map, id);
                }
            } else if (element.getValue() instanceof Map) {
                DBVContainer child = new DBVContainer(this, id, (Map<String, Object>) element.getValue());
                containers.put(child.getName(), child);
            }
        }
    }

    synchronized void dispose() {
        for (DBVEntity entity : entities.values()) {
            entity.dispose();
        }
        entities.clear();

        for (DBVContainer container : containers.values()) {
            container.dispose();
        }
        containers.clear();
    }

    public DBSObjectContainer getRealContainer(DBRProgressMonitor monitor) throws DBException {
        DBSObjectContainer realParent = parent.getRealContainer(monitor);
        if (realParent == null) {
            return null;
        }
        DBSObject child = realParent.getChild(monitor, name);
        if (child instanceof DBSObjectContainer) {
            return (DBSObjectContainer) child;
        }
        log.warn("Child '" + name + "' of '" + realParent.getName() + "' is not an object container");
        return null;
    }

    @Override
    public DBVContainer getParentObject() {
        return parent;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return parent.getDataSource();
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    public Collection<DBVContainer> getContainers() {
        return containers.values();
    }

    public DBVContainer getContainer(String name, boolean createNew) {
        DBVContainer container = containers.get(name);
        if (container == null && createNew) {
            container = new DBVContainer(this, name);
            addContainer(container);
        }
        return container;
    }

    void addContainer(DBVContainer container) {
        containers.put(container.getName(), container);
    }

    @NotNull
    public Collection<DBVEntity> getEntities() {
        return entities.values();
    }

    public synchronized DBVEntity getEntity(String name, boolean createNew) {
        DBVEntity entity = entities.get(name);
        if (entity == null && createNew) {
            entity = new DBVEntity(this, name, (String) null);
            entities.put(name, entity);
        }
        return entity;
    }

    synchronized void addEntity(DBVEntity entity) {
        entities.put(entity.getName(), entity);
    }

    synchronized void removeEntity(DBVEntity entity) {
        entities.remove(entity.getName());
        entity.dispose();
    }

    @Override
    public boolean hasValuableData() {
        if (!CommonUtils.isEmpty(getProperties())) {
            return true;
        }
        for (DBVEntity entity : getEntities()) {
            if (entity.hasValuableData()) {
                return true;
            }
        }
        for (DBVContainer child : getContainers()) {
            if (child.hasValuableData()) {
                return true;
            }
        }
        return false;
    }

    void copyFrom(DBVContainer container, DBVModel targetModel) {
        if (container instanceof DBVModel) {
            this.name = targetModel.getId();
        } else {
            this.name = container.name;
        }
        this.description = container.description;

        this.containers.clear();
        for (DBVContainer child : container.getContainers()) {
            DBVContainer myChild = new DBVContainer(this, child.getName());
            myChild.copyFrom(child, targetModel);
            containers.put(myChild.getName(), myChild);
        }

        this.entities.clear();
        for (DBVEntity child : container.getEntities()) {
            DBVEntity myChild = new DBVEntity(this, child, targetModel);
            entities.put(myChild.getName(), myChild);
        }

        super.copyFrom(container);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return !containers.isEmpty() ? containers.values() : entities.values();
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return !containers.isEmpty() ? containers.get(childName) : entities.get(childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return !containers.isEmpty() ? DBVContainer.class : DBVEntity.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        // do nothing
    }

    void renameEntity(DBVEntity entity, String oldName, String newName) {
        if (entities.remove(oldName) != null) {
            entities.put(newName, entity);
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
