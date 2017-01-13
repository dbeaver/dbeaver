package org.jkiss.dbeaver.ui.views.lock;

import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;

public class LockGraphNodeEditPart extends AbstractGraphicalEditPart {
	
	private LockGraphConnectionAnchor sourceAnchor;

	private LockGraphConnectionAnchor targetAnchor;

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	protected IFigure createFigure() {
		LockGraphNode node = (LockGraphNode) getModel(); 
		LockGraph graph = (LockGraph)((LockGraphEditPart) getParent()).getModel();
		LockGraphNodeFigure nodeFigure = new LockGraphNodeFigure(node.getTitle(),(node == graph.getSelection()));
		this.targetAnchor = new LockGraphConnectionAnchor(nodeFigure);
		this.sourceAnchor = new LockGraphConnectionAnchor(nodeFigure);
		return nodeFigure;
	}

	@Override
	protected List<LockGraphEdge> getModelSourceConnections() {
		return ((LockGraphNode) getModel()).getSourceEdges();
	}

	@Override
	protected List<LockGraphEdge> getModelTargetConnections() {
		return ((LockGraphNode) getModel()).getTargetEdges();
	}

	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		return this.sourceAnchor;
	}

	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return this.sourceAnchor;
	}

	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		return this.targetAnchor;
	}

	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return this.targetAnchor;
	}

	@Override
	protected void refreshVisuals() {
		LockGraphNode node = (LockGraphNode) getModel();
		LockGraph lgraph = (LockGraph)((LockGraphEditPart) getParent()).getModel();
		LockGraphNodeFigure nodeFigure = (LockGraphNodeFigure) getFigure();
		LockGraphEditPart graph = (LockGraphEditPart) getParent();
        GridData gridData = new GridData(55,30);		
		gridData.horizontalAlignment = GridData.CENTER;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.verticalSpan = 10;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		int span = lgraph.getMaxWidth() / node.getSpan();
		int spanMod =  lgraph.getMaxWidth() % node.getSpan();
		gridData.horizontalSpan = 0 ;
		if (span > 1 && node.getLevelPosition() != LockGraphNode.LevelPosition.RIGHT) {
			gridData.horizontalSpan =  span;
		} else if (spanMod > 0 && node.getLevelPosition() == LockGraphNode.LevelPosition.RIGHT) {
			gridData.horizontalSpan =  span + spanMod;
		}
		graph.setLayoutConstraint(this, nodeFigure,gridData);
	}

	@Override
	protected void createEditPolicies() {
		SelectionPolicy selectionPolicy = new SelectionPolicy();
		selectionPolicy.setDragAllowed(false);
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, selectionPolicy); 
	} 
	
	

}

class SelectionPolicy extends NonResizableEditPolicy {

	@Override
	protected void hideSelection() {
	}

	@Override
	protected void showSelection() {
	
		LockManagerViewer viewer = ((LockGraph)getHost().getParent().getModel()).getLockManagerViewer();
		
		if (viewer != null) {
			viewer.setTableLockSelect(((LockGraphNode)getHost().getModel()).getLock());
		}
		
	}		
}

