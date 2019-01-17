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
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Table collector
 */
public class DiagramObjectCollector {

    private static final Log log = Log.getLog(DiagramObjectCollector.class);

    private final EntityDiagram diagram;
    private final List<ERDEntity> erdEntities = new ArrayList<>();

    public DiagramObjectCollector(EntityDiagram diagram)
    {
        this.diagram = diagram;
    }

    public static Collection<DBSEntity> collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots)
        throws DBException
    {
        Set<DBSEntity> tables = new LinkedHashSet<>();
        collectTables(monitor, roots, tables);
        return tables;
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots,
        Set<DBSEntity> tables)
        throws DBException
    {
        for (DBSObject root : roots) {
            if (monitor.isCanceled()) {
                break;
            }
            if (root instanceof DBSAlias) {
                root = ((DBSAlias) root).getTargetObject(monitor);
            }
            if (root instanceof DBSFolder) {
                collectTables(monitor, ((DBSFolder) root).getChildrenObjects(monitor), tables);
            } else if (root instanceof DBSEntity) {
                tables.add((DBSEntity) root);
            }
            if (root instanceof DBSObjectContainer) {
                collectTables(monitor, (DBSObjectContainer) root, tables);
            }
        }
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        DBSObjectContainer container,
        Set<DBSEntity> tables)
        throws DBException
    {
        if (monitor.isCanceled()) {
            return;
        }
        container.cacheStructure(monitor, DBSObjectContainer.STRUCT_ALL);
        final Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            Class<? extends DBSObject> childType = container.getChildType(monitor);
            DBSObjectFilter objectFilter = container.getDataSource().getContainer().getObjectFilter(childType, container, true);
            for (DBSObject entity : children) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (objectFilter != null && !objectFilter.matches(entity.getName())) {
                    continue;
                }
                if (entity instanceof DBSEntity) {
                    tables.add((DBSEntity) entity);
                } else if (entity instanceof DBSObjectContainer) {
                    collectTables(monitor, (DBSObjectContainer) entity, tables);
                }
            }
        }
    }

    public void generateDiagramObjects(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots)
        throws DBException
    {
        boolean showViews = ERDActivator.getDefault().getPreferenceStore().getBoolean(ERDConstants.PREF_DIAGRAM_SHOW_VIEWS);
        Collection<DBSEntity> tables = collectTables(monitor, roots);
        for (DBSEntity table : tables) {
            if (DBUtils.isHiddenObject(table)) {
                // Skip hidden tables
                continue;
            }
            if (!showViews && table instanceof DBSTable && ((DBSTable) table).isView()) {
                // Skip views
                continue;
            }
            addDiagramEntity(monitor, table);
        }

        // Add new relations
        for (ERDEntity erdEntity : erdEntities) {
            erdEntity.addModelRelations(monitor, diagram, true, false);
        }
    }

    private void addDiagramEntity(DBRProgressMonitor monitor, DBSEntity table)
    {
        if (diagram.containsTable(table) && !diagram.getDecorator().allowEntityDuplicates()) {
            // Avoid duplicates
            return;
        }
        ERDEntity erdEntity = ERDUtils.makeEntityFromObject(monitor, diagram, erdEntities, table, null);
        if (erdEntity != null) {
            erdEntities.add(erdEntity);
        }
    }

    private boolean aliasExist(String alias) {
        for (ERDEntity entity : erdEntities) {
            if (CommonUtils.equalObjects(entity.getAlias(), alias)) {
                return true;
            }
        }
        return false;
    }

    public List<ERDEntity> getDiagramEntities()
    {
        return erdEntities;
    }

    public static List<ERDEntity> generateEntityList(final EntityDiagram diagram, Collection<DBPNamedObject> objects)
    {
        final List<DBSObject> roots = new ArrayList<>();
        for (DBPNamedObject object : objects) {
            if (object instanceof DBSObject) {
                roots.add((DBSObject) object);
            }
        }

        final List<ERDEntity> entities = new ArrayList<>();

        try {
            UIUtils.runInProgressService(monitor -> {
                DiagramObjectCollector collector = new DiagramObjectCollector(diagram);
                try {
                    collector.generateDiagramObjects(monitor, roots);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
                entities.addAll(collector.getDiagramEntities());
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // interrupted
        }
        return entities;
    }

}
