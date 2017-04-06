/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.ui.locks.graph;

import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class LockGraphEditPart extends AbstractGraphicalEditPart {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE,
				new LockGraphXYLayoutEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		FreeformLayer freeformLayer = new FreeformLayer();
		freeformLayer.setLayoutManager(new GridLayout(((LockGraph) getModel()).getMaxWidth(), true));
		return freeformLayer;
	}

	@Override
	protected List<LockGraphNode> getModelChildren() {
		List<LockGraphNode> nodes = ((LockGraph) getModel()).getNodes();
		return nodes;
	}

	@Override
	protected void refreshVisuals() {
		ConnectionLayer connectionLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer.setConnectionRouter(new ShortestPathConnectionRouter(getFigure()));
	} 
	
	
}
