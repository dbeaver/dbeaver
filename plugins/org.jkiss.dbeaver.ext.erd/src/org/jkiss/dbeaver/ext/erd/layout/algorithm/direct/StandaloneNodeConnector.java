/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.layout.algorithm.direct;

import org.eclipse.draw2d.graph.*;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.graphics.Point;

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
            // Order unconnected nodes by their geometrical size
            Collections.sort(nodeList, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2)
                {
                    final int connCount1 = o2.outgoing.size() + o2.incoming.size();
                    final int connCount2 = o1.outgoing.size() + o1.incoming.size();
                    if (connCount1 == 0 && connCount1 == connCount2) {
                        if (o1.data instanceof NodeEditPart && o2.data instanceof NodeEditPart) {
                            return ((NodeEditPart) o1.data).getFigure().getMinimumSize().height - ((NodeEditPart) o2.data).getFigure().getMinimumSize().height;
                        } else {
                            return 0;
                        }
                    } else {
                        return connCount1 - connCount2;
                    }
                }
            });

            // Find unconnected nodes
            List<Node> unconnectedNodes = new ArrayList<>();
            for (int i = 0; i < nodeList.size(); i++) {
                Node sourceNode = (Node) nodeList.get(i);
                if (sourceNode.outgoing.size() + sourceNode.incoming.size() == 0) {
                    unconnectedNodes.add(sourceNode);
                }
            }

            final int nodeCount = unconnectedNodes.size();
            if (nodeCount > 1) {
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