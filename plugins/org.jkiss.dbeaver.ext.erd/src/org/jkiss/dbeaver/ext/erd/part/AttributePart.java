/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
 * @author Serge Rieder
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