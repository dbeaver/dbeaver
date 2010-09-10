/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.EdgeList;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;

/**
 * Creates dummy edges between nodes, to be used with NodeJoiningDirectedGraphLayout
 * @author Serge Rieder
 */
public class DummyEdgeCreator
{

	NodeList nodeList;
	EdgeList edgeList;
	DirectedGraph graph;

	List<Edge> edgesAdded;
	NodeList candidateList;
	int targetNodeIndex;

	boolean cleanNextTime = false;

	public void visit(DirectedGraph g)
	{
		cleanNextTime = true;
		init(g);
		setDummyEdges();
	}

	/**
	 * @param graph
	 */
	private void init(DirectedGraph graph)
	{

		this.graph = graph;
		this.nodeList = graph.nodes;
		this.edgeList = graph.edges;
		edgesAdded = new ArrayList<Edge>();

	}

	protected void setDummyEdges()
	{

		Node targetNode;

		int nodeCount = nodeList.size();

		boolean addedEdge = false;

		//if node count is only one then we don't have to worry about whether
		// the nodes are connected
		if (nodeCount > 1)
		{
			for (Iterator iter = nodeList.iterator(); iter.hasNext();)
			{
				Node sourceNode = (Node) iter.next();

				//we will need to set up a dummy relationship for any table not
				// in one already
				if (sourceNode.outgoing.size() == 0 && sourceNode.incoming.size() == 0)
				{

					targetNode = findTargetNode(sourceNode);
					Edge edge = newDummyEdge(targetNode, sourceNode);
					edgesAdded.add(edge);

				}
			}
		}
	}

	/**
	 * creates a new dummy edge to be used in the graph
	 */
	private Edge newDummyEdge(Node targetNode, Node sourceNode)
	{
		boolean addedEdge;
		DummyEdgePart edgePart = new DummyEdgePart();
		Edge edge = new Edge(edgePart, sourceNode, targetNode);
		edge.weight = 2;
		edgeList.add(edge);

		targetNode = sourceNode;
		addedEdge = true;
		return edge;
	}

	/**
	 * @return a suitable first table to relate to. Will only be called if there
	 *         are > 1 table
	 */
	private Node findTargetNode(Node cantBeThis)
	{

		if (candidateList == null)
		{

			candidateList = new NodeList();

			boolean relationshipFound = false;

			//first look for set of targets which are already in relationships
			for (Iterator iter = nodeList.iterator(); iter.hasNext();)
			{
				Node element = (Node) iter.next();
				if ((element.incoming.size() + element.outgoing.size()) >= 1)
				{
					candidateList.add(element);
					relationshipFound = true;
				}
			}

			//if none found, then just use the existing set
			if (!relationshipFound)
			{
				candidateList = nodeList;
			}
			// sort the target set with those in fewest relationships coming
			// first
			else
			{

				Comparator<Object> comparator = new Comparator<Object>()
				{

					public int compare(Object o1, Object o2)
					{
						Node t1 = (Node) o1;
						Node t2 = (Node) o2;
						return t1.incoming.size() - (t2.incoming.size());
					}

				};

				try
				{
					Collections.sort(candidateList, comparator);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				//System.out.println("Sorted set: " + candidateList);

			}

		}

		//handle situation where first table is the top of the set - we will
		// want the next one then
		Node toReturn = getNext();
		if (toReturn == cantBeThis)
		{
			toReturn = getNext();
		}
		return toReturn;

	}

	private Node getNext()
	{
		if (targetNodeIndex == candidateList.size() - 1)
			targetNodeIndex = 0;
		else
			targetNodeIndex++;

		return (Node) candidateList.get(targetNodeIndex);
	}

	protected void removeDummyEdges()
	{
		for (Iterator<Edge> iter = edgesAdded.iterator(); iter.hasNext();)
		{
			Edge edge = iter.next();
			edgeList.remove(edge);

		}
	}

}