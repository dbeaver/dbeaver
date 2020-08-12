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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.action.DiagramTogglePersistAction;
import org.jkiss.dbeaver.ext.erd.model.DiagramLoader;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.virtual.DBVObject;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.IEntityStructureEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Embedded ERD editor
 */
public class ERDEditorEmbedded extends ERDEditorPart implements IDatabaseEditor, IEntityStructureEditor, IActiveWorkbenchPart {

    private static final Log log = Log.getLog(ERDEditorEmbedded.class);

    private static final String PROP_DIAGRAM_STATE = "erd.diagram.state";
    private static final String PROPS_DIAGRAM_SERIALIZED = "serialized";

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
    public void recreateEditorControl() {
        // Not implemented
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    protected void fillDefaultEditorContributions(IContributionManager toolBarManager) {
        super.fillDefaultEditorContributions(toolBarManager);

        toolBarManager.add(ActionUtils.makeActionContribution(new DiagramTogglePersistAction(this), true));
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
        loadDiagram(false);
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
        //super.createEditorControl(parent);
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
        if (object instanceof DBPDataSourceContainer && object.getDataSource() != null) {
            object = object.getDataSource();
        }
        return object;
    }

    @Override
    protected synchronized void loadDiagram(final boolean refreshMetadata)
    {
        final DBSObject object = getRootObject();
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
                public EntityDiagram evaluate(DBRProgressMonitor monitor) {
                    try {
                        return loadFromDatabase(monitor);
                    } catch (DBException e) {
                        log.error("Error loading ER diagram", e);
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
    public DBCExecutionContext getExecutionContext()
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
            diagram = new EntityDiagram(getDecorator(), dbObject, "New Object");
        } else {
            diagram = new EntityDiagram(getDecorator(), dbObject, dbObject.getName());

            // Fill from database even if we loaded from state (something could change since last view)
            diagram.fillEntities(
                monitor,
                collectDatabaseTables(monitor, dbObject, diagram),
                dbObject);

            boolean hasPersistedState = false;
            try {
                // Load persisted state
                DBVObject vObject = this.getVirtualObject();
                if (vObject != null) {
                    Map<String, Object> diagramState = vObject.getProperty(PROP_DIAGRAM_STATE);
                    if (diagramState != null) {
                        String serializedDiagram = (String) diagramState.get(PROPS_DIAGRAM_SERIALIZED);
                        if (!CommonUtils.isEmpty(serializedDiagram)) {
                            Document xmlDocument = XMLUtils.parseDocument(new StringReader(serializedDiagram));
                            DiagramLoader.loadDiagram(monitor, xmlDocument, dbObject.getDataSource().getContainer().getProject(), diagram);
                            hasPersistedState = true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error loading ER diagram from saved state", e);
            }
            diagram.setLayoutManualAllowed(true);
            diagram.setNeedsAutoLayout(!hasPersistedState);
        }

        return diagram;
    }

    private Collection<DBSEntity> collectDatabaseTables(DBRProgressMonitor monitor, DBSObject root, EntityDiagram diagram) throws DBException
    {
        Set<DBSEntity> result = new LinkedHashSet<>();

        // Cache structure
        if (root instanceof DBSObjectContainer) {
            monitor.beginTask("Load '" + root.getName() + "' content", 3);
            DBSObjectContainer objectContainer = (DBSObjectContainer) root;
            try {
                DBExecUtils.tryExecuteRecover(monitor, objectContainer.getDataSource(), param -> {
                    try {
                        objectContainer.cacheStructure(monitor, DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ASSOCIATIONS | DBSObjectContainer.STRUCT_ATTRIBUTES);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Cache database model", "Error caching database model", e);
            }
            boolean showViews = ERDActivator.getDefault().getPreferenceStore().getBoolean(ERDConstants.PREF_DIAGRAM_SHOW_VIEWS);
            Collection<? extends DBSObject> entities = objectContainer.getChildren(monitor);
            if (entities != null) {
                Class<? extends DBSObject> childType = objectContainer.getPrimaryChildType(monitor);
                DBSObjectFilter objectFilter = objectContainer.getDataSource().getContainer().getObjectFilter(childType, objectContainer, true);

                for (DBSObject entity : entities) {
                    if (entity instanceof DBSEntity) {
                        if (objectFilter != null && objectFilter.isEnabled() && !objectFilter.matches(entity.getName())) {
                            continue;
                        }

                        final DBSEntity entity1 = (DBSEntity) entity;

                        if (entity1.getEntityType() == DBSEntityType.TABLE ||
                            entity1.getEntityType() == DBSEntityType.CLASS ||
                            entity1.getEntityType() == DBSEntityType.VIRTUAL_ENTITY ||
                            (showViews && DBUtils.isView(entity1))
                            )

                        {
                            result.add(entity1);
                        }
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
                Collection<? extends DBSEntityAssociation> fks = DBVUtils.getAllAssociations(monitor, rootTable);
                if (fks != null) {
                    for (DBSEntityAssociation fk : fks) {
                        DBSEntity associatedEntity = fk.getAssociatedEntity();
                        if (associatedEntity != null) {
                            result.add(DBVUtils.getRealEntity(monitor, associatedEntity));
                        }
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
                Collection<? extends DBSEntityAssociation> refs = DBVUtils.getAllReferences(monitor, rootTable);
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

        // Remove entities already loaded in the diagram
        for (ERDEntity diagramEntity : diagram.getEntities()) {
            result.remove(diagramEntity.getObject());
        }

        if (!ERDActivator.getDefault().getPreferenceStore().getBoolean(ERDConstants.PREF_DIAGRAM_SHOW_PARTITIONS)) {
            result.removeIf(entity -> entity instanceof DBSTablePartition);
        }

        return result;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        try {
            // Save in virtual model as entity property.
            DBVObject vObject = this.getVirtualObject();
            if (vObject == null) {
                return;
            }
            Map<String, Object> diagramStateMap = new LinkedHashMap<>();
            vObject.setProperty(PROP_DIAGRAM_STATE, diagramStateMap);

            String diagramState = DiagramLoader.serializeDiagram(RuntimeUtils.makeMonitor(monitor), getDiagramPart(), getDiagram(), false, true);
            diagramStateMap.put(PROPS_DIAGRAM_SERIALIZED, diagramState);

            vObject.persistConfiguration();

            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            log.error("Error saving diagram", e);
        }
        updateToolbarActions();
    }

    public boolean isStateSaved() {
        DBVObject vObject = this.getVirtualObject();
        return (vObject != null && vObject.getProperty(PROP_DIAGRAM_STATE) != null);
    }

    public void resetSavedState(boolean refreshDiagram) {
        try {
            DBVObject vObject = this.getVirtualObject();
            if (vObject != null && vObject.getProperty(PROP_DIAGRAM_STATE) != null) {
                vObject.setProperty(PROP_DIAGRAM_STATE, null);
                vObject.persistConfiguration();
            }
        } catch (Exception e) {
            log.error("Error resetting diagram state", e);
        }
        if (refreshDiagram) {
            refreshDiagram(true, true);
        }
    }

    @Nullable
    private DBVObject getVirtualObject() {
        DBSObject rootObject = getRootObject();
        if (rootObject instanceof DBSEntity) {
            return DBVUtils.getVirtualEntity((DBSEntity) rootObject, true);
        } else {
            return DBVUtils.getVirtualObject(rootObject, true);
        }
    }

}
