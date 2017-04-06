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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockGraphManager;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;

public class LockGraphicalView extends ViewPart {

	private static final LockGraph EMPTY_GRAPH = new LockGraph(null);
	
	private DefaultEditDomain editDomain;

	private GraphicalViewer graphicalViewer;

	
	private final LockGraphManager<?, ?> graphManager;
	
	private final LockManagerViewer viewer;	
	
	public LockGraphicalView(LockManagerViewer viewer) {
		super();
        this.viewer = viewer;
        this.graphManager = viewer.getGraphManager();
	}

	@Override
	public void createPartControl(Composite parent) {
		setEditDomain(new DefaultEditDomain(null));
		setGraphicalViewer(new ScrollingGraphicalViewer());
		getGraphicalViewer().createControl(parent);
		getGraphicalViewer().setRootEditPart(new FreeformGraphicalRootEditPart());
		getGraphicalViewer().setEditPartFactory(new LockGraphEditPartFactory());
		getGraphicalViewer().setContextMenu(new ContextMenuProvider(graphicalViewer){

			@Override
			public void buildContextMenu(IMenuManager menu) {
			    
				menu.add(viewer.getKillAction());
				
			}
			
		});
	}		

	public void drawGraf(DBAServerLock<?> selection)
	{
		if (selection == null) return;
		
		LockGraph g = selection == null ? EMPTY_GRAPH : graphManager.getGraph(selection);
		
		if (g == null) return;
		
		if (g != EMPTY_GRAPH) {
			g.setLockManagerViewer(viewer);
		}

		getGraphicalViewer().setContents(g);
		getGraphicalViewer().getControl().setBackground(ColorConstants.listBackground);
	}
	
	protected DefaultEditDomain getEditDomain() {
		return this.editDomain;
	}

	protected GraphicalViewer getGraphicalViewer() {
		return this.graphicalViewer;
	}

	protected void setEditDomain(DefaultEditDomain anEditDomain) {
		this.editDomain = anEditDomain;
	}

	@Override
	public void setFocus() {
		getGraphicalViewer().getControl().setFocus();
	}

	protected void setGraphicalViewer(GraphicalViewer viewer) {
		getEditDomain().addViewer(viewer);
		this.graphicalViewer = viewer;
	}

}
