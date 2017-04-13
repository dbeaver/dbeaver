/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Embedded ERD editor
 */
public class ERDEditorEmbedded extends ERDEditorPart implements IDatabaseEditor, IActiveWorkbenchPart {

    private Composite parent;

    /**
     * No-arg constructor
     */
    public ERDEditorEmbedded()
    {
    }

    @Override
    public IDatabaseEditorInput getEditorInput()
    {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    public void activatePart()
    {
        if (progressControl == null) {
            super.createPartControl(parent);
            parent.layout();
        }
        if (isLoaded()) {
            return;
        }
        loadDiagram();
    }

    @Override
    public void deactivatePart()
    {
    }

    @Override
    public void createPartControl(Composite parent)
    {
        // Do not create controls here - do it on part activation
        this.parent = parent;
        //super.createPartControl(parent);
    }

    @Override
    public void setFocus()
    {
        if (progressControl != null) {
            super.setFocus();
        }
    }

    private DBSObject getRootObject()
    {
        DBSObject object = getEditorInput().getDatabaseObject();
        if (object == null) {
            return null;
        }
        if (object instanceof DBPDataSourceContainer && object.getDataSource() instanceof DBSObject) {
            object = object.getDataSource();
        }
        return object;
    }

    @Override
    protected synchronized void loadDiagram()
    {
        DBSObject object = getRootObject();
        if (object == null) {
            return;
        }
        if (diagramLoadingJob != null) {
            // Do not start new one while old is running
            return;
        }
        diagramLoadingJob = LoadingJob.createService(
            new DatabaseLoadService<EntityDiagram>("Load diagram '" + object.getName() + "'", object.getDataSource()) {
                @Override
                public EntityDiagram evaluate()
                    throws InvocationTargetException, InterruptedException {
                    try {
                        return loadFromDatabase(getProgressMonitor());
                    } catch (DBException e) {
                        log.error(e);
                    }

                    return null;
                }
            },
            progressControl.createLoadVisualizer());
        diagramLoadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                diagramLoadingJob = null;
            }
        });
        diagramLoadingJob.schedule();
    }

    @Override
    public org.jkiss.dbeaver.model.exec.DBCExecutionContext getExecutionContext()
    {
        return getEditorInput().getExecutionContext();
    }

    private EntityDiagram loadFromDatabase(DBRProgressMonitor monitor)
        throws DBException
    {
        DBSObject dbObject = getRootObject();
        if (dbObject == null) {
            log.error("Database object must be entity container to render ERD diagram");
            return null;
        }
        EntityDiagram diagram;
        if (!dbObject.isPersisted()) {
            diagram = new EntityDiagram(dbObject, "New Object");
        } else {
            diagram = new EntityDiagram(dbObject, dbObject.getName());

            diagram.fillTables(
                monitor,
                collectDatabaseTables(monitor, dbObject),
                dbObject);
        }

        return diagram;
    }

    private Collection<DBSEntity> collectDatabaseTables(DBRProgressMonitor monitor, DBSObject root) throws DBException
    {
        Set<DBSEntity> result = new LinkedHashSet<>();

        // Cache structure
        if (root instanceof DBSObjectContainer) {
            monitor.beginTask("Load '" + root.getName() + "' content", 3);
            DBSObjectContainer objectContainer = (DBSObjectContainer) root;
            try {
                objectContainer.cacheStructure(monitor, DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ASSOCIATIONS | DBSObjectContainer.STRUCT_ATTRIBUTES);
            } catch (DBException e) {
                log.error("Error caching structure", e);
            }
            Collection<? extends DBSObject> entities = objectContainer.getChildren(monitor);
            for (DBSObject entity : CommonUtils.safeCollection(entities)) {
                if (entity instanceof DBSEntity) {
                    final DBSEntity entity1 = (DBSEntity) entity;
                    if (entity1.getEntityType() == DBSEntityType.TABLE ||
                        entity1.getEntityType() == DBSEntityType.CLASS ||
                        entity1.getEntityType() == DBSEntityType.VIRTUAL_ENTITY)
                    {
                        result.add(entity1);
                    }
                }
            }
            monitor.done();

        } else if (root instanceof DBSEntity) {
            monitor.beginTask("Load '" + root.getName() + "' relations", 3);
            DBSEntity rootTable = (DBSEntity) root;
            result.add(rootTable);
            try {
                monitor.subTask("Read foreign keys");
                Collection<? extends DBSEntityAssociation> fks = rootTable.getAssociations(monitor);
                if (fks != null) {
                    for (DBSEntityAssociation fk : fks) {
                        result.add(fk.getAssociatedEntity());
                    }
                }
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Can't load table foreign keys", e);
            }
            if (monitor.isCanceled()) {
                return result;
            }
            try {
                monitor.subTask("Read references");
                Collection<? extends DBSEntityAssociation> refs = rootTable.getReferences(monitor);
                if (refs != null) {
                    for (DBSEntityAssociation ref : refs) {
                        result.add(ref.getParentObject());
                    }
                }
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Can't load table references", e);
            }
            if (monitor.isCanceled()) {
                return result;
            }
            try {
                monitor.subTask("Read associations");
                List<DBSEntity> secondLevelEntities = new ArrayList<>();
                for (DBSEntity entity : result) {
                    if (entity != rootTable && entity.getEntityType() == DBSEntityType.ASSOCIATION) {
                        // Read all association's associations
                        Collection<? extends DBSEntityAssociation> fks = entity.getAssociations(monitor);
                        if (fks != null) {
                            for (DBSEntityAssociation association : fks) {
                                if (association.getConstraintType() != DBSEntityConstraintType.INHERITANCE) {
                                    secondLevelEntities.add(association.getAssociatedEntity());
                                }
                            }
                        }
                    }
                }
                result.addAll(secondLevelEntities);
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Can't load table references", e);
            }

            monitor.done();
        }

        return result;
    }

}
