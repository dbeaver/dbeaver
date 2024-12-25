/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * GraphicalViewer which also knows about ValidationMessageHandler to output
 * error messages to
 * @author Serge Rider
 */
public class ERDGraphicalViewer extends ScrollingGraphicalViewer implements DBPEventListener {
    private static final Log log = Log.getLog(ERDGraphicalViewer.class);

    private final ERDEditorPart editor;
	private final ValidationMessageHandler messageHandler;
    private boolean loadContents = false;

    private static class DataSourceInfo {
        int tableCount = 0;
    }

    private final Map<DBPDataSourceContainer, DataSourceInfo> usedDataSources = new IdentityHashMap<>();

	/**
	 * ValidationMessageHandler to receive messages
	 * @param messageHandler message handler 
	 */
	public ERDGraphicalViewer(ERDEditorPart editor, ValidationMessageHandler messageHandler)
	{
		super();
        this.editor = editor;
		this.messageHandler = messageHandler;

        setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
        setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD2), MouseWheelHorizontalScrollHandler.SINGLETON);
    }

    public ERDEditorPart getEditor()
    {
        return editor;
    }

    @Override
    public void setControl(Control control)
    {
        super.setControl(control);

        if (control != null) {
            ERDEditorAdapter.mapControl(control, editor);
            UIUtils.addFocusTracker(editor.getSite(), ERDUIConstants.ERD_CONTROL_ID, control);

            ERDThemeSettings.instance.addPropertyListener(
                ERDUIConstants.PROP_DIAGRAM_FONT,
                s -> applyThemeSettings(),
                control
            );

            applyThemeSettings();
        }
    }

    @Override
    protected void handleDispose(DisposeEvent e) {
        if (getControl() != null) {
            ERDEditorAdapter.unmapControl(getControl());
        }
        super.handleDispose(e);
    }

    /**
	 * @return Returns the messageLabel.
	 */
	public ValidationMessageHandler getValidationHandler()
	{
		return messageHandler;
	}

	/**
	 * This method is invoked when this viewer's control loses focus. It removes
	 * focus from the {@link AbstractEditPartViewer#focusPart focusPart}, if
	 * there is one.
	 * 
	 * @param fe
	 *            the focusEvent received by this viewer's control
	 */
	@Override
    protected void handleFocusLost(FocusEvent fe)
	{
		//give the superclass a chance to handle this first
		super.handleFocusLost(fe);
		//call reset on the MessageHandler itself
		messageHandler.reset();
	}

    private void applyThemeSettings()
    {
        this.getControl().setFont(ERDThemeSettings.instance.diagramFont);
        editor.refreshDiagram(true, false);
/*
        DiagramPart diagramPart = editor.getDiagramPart();
        if (diagramPart != null) {
            diagramPart.resetFonts();
            diagramPart.refresh();
        }
*/
    }

    @Override
    public void setContents(EditPart editpart)
    {
        loadContents = true;
        try {
            super.setContents(editpart);
            // Reset palette contents
            if (editpart instanceof DiagramPart diagramPart) {
                List<DBSEntity> tables = new ArrayList<>();
                for (Object child : editpart.getChildren()) {
                    if (child instanceof EntityPart childPart) {
                        tables.add(childPart.getEntity().getObject());
                    }
                }
                DBRProgressMonitor monitor = editor.getDiagram().getMonitor();
                monitor.beginTask(ERDUIMessages.erd_job_rearrange_diagram, tables.size() + 2);
                tables.sort(DBUtils.nameComparator());
                Map<PaletteDrawer, List<ToolEntryTable>> toolMap = new LinkedHashMap<>();
                for (DBSEntity table : tables) {
                    DBPDataSourceContainer container = table.getDataSource().getContainer();
                    PaletteDrawer drawer = getContainerPaletteDrawer(container);
                    if (drawer != null) {
                        List<ToolEntryTable> tools = toolMap.get(drawer);
                        if (tools == null) {
                            tools = new ArrayList<>(tables.size());
                            toolMap.put(drawer, tools);
                        }
                        tools.add(new ToolEntryTable(table));
                    }
                    monitor.worked(1);
                }
                monitor.subTask(ERDUIMessages.erd_job_set_diagram_palette);
                for (Map.Entry<PaletteDrawer, List<ToolEntryTable>> entry : toolMap.entrySet()) {
                    List<PaletteEntry> paletteEntries = new ArrayList<>(entry.getValue());
                    entry.getKey().setChildren(paletteEntries);
                }
                monitor.worked(1);
                monitor.subTask(ERDUIMessages.erd_job_visuallize_content);
                diagramPart.getDiagram().getModelAdapter().handleContentChange(editor);
                monitor.worked(1);
            }
        }
        finally {
            loadContents = false;
        }
    }

    public void handleTableActivate(DBSEntity table)
    {
        if (table.getDataSource() != null) {
            DBPDataSourceContainer container = table.getDataSource().getContainer();
            if (container != null) {
                synchronized (usedDataSources) {
                    DataSourceInfo dataSourceInfo = usedDataSources.get(container);
                    if (dataSourceInfo == null) {
                        dataSourceInfo = new DataSourceInfo();
                        usedDataSources.put(container, dataSourceInfo);
                        acquireContainer(container);
                    }
                    dataSourceInfo.tableCount++;
                }
            }
        }

        if (!loadContents) {
            final PaletteContainer drawer = getContainerPaletteDrawer(table.getDataSource().getContainer());
            if (drawer != null) {
                // Add entry (with right order)
                List children = drawer.getChildren();
                int index = 0;
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    Object child = children.get(i);
                    if (child instanceof ToolEntryTable) {
                        if (((ToolEntryTable) child).table.getName().compareTo(table.getName()) > 0) {
                            index = i;
                            break;
                        }
                    }
                }
                drawer.add(index, new ToolEntryTable(table));
            }
        }
    }

    public void handleTableDeactivate(DBSEntity table)
    {
        final PaletteContainer drawer = getContainerPaletteDrawer(table.getDataSource().getContainer());
        if (drawer != null) {
            for (Object entry : drawer.getChildren()) {
                if (entry instanceof ToolEntryTable && ((ToolEntryTable)entry).table == table) {
                    drawer.remove((ToolEntryTable)entry);
                    break;
                }
            }
        }
        if (table.getDataSource() != null) {
            DBPDataSourceContainer container = table.getDataSource().getContainer();
            if (container != null) {
                synchronized (usedDataSources) {
                    DataSourceInfo dataSourceInfo = usedDataSources.get(container);
                    if (dataSourceInfo == null) {
                        log.warn("Datasource '" + container + "' not registered in ERD viewer");
                    } else {
                        dataSourceInfo.tableCount--;
                        if (dataSourceInfo.tableCount <= 0) {
                            usedDataSources.remove(container);
                            releaseContainer(container);
                        }
                    }
                }
            }
        }
    }

    private void acquireContainer(DBPDataSourceContainer container)
    {
        container.acquire(editor);
        container.getRegistry().addDataSourceListener(this);

        PaletteRoot paletteRoot = editor.getPaletteRoot();

        PaletteDrawer dsDrawer = new PaletteDrawer(
            container.getName(),
            DBeaverIcons.getImageDescriptor(container.getDriver().getIcon()));
        dsDrawer.setDescription(container.getDescription());
        dsDrawer.setId(container.getId());

        paletteRoot.add(dsDrawer);
    }

    private void releaseContainer(DBPDataSourceContainer container)
    {
        PaletteDrawer drawer = getContainerPaletteDrawer(container);
        if (drawer != null) {
            editor.getPaletteRoot().remove(drawer);
        }

        container.getRegistry().removeDataSourceListener(this);
        container.release(editor);
    }

    PaletteDrawer getContainerPaletteDrawer(DBPDataSourceContainer container)
    {
        for (Object child : editor.getPaletteRoot().getChildren()) {
            if (child instanceof PaletteDrawer && container.getId().equals(((PaletteDrawer) child).getId())) {
                return (PaletteDrawer) child;
            }
        }
        return null;
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        DBSObject object = event.getObject();
        if (object == null || DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }
        if (object instanceof DBPDataSourceContainer) {
            handleDataSourceContainerChange(event, (DBPDataSourceContainer) object);
            return;
        }

        // Object change
        DBPEvent.Action action = event.getAction();
        if (action == DBPEvent.Action.OBJECT_SELECT || !usedDataSources.containsKey(object.getDataSource().getContainer())) {
            return;
        }
        DBSEntity entity;
        DBSEntityAttribute entityAttribute = null;
        DBSEntityAssociation entityAssociation = null;
        if (object instanceof DBSEntityAttribute) {
            entityAttribute = (DBSEntityAttribute) object;
            entity = entityAttribute.getParentObject();
        } else if (object instanceof DBSEntity) {
            entity = (DBSEntity) object;
        } else if (object instanceof DBSEntityAssociation) {
            entityAssociation = (DBSEntityAssociation) object;
            entity = entityAssociation.getParentObject();
        } else {
            return;
        }

        EntityDiagram diagram = editor.getDiagram();

        /*if (entityAssociation != null) {
            // For association just refresh both entities
            ERDEntity erdEntity1 = diagram.getEntity(entityAssociation.getParentObject());
            ERDEntity erdEntity2 = diagram.getEntity(entityAssociation.getAssociatedEntity());
            if (erdEntity1 != null || erdEntity2 != null) {
                UIUtils.asyncExec(() -> {
                    if (erdEntity1 != null) {
                        erdEntity1.firePropertyChange(ERDEntity.PROP_INPUT, null, null);
                    }
                    if (erdEntity2 != null) {
                        erdEntity2.firePropertyChange(ERDEntity.PROP_OUTPUT, null, null);
                    }
                });
            }
            return;
        }*/

        switch (action) {
            case OBJECT_ADD: {
                if (entityAttribute != null) {
                    // New attribute or association
                    ERDEntity erdEntity = diagram.getEntity(entity);
                    if (erdEntity != null) {
                        UIUtils.asyncExec(() -> {
                            erdEntity.reloadAttributes(diagram);
                            erdEntity.firePropertyChange(ERDEntity.PROP_CONTENTS, null, null);
                        });
                    }
                } else if (entityAssociation != null) {
                    ERDEntity erdEntity = diagram.getEntity(entity);
                    ERDEntity targetEntity = diagram.getEntity(entityAssociation.getAssociatedEntity());
                    DBSEntityAssociation addedAssociation = entityAssociation;
                    UIUtils.asyncExec(() -> {
                        if (erdEntity != null &&
                            erdEntity.getAssociation(addedAssociation) == null &&
                            erdEntity.getReferenceAssociation(addedAssociation) == null &&
                            erdEntity != null &&
                            targetEntity != null) {
                            ERDAssociation erdAssociation = new ERDAssociation(addedAssociation, erdEntity, targetEntity, true);
                            erdAssociation.resolveAttributes();
                        }
                    });
                } else {
                    // New entity. Add it if it has the same object container
                    // or if this entity was created from the same editor
                    DBSObject diagramContainer = diagram.getObject();
                    IEditorPart entityOwnerEditor = (IEditorPart) event.getOptions().get(DBEObjectManager.OPTION_ACTIVE_EDITOR);
                    IEditorPart erdOwnerEditor = getEditor();
                    if (erdOwnerEditor.getSite() instanceof MultiPageEditorSite) {
                        erdOwnerEditor = ((MultiPageEditorSite) erdOwnerEditor.getSite()).getMultiPageEditor();
                    }
                    boolean ownsObject =
                        getEditor().getDiagram().isEditEnabled() &&
                        entityOwnerEditor != null &&
                        entityOwnerEditor == erdOwnerEditor;
                    if (ownsObject || diagramContainer == entity.getParentObject()) {

                        ERDEntity erdEntity = ERDUtils.makeEntityFromObject(
                            new VoidProgressMonitor(),
                            diagram,
                            Collections.emptyList(),
                            entity,
                            null);
                        UIUtils.asyncExec(() -> {
                            diagram.addEntity(erdEntity, true);

                            // Set proper entity location
                            EntityPart entityPart = getEditor().getDiagramPart().getEntityPart(erdEntity);
                            if (entityPart != null) {
                                // Layout entity on diagram

                                // Get cursor location on diagram
                                Display display = Display.getCurrent();
                                Point loc = display.getCursorLocation();
                                Point diagramLoc = getControl().toDisplay(0, 0);
                                loc.x -= diagramLoc.x;
                                loc.y -= diagramLoc.y;

                                Dimension size = entityPart.getFigure().getPreferredSize();
                                Rectangle curBounds = new Rectangle();
                                curBounds.width = size.width;
                                curBounds.height = size.height;
                                curBounds.x = loc.x;
                                curBounds.y = loc.y;
                                entityPart.modifyBounds(curBounds);
                                getEditor().setDirty(true);
                                //autoLayoutEntity(entityPart);
                            }
                        });
                    }
                }
                break;
            }
            case OBJECT_REMOVE: {
                ERDEntity erdEntity = diagram.getEntity(entity);
                if (erdEntity != null) {
                    DBSEntityAttribute removedAttribute = entityAttribute;
                    DBSEntityAssociation removedAssociation = entityAssociation;
                    UIUtils.asyncExec(() -> {
                        if (removedAttribute != null) {
                            ERDEntityAttribute erdAttribute = erdEntity.getAttribute(removedAttribute);
                            if (erdAttribute != null) {
                                erdEntity.removeAttribute(erdAttribute, false);
                                erdEntity.firePropertyChange(ERDEntity.PROP_CONTENTS, null, null);
                            }
                        } else if (removedAssociation != null) {
                            ERDAssociation erdAssociation = erdEntity.getAssociation(removedAssociation);
                            if (erdAssociation != null) {
                                erdEntity.removeAssociation(erdAssociation, true);

                                if (erdAssociation.getTargetEntity() instanceof ERDEntity refEntity) {
                                    if (refEntity != null) {
                                        refEntity.removeReferenceAssociation(erdAssociation, true);
                                    }
                                }
                            }

                        } else {
                            // Entity delete
                            diagram.removeEntity(erdEntity, true);
                        }
                    });
                }
                break;
            }
            case OBJECT_UPDATE: {
                ERDEntity erdEntity = diagram.getEntity(entity);
                if (erdEntity != null) {
                    DBSEntityAttribute updatedAttribute = entityAttribute;
                    DBSEntityAssociation updatedAssociation = entityAssociation;

                    UIUtils.asyncExec(() -> {
                        if (updatedAttribute != null) {
                            ERDEntityAttribute erdAttribute = erdEntity.getAttribute(updatedAttribute);
                            if (erdAttribute != null) {
                                erdAttribute.firePropertyChange(ERDEntityAttribute.PROP_NAME, null, updatedAttribute.getName());
                                // Resize entity
                                erdEntity.firePropertyChange(ERDObject.PROP_SIZE, null, null);
                            }
                        } else if (updatedAssociation != null) {

                        } else {
                            erdEntity.reloadAttributes(diagram);
                            erdEntity.firePropertyChange(ERDEntity.PROP_CONTENTS, null, null);
                        }
                    });
                }
                break;
            }
        }
    }

    // TODO: implement
    private void autoLayoutEntity(EntityPart entityPart) {
        Rectangle figureBounds = entityPart.getFigure().getBounds();

        DBPPreferenceStore prefStore = ERDUIActivator.getDefault().getPreferences();
        int hMargin = prefStore.getInt(ERDUIConstants.PREF_GRID_WIDTH);
        int vMargin = prefStore.getInt(ERDUIConstants.PREF_GRID_HEIGHT);

        //getEditor().getDiagramPart().getE
    }

    private void handleDataSourceContainerChange(DBPEvent event, DBPDataSourceContainer container) {
        if (usedDataSources.containsKey(container) &&
            event.getAction() == DBPEvent.Action.OBJECT_UPDATE &&
            Boolean.FALSE.equals(event.getEnabled())) {
            // Close editor only if it is simple disconnect
            // Workbench shutdown doesn't close editor
            UIUtils.asyncExec(() -> {
                IWorkbenchPartSite site = editor.getSite();
                if (site != null && site.getWorkbenchWindow() != null) {
                    site.getWorkbenchWindow().getActivePage().closeEditor(editor, false);
                }
            });
        }
    }

    private static class ToolEntryTable extends ToolEntry {
        private final DBSEntity table;

        ToolEntryTable(DBSEntity table)
        {
            super(table.getName(), table.getDescription(),
                DBeaverIcons.getImageDescriptor(DBIcon.TREE_TABLE),
                DBeaverIcons.getImageDescriptor(DBIcon.TREE_TABLE));
            this.setUserModificationPermission(PERMISSION_NO_MODIFICATION);
            setDescription(DBUtils.getObjectFullName(table, DBPEvaluationContext.UI));
            this.table = table;
        }

        @Override
        public Tool createTool()
        {
            return new ToolSelectTable(table);
        }
    }

    public static class ToolSelectTable extends SelectionTool {

        private final DBSEntity table;

        ToolSelectTable(DBSEntity table)
        {
            this.table = table;
        }

        @Override
        public void activate()
        {
            //ERDGraphicalViewer.this.reveal(part);
            DefaultEditDomain editDomain = (DefaultEditDomain) getDomain();
            final ERDEditorPart editorPart = (ERDEditorPart)editDomain.getEditorPart();
            final GraphicalViewer viewer = editorPart.getViewer();
            for (EditPart child : editorPart.getDiagramPart().getChildren()) {
                if (child instanceof EntityPart) {
                    if (((EntityPart)child).getEntity().getObject() == table) {
                        viewer.reveal(child);
                        viewer.select(child);
                        break;
                    }
                }
            }
            super.activate();
        }
    }

    /**
     * Handler that provides horizontal scrolling using mouse wheel.
     */
    private static class MouseWheelHorizontalScrollHandler implements MouseWheelHandler {
        public static final MouseWheelHandler SINGLETON = new MouseWheelHorizontalScrollHandler();

        @Override
        public void handleMouseWheel(Event event, EditPartViewer viewer) {
            if (viewer instanceof ScrollingGraphicalViewer) {
                final Control control = viewer.getControl();
                if (control instanceof FigureCanvas canvas) {
                    final ScrollBar hBar = canvas.getHorizontalBar();

                    canvas.scrollToX(hBar.getSelection() - (hBar.getIncrement() * event.count));
                    event.doit = false;
                }
            }
        }
    }
}