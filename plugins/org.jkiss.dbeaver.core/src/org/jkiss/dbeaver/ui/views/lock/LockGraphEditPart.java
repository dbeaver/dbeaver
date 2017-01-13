package org.jkiss.dbeaver.ui.views.lock;

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
