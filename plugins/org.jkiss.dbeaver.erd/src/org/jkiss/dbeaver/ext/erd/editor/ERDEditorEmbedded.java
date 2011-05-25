/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Embedded ERD editor
 */
public class ERDEditorEmbedded extends ERDEditorPart implements IDatabaseNodeEditor, IActiveWorkbenchPart {

    private Composite parent;

    /**
     * No-arg constructor
     */
    public ERDEditorEmbedded()
    {
    }

    public IDatabaseNodeEditorInput getEditorInput()
    {
        return (IDatabaseNodeEditorInput)super.getEditorInput();
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

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
        if (object instanceof DBSDataSourceContainer && object.getDataSource() instanceof DBSObject) {
            object = (DBSObject) object.getDataSource();
        }
        return object;
    }

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
        diagramLoadingJob = LoadingUtils.createService(
            new DatabaseLoadService<EntityDiagram>("Load diagram '" + object.getName() + "'", object.getDataSource()) {
                public EntityDiagram evaluate()
                    throws InvocationTargetException, InterruptedException
                {
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

    public DBPDataSource getDataSource()
    {
        return getEditorInput().getDataSource();
    }

    public void refreshPart(Object source)
    {
        if (isLoaded()) {
            loadDiagram();
        }
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

    private Collection<DBSTable> collectDatabaseTables(DBRProgressMonitor monitor, DBSObject root) throws DBException
    {
        Set<DBSTable> result = new HashSet<DBSTable>();

        // Cache structure
        if (root instanceof DBSEntityContainer) {
            monitor.beginTask("Load '" + root.getName() + "' content", 3);
            DBSEntityContainer entityContainer = (DBSEntityContainer) root;
            entityContainer.cacheStructure(monitor, DBSEntityContainer.STRUCT_ENTITIES | DBSEntityContainer.STRUCT_ASSOCIATIONS | DBSEntityContainer.STRUCT_ATTRIBUTES);
            Collection<? extends DBSObject> entities = entityContainer.getChildren(monitor);
            for (DBSObject entity : entities) {
                if (entity instanceof DBSTable) {
                    result.add((DBSTable) entity);
                }
            }
            monitor.done();

        } else if (root instanceof DBSTable) {
            monitor.beginTask("Load '" + root.getName() + "' relations", 2);
            DBSTable rootTable = (DBSTable) root;
            result.add(rootTable);
            try {
                monitor.subTask("Read foreign keys");
                Collection<? extends DBSForeignKey> fks = rootTable.getForeignKeys(monitor);
                if (fks != null) {
                    for (DBSForeignKey fk : fks) {
                        result.add(fk.getReferencedKey().getTable());
                    }
                }
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Could not load table foreign keys", e);
            }
            if (monitor.isCanceled()) {
                return result;
            }
            try {
                monitor.subTask("Read references");
                Collection<? extends DBSForeignKey> refs = rootTable.getReferences(monitor);
                if (refs != null) {
                    for (DBSForeignKey ref : refs) {
                        result.add(ref.getTable());
                    }
                }
                monitor.worked(1);
            } catch (DBException e) {
                log.warn("Could not load table references", e);
            }
            monitor.done();
        }

        return result;
    }

}
