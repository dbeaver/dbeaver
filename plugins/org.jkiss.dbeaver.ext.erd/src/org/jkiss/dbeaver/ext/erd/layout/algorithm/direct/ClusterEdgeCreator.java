/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Creates dummy edges between nodes, to be used with NodeJoiningDirectedGraphLayout
 *
 * @author Serge Rider
 */
public class ClusterEdgeCreator {

    //sets up maximum depth of recursion to set up initial cluster list
    private static final int INITIAL_RECURSION_DEPTH = 3;

    NodeList nodeList;
    EdgeList edgeList;
    DirectedGraph graph;

    //List edgesAdded;
    List<Node> encountered = new ArrayList<>();
    List<Cluster> clusters = new ArrayList<>();

    Cluster currentCluster = null;

    public ClusterEdgeCreator() {
        super();
    }

    public void visit(DirectedGraph graph) {

        try {

            this.graph = graph;
            this.nodeList = graph.nodes;
            this.edgeList = graph.edges;
            //edgesAdded = new ArrayList();

            //iterate through all of the nodes in the node list
            for (Iterator<?> iter = nodeList.iterator(); iter.hasNext();) {
                Node node = (Node) iter.next();

                //check whether we have already come across this node
                if (!encountered.contains(node)) {
                    //create a new cluster for this node
                    currentCluster = new Cluster();
                    clusters.add(currentCluster);
                    encountered.add(node);
                    currentCluster.set.add(node);

                    // recursively add any other nodes reachable from it
                    int depth = INITIAL_RECURSION_DEPTH;
                    recursivelyAddToCluster(node, depth);
                }
            }

/*
            for (Iterator<Cluster> iter = clusters.iterator(); iter.hasNext();) {
                Cluster cluster = iter.next();
            }
*/

            coalesceRemainingClusters();

            joinClusters();

        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * If recursion fails to join all the remaining
     */
    private void coalesceRemainingClusters() {
    }

    /**
     * Joins the clusters together
     */
    private void joinClusters() {
        if (clusters.size() > 1) {

            Node sourceNode = null;
            Node targetNode = null;

            //add an edge from each successive cluster to next
            for (Iterator<Cluster> iter = clusters.iterator(); iter.hasNext();) {
                Cluster cluster = iter.next();
                if (sourceNode != null) {
                    //use first node in set as target node
                    targetNode = cluster.set.get(0);
                    newDummyEdge(sourceNode, targetNode);
                }

                //set up source node for the next iteration using last node in
                // set
                sourceNode = cluster.set.get(cluster.set.size() - 1);

            }

        }
    }

    private void recursivelyAddToCluster(Node node, int depth) {

        if (depth > 3) {
            // do nothing
        } else {
            depth++;
            EdgeList incoming = node.incoming;
            for (Iterator<?> iter = incoming.iterator(); iter.hasNext();) {
                Edge edge = (Edge) iter.next();
                Node incomingNode = edge.source;

                if (!encountered.contains(incomingNode)) {
                    encountered.add(incomingNode);
                    currentCluster.set.add(incomingNode);
                    recursivelyAddToCluster(incomingNode, depth);
                }
            }

            EdgeList outgoing = node.outgoing;
            for (Iterator<?> iter = outgoing.iterator(); iter.hasNext();) {
                Edge edge = (Edge) iter.next();
                Node outgoingNode = edge.target;

                if (!encountered.contains(outgoingNode)) {
                    encountered.add(outgoingNode);
                    currentCluster.set.add(outgoingNode);
                    recursivelyAddToCluster(outgoingNode, depth);
                }
            }
        }

    }

    /**
     * creates a new dummy edge to be used in the graph
     */
    private Edge newDummyEdge(Node sourceNode, Node targetNode) {
        //boolean addedEdge;
        DummyEdgePart edgePart = new DummyEdgePart();
        Edge edge = new Edge(edgePart, sourceNode, targetNode);
        edge.weight = 2;

        //add the new edge to the edge list
        edgeList.add(edge);

        //targetNode = sourceNode;
        //addedEdge = true;
        return edge;
    }

    /**
     * Very thin wrapper around List
     */
    private class Cluster {

        List<Node> set = new ArrayList<>();

        public String toString() {
            return set.toString();
        }
    }

}