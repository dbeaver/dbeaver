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

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraph;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphEdge;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphNode;

import java.util.List;

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
		LockGraph graph = (LockGraph) getParent().getModel();
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

	static class SelectionPolicy extends NonResizableEditPolicy {

		@Override
		protected void hideSelection() {
		}

		@Override
		protected void showSelection() {

/*
			LockManagerViewer viewer = ((LockGraph)getHost().getParent().getModel()).getLockManagerViewer();

			if (viewer != null) {
				viewer.setTableLockSelect(((LockGraphNode)getHost().getModel()).getLock());
			}
*/

		}
	}

}


