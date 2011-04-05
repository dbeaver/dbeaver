/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * An ConnectionEditPart base class which is property aware, that is, can handle property change notification events
 * All our ConnectionEditPart are subclasses of this
 * @author Serge Rieder
 */
public abstract class PropertyAwareConnectionPart extends AbstractConnectionEditPart implements PropertyChangeListener, DBPNamedObject
{
    public String getName()
    {
        return ((ERDObject)getModel()).getName();
    }

    protected boolean isEditEnabled()
    {
        return getRoot().getContents() instanceof DiagramPart && ((DiagramPart) getRoot().getContents()).getDiagram().isLayoutManualAllowed();
    }

	/**
	 * @see org.eclipse.gef.EditPart#activate()
	 */
	public void activate()
	{
		super.activate();
		ERDObject<?> erdObject = (ERDObject<?>) getModel();
		erdObject.addPropertyChangeListener(this);
	}

	/**
	 * @see org.eclipse.gef.EditPart#deactivate()
	 */
	public void deactivate()
	{
		super.deactivate();
		ERDObject<?> erdObject = (ERDObject<?>) getModel();
		erdObject.removePropertyChangeListener(this);
	}

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{

		String property = evt.getPropertyName();

		if (ERDObject.CHILD.equals(property))
			refreshChildren();
		else if (ERDObject.INPUT.equals(property))
			refreshTargetConnections();
		else if (ERDObject.OUTPUT.equals(property))
			refreshSourceConnections();

		/*
		 * if (FlowElement.CHILDREN.equals(prop)) refreshChildren(); else if
		 * (FlowElement.INPUTS.equals(prop)) refreshTargetConnections(); else if
		 * (FlowElement.OUTPUTS.equals(prop)) refreshSourceConnections(); else
		 * if (Activity.NAME.equals(prop)) refreshVisuals(); // Causes Graph to
		 * re-layout
		 */
		((GraphicalEditPart) (getViewer().getContents())).getFigure().revalidate();
	}

}