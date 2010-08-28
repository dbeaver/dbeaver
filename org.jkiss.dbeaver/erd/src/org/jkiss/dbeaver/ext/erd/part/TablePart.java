/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.TextCellEditor;

import org.jkiss.dbeaver.ext.erd.directedit.ExtendedDirectEditManager;
import org.jkiss.dbeaver.ext.erd.directedit.LabelCellEditorLocator;
import org.jkiss.dbeaver.ext.erd.directedit.TableNameCellEditorValidator;
import org.jkiss.dbeaver.ext.erd.directedit.ValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.editor.ValidationEnabledGraphicalViewer;
import org.jkiss.dbeaver.ext.erd.figures.EditableLabel;
import org.jkiss.dbeaver.ext.erd.figures.TableFigure;
import org.jkiss.dbeaver.ext.erd.model.Table;
import org.jkiss.dbeaver.ext.erd.part.connector.BottomAnchor;
import org.jkiss.dbeaver.ext.erd.part.connector.TopAnchor;
import org.jkiss.dbeaver.ext.erd.policy.TableContainerEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.TableDirectEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.TableEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.TableLayoutEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.TableNodeEditPolicy;

/**
 * Represents the editable/resizable table which can have columns added,
 * removed, renamed etc.
 * 
 * @author Phil Zoio
 */
public class TablePart extends PropertyAwarePart implements NodeEditPart
{

	protected DirectEditManager manager;

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
	public Table getTable()
	{
		return (Table) getModel();
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

		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new TableNodeEditPolicy());
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new TableLayoutEditPolicy());
		installEditPolicy(EditPolicy.CONTAINER_ROLE, new TableContainerEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new TableEditPolicy());
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new TableDirectEditPolicy());

	}

	//******************* Direct editing related methods *********************/

	/**
	 * @see org.eclipse.gef.EditPart#performRequest(org.eclipse.gef.Request)
	 */
	public void performRequest(Request request)
	{
		if (request.getType() == RequestConstants.REQ_DIRECT_EDIT)
		{
			if (request instanceof DirectEditRequest
					&& !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy()))
				return;
			performDirectEdit();
		}
	}

	private boolean directEditHitTest(Point requestLoc)
	{
		TableFigure figure = (TableFigure) getFigure();
		EditableLabel nameLabel = figure.getNameLabel();
		nameLabel.translateToRelative(requestLoc);
		if (nameLabel.containsPoint(requestLoc))
			return true;
		return false;
	}

	protected void performDirectEdit()
	{
		if (manager == null)
		{
			ValidationEnabledGraphicalViewer viewer = (ValidationEnabledGraphicalViewer) getViewer();
			ValidationMessageHandler handler = viewer.getValidationHandler();

			TableFigure figure = (TableFigure) getFigure();
			EditableLabel nameLabel = figure.getNameLabel();
			manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(nameLabel),
					nameLabel, new TableNameCellEditorValidator(handler));
		}
		manager.show();
	}

	public void handleNameChange(String value)
	{
		TableFigure tableFigure = (TableFigure) getFigure();
		EditableLabel label = tableFigure.getNameLabel();
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
		TableFigure tableFigure = (TableFigure) getFigure();
		EditableLabel label = tableFigure.getNameLabel();
		Table table = getTable();
		label.setText(table.getName());
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
		TableFigure tableFigure = (TableFigure) getFigure();
		EditableLabel label = tableFigure.getNameLabel();
		label.setText(getTable().getName());
		label.setVisible(true);
		refreshVisuals();
	}

	/**
	 * handles change in bounds, to be overridden by subclass
	 */
	protected void handleBoundsChange(PropertyChangeEvent evt)
	{
		TableFigure tableFigure = (TableFigure) getFigure();
		Rectangle constraint = (Rectangle) evt.getNewValue();
		SchemaDiagramPart parent = (SchemaDiagramPart) getParent();
		parent.setLayoutConstraint(this, tableFigure, constraint);
	}

	//******************* Layout related methods *********************/

	/**
	 * Creates a figure which represents the table
	 */
	protected IFigure createFigure()
	{
		Table table = getTable();
		EditableLabel label = new EditableLabel(table.getName());
		TableFigure tableFigure = new TableFigure(label);
		return tableFigure;
	}

	/**
	 * Reset the layout constraint, and revalidate the content pane
	 */
	protected void refreshVisuals()
	{
		TableFigure tableFigure = (TableFigure) getFigure();
		Point location = tableFigure.getLocation();
		SchemaDiagramPart parent = (SchemaDiagramPart) getParent();
		Rectangle constraint = new Rectangle(location.x, location.y, -1, -1);
		parent.setLayoutConstraint(this, tableFigure, constraint);
	}

	/**
	 * @return the Content pane for adding or removing child figures
	 */
	public IFigure getContentPane()
	{
		TableFigure figure = (TableFigure) getFigure();
		return figure.getColumnsFigure();
	}

	/**
	 * @see NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection)
	{
		return new TopAnchor(getFigure());
	}

	/**
	 * @see org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.Request)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(Request request)
	{
		return new TopAnchor(getFigure());
	}

	/**
	 * @see NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection)
	{
		return new BottomAnchor(getFigure());
	}

	/**
	 * @see org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.Request)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(Request request)
	{
		return new BottomAnchor(getFigure());
	}

	/**
	 * Sets the width of the line when selected
	 */
	public void setSelected(int value)
	{
		super.setSelected(value);
		TableFigure tableFigure = (TableFigure) getFigure();
		if (value != EditPart.SELECTED_NONE)
			tableFigure.setSelected(true);
		else
			tableFigure.setSelected(false);
		tableFigure.repaint();
	}
}