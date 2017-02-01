/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.directedit.ColumnNameTypeCellEditorValidator;
import org.jkiss.dbeaver.ext.erd.directedit.ExtendedDirectEditManager;
import org.jkiss.dbeaver.ext.erd.directedit.LabelCellEditorLocator;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeStyle;
import org.jkiss.dbeaver.ext.erd.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.ext.erd.figures.AttributeItemFigure;
import org.jkiss.dbeaver.ext.erd.figures.EditableLabel;
import org.jkiss.dbeaver.ext.erd.model.ERDEntityAttribute;

import java.beans.PropertyChangeEvent;

/**
 * Represents an editable Column object in the model
 * @author Serge Rider
 */
public class AttributePart extends PropertyAwarePart
{

	protected DirectEditManager manager;

    @Override
    public boolean isSelectable() {
        return true;
    }

    /**
	 * @return the ColumnLabel representing the Column
	 */
	@Override
    protected AttributeItemFigure createFigure()
	{
		ERDEntityAttribute column = (ERDEntityAttribute) getModel();
        AttributeItemFigure attributeFigure = new AttributeItemFigure(column);

        DiagramPart diagramPart = (DiagramPart) getParent().getParent();
		boolean showNullability = diagramPart.getDiagram().hasAttributeStyle(ERDAttributeStyle.NULLABILITY);
        Font columnFont = diagramPart.getNormalFont();
        Color columnColor = diagramPart.getContentPane().getForegroundColor();
        if (column.isInPrimaryKey()) {
            columnFont = diagramPart.getBoldFont();
            if (showNullability && !column.getObject().isRequired()) {
                columnFont = diagramPart.getBoldItalicFont();
            }
/*
            if (!column.isInForeignKey()) {
                columnFont = diagramPart.getBoldFont();
            } else {
                columnFont = diagramPart.getBoldItalicFont();
            }
*/
        } else {
            if (showNullability && !column.getObject().isRequired()) {
                columnFont = diagramPart.getItalicFont();
            }
        }
        if (column.isInForeignKey()) {
            //columnColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
        }
        attributeFigure.setFont(columnFont);
        attributeFigure.setForegroundColor(columnColor);
        return attributeFigure;
    }

	@Override
	public AttributeItemFigure getFigure() {
		return (AttributeItemFigure)super.getFigure();
	}

	/**
	 * Create EditPolicies for the column label
	 */
	@Override
    protected void createEditPolicies()
	{
		//installEditPolicy(EditPolicy.COMPONENT_ROLE, new AttributeEditPolicy());
		//installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new ColumnDirectEditPolicy());
		//installEditPolicy(EditPolicy.LAYOUT_ROLE, null);
	}

	@Override
    public void performRequest(Request request)
	{
		if (request.getType() == RequestConstants.REQ_DIRECT_EDIT)
		{
/*
			if (request instanceof DirectEditRequest
					&& !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy()))
				return;
			performDirectEdit();
*/
        } else if (request.getType() == RequestConstants.REQ_OPEN) {
            getAttribute().openEditor();
        }
	}

	private boolean directEditHitTest(Point requestLoc)
	{
		IFigure figure = getFigure();
		figure.translateToRelative(requestLoc);
        return figure.containsPoint(requestLoc);
    }

	protected void performDirectEdit()
	{
		if (manager == null)
		{
			ERDGraphicalViewer viewer = (ERDGraphicalViewer) getViewer();
			ValidationMessageHandler handler = viewer.getValidationHandler();

			Label l = getFigure();
			ColumnNameTypeCellEditorValidator columnNameTypeCellEditorValidator = new ColumnNameTypeCellEditorValidator(
					handler);

			manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(l), l,
					columnNameTypeCellEditorValidator);
		}
		manager.show();
	}

	/**
	 * Sets the width of the line when selected
	 */
	@Override
    public void setSelected(int value)
	{
		super.setSelected(value);
		EditableLabel columnLabel = getFigure();
		if (value != EditPart.SELECTED_NONE)
			columnLabel.setSelected(true);
		else
			columnLabel.setSelected(false);
		columnLabel.repaint();
	}

	public void handleNameChange(String textValue)
	{
		EditableLabel label = getFigure();
		label.setVisible(false);
		setSelected(EditPart.SELECTED_NONE);
		label.revalidate();
	}

	/**
	 * Handles when successfully applying direct edit
	 */
	@Override
    protected void commitNameChange(PropertyChangeEvent evt)
	{
		AttributeItemFigure label = getFigure();
		label.setText(getAttribute().getLabelText());
		setSelected(EditPart.SELECTED_PRIMARY);
		label.revalidate();
	}


	/**
	 * Reverts state back to prior edit state
	 */
	public void revertNameChange(String oldValue)
	{
		AttributeItemFigure label = getFigure();
		label.setVisible(true);
		setSelected(EditPart.SELECTED_PRIMARY);
		label.revalidate();
	}

	/**
	 * We don't need to explicitly handle refresh visuals because the times when
	 * this needs to be done it is handled by the table e.g. handleNameChange()
	 */
	@Override
    protected void refreshVisuals()
	{
		ERDEntityAttribute column = (ERDEntityAttribute) getModel();
		getFigure().setText(column.getLabelText());
	}
	
	public ERDEntityAttribute getAttribute()
	{
		return (ERDEntityAttribute) getModel();
	}
	
    @Override
    public String toString()
    {
        return ERDMessages.column_ + getAttribute().getLabelText();
    }

}