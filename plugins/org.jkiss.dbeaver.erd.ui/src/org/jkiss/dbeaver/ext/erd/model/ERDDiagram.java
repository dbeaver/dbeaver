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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Point;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDObject;
import org.jkiss.dbeaver.ext.erd.ERDUIUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.virtual.DBVUtils;

import java.util.*;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hierarchy
 *
 * @author Serge Rider
 */
public class ERDDiagram extends ERDObject<DBSObject> implements ERDContainer {
    private static final Log log = Log.getLog(ERDDiagram.class);

    private static class DataSourceInfo {
        int index;
        List<ERDEntity> entities = new ArrayList<>();

        public DataSourceInfo(int index) {
            this.index = index;
        }
    }

    private final ERDContentProvider contentProvider;
    private String name;
    private final List<ERDEntity> entities = new ArrayList<>();
    private final Map<DBPDataSourceContainer, DataSourceInfo> dataSourceMap = new LinkedHashMap<>();
    private final Map<DBPDataSourceContainer, Map<DBSObjectContainer, Integer>> dataSourceContainerMap = new LinkedHashMap<>();
    private boolean layoutManualDesired = true;
    private boolean layoutManualAllowed = false;
    private boolean needsAutoLayout;

    private final Map<DBSEntity, ERDEntity> entityMap = new IdentityHashMap<>();

    private final List<ERDNote> notes = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    public ERDDiagram(DBSObject container, String name, ERDContentProvider contentProvider) {
        super(container);
        this.name = name;
        this.contentProvider = contentProvider;

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
    }

    @Override
    public ERDContentProvider getContentProvider() {
        return contentProvider;
    }

    public int getEntityOrder(ERDEntity entity) {
        synchronized (entities) {
            return entities.indexOf(entity);
        }
    }

    public void addEntity(ERDEntity entity, boolean reflect) {
        addEntity(entity, -1, reflect);
    }

    public void addEntity(ERDEntity entity, int i, boolean reflect) {
        DBSEntity object = entity.getObject();
        if (object == null) {
            log.debug("Null object passed");
            return;
        } else if (object.getDataSource() == null) {
            log.debug("Object " + object.getName() + " is not connected with datasource");
            return;
        }
        synchronized (entities) {
            if (i < 0) {
                entities.add(entity);
            } else {
                entities.add(i, entity);
            }
            entityMap.put(object, entity);

            DBPDataSourceContainer dataSource = object.getDataSource().getContainer();
            DataSourceInfo dsInfo = dataSourceMap.computeIfAbsent(dataSource, dsc -> new DataSourceInfo(dataSourceMap.size()));
            dsInfo.entities.add(entity);

            DBSObjectContainer container = DBUtils.getParentOfType(DBSObjectContainer.class, entity.getObject());
            if (container != null) {
                dataSourceContainerMap.putIfAbsent(dataSource, new LinkedHashMap<>());
                Map<DBSObjectContainer, Integer> containerMap = dataSourceContainerMap.get(dataSource);
                containerMap.putIfAbsent(container, containerMap.size());
                }
            }


        if (reflect) {
            firePropertyChange(CHILD, null, entity);
/*
            for (ERDAssociation rel : entity.getReferences()) {
                entity.firePropertyChange(INPUT, null, rel);
            }
            for (ERDAssociation rel : entity.getAssociations()) {
                entity.firePropertyChange(OUTPUT, null, rel);
            }
*/
        }

        resolveRelations(reflect);

        if (reflect) {
            for (ERDAssociation rel : entity.getReferences()) {
                rel.getSourceEntity().firePropertyChange(OUTPUT, null, rel);
            }
            for (ERDAssociation rel : entity.getAssociations()) {
                rel.getTargetEntity().firePropertyChange(INPUT, null, rel);
            }
        }
    }


    private void resolveRelations(boolean reflect) {
        // Resolve incomplete relations
        for (ERDEntity erdEntity : getEntities()) {
            erdEntity.resolveRelations(this, reflect);
        }
    }

    public synchronized void removeEntity(ERDEntity entity, boolean reflect) {
        synchronized (entities) {
            entityMap.remove(entity.getObject());
            entities.remove(entity);

            DBPDataSourceContainer dataSource = entity.getObject().getDataSource().getContainer();
            DataSourceInfo dsInfo = dataSourceMap.get(dataSource);
            dsInfo.entities.remove(entity);
            if (dsInfo.entities.isEmpty()) {
                dataSourceMap.remove(dataSource);
            }

        }
        if (reflect) {
            firePropertyChange(CHILD, entity, null);
        }
    }

    /**
     * @return the Tables for the current schema
     */
    @Override
    public List<ERDEntity> getEntities() {
        return entities;
    }

    public List<ERDNote> getNotes() {
        return notes;
    }

    public void addNote(ERDNote note, boolean reflect) {
        synchronized (notes) {
            notes.add(note);
        }

        if (reflect) {
            firePropertyChange(CHILD, null, note);
        }
    }

    public void removeNote(ERDNote note, boolean reflect) {
        synchronized (notes) {
            notes.remove(note);
        }

        if (reflect) {
            firePropertyChange(CHILD, note, null);
        }
    }

    /**
     * @return the name of the schema
     */
    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param layoutManualAllowed The layoutManualAllowed to set.
     */
    public void setLayoutManualAllowed(boolean layoutManualAllowed) {
        this.layoutManualAllowed = layoutManualAllowed;
    }

    /**
     * @return Returns the layoutManualDesired.
     */
    public boolean isLayoutManualDesired() {
        return layoutManualDesired;
    }

    /**
     * @param layoutManualDesired The layoutManualDesired to set.
     */
    public void setLayoutManualDesired(boolean layoutManualDesired) {
        this.layoutManualDesired = layoutManualDesired;
    }

    /**
     * @return Returns whether we can lay out individual entities manually using the XYLayout
     */
    public boolean isLayoutManualAllowed() {
        return layoutManualAllowed;
    }

    public int getEntityCount() {
        return entities.size();
    }

    public ERDDiagram copy() {
        ERDDiagram copy = new ERDDiagram(object, name, contentProvider);
        copy.entities.addAll(this.entities);
        copy.entityMap.putAll(this.entityMap);
        copy.layoutManualDesired = this.layoutManualDesired;
        copy.layoutManualAllowed = this.layoutManualAllowed;
        return copy;
    }

    public void fillEntities(DBRProgressMonitor monitor, Collection<DBSEntity> entities, DBSObject dbObject) throws DBException {
        // Load entities
        monitor.beginTask("Load entities metadata", entities.size());
        List<ERDEntity> entityCache = new ArrayList<>();
        for (DBSEntity table : entities) {
            if (monitor.isCanceled()) {
                break;
            }
            try {
                table = DBVUtils.getRealEntity(monitor, table);
            } catch (DBException e) {
                log.error("Error resolving real entity for " + table.getName());
            }
            if (entityMap.containsKey(table)) {
                continue;
            }
            monitor.subTask("Load " + table.getName());
            ERDEntity erdEntity = ERDUIUtils.makeEntityFromObject(monitor, this, entityCache, table, null);
            erdEntity.setPrimary(table == dbObject);

            addEntity(erdEntity, false);
            entityMap.put(table, erdEntity);
            entityCache.add(erdEntity);

            monitor.worked(1);
        }

        monitor.done();

        // Load relations
        monitor.beginTask("Load entities' relations", entities.size());
        for (ERDEntity erdEntity : entityCache) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask("Load " + erdEntity.getName());
            erdEntity.addModelRelations(monitor, this, true, false);
            monitor.worked(1);
        }
        monitor.done();
    }

    public boolean containsTable(DBSEntity table) {
        for (ERDEntity erdEntity : entities) {
            if (erdEntity.getObject() == table) {
                return true;
            }
        }
        return false;
    }

    public Map<DBSEntity, ERDEntity> getEntityMap() {
        return entityMap;
    }

    public ERDEntity getEntity(DBSEntity table) {
        return entityMap.get(table);
    }

    public List<ERDEntity> getEntities(DBSEntity table) {
        List<ERDEntity> result = new ArrayList<>();
        for (ERDEntity entity : entities) {
            if (entity.getObject() == table) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<DBPDataSourceContainer> getDataSources() {
        return new ArrayList<>(dataSourceMap.keySet());
    }

    public List<ERDEntity> getEntities(DBPDataSourceContainer dataSourceContainer) {
        DataSourceInfo dsInfo = dataSourceMap.get(dataSourceContainer);
        return dsInfo == null ? Collections.emptyList() : dsInfo.entities;
    }

    public int getDataSourceIndex(DBPDataSourceContainer dataSource) {
        DataSourceInfo dsInfo = dataSourceMap.get(dataSource);
        return dsInfo == null ? 0 : dsInfo.index;
    }

    public int getContainerIndex(DBPDataSourceContainer dataSource, DBSObjectContainer container) {
        Map<DBSObjectContainer, Integer> containerMap = dataSourceContainerMap.get(dataSource);
        Integer index;
        if (containerMap != null) {
            index = containerMap.get(container);
            return index == null ? 0 : index;
        }
        return 0;
    }

    public void clear() {
        this.entities.clear();
        this.entityMap.clear();
    }

    public boolean isNeedsAutoLayout() {
        return needsAutoLayout;
    }

    public void setNeedsAutoLayout(boolean needsAutoLayout) {
        this.needsAutoLayout = needsAutoLayout;
    }

    public void addInitRelationBends(ERDElement<?> sourceEntity, ERDElement<?> targetEntity, String relName, List<Point> bends) {
        for (ERDAssociation rel : sourceEntity.getReferences()) {
            if (rel.getSourceEntity() == targetEntity && relName.equals(rel.getObject().getName())) {
                rel.setInitBends(bends);
            }
        }
    }

    public List<ERDObject> getContents() {
        List<ERDObject> children = new ArrayList<>(entities.size() + notes.size());
        children.addAll(entities);
        children.addAll(notes);
        return children;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String message) {
        errorMessages.add(message);
    }

}
