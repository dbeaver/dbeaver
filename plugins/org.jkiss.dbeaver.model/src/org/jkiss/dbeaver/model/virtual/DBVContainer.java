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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual container
 */
public class DBVContainer extends DBVObject implements DBSObjectContainer {

    private final DBVContainer parent;
    private String name;
    private String description;
    private Map<String, DBVContainer> containers = new LinkedHashMap<>();
    private Map<String, DBVEntity> entities = new LinkedHashMap<>();

    public DBVContainer(DBVContainer parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    // Copy constructor
    DBVContainer(DBVContainer parent, DBVContainer source)
    {
        this.parent = parent;
        this.name = source.name;
        this.description = source.description;
        for (Map.Entry<String, DBVContainer> ce : source.containers.entrySet()) {
            this.containers.put(ce.getKey(), new DBVContainer(this, ce.getValue()));
        }
        for (Map.Entry<String, DBVEntity> ee : source.entities.entrySet()) {
            this.entities.put(ee.getKey(), new DBVEntity(this, ee.getValue()));
        }
    }

    public DBSObjectContainer getRealContainer(DBRProgressMonitor monitor) throws DBException
    {
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
    public DBVContainer getParentObject()
    {
        return parent;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return parent.getDataSource();
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public Collection<DBVContainer> getContainers() {
        return containers.values();
    }

    public DBVContainer getContainer(String name, boolean createNew)
    {
        DBVContainer container = containers.get(name);
        if (container == null && createNew) {
            container = new DBVContainer(this, name);
            addContainer(container);
        }
        return container;
    }

    void addContainer(DBVContainer container)
    {
        containers.put(container.getName(), container);
    }

    public Collection<DBVEntity> getEntities() {
        return entities.values();
    }

    public DBVEntity getEntity(String name, boolean createNew)
    {
        String dictName = name;
        DBVEntity entity = entities.get(dictName);
        if (entity == null && createNew) {
            entity = new DBVEntity(this, name, null);
            entities.put(dictName, entity);
        }
        return entity;
    }

    void addEntity(DBVEntity entity)
    {
        entities.put(entity.getName(), entity);
    }

    @Override
    public boolean hasValuableData() {
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

    void copyFrom(DBVContainer container) {
        this.name = container.name;
        this.description = container.description;

        this.containers.clear();
        for (DBVContainer child : container.getContainers()) {
            DBVContainer myChild = new DBVContainer(this, child.getName());
            myChild.copyFrom(child);
            containers.put(myChild.getName(), myChild);
        }

        this.entities.clear();
        for (DBVEntity child : container.getEntities()) {
            DBVEntity myChild = new DBVEntity(this, child);
            entities.put(myChild.getName(), myChild);
        }
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return !containers.isEmpty() ? containers.values() : entities.values();
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return !containers.isEmpty() ? containers.get(childName) : entities.get(childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return !containers.isEmpty() ? DBVContainer.class : DBVEntity.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        // do nothing
    }

    @Override
    public String toString() {
        return name;
    }
}
