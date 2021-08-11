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

import org.eclipse.gef3.EditPart;
import org.eclipse.gef3.EditPartFactory;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraph;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphEdge;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphNode;


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
