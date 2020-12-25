/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 * Created on Jul 16, 2004
 */
package org.jkiss.dbeaver.erd.ui.layout.algorithm.direct;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

/**
 * Extended version of DirectedGraphLayout which allows DirectedGraphLayout
 * functionality to be used even when graph nodes either have no edges, or when part
 * of clusters isolated from other clusters of Nodes
 * 
 * @author Serge Rider
 */
public class NodeJoiningDirectedGraphLayout extends DirectedGraphLayout
{

    private AbstractGraphicalEditPart diagram;

    public NodeJoiningDirectedGraphLayout(AbstractGraphicalEditPart diagram)
    {
        this.diagram = diagram;
    }

    /**
	 * @param graph public method called to handle layout task
	 */
	@Override
    public void visit(DirectedGraph graph)
	{
		//add dummy edges so that graph does not fall over because some nodes
		// are not in relationships
		new StandaloneNodeConnector(diagram).visit(graph);
		
		// create edges to join any isolated clusters
        // TODO: investigate - cluster edges makes diagram ugly
        // TODO: what the reason to do it???
		//new ClusterEdgeCreator().visit(graph);

		super.visit(graph);
	}

}