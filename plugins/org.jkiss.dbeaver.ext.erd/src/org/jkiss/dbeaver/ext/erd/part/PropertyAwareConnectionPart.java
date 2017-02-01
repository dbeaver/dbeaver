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
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * An ConnectionEditPart base class which is property aware, that is, can handle property change notification events
 * All our ConnectionEditPart are subclasses of this
 * @author Serge Rider
 */
public abstract class PropertyAwareConnectionPart extends AbstractConnectionEditPart implements PropertyChangeListener, DBPNamedObject
{
    @NotNull
	@Override
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