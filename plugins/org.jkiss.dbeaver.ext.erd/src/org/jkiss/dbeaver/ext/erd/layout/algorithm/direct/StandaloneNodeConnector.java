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
 * @author Serge Rider
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