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
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.jface.viewers.TextCellEditor;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.ext.erd.ERDUIMessages;
import org.jkiss.dbeaver.ext.erd.ERDUIUtils;
import org.jkiss.dbeaver.ext.erd.command.AttributeCheckCommand;
import org.jkiss.dbeaver.ext.erd.directedit.ColumnNameTypeCellEditorValidator;
import org.jkiss.dbeaver.ext.erd.directedit.ExtendedDirectEditManager;
import org.jkiss.dbeaver.ext.erd.directedit.LabelCellEditorLocator;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.ext.erd.figures.AttributeItemFigure;
import org.jkiss.dbeaver.ext.erd.figures.EditableLabel;
import org.jkiss.dbeaver.ext.erd.policy.AttributeConnectionEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.AttributeDirectEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.AttributeDragAndDropEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.AttributeEditPolicy;

import java.beans.PropertyChangeEvent;
import java.util.Map;

/**
 * Represents an editable Column object in the model
 *
 * @author Serge Rider
 */
public class AttributePart extends PropertyAwarePart {

    public static final String PROP_CHECKED = "CHECKED";

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
        if (isEditEnabled()) {
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new AttributeEditPolicy());
            installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new AttributeDirectEditPolicy());
            //installEditPolicy(EditPolicy.LAYOUT_ROLE, null);

            if (getEditPolicy(EditPolicy.CONTAINER_ROLE) == null && isColumnDragAndDropSupported()) {
                installEditPolicy(EditPolicy.CONTAINER_ROLE, new AttributeConnectionEditPolicy(this));
                installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new AttributeDragAndDropEditPolicy(this));
            }
        }
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
/*
			if (request instanceof DirectEditRequest
					&& !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy()))
				return;
			performDirectEdit();
*/
        } else if (request.getType() == RequestConstants.REQ_OPEN) {
            ERDUIUtils.openObjectEditor(getAttribute());
        }
    }

    public AttributeCheckCommand createAttributeCheckCommand(boolean newChecked) {
        return new AttributeCheckCommand(this, newChecked);
    }

    private boolean directEditHitTest(Point requestLoc) {
        IFigure figure = getFigure();
        figure.translateToRelative(requestLoc);
        return figure.containsPoint(requestLoc);
    }

    protected void performDirectEdit() {
        ERDGraphicalViewer viewer = (ERDGraphicalViewer) getViewer();
        ValidationMessageHandler handler = viewer.getValidationHandler();

        Label l = getFigure().getLabel();
        ColumnNameTypeCellEditorValidator columnNameTypeCellEditorValidator = new ColumnNameTypeCellEditorValidator(
                handler);

        DirectEditManager manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(l), l,
                columnNameTypeCellEditorValidator);

        manager.show();
    }

    /**
     * Sets the width of the line when selected
     */
    @Override
    public void setSelected(int value) {
        super.setSelected(value);
        EditableLabel columnLabel = getFigure().getLabel();
        if (value != EditPart.SELECTED_NONE)
            columnLabel.setSelected(true);
        else
            columnLabel.setSelected(false);
        columnLabel.repaint();
    }

    public void handleNameChange(String textValue) {
        EditableLabel label = getFigure().getLabel();
        label.setVisible(false);
        setSelected(EditPart.SELECTED_NONE);
        label.revalidate();
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
     * Reverts state back to prior edit state
     */
    public void revertNameChange(String oldValue) {
        AttributeItemFigure figure = getFigure();
        figure.setVisible(true);
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

}