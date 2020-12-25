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
package org.jkiss.dbeaver.erd.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Table collector
 */
public class DiagramObjectCollector {

    private static final Log log = Log.getLog(DiagramObjectCollector.class);

    private final ERDDiagram diagram;
    private final List<ERDEntity> erdEntities = new ArrayList<>();
    private boolean showViews;
    private boolean showPartitions;

    public DiagramObjectCollector(ERDDiagram diagram)
    {
        this.diagram = diagram;
    }

    public static Collection<DBSEntity> collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots,
        DiagramCollectSettings settings,
        boolean forceShowViews)
        throws DBException
    {
        Set<DBSEntity> tables = new LinkedHashSet<>();
        collectTables(monitor, roots, tables, settings, forceShowViews);
        return tables;
    }

    public void setShowViews(boolean showViews) {
        this.showViews = showViews;
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots,
        Set<DBSEntity> tables,
        DiagramCollectSettings settings,
        boolean forceShowViews)
        throws DBException
    {
        boolean showPartitions = settings.isShowPartitions();
        boolean showViews = settings.isShowViews();

        for (DBSObject root : roots) {
            if (monitor.isCanceled()) {
                break;
            }
            root = DBUtils.getPublicObject(root);
            if (root instanceof DBSAlias) {
                root = ((DBSAlias) root).getTargetObject(monitor);
            }

            if (root instanceof DBSFolder) {
                collectTables(monitor, ((DBSFolder) root).getChildrenObjects(monitor), tables, settings, false);
            } else if (root instanceof DBSEntity) {
                if ((root instanceof DBSTablePartition && !showPartitions) || (DBUtils.isView((DBSEntity) root) && !(showViews || forceShowViews))) {
                    continue;
                }
                tables.add((DBSEntity) root);
            }
            if (root instanceof DBSObjectContainer) {
                collectTables(monitor, (DBSObjectContainer) root, tables, showViews, showPartitions);
            }
        }
    }

    private static void collectTables(
        DBRProgressMonitor monitor,
        DBSObjectContainer container,
        Set<DBSEntity> tables,
        boolean showViews,
        boolean showPartitions)
        throws DBException
    {
        if (monitor.isCanceled()) {
            return;
        }
        container.cacheStructure(monitor, DBSObjectContainer.STRUCT_ALL);
        final Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            Class<? extends DBSObject> childType = container.getPrimaryChildType(monitor);
            DBSObjectFilter objectFilter = container.getDataSource().getContainer().getObjectFilter(childType, container, true);
            for (DBSObject entity : children) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (objectFilter != null && !objectFilter.matches(entity.getName())) {
                    continue;
                }
                if (entity instanceof DBSEntity) {
                    if ((entity instanceof DBSTablePartition && !showPartitions) || (DBUtils.isView((DBSEntity) entity) && !showViews)) {
                        continue;
                    }

                    tables.add((DBSEntity) entity);
                } else if (entity instanceof DBSObjectContainer) {
                    collectTables(monitor, (DBSObjectContainer) entity, tables, showViews, showPartitions);
                }
            }
        }
    }

    public void generateDiagramObjects(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots,
        DiagramCollectSettings settings)
        throws DBException
    {
        Collection<DBSEntity> tables = collectTables(monitor, roots, settings, showViews);
        for (DBSEntity table : tables) {
            if (DBUtils.isHiddenObject(table)) {
                // Skip hidden tables
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
        if (diagram.containsTable(table) && !diagram.getContentProvider().allowEntityDuplicates()) {
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

    public static List<ERDEntity> generateEntityList(
        DBRProgressMonitor monitor,
        final ERDDiagram diagram,
        Collection<DBPNamedObject> objects,
        DiagramCollectSettings settings,
        boolean forceShowViews)
    {
        final List<DBSObject> roots = new ArrayList<>();
        for (DBPNamedObject object : objects) {
            if (object instanceof DBSObject) {
                roots.add((DBSObject) object);
            }
        }
        if (roots.isEmpty()) {
            return Collections.emptyList();
        }

        final List<ERDEntity> entities = new ArrayList<>();

        monitor.beginTask("Collect diagram objects", 1);
        DiagramObjectCollector collector = new DiagramObjectCollector(diagram);
        collector.setShowViews(forceShowViews);
        //boolean showViews = ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS);

        try {
            DBExecUtils.tryExecuteRecover(monitor, roots.get(0).getDataSource(), monitor1 -> {
                try {
                    collector.generateDiagramObjects(monitor1, roots, settings);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Exception e) {
            log.error(e);
        }
        entities.addAll(collector.getDiagramEntities());
        monitor.done();

        return entities;
    }

}
