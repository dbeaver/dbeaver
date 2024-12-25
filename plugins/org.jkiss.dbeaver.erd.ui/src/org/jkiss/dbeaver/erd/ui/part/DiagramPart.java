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
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDNote;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.command.EntityAddCommand;
import org.jkiss.dbeaver.erd.ui.command.EntityRemoveCommand;
import org.jkiss.dbeaver.erd.ui.editor.ERDThemeSettings;
import org.jkiss.dbeaver.erd.ui.figures.EntityDiagramFigure;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.layout.DelegatingLayoutManager;
import org.jkiss.dbeaver.erd.ui.layout.GraphAnimation;
import org.jkiss.dbeaver.erd.ui.layout.GraphLayoutAuto;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.policy.DiagramContainerEditPolicy;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouter;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouterDescriptor;
import org.jkiss.dbeaver.erd.ui.router.ERDConnectionRouterRegistry;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Edit part for Schema object, and uses a SchemaDiagram figure as
 * the container for all graphical objects
 *
 * @author Serge Rider
 */
public class DiagramPart extends PropertyAwarePart {
    private ERDConnectionRouter router;
    private final CommandStackEventListener stackListener = new CommandStackEventListener() {

        @Override
        public void stackChanged(CommandStackEvent commandStackEvent) {
            if (delegatingLayoutManager.getActiveLayoutManager() instanceof GraphLayoutAuto) {
                if (!GraphAnimation.captureLayout(getFigure())) {
                    return;
                }
                while (GraphAnimation.step())
                    getFigure().getUpdateManager().performUpdate();
                GraphAnimation.end();
            } else {
                getFigure().getUpdateManager().performUpdate();
            }
        }
    };
    private DelegatingLayoutManager delegatingLayoutManager;

    public DiagramPart() {
        //default constructor
    }

    /**
     * Adds this EditPart as a command stack listener, which can be used to call
     * performUpdate() when it changes
     */
    @Override
    public void activate()
    {
        super.activate();
        getViewer().getEditDomain().getCommandStack().addCommandStackEventListener(stackListener);
    }

    @Override
    protected boolean isListensModelChanges() {
        return true;
    }

    /**
     * Removes this EditPart as a command stack listener
     */
    @Override
    public void deactivate() {
        getViewer().getEditDomain().getCommandStack().removeCommandStackEventListener(stackListener);
        super.deactivate();
    }

    @Override
    public void performRequest(Request request) {
        getDiagram().getModelAdapter().performPartRequest(this, request);
    }

    @Override
    protected IFigure createFigure() {
        EntityDiagramFigure figure = new EntityDiagramFigure(this);
        delegatingLayoutManager = new DelegatingLayoutManager(this);
        figure.setLayoutManager(delegatingLayoutManager);
        Control control = getViewer().getControl();
        ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        if ((control.getStyle() & SWT.MIRRORED) == 0) {
            cLayer.setAntialias(SWT.ON);
        }
        ERDConnectionRouterDescriptor routerDescriptor = getEditor().getDiagramRouter();
        if (routerDescriptor == null) {
            routerDescriptor = ERDConnectionRouterRegistry.getInstance().getActiveRouter();
        }
        router = routerDescriptor.createRouter();
        router.setContainer(figure);
        cLayer.setConnectionRouter(router);
        return figure;
    }

    @Override
    @NotNull
    public EntityDiagram getDiagram()
    {
        return (EntityDiagram) getModel();
    }

    public Font getNormalFont() {
        return ERDThemeSettings.instance.diagramFont;
    }

    public Font getBoldFont() {
        return ERDThemeSettings.instance.diagramFontBold;
    }

    /**
     * The method designed for diagram re-arrangement, reset alignment elements
     * to original
     */
    public void resetArrangement() {
        if (getEditor() == null) {
            return;
        }
        RearrangeDiagramService diagramService = new RearrangeDiagramService(this);
        LoadingJob.createService(
            diagramService,
            getEditor()
                .getProgressControl()
                .createLoadVisualizer())
            .schedule();
    }

    void rearrangeDiagram(@NotNull DBRProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }
        monitor.beginTask(ERDUIMessages.erd_job_rearrange_diagram, getChildren().size() + 2);
        getChildren().forEach(c -> {
            if (c instanceof NodePart nodePart) {
                UIUtils.syncExec(() -> resetConnectionConstraints(monitor, nodePart.getSourceConnections()));
                monitor.worked(1);
            }
        });
        monitor.subTask(ERDUIMessages.erd_job_reset_element_position);
        delegatingLayoutManager.rearrange(monitor, getFigure());
        if (monitor.isCanceled()) {
            return;
        } else {
            monitor.worked(1);
        }
        monitor.subTask(ERDUIMessages.erd_job_repaint_diagram);
        UIUtils.syncExec(() -> getFigure().repaint());
        monitor.worked(1);
    }

    private void resetConnectionConstraints(DBRProgressMonitor monitor, List<?> sourceConnections) {
        if (monitor.isCanceled()) {
            return;
        }
        if (!CommonUtils.isEmpty(sourceConnections)) {
            for (Object sc : sourceConnections) {
                if (sc instanceof AbstractConnectionEditPart abstractPart) {
                    abstractPart.getConnectionFigure().setRoutingConstraint(null);
                    if (sc instanceof AssociationPart associationPart) {
                        associationPart.getAssociation().setInitBends(null);
                        associationPart.setConnectionRouting(monitor, (PolylineConnection) abstractPart.getConnectionFigure());
                    }
                }
            }
        }
    }

    /**
     * @return the children Model objects as a new ArrayList
     */
    @Override
    protected List<?> getModelChildren()
    {
        return getDiagram().getContents();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#isSelectable()
     */
    @Override
    public boolean isSelectable()
    {
        return false;
    }

    /**
     * Creates EditPolicy objects for the EditPart. The LAYOUT_ROLE policy is
     * left to the delegating layout manager
     */
    @Override
    protected void createEditPolicies()
    {
        if (!getEditor().isReadOnly()) {
            installEditPolicy(EditPolicy.CONTAINER_ROLE, new DiagramContainerEditPolicy());
            installEditPolicy(EditPolicy.LAYOUT_ROLE, null);
            getDiagram().getModelAdapter().installPartEditPolicies(this);
        }
    }


    /**
     * Updates the table bounds in the model so that the same bounds can be
     * restored after saving
     */
    public void setTableModelBounds()
    {

        List<?> entityParts = getChildren();

        for (Object child : entityParts) {
            if (child instanceof NodePart entityPart) {
                IFigure entityFigure = entityPart.getFigure();

                //if we don't find a node for one of the children then we should
                // continue
                if (entityFigure == null) {
                    continue;
                }

                Rectangle bounds = entityFigure.getBounds().getCopy();
                entityPart.setBounds(bounds);
            }
        }

    }

    /**
     * Updates the bounds of the table figure (without invoking any event
     * handling), and sets layout constraint data
     *
     * @return whether the procedure execute successfully without any omissions.
     *         The latter occurs if any Table objects have no bounds set or if
     *         no figure is available for the EntityPart
     */
    public boolean setTableFigureBounds(boolean updateConstraint)
    {
        List<?> nodeParts = getChildren();

        for (Object child : nodeParts) {
            if (child instanceof NodePart entityPart) {
                //now check whether we can find an entry in the tableToNodesMap
                Rectangle bounds = entityPart.getBounds();
                if (bounds == null) {
                    //TODO handle this better
                    return false;
                } else {
                    IFigure entityFigure = entityPart.getFigure();
                    if (entityFigure == null) {
                        return false;
                    } else {
                        if (updateConstraint) {
                            //pass the constraint information to the xy layout
                            //setting the width and height so that the preferred size will be applied
                            delegatingLayoutManager.setXYLayoutConstraint(entityFigure, new Rectangle(bounds.x, bounds.y, -1, -1));
                        }
                    }
                }
            }

        }
        return true;

    }

    public void changeLayout()
    {
        //Boolean layoutType = (Boolean) evt.getNewValue();
        //boolean isManualLayoutDesired = layoutType.booleanValue();
        getFigure().setLayoutManager(delegatingLayoutManager);
    }

    /**
     * Sets layout constraint only if XYLayout is active
     */
    @Override
    public void setLayoutConstraint(EditPart child, IFigure childFigure, Object constraint)
    {
        super.setLayoutConstraint(child, childFigure, constraint);
    }

    /**
     * Passes on to the delegating layout manager that the layout type has
     * changed. The delegating layout manager will then decide whether to
     * delegate layout to the XY or Graph layout
     */
    @Override
    protected void handleChildChange(PropertyChangeEvent evt)
    {
        super.handleChildChange(evt);
    }

    @Override
    public Object getAdapter(Class key)
    {
        if (key == SnapToHelper.class) {
            final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
            if (store.getBoolean(ERDUIConstants.PREF_GRID_ENABLED) && store.getBoolean(ERDUIConstants.PREF_GRID_SNAP_ENABLED)) {
                return new SnapToGrid(this);
            } else {
                return null;
            }
        }
        return super.getAdapter(key);
    }

    @Nullable
    public NodePart getChildByObject(Object object) {
        for (Object child : getChildren()) {
            if (child instanceof NodePart && ((NodePart) child).getElement().getObject() == object) {
                return (NodePart) child;
            }
        }
        return null;
    }

    @Nullable
    public EntityPart getEntityPart(ERDEntity erdEntity)
    {
        for (Object child : getChildren()) {
            if (child instanceof EntityPart && ((EntityPart) child).getEntity() == erdEntity) {
                return (EntityPart) child;
            }
        }
        return null;
    }

    public List<EntityPart> getEntityParts() {
        List<EntityPart> result = new ArrayList<>();
        for (Object child : getChildren()) {
            if (child instanceof EntityPart) {
                result.add((EntityPart)child);
            }
        }
        return result;
    }

    @Nullable
    public NotePart getNotePart(ERDNote erdNote)
    {
        for (Object child : getChildren()) {
            if (child instanceof NotePart && ((NotePart) child).getNote() == erdNote) {
                return (NotePart) child;
            }
        }
        return null;
    }

    @NotNull
    public Command createEntityAddCommand(List<ERDEntity> entities, Point location) {
        return new EntityAddCommand(this, entities, location);
    }

    public Command createEntityDeleteCommand(EntityPart entityPart) {
        return new EntityRemoveCommand(entityPart);
    }

    @Override
    public String toString()
    {
        return ERDUIMessages.entity_diagram_ + " " + getDiagram().getName();
    }

    /**
     * Gets the diagram router
     *
     * @return - router
     */
    public ERDConnectionRouter getActiveRouter() {
        return router;
    }
}