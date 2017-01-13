package org.jkiss.dbeaver.ui.views.lock;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;


public class LockGraphEditPartFactory implements EditPartFactory {

	public EditPart createEditPart(EditPart context, Object model) {
		
		EditPart editPart = null;
		
		if (model instanceof LockGraph) {
			
			editPart = new LockGraphEditPart();
			
		} else if (model instanceof LockGraphEdge) {
			
			editPart = new LockGraphEdgeEditPart();
			
		} else if (model instanceof LockGraphNode) {
			
			editPart = new LockGraphNodeEditPart();
			
		}

		if (editPart != null) {
			
			editPart.setModel(model);
		}

		return editPart;
	}
}
