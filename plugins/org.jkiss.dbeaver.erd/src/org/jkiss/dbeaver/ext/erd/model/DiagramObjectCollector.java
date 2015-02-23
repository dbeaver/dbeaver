/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Table collector
 */
public class DiagramObjectCollector {

    static final Log log = Log.getLog(DiagramObjectCollector.class);

    private final EntityDiagram diagram;
    private final List<ERDEntity> erdEntities = new ArrayList<ERDEntity>();
    private final Map<DBSEntity, ERDEntity> tableMap = new HashMap<DBSEntity, ERDEntity>();

    public DiagramObjectCollector(EntityDiagram diagram)
    {
        this.diagram = diagram;
        this.tableMap.putAll(diagram.getTableMap());
    }

    public static Collection<DBSEntity> collectTables(
        DBRProgressMonitor monitor,
        Collection<? extends DBSObject> roots)
        throws DBException
    {
        Set<DBSEntity> tables = new LinkedHashSet<DBSEntity>();
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
            for (DBSObject entity : children) {
                if (monitor.isCanceled()) {
                    break;
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
        Collection<DBSEntity> tables = collectTables(monitor, roots);
        for (DBSEntity table : tables) {
            if (table instanceof DBPHiddenObject && ((DBPHiddenObject) table).isHidden()) {
                // Skip hidden tables
                continue;
            }
            addDiagramEntity(monitor, table);
        }

        // Add new relations
        for (ERDEntity erdEntity : erdEntities) {
            erdEntity.addRelations(monitor, tableMap, false);
        }
    }

    private void addDiagramEntity(DBRProgressMonitor monitor, DBSEntity table)
    {
        if (diagram.containsTable(table)) {
            // Avoid duplicates
            return;
        }
        ERDEntity erdEntity = ERDEntity.fromObject(monitor, diagram, table);
        if (erdEntity != null) {
            erdEntities.add(erdEntity);
            tableMap.put(table, erdEntity);
        }
    }

    public List<ERDEntity> getDiagramEntities()
    {
        return erdEntities;
    }

    public static List<ERDEntity> generateEntityList(final EntityDiagram diagram, Collection<DBPNamedObject> objects)
    {
        final List<DBSObject> roots = new ArrayList<DBSObject>();
        for (DBPNamedObject object : objects) {
            if (object instanceof DBSObject) {
                roots.add((DBSObject) object);
            }
        }

        final List<ERDEntity> entities = new ArrayList<ERDEntity>();

        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    DiagramObjectCollector collector = new DiagramObjectCollector(diagram);
                    try {
                        collector.generateDiagramObjects(monitor, roots);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                    entities.addAll(collector.getDiagramEntities());
                }
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // interrupted
        }
        return entities;
    }

}
