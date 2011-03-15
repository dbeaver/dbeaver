/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout.algorithm.direct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.*;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.jkiss.dbeaver.ext.erd.layout.GraphAnimation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.*;

/**
 * Visitor with support for populating nodes and edges of DirectedGraph
 * from model objects
 *
 * @author Serge Rieder
 */
public class DirectedGraphLayoutVisitor {
    static final Log log = LogFactory.getLog(DirectedGraphLayoutVisitor.class);

    Map<EditPart, Object> partToNodesMap;
    DirectedGraph graph;

    /**
     * Public method for reading graph nodes
     */
    public void layoutDiagram(AbstractGraphicalEditPart diagram)
    {
        partToNodesMap = new IdentityHashMap<EditPart, Object>();

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
            addEntityNode((EntityPart) child);
        }
    }

    /**
     * Adds nodes to the graph object for use by the GraphLayoutAuto
     */
    protected void addEntityNode(EntityPart entityPart)
    {
        Node entityNode;
        if (entityPart.getTable().hasSelfLinks()) {
            entityNode = new Subgraph(entityPart);
        } else {
            entityNode = new Node(entityPart);
        }
        Dimension preferredSize = entityPart.getFigure().getPreferredSize(400, 300);
        entityNode.width = preferredSize.width;
        entityNode.height = preferredSize.height;
        entityNode.setPadding(new Insets(10, 8, 10, 12));
        partToNodesMap.put(entityPart, entityNode);
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
        //conn.setTargetDecoration(new PolygonDecoration());
        if (edgeNodes != null) {
            List<AbsoluteBendpoint> bends = new ArrayList<AbsoluteBendpoint>();
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
        } else if (connEdge.source.getParent() != null && connEdge.source.getParent() == connEdge.target.getParent()) {
            // Self link
            //EntityPart entity = (EntityPart) connEdge.source.getParent().data;
            //final Dimension entitySize = entity.getFigure().getSize();
            int entityWidth = connEdge.source.getParent().width;
            int entityHeight = connEdge.source.getParent().height;

            List<RelativeBendpoint> bends = new ArrayList<RelativeBendpoint>();
            {
                RelativeBendpoint bp1 = new RelativeBendpoint(conn);
                bp1.setRelativeDimensions(new Dimension(entityWidth, entityHeight / 2), new Dimension(entityWidth / 2, entityHeight / 2));
                bends.add(bp1);
            }
            {
                RelativeBendpoint bp2 = new RelativeBendpoint(conn);
                bp2.setRelativeDimensions(new Dimension(-entityWidth, entityHeight / 2), new Dimension(entityWidth, entityHeight));
                bends.add(bp2);
            }
            conn.setRoutingConstraint(bends);

            //conn.setSourceAnchor(new EllipseAnchor(entity.getFigure()));
            //conn.setTargetAnchor(new EllipseAnchor(entity.getFigure()));
        } else {
            conn.setRoutingConstraint(Collections.EMPTY_LIST);
        }

    }

}