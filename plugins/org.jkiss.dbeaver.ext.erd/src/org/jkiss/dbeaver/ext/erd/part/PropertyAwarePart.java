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
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract EditPart implementation which is property aware and responds to
 * PropertyChangeEvents fired from the model
 * @author Serge Rieder
 */
public abstract class PropertyAwarePart extends AbstractGraphicalEditPart implements PropertyChangeListener, DBPNamedObject
{
    @NotNull
	@Override
    public String getName()
    {
        return ((ERDObject)getModel()).getName();
    }

    protected boolean isEditEnabled()
    {
        return getParent() instanceof DiagramPart && ((DiagramPart) getParent()).getDiagram().isLayoutManualAllowed();
    }

	/**
	 * @see org.eclipse.gef.EditPart#activate()
	 */
	@Override
    public void activate()
	{
		super.activate();
		ERDObject<?> erdObject = (ERDObject<?>) getModel();
		erdObject.addPropertyChangeListener(this);
	}

	/**
	 * @see org.eclipse.gef.EditPart#deactivate()
	 */
	@Override
    public void deactivate()
	{
		super.deactivate();
		ERDObject<?> erdObject = (ERDObject<?>) getModel();
		erdObject.removePropertyChangeListener(this);
	}

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
    public void propertyChange(PropertyChangeEvent evt)
	{

		String property = evt.getPropertyName();

		if (ERDObject.CHILD.equals(property))
		{
			handleChildChange(evt);
		}
		if (ERDObject.REORDER.equals(property))
		{
			handleReorderChange(evt);
		}
		else if (ERDObject.OUTPUT.equals(property))
		{
			handleOutputChange(evt);
		}
		else if (ERDObject.INPUT.equals(property))
		{
			handleInputChange(evt);
		}
		else if (ERDObject.NAME.equals(property))
		{
			commitNameChange(evt);
		}

		//we want direct edit name changes to update immediately
		//not use the Graph animation, if automatic layout is being used
		if (ERDObject.NAME.equals(property))
		{
			GraphicalEditPart graphicalEditPart = (GraphicalEditPart) (getViewer().getContents());
			IFigure partFigure = graphicalEditPart.getFigure();
			partFigure.getUpdateManager().performUpdate();
		}

	}

	/**
	 * Called when change to one of the inputs occurs
	 */
	private void handleInputChange(PropertyChangeEvent evt)
	{

		//this works but is not efficient
		//refreshTargetConnections();

		//a more efficient implementation should either remove or add the
		//relevant target connection
		//using the removeTargetConnection(ConnectionEditPart connection) or
		//addTargetConnection(ConnectionEditPart connection, int index)

		Object newValue = evt.getNewValue();
		Object oldValue = evt.getOldValue();

		if (!((oldValue != null) ^ (newValue != null)))
		{
			throw new IllegalStateException("Exactly one of old or new values must be non-null for INPUT event");
		}

		if (newValue != null)
		{
			//add new connection
			ConnectionEditPart editPart = createOrFindConnection(newValue);
			int modelIndex = getModelTargetConnections().indexOf(newValue);
			addTargetConnection(editPart, modelIndex);

		}
		else
		{

			//remove connection
			List<?> children = getTargetConnections();

			ConnectionEditPart partToRemove = null;
			for (Iterator<?> iter = children.iterator(); iter.hasNext();)
			{
				ConnectionEditPart part = (ConnectionEditPart) iter.next();
				if (part.getModel() == oldValue)
				{
					partToRemove = part;
					break;
				}
			}

			if (partToRemove != null)
				removeTargetConnection(partToRemove);
		}
		
		getContentPane().revalidate();

	}

	/**
	 * Called when change to one of the outputs occurs
	 */
	private void handleOutputChange(PropertyChangeEvent evt)
	{

		//this works but is not efficient
		//refreshSourceConnections();

		// a more efficient implementation should either remove or add the
		// relevant target connect
		//using the removeSourceConnection(ConnectionEditPart connection) or
		//addSourceConnection(ConnectionEditPart connection, int index)

		Object newValue = evt.getNewValue();
		Object oldValue = evt.getOldValue();

		if (!((oldValue != null) ^ (newValue != null)))
		{
			throw new IllegalStateException("Exactly one of old or new values must be non-null for INPUT event");
		}

		if (newValue != null)
		{
			//add new connection
			ConnectionEditPart editPart = createOrFindConnection(newValue);
			int modelIndex = getModelSourceConnections().indexOf(newValue);
			addSourceConnection(editPart, modelIndex);

		}
		else
		{

			//remove connection
			List<?> children = getSourceConnections();

			ConnectionEditPart partToRemove = null;
			for (Iterator<?> iter = children.iterator(); iter.hasNext();)
			{
				ConnectionEditPart part = (ConnectionEditPart) iter.next();
				if (part.getModel() == oldValue)
				{
					partToRemove = part;
					break;
				}
			}

			if (partToRemove != null)
				removeSourceConnection(partToRemove);
		}
		
		getContentPane().revalidate();

	}

	/**
	 * called when child added or removed
	 */
	protected void handleChildChange(PropertyChangeEvent evt)
	{

		//we could do this but it is not very efficient
		//refreshChildren();

		Object newValue = evt.getNewValue();
		Object oldValue = evt.getOldValue();

		if (!((oldValue != null) ^ (newValue != null)))
		{
			throw new IllegalStateException("Exactly one of old or new values must be non-null for CHILD event");
		}

		if (newValue != null)
		{
			//add new child
			EditPart editPart = createChild(newValue);
			int modelIndex = getModelChildren().indexOf(newValue);
			addChild(editPart, modelIndex);

		}
		else
		{

			List<?> children = getChildren();

			EditPart partToRemove = null;
			for (Iterator<?> iter = children.iterator(); iter.hasNext();)
			{
				EditPart part = (EditPart) iter.next();
				if (part.getModel() == oldValue)
				{
					partToRemove = part;
					break;
				}
			}

			if (partToRemove != null)
				removeChild(partToRemove);
		}

		//getContentPane().revalidate();

	}

	/**
	 * Called when columns are re-ordered within
	 */
	protected void handleReorderChange(PropertyChangeEvent evt)
	{
		refreshChildren();
		refreshVisuals();
	}

	protected void commitNameChange(PropertyChangeEvent evt)
	{
	}

}