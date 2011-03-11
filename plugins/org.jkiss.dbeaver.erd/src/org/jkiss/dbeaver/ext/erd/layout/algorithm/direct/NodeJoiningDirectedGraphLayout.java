/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 16, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout.algorithm.direct;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;

/**
 * Extended version of DirectedGraphLayout which allows DirectedGraphLayout
 * functionality to be used even when graph nodes either have no edges, or when part
 * of clusters isolated from other clusters of Nodes
 * 
 * @author Serge Rieder
 */
public class NodeJoiningDirectedGraphLayout extends DirectedGraphLayout
{

	/**
	 * @param graph public method called to handle layout task
	 */
	public void visit(DirectedGraph graph)
	{
		//add dummy edges so that graph does not fall over because some nodes
		// are not in relationships
		new DummyEdgeCreator().visit(graph);
		
		// create edges to join any isolated clusters
        // TODO: investigate - cluster edges makes diagram ugly
        // TODO: what the reason to do it???
		//new ClusterEdgeCreator().visit(graph);

		super.visit(graph);
	}

}