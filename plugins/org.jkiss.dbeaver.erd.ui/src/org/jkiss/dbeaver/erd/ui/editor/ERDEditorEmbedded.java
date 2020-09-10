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
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.action.DiagramTogglePersistAction;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.model.DiagramLoader;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.virtual.DBVObject;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
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
import java.util.LinkedHashMap;
import java.util.Map;

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
            diagram = new EntityDiagram(dbObject, "New Object", getContentProvider(), getDecorator());
        } else {
            diagram = new EntityDiagram(dbObject, dbObject.getName(), getContentProvider(), getDecorator());

            // Fill from database even if we loaded from state (something could change since last view)
            diagram.fillEntities(
                monitor,
                ERDUtils.collectDatabaseTables(
                    monitor,
                    dbObject,
                    diagram,
                    ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS),
                    ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS)),
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
