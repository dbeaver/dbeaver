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
package org.jkiss.dbeaver.erd.ui.part;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.model.*;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.command.AttributeCheckCommand;
import org.jkiss.dbeaver.erd.ui.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.erd.ui.editor.ERDHighlightingHandle;
import org.jkiss.dbeaver.erd.ui.figures.AttributeItemFigure;
import org.jkiss.dbeaver.erd.ui.figures.EditableLabel;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.policy.AttributeConnectionEditPolicy;
import org.jkiss.dbeaver.erd.ui.policy.AttributeDragAndDropEditPolicy;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents an editable Column object in the model
 *
 * @author Serge Rider
 */
public class AttributePart extends NodePart {

    public static final String PROP_CHECKED = "CHECKED";

    private ERDHighlightingHandle associatedRelationsHighlighing = null;
    private AccessibleGraphicalEditPart accPart;

    public AttributePart() {

    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    public ERDEntityAttribute getAttribute() {
        return (ERDEntityAttribute) getModel();
    }

    public ERDEntity getEntity() {
        return (ERDEntity) getParent().getModel();
    }

    public String getAttributeLabel() {
        return ERDUIUtils.getFullAttributeLabel(getDiagram(), getAttribute(), false);
    }

    @Override
    protected void addSourceConnection(ConnectionEditPart connection, int index) {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        if (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI) || ERDAttributeVisibility.isHideAttributeAssociations(store)) {
            return;
        }
        if (((AssociationPart) connection).getAssociation().getSourceAttributes().contains(getAttribute())) {
            super.addSourceConnection(connection, index);
        }
    }

    @Override
    protected List<ERDAssociation> getModelSourceConnections() {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        if (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI) || ERDAttributeVisibility.isHideAttributeAssociations(store)) {
            return Collections.emptyList();
        }
        List<ERDAssociation> list = new ArrayList<>();
        for (ERDAssociation erdAssociation : super.getModelSourceConnections()) {
            if (erdAssociation.getSourceAttributes().contains(getAttribute()) && erdAssociation.getSourceEntity() != null) {
                list.add(erdAssociation);
            }
        }
        return list;
    }

    @Override
    protected List<ERDAssociation> getModelTargetConnections() {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        if (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI) || ERDAttributeVisibility.isHideAttributeAssociations(store)) {
            return Collections.emptyList();
        }
        List<ERDAssociation> list = new ArrayList<>();
        for (ERDAssociation erdAssociation : super.getModelTargetConnections()) {
            if (erdAssociation.getTargetAttributes().contains(getAttribute()) && erdAssociation.getTargetEntity() != null) {
                list.add(erdAssociation);
            }
        }
        return list;
    }

    @Override
    protected void addTargetConnection(ConnectionEditPart connection, int index) {
        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
        if (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI) || ERDAttributeVisibility.isHideAttributeAssociations(store)) {
            return;
        }
        if (((AssociationPart) connection).getAssociation().getTargetAttributes().contains(getAttribute())) {
            super.addTargetConnection(connection, index);
        }
    }

    /**
     * @return the ColumnLabel representing the Column
     */
    @Override
    protected AttributeItemFigure createFigure() {
        return new AttributeItemFigure(this);
    }

    @Override
    public AttributeItemFigure getFigure() {
        return (AttributeItemFigure) super.getFigure();
    }

    /**
     * Create EditPolicies for the column label
     */
    @Override
    protected void createEditPolicies() {
        if (!getEditor().isReadOnly()) {
            if (isLayoutEnabled()) {
                if (getEditPolicy(EditPolicy.CONTAINER_ROLE) == null && isColumnDragAndDropSupported()) {
                    installEditPolicy(EditPolicy.CONTAINER_ROLE, new AttributeConnectionEditPolicy(this));
                    installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new AttributeDragAndDropEditPolicy(this));
                }
            }
            getDiagram().getModelAdapter().installPartEditPolicies(this);
        }
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            ERDUIUtils.openObjectEditor(getDiagram(), getAttribute());
        } else {
            getDiagram().getModelAdapter().performPartRequest(this, request);
        }
    }

    public AttributeCheckCommand createAttributeCheckCommand(boolean newChecked) {
        return new AttributeCheckCommand<>(this, newChecked);
    }

    /**
     * Sets the width of the line when selected
     */
    @Override
    public void setSelected(int value) {
        super.setSelected(value);
        EditableLabel columnLabel = getFigure().getLabel();
        columnLabel.setSelected(value != EditPart.SELECTED_NONE);
        if (false) {
            IFigure rightPanel = getFigure().getRightPanel();
            if (rightPanel instanceof EditableLabel) {
                ((EditableLabel) rightPanel).setSelected(value != EditPart.SELECTED_NONE);
            }
        }
        columnLabel.repaint();

        if (value != EditPart.SELECTED_NONE) {
            if (this.getViewer() instanceof ERDGraphicalViewer && associatedRelationsHighlighing == null) {
                Color color = UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_FK_HIGHLIGHTING);
                associatedRelationsHighlighing = ((ERDGraphicalViewer) this.getViewer()).getEditor().getHighlightingManager().highlightAttributeAssociations(this, color);
            }
        } else if (associatedRelationsHighlighing != null) {
            associatedRelationsHighlighing.release();
            associatedRelationsHighlighing = null;
        }
    }

    public void handleNameChange() {
        AttributeItemFigure figure = getFigure();
        figure.updateLabels();
        setSelected(EditPart.SELECTED_NONE);
        figure.revalidate();
        //EditableLabel label = getFigure().getLabel();
        //label.setText(textValue);
        //setSelected(EditPart.SELECTED_NONE);
        //label.revalidate();
    }

    /**
     * Handles when successfully applying direct edit
     */
    @Override
    protected void commitNameChange(PropertyChangeEvent evt) {
        AttributeItemFigure figure = getFigure();
        figure.updateLabels();
        setSelected(EditPart.SELECTED_PRIMARY);
        figure.revalidate();
    }

    /**
     * We don't need to explicitly handle refresh visuals because the times when
     * this needs to be done it is handled by the table e.g. handleNameChange()
     */
    @Override
    protected void refreshVisuals() {
        getFigure().updateLabels();
    }

    @Override
    public DragTracker getDragTracker(Request request) {
        DragEditPartsTracker dragTracker = new DragEditPartsTracker(this);
        dragTracker.setDefaultCursor(SharedCursors.CURSOR_TREE_MOVE);
        return dragTracker;
    }

    @Override
    public EditPart getTargetEditPart(Request request) {
        if (RequestConstants.REQ_MOVE.equals(request.getType()) || RequestConstants.REQ_ADD.equals(request.getType())) {
            return this;
        }
        return super.getTargetEditPart(request);
    }

    // Add nested figures to visuals (to make hit test work properly)
    @Override
    protected void registerVisuals() {
        super.registerVisuals();
        Map visualPartMap = this.getViewer().getVisualPartMap();
        visualPartMap.put(getFigure().getCheckBox(), this);
        visualPartMap.put(getFigure().getLabel(), this);
    }

    // Remove nested figures from visuals
    @Override
    protected void unregisterVisuals() {
        Map visualPartMap = this.getViewer().getVisualPartMap();
        visualPartMap.remove(getFigure().getLabel());
        visualPartMap.remove(getFigure().getCheckBox());
        super.unregisterVisuals();
    }

    @Override
    public String toString() {
        return ERDUIMessages.column_.trim() + " " + getAttribute().getLabelText();
    }

    @Override
    public ERDElement getElement() {
        return getEntity();
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
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
                    e.result = NLS.bind(ERDUIMessages.erd_accessibility_attribute_part,
                        ERDUIUtils.getFullAttributeLabel(getDiagram(), getAttribute(), true, true));
                }
            };
        }

        return this.accPart;
    }
}