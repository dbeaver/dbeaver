/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout.algorithm.direct;

import org.jkiss.dbeaver.Log;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.*;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.jkiss.dbeaver.ext.erd.layout.GraphAnimation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor with support for populating nodes and edges of DirectedGraph
 * from model objects
 *
 * @author Serge Rieder
 */
public class DirectedGraphLayoutVisitor {
    static final Log log = Log.getLog(DirectedGraphLayoutVisitor.class);

    Map<EditPart, Object> partToNodesMap;
    DirectedGraph graph;

    /**
     * Public method for reading graph nodes
     */
    public void layoutDiagram(AbstractGraphicalEditPart diagram)
    {
        partToNodesMap = new IdentityHashMap<>();

        graph = new DirectedGraph();
        graph.setDirection(PositionConstants.EAST);

        addDiagramNodes(diagram);
        if (graph.nodes.size() > 0) {
            addDiagramEdges(diagram);
            try {
                //new CompoundDirectedGraphLayout().visit(graph);
                new NodeJoiningDirectedGraphLayout(diagram).visit(graph);
            } catch (Exception e) {
                log.error("Diagram layout error", e);
            }
            applyDiagramResults(diagram);
        }

    }

    //******************* DiagramPart contribution methods **********/

    protected void addDiagramNodes(AbstractGraphicalEditPart diagram)
    {
        GraphAnimation.recordInitialState(diagram.getFigure());
        //IFigure fig = diagram.getFigure();
        for (Object child : diagram.getChildren()) {
            addEntityNode((NodeEditPart) child);
        }
    }

    /**
     * Adds nodes to the graph object for use by the GraphLayoutAuto
     */
    protected void addEntityNode(NodeEditPart nodeEditPart)
    {
        Node entityNode;
        if (nodeEditPart instanceof EntityPart && ((EntityPart)nodeEditPart).getTable().hasSelfLinks()) {
            entityNode = new Subgraph(nodeEditPart);
        } else {
            entityNode = new Node(nodeEditPart);
        }
        Dimension preferredSize = nodeEditPart.getFigure().getPreferredSize(400, 300);
        entityNode.width = preferredSize.width;
        entityNode.height = preferredSize.height;
        entityNode.setPadding(new Insets(20, 20, 10, 20));
        partToNodesMap.put(nodeEditPart, entityNode);
        graph.nodes.add(entityNode);

        if (entityNode instanceof Subgraph) {
            Node sourceAnchor = new Node("Fake node for source links", (Subgraph) entityNode);
            sourceAnchor.width = 0;
            sourceAnchor.height = 0;

            Node targetAnchor = new Node("Fake node for target links", (Subgraph) entityNode);
            targetAnchor.width = 0;
            targetAnchor.height = 0;
        }

/*
*/
    }

    protected void addDiagramEdges(AbstractGraphicalEditPart diagram)
    {
        for (Object child : diagram.getChildren()) {
            addEntityEdges((GraphicalEditPart) child);
        }
    }

    //******************* Entity contribution methods **********/

    protected void addEntityEdges(GraphicalEditPart entityPart)
    {
        List<?> outgoing = entityPart.getSourceConnections();
        for (int i = 0; i < outgoing.size(); i++) {
            AbstractConnectionEditPart connectionPart = (AbstractConnectionEditPart) entityPart.getSourceConnections().get(i);
            addConnectionEdges(connectionPart);
        }
    }

    //******************* Connection contribution methods **********/

    protected void addConnectionEdges(AbstractConnectionEditPart connectionPart)
    {
        GraphAnimation.recordInitialState((Connection) connectionPart.getFigure());
        Node source = (Node) partToNodesMap.get(connectionPart.getSource());
        Node target = (Node) partToNodesMap.get(connectionPart.getTarget());
        if (source == null || target == null) {
            log.warn("Source or target node not found");
            return;
        }

        if (source instanceof Subgraph && target instanceof Subgraph) {
            source = ((Subgraph) source).members.getNode(0);
            target = ((Subgraph) target).members.getNode(1);
        }

        Edge e = new Edge(connectionPart, source, target);
        e.setPadding(10);
        e.weight = 2;
        graph.edges.add(e);
        partToNodesMap.put(connectionPart, e);
    }

    //******************* DiagramPart apply methods **********/

    protected void applyDiagramResults(AbstractGraphicalEditPart diagram)
    {
        for (Object child : diagram.getChildren()) {
            applyEntityResults((GraphicalEditPart) child);
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

        for (int i = 0; i < entityPart.getSourceConnections().size(); i++) {
            AbstractConnectionEditPart relationship = (AbstractConnectionEditPart) entityPart.getSourceConnections().get(i);
            applyConnectionResults(relationship);
        }
    }

    //******************* Connection apply methods **********/

    protected void applyConnectionResults(AbstractConnectionEditPart connectionPart)
    {

        Edge connEdge = (Edge) partToNodesMap.get(connectionPart);
        NodeList edgeNodes = connEdge.vNodes;

        PolylineConnection conn = (PolylineConnection) connectionPart.getConnectionFigure();
        //conn.setOpaque(true);
        //conn.setLineJoin(SWT.JOIN_BEVEL);
        //conn.setTargetDecoration(new PolygonDecoration());
        if (edgeNodes != null && edgeNodes.size() > 1) {
            List<AbsoluteBendpoint> bends = new ArrayList<>();
            for (int i = 0; i < edgeNodes.size(); i++) {
                Node vn = edgeNodes.getNode(i);
                int x = vn.x;
                int y = vn.y;
                bends.add(new AbsoluteBendpoint(x, y));
/*
				if (connEdge.isFeedback()) {
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
					bends.add(new AbsoluteBendpoint(x, y));
				} else {
					bends.add(new AbsoluteBendpoint(x, y));
					bends.add(new AbsoluteBendpoint(x, y + vn.height));
				}
*/
            }
            conn.setRoutingConstraint(bends);
        }

    }

}