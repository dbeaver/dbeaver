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
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.policy.EntityContainerEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.EntityEditPolicy;
import org.jkiss.dbeaver.ext.erd.policy.EntityNodeEditPolicy;
import org.jkiss.dbeaver.model.DBUtils;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Represents the editable/resizable table which can have columns added,
 * removed, renamed etc.
 * 
 * @author Serge Rieder
 */
public class EntityPart extends NodePart
{
	protected DirectEditManager manager;

    public EntityPart()
    {
    }

    /**
	 * Returns the Table model object represented by this EditPart
	 */
	public ERDEntity getTable()
	{
		return (ERDEntity) getModel();
	}

	/**
	 * @return the children Model objects as a new ArrayList
	 */
	@Override
    protected List<ERDEntityAttribute> getModelChildren()
	{
		return getTable().getColumns();
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelSourceConnections()
	 */
	@Override
    protected List<ERDAssociation> getModelSourceConnections()
	{
		return getTable().getForeignKeyRelationships();
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelTargetConnections()
	 */
	@Override
    protected List<ERDAssociation> getModelTargetConnections()
	{
		return getTable().getPrimaryKeyRelationships();
	}

	//******************* Editing related methods *********************/

	/**
	 * Creates edit policies and associates these with roles
	 */
	@Override
    protected void createEditPolicies()
	{
        final boolean editEnabled = isEditEnabled();
        if (editEnabled) {
            installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new EntityNodeEditPolicy());
            //installEditPolicy(EditPolicy.LAYOUT_ROLE, new EntityLayoutEditPolicy());
            installEditPolicy(EditPolicy.CONTAINER_ROLE, new EntityContainerEditPolicy());
            installEditPolicy(EditPolicy.COMPONENT_ROLE, new EntityEditPolicy());
            //installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new EntityDirectEditPolicy());
        }
	}

    //******************* Direct editing related methods *********************/

	/**
	 * @see org.eclipse.gef.EditPart#performRequest(org.eclipse.gef.Request)
	 */
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
            getTable().openEditor();
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
			ERDGraphicalViewer viewer = getViewer();
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
		ERDEntity entity = getTable();
		label.setText(entity.getObject().getName());
		label.setVisible(true);
		refreshVisuals();
	}

	//******************* Miscellaneous stuff *********************/

	/**
	 * @see org.eclipse.gef.editparts.AbstractEditPart#toString()
	 */
	public String toString()
	{
        return DBUtils.getObjectFullName(getTable().getObject());
	}

	//******************* Listener related methods *********************/

	/**
	 * Handles change in name when committing a direct edit
	 */
	@Override
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
	@Override
    protected IFigure createFigure()
	{
        final EntityFigure figure = new EntityFigure(getTable());
        final EntityDiagram diagram = ((DiagramPart) getParent()).getDiagram();
        Rectangle bounds = diagram.getInitBounds(getTable());
        if (bounds != null) {
            figure.setLocation(bounds.getLocation());
        }

        return figure;
    }

	/**
	 * Reset the layout constraint, and revalidate the content pane
	 */
	@Override
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
	@Override
    public IFigure getContentPane()
	{
//		EntityFigure figure = (EntityFigure) getFigure();
//		return figure.getColumnsFigure();
        return getFigure();
	}

	@Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection)
	{
		return new ChopboxAnchor(getFigure());
	}

	@Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request)
	{
        return new ChopboxAnchor(getFigure());
		//return new TopAnchor(getFigure());
	}

	@Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection)
	{
        return new ChopboxAnchor(getFigure());
		//return new BottomAnchor(getFigure());
	}

	@Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request)
	{
		return new ChopboxAnchor(getFigure());
	}

	/**
	 * Sets the width of the line when selected
	 */
	@Override
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

    public AssociationPart getConnectionPart(ERDAssociation rel, boolean source)
    {
        for (Object conn : source ? getSourceConnections() : getTargetConnections()) {
            if (conn instanceof AssociationPart && ((AssociationPart) conn).getAssociation() == rel) {
                return (AssociationPart) conn;
            }
        }
        return null;
    }

    @Override
    public ERDGraphicalViewer getViewer() {
        return (ERDGraphicalViewer)super.getViewer();
    }

    @Override
    public void activate()
    {
        super.activate();
        getViewer().handleTableActivate(getTable().getObject());
    }

    @Override
    public void deactivate()
    {
        getViewer().handleTableDeactivate(getTable().getObject());
        super.deactivate();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }
}