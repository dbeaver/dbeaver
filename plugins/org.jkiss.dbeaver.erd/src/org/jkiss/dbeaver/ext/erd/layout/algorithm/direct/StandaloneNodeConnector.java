/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout.algorithm.direct;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.graph.*;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Creates dummy edges between nodes, to be used with NodeJoiningDirectedGraphLayout
 *
 * @author Serge Rieder
 */
public class StandaloneNodeConnector {

    private AbstractGraphicalEditPart diagram;

    private NodeList nodeList;
    private EdgeList edgeList;

    public StandaloneNodeConnector(AbstractGraphicalEditPart diagram)
    {
        this.diagram = diagram;
    }

    //private boolean cleanNextTime = false;

    public void visit(DirectedGraph graph)
    {
        this.nodeList = graph.nodes;
        this.edgeList = graph.edges;

        setDummyEdges();
    }

    protected void setDummyEdges()
    {
        //if node count is only one then we don't have to worry about whether
        // the nodes are connected
        if (nodeList.size() > 1) {

            // Order nodes by their connections count
            Collections.sort(nodeList, new Comparator<Node>() {
                public int compare(Node o1, Node o2)
                {
                    return (o2.outgoing.size() + o2.incoming.size()) - (o1.outgoing.size() + o1.incoming.size());
                }
            });

            // Find unconnected nodes
            List<Node> unconnectedNodes = new ArrayList<Node>();
            for (int i = 0; i < nodeList.size(); i++) {
                Node sourceNode = (Node) nodeList.get(i);
                if (sourceNode.outgoing.size() + sourceNode.incoming.size() == 0) {
                    unconnectedNodes.add(sourceNode);
                }
            }

            final int nodeCount = unconnectedNodes.size();
            if (nodeCount > 1) {
                // Order unconnected nodes by their geometrical size
                Collections.sort(unconnectedNodes, new Comparator<Node>() {
                    public int compare(Node o1, Node o2)
                    {
                        if (o1.data instanceof EntityPart && o2.data instanceof EntityPart) {
                            return ((EntityPart) o1.data).getFigure().getSize().height - ((EntityPart) o2.data).getFigure().getSize().height;
                        } else {
                            return 0;
                        }
                    }
                });

                // Connect all unconnected nodes between each other
                final Point diagramSize = diagram.getViewer().getControl().getSize();
                double horizontalRatio = (float)diagramSize.x / (float)diagramSize.y;
                double middleRowSize = Math.sqrt(nodeCount);

                int nodesInLine = (int)(middleRowSize * horizontalRatio) + 1;//(int)Math.sqrt(nodeCount) + 1;

                for (int i = 0; i < nodeCount; i++) {
                    for (int k = 0; k < nodesInLine - 1 && i < nodeCount - 1; k++, i++) {
                        Node sourceNode = unconnectedNodes.get(i);
                        Node targetNode = unconnectedNodes.get(i + 1);

                        Edge edge = new Edge(null, sourceNode, targetNode);
                        edge.weight = 2;
                        edgeList.add(edge);
                    }
                }
            }

        }
    }

}