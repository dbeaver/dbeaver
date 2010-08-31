/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

/**
 * Visitor with support for populating nodes and edges of DirectedGraph
 * from model objects
 * @author Serge Rieder
 */
public class DirectedGraphLayoutVisitor
{

	Map<EditPart, Object> partToNodesMap;
	DirectedGraph graph;

	/**
	 * Public method for reading graph nodes
	 */
	public void layoutDiagram(AbstractGraphicalEditPart diagram)
	{

		partToNodesMap = new HashMap<EditPart, Object>();
		
		graph = new DirectedGraph();
        graph.setDirection(PositionConstants.SOUTH);
		addDiagramNodes(diagram);
		if (graph.nodes.size() > 0)
		{	
			addDiagramEdges(diagram);
			new NodeJoiningDirectedGraphLayout().visit(graph);
			applyDiagramResults(diagram);
		}

	}

	//******************* DiagramPart contribution methods **********/

	protected void addDiagramNodes(AbstractGraphicalEditPart diagram)
	{
		GraphAnimation.recordInitialState(diagram.getFigure());
		//IFigure fig = diagram.getFigure();
		for (Object child : diagram.getChildren())
		{
			addEntityNodes((GraphicalEditPart) child);
		}
	}

	/**
	 * Adds nodes to the graph object for use by the GraphLayoutManager
	 */
	protected void addEntityNodes(GraphicalEditPart entityPart)
	{
		Node n = new Node(entityPart);
        Dimension preferredSize = entityPart.getFigure().getPreferredSize(400, 300);
        n.width = preferredSize.width;
		n.height = preferredSize.height;
		n.setPadding(new Insets(10, 8, 10, 12));
		partToNodesMap.put(entityPart, n);
		graph.nodes.add(n);
	}

	protected void addDiagramEdges(AbstractGraphicalEditPart diagram)
	{
		for (Object child : diagram.getChildren())
		{
			addEntityEdges((GraphicalEditPart) child);
		}
	}

	//******************* Entity contribution methods **********/

	protected void addEntityEdges(GraphicalEditPart entityPart)
	{
		List outgoing = entityPart.getSourceConnections();
		for (int i = 0; i < outgoing.size(); i++)
		{
			AbstractConnectionEditPart connectionPart = (AbstractConnectionEditPart) entityPart.getSourceConnections().get(i);
			addConnectionEdges(connectionPart);
		}
	}

	//******************* Connection contribution methods **********/

	protected void addConnectionEdges(AbstractConnectionEditPart connectionPart)
	{
		GraphAnimation.recordInitialState((Connection) connectionPart.getFigure());
		Node source = (Node)partToNodesMap.get(connectionPart.getSource());
		Node target = (Node)partToNodesMap.get(connectionPart.getTarget());
		Edge e = new Edge(connectionPart, source, target);
        e.setPadding(10);
		e.weight = 2;
		graph.edges.add(e);
		partToNodesMap.put(connectionPart, e);
	}

	//******************* DiagramPart apply methods **********/

	protected void applyDiagramResults(AbstractGraphicalEditPart diagram)
	{
		for (Object child : diagram.getChildren())
		{
			applyEntityResults((GraphicalEditPart)child);
		}
	}

	//******************* EntityPart apply methods **********/

	public void applyEntityResults(GraphicalEditPart entityPart)
	{

		Node n = (Node) partToNodesMap.get(entityPart);
		IFigure tableFigure = entityPart.getFigure();

        Dimension preferredSize = tableFigure.getPreferredSize();
        Rectangle bounds = new Rectangle(n.x, n.y, preferredSize.width, preferredSize.height);

		tableFigure.setBounds(bounds);

		for (int i = 0; i < entityPart.getSourceConnections().size(); i++)
		{
			AbstractConnectionEditPart relationship = (AbstractConnectionEditPart) entityPart.getSourceConnections().get(i);
			applyConnectionResults(relationship);
		}
	}

	//******************* Connection apply methods **********/

	protected void applyConnectionResults(AbstractConnectionEditPart connectionPart)
	{

		Edge e = (Edge) partToNodesMap.get(connectionPart);
		NodeList nodes = e.vNodes;

		PolylineConnection conn = (PolylineConnection) connectionPart.getConnectionFigure();
		conn.setTargetDecoration(new PolygonDecoration());
		if (nodes != null)
		{
			List<AbsoluteBendpoint> bends = new ArrayList<AbsoluteBendpoint>();
			for (int i = 0; i < nodes.size(); i++)
			{
				Node vn = nodes.getNode(i);
				int x = vn.x;
				int y = vn.y;
				if (e.isFeedback())
				{
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
					bends.add(new AbsoluteBendpoint(x, y));

				}
				else
				{
					bends.add(new AbsoluteBendpoint(x, y));
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
				}
			}
			conn.setRoutingConstraint(bends);
		}
		else
		{
			conn.setRoutingConstraint(Collections.EMPTY_LIST);
		}

	}

}