/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.jkiss.dbeaver.ext.erd.directedit.ExtendedDirectEditManager;
import org.jkiss.dbeaver.ext.erd.directedit.LabelCellEditorLocator;
import org.jkiss.dbeaver.ext.erd.directedit.TableNameCellEditorValidator;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.editor.ERDGraphicalViewer;
import org.jkiss.dbeaver.ext.erd.figures.EditableLabel;
import org.jkiss.dbeaver.ext.erd.figures.EntityFigure;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Represents the editable/resizable table which can have columns added,
 * removed, renamed etc.
 * 
 * @author Serge Rieder
 */
public class EntityPart extends PropertyAwarePart implements NodeEditPart
{

	protected DirectEditManager manager;
    private Rectangle bounds;


    /**
     * @return Returns the bounds.
     */
    public Rectangle getBounds()
    {
        return bounds;
    }

    /**
     * Sets bounds without firing off any event notifications
     *
     * @param bounds
     *            The bounds to set.
     */
    public void setBounds(Rectangle bounds)
    {
        this.bounds = bounds;
    }

    /**
     * If modified, sets bounds and fires off event notification
     *
     * @param bounds
     *            The bounds to set.
     */
    public void modifyBounds(Rectangle bounds)
    {
        Rectangle oldBounds = this.bounds;
        if (!bounds.equals(oldBounds))
        {
            this.bounds = bounds;

            EntityFigure entityFigure = (EntityFigure) getFigure();
            DiagramPart parent = (DiagramPart) getParent();
            parent.setLayoutConstraint(this, entityFigure, bounds);
        }
    }

	//******************* Life-cycle related methods *********************/

	/**
	 * @see org.eclipse.gef.EditPart#activate()
	 */
	public void activate()
	{
		super.activate();
	}

	/**
	 * @see org.eclipse.gef.EditPart#deactivate()
	 */
	public void deactivate()
	{
		super.deactivate();
	}

	//******************* Model related methods *********************/

	/**
	 * Returns the Table model object represented by this EditPart
	 */
	public ERDTable getTable()
	{
		return (ERDTable) getModel();
	}

	/**
	 * @return the children Model objects as a new ArrayList
	 */
	protected List getModelChildren()
	{
		return getTable().getColumns();
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelSourceConnections()
	 */
	protected List getModelSourceConnections()
	{
		return getTable().getForeignKeyRelationships();
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelTargetConnections()
	 */
	protected List getModelTargetConnections()
	{
		return getTable().getPrimaryKeyRelationships();
	}

	//******************* Editing related methods *********************/

	/**
	 * Creates edit policies and associates these with roles
	 */
	protected void createEditPolicies()
	{
		//installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new EntityNodeEditPolicy());
		//installEditPolicy(EditPolicy.LAYOUT_ROLE, new EntityLayoutEditPolicy());
		//installEditPolicy(EditPolicy.CONTAINER_ROLE, new EntityContainerEditPolicy());
		//installEditPolicy(EditPolicy.COMPONENT_ROLE, new EntityEditPolicy());
		//installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new EntityDirectEditPolicy());

	}

	//******************* Direct editing related methods *********************/

	/**
	 * @see org.eclipse.gef.EditPart#performRequest(org.eclipse.gef.Request)
	 */
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
		}
	}

	private boolean directEditHitTest(Point requestLoc)
	{
		EntityFigure figure = (EntityFigure) getFigure();
		EditableLabel nameLabel = figure.getNameLabel();
		nameLabel.translateToRelative(requestLoc);
        return nameLabel.containsPoint(requestLoc);
    }

	protected void performDirectEdit()
	{
		if (manager == null)
		{
			ERDGraphicalViewer viewer = (ERDGraphicalViewer) getViewer();
			ValidationMessageHandler handler = viewer.getValidationHandler();

			EntityFigure figure = (EntityFigure) getFigure();
			EditableLabel nameLabel = figure.getNameLabel();
			manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(nameLabel),
					nameLabel, new TableNameCellEditorValidator(handler));
		}
		manager.show();
	}

	public void handleNameChange(String value)
	{
		EntityFigure entityFigure = (EntityFigure) getFigure();
		EditableLabel label = entityFigure.getNameLabel();
		label.setVisible(false);
		refreshVisuals();
	}

	/**
	 * Reverts to existing name in model when exiting from a direct edit
	 * (possibly before a commit which will result in a change in the label
	 * value)
	 */
	public void revertNameChange()
	{
		EntityFigure entityFigure = (EntityFigure) getFigure();
		EditableLabel label = entityFigure.getNameLabel();
		ERDTable table = getTable();
		label.setText(table.getObject().getName());
		label.setVisible(true);
		refreshVisuals();
	}

	//******************* Miscellaneous stuff *********************/

	/**
	 * @see org.eclipse.gef.editparts.AbstractEditPart#toString()
	 */
	public String toString()
	{
		return getModel().toString();
	}

	//******************* Listener related methods *********************/

	/**
	 * Handles change in name when committing a direct edit
	 */
	protected void commitNameChange(PropertyChangeEvent evt)
	{
		EntityFigure entityFigure = (EntityFigure) getFigure();
		EditableLabel label = entityFigure.getNameLabel();
		label.setText(getTable().getObject().getName());
		label.setVisible(true);
		refreshVisuals();
	}

	//******************* Layout related methods *********************/

	/**
	 * Creates a figure which represents the table
	 */
	protected IFigure createFigure()
	{
		ERDTable table = getTable();
		EditableLabel label = new EditableLabel(table.getObject().getName());
		return new EntityFigure(label);
	}

	/**
	 * Reset the layout constraint, and revalidate the content pane
	 */
	protected void refreshVisuals()
	{
		EntityFigure entityFigure = (EntityFigure) getFigure();
		Point location = entityFigure.getLocation();
		DiagramPart parent = (DiagramPart) getParent();
		Rectangle constraint = new Rectangle(location.x, location.y, -1, -1);
		parent.setLayoutConstraint(this, entityFigure, constraint);
	}

	/**
	 * @return the Content pane for adding or removing child figures
	 */
	public IFigure getContentPane()
	{
		EntityFigure figure = (EntityFigure) getFigure();
		return figure.getColumnsFigure();
	}

	/**
	 * @see NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection)
	{
		return new ChopboxAnchor(getFigure());
	}

	/**
	 * @see org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.Request)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(Request request)
	{
        return new ChopboxAnchor(getFigure());
		//return new TopAnchor(getFigure());
	}

	/**
	 * @see NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection)
	{
        return new ChopboxAnchor(getFigure());
		//return new BottomAnchor(getFigure());
	}

	/**
	 * @see org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.Request)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(Request request)
	{
		return new ChopboxAnchor(getFigure());
	}

	/**
	 * Sets the width of the line when selected
	 */
	public void setSelected(int value)
	{
		super.setSelected(value);
		EntityFigure entityFigure = (EntityFigure) getFigure();
		if (value != EditPart.SELECTED_NONE)
			entityFigure.setSelected(true);
		else
			entityFigure.setSelected(false);
		entityFigure.repaint();
	}
}