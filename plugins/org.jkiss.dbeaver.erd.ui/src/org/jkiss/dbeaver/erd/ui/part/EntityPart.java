/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.erd.ui.figures.AttributeItemFigure;
import org.jkiss.dbeaver.erd.ui.figures.EditableLabel;
import org.jkiss.dbeaver.erd.ui.figures.EntityFigure;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.policy.EntityConnectionEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.EntityContainerEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.EntityEditPolicy;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the editable/resizable table which can have columns added,
 * removed, renamed etc.
 *
 * @author Serge Rider
 */
public class EntityPart extends NodePart {
    protected DirectEditManager manager;
    private AccessibleGraphicalEditPart accPart;

    public EntityPart() {
    }

    /**
     * Returns the Table model object represented by this EditPart
     */
    public ERDEntity getEntity() {
        return (ERDEntity) getModel();
    }

    @Override
    protected List<ERDEntityAttribute> getModelChildren() {
        return getEntity().getAttributes();
    }

    //******************* Editing related methods *********************/

    /**
     * Creates edit policies and associates these with roles
     */
    @Override
    protected void createEditPolicies() {
        if (!getEditor().isReadOnly()) {
            final boolean layoutEnabled = isLayoutEnabled();
            if (layoutEnabled) {
                installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new EntityConnectionEditPolicy());
                installEditPolicy(EditPolicy.CONTAINER_ROLE, new EntityContainerEditPolicy());
                installEditPolicy(EditPolicy.COMPONENT_ROLE, new EntityEditPolicy());
            }

            getDiagram().getModelAdapter().installPartEditPolicies(this);
        }
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            ERDUIUtils.openObjectEditor(getDiagram(), getEntity());
        } else {
            getDiagram().getModelAdapter().performPartRequest(this, request);
        }
    }

    /**
     *  Some routing methods doesn't support attribute associations
     *  Also not all visibility settings can allow an attribute-attribute
     *  relations
     *
     * @return is attribute associations possible
     */
    protected boolean isMixedAssociationSupported() {
        return false;
    }

    public void handleNameChange() {
        EntityFigure entityFigure = getFigure();
        EditableLabel label = entityFigure.getNameLabel();
        label.setText(getEntity().getName());
        label.setVisible(true);
        refreshVisuals();
    }

    //******************* Miscellaneous stuff *********************/

    public String toString() {
        return DBUtils.getObjectFullName(getEntity().getObject(), DBPEvaluationContext.UI);
    }

    //******************* Listener related methods *********************/

    /**
     * Handles change in name when committing a direct edit
     */
    @Override
    protected void commitNameChange(PropertyChangeEvent evt) {
        EntityFigure entityFigure = getFigure();
        EditableLabel label = entityFigure.getNameLabel();
        label.setText(getEntity().getObject().getName());
        label.setVisible(true);
        refreshVisuals();
        entityFigure.refreshColors();
    }

    @Override
    protected void commitRefresh(PropertyChangeEvent evt) {
        super.commitRefresh(evt);
    }

    // Workaround: attribute figures aren't direct children of entity figure
    // so we delegate child removal to entity part
    @Override
    protected void removeChildVisual(EditPart childEditPart) {
        EntityFigure figure = getFigure();
        if (childEditPart instanceof AttributePart) {
            AttributeItemFigure childFigure = ((AttributePart) childEditPart).getFigure();
            figure.remove(childFigure);
        } else {
            super.removeChildVisual(childEditPart);
        }
    }

    //******************* Layout related methods *********************/

    /**
     * Creates a figure which represents the table
     */
    @Override
    protected EntityFigure createFigure() {
        final EntityDiagram diagram = getDiagram();

        final EntityFigure figure = createFigureImpl();

        EntityDiagram.NodeVisualInfo visualInfo = diagram.getVisualInfo(getEntity().getObject());
        if (visualInfo != null) {
            if (visualInfo.initBounds != null) {
                figure.setLocation(visualInfo.initBounds.getLocation());
            }
            if (visualInfo.bgColor != null) {
                figure.setBackgroundColor(visualInfo.bgColor);
            }
            if (getEntity().getAttributeVisibility() == null && visualInfo.attributeVisibility != null) {
                getEntity().setAttributeVisibility(visualInfo.attributeVisibility);
            }
        }

        return figure;
    }

    protected EntityFigure createFigureImpl() {
        return new EntityFigure(this);
    }

    @Override
    public EntityFigure getFigure() {
        return (EntityFigure) super.getFigure();
    }

    /**
     * Reset the layout constraint, and revalidate the content pane
     */
    @Override
    protected void refreshVisuals() {
        EntityFigure entityFigure = getFigure();
        Point location = entityFigure.getLocation();
        Rectangle constraint = new Rectangle(location.x, location.y, -1, -1);
        getDiagramPart().setLayoutConstraint(this, entityFigure, constraint);
    }

    /**
     * @return the Content pane for adding or removing child figures
     */
    @Override
    public EntityFigure getContentPane() {
//		EntityFigure figure = (EntityFigure) getFigure();
//		return figure.getColumnsFigure();
        return getFigure();
    }

    /**
     * Sets the width of the line when selected
     */
    @Override
    public void setSelected(int value) {
        super.setSelected(value);
        EntityFigure entityFigure = getFigure();
        if (value != EditPart.SELECTED_NONE)
            entityFigure.setSelected(true);
        else
            entityFigure.setSelected(false);
        entityFigure.repaint();
    }

    @Override
    public ERDGraphicalViewer getViewer() {
        return (ERDGraphicalViewer) super.getViewer();
    }

    @Override
    public void activate() {
        super.activate();
        getViewer().handleTableActivate(getEntity().getObject());
    }

    @Override
    public void deactivate() {
        getViewer().handleTableDeactivate(getEntity().getObject());
        super.deactivate();
    }

    // Add nested figures to visuals (to make hit test work properly)
    @Override
    protected void registerVisuals() {
        super.registerVisuals();
        Map visualPartMap = this.getViewer().getVisualPartMap();
        visualPartMap.put(getFigure().getNameLabel(), this);
        visualPartMap.put(getFigure().getKeyFigure(), this);
        visualPartMap.put(getFigure().getColumnsFigure(), this);
    }

    // Remove nested figures from visuals
    @Override
    protected void unregisterVisuals() {
        Map visualPartMap = this.getViewer().getVisualPartMap();
        visualPartMap.remove(getFigure().getColumnsFigure());
        visualPartMap.remove(getFigure().getKeyFigure());
        visualPartMap.remove(getFigure().getNameLabel());
        super.unregisterVisuals();
    }

    @Override
    public EditPart getTargetEditPart(Request request) {
        if (RequestConstants.REQ_MOVE.equals(request.getType()) || RequestConstants.REQ_ADD.equals(request.getType())) {
            return this;
        }
        return super.getTargetEditPart(request);
    }

    @Override
    public DragTracker getDragTracker(Request request) {
        return super.getDragTracker(request);
    }

    @Override
    public ERDElement getElement() {
        return getEntity();
    }

    @Override
    protected List<ERDAssociation> getModelSourceConnections() {
        if (!supportsAttributeAssociations()) {
            return super.getModelSourceConnections();
        }
        List<ERDAssociation> list = new ArrayList<>();
        for (ERDAssociation erdAssociation : super.getModelSourceConnections()) {
            if (erdAssociation.getObject().getConstraintType() == DBSEntityConstraintType.INHERITANCE
                || isMixedAssociationSupported() && erdAssociation.getTargetAttributes().size() == 0) {
                list.add(erdAssociation);
            }
        }
        return list;

    }

    private boolean supportsAttributeAssociations() {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        return store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI)
               && !ERDAttributeVisibility.isHideAttributeAssociations(store);
    }

    @Override
    protected List<ERDAssociation> getModelTargetConnections() {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        if (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI)
            || ERDAttributeVisibility.isHideAttributeAssociations(store)) {
            return super.getModelTargetConnections();
        }
        List<ERDAssociation> list = new ArrayList<>();
        for (ERDAssociation erdAssociation : super.getModelTargetConnections()) {
            if (erdAssociation.getObject().getConstraintType() == DBSEntityConstraintType.INHERITANCE
                || (isMixedAssociationSupported() && erdAssociation.getTargetAttributes().size() == 0)) {
                list.add(erdAssociation);
            }
        }
        return list;
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connectionEditPart) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connectionEditPart) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    protected AccessibleEditPart getAccessibleEditPart() {
        if (this.accPart == null) {
            this.accPart = new AccessibleGraphicalEditPart() {
                public void getName(AccessibleEvent e) {
                    e.result = NLS.bind(ERDUIMessages.erd_accessibility_entity_part, new Object[]{
                        EntityPart.this.getName(),
                        EntityPart.this.getEntity().getAttributes().size(),
                        sourceConnections == null ? 0 : sourceConnections.size(),
                        targetConnections == null ? 0 : targetConnections.size(),

                    });
                }
            };
        }

        return this.accPart;
    }
}