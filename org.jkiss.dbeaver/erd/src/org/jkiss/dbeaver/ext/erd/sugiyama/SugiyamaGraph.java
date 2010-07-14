// $Id: SugiyamaGraph.java 129 2006-08-29 16:21:02Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.sugiyama;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.model.ERDLink;
import org.jkiss.dbeaver.ext.erd.model.ERDNode;

public class SugiyamaGraph {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SugiyamaGraph.class);
    
    /**
     * A map of lists of SugiyamaNodes.
     */
    private HashMap<SugiyamaNode, List<SugiyamaNode>> adjList;
    
    /**
     * A map of FigNodes to SugiyamaNodes.
     */
    private HashMap<ERDNode, SugiyamaNode> figNodes;
    
    /**
     * Construct a graph.
     */
    public SugiyamaGraph() {
        adjList = new HashMap<SugiyamaNode, List<SugiyamaNode>>();
        figNodes = new HashMap<ERDNode, SugiyamaNode>();
    }
    
    /**
     * Adds a node to the graph we want to layout.
     * @param aNode
     */
    public void addNode(SugiyamaNode aNode) {
        if (!adjList.containsKey(aNode)) {
            adjList.put(aNode, new ArrayList<SugiyamaNode>());
            figNodes.put(aNode.getNode(), aNode);
        }
    }
    
    /**
     * Adds an edge to the graph we want to layout.
     * @param anEdge
     */
    public void addEdge(SugiyamaEdge anEdge) {
        if (adjList.containsKey(getTail(anEdge)) && adjList.containsKey(getHead(anEdge))) { 
            List<SugiyamaNode> adj = adjList.get(getTail(anEdge));
            SugiyamaNode head = getHead(anEdge);
            if (!adj.contains(head)) {
                adj.add(head);
            }
        }
    }
    
    /**
     * Inserts dummy nodes along an edge.
     * @param anEdge
     */
    public void subdivideEdge(SugiyamaEdge anEdge, int span) {
        List<SugiyamaNode> adj = adjList.get(getTail(anEdge));
        adj.remove(getHead(anEdge));
        
        SugiyamaNode prev = getTail(anEdge);
        for(int i = 1; i < span; i++) {
            log.debug("Adding a dummy node");
            
            SugiyamaNode dummy = new SugiyamaNode(anEdge);
            dummy.setLevel(getTail(anEdge).getLevel() - i);
            adjList.get(prev).add(dummy);
            adjList.put(dummy, new ArrayList<SugiyamaNode>());
            prev = dummy;
        }
        
        adjList.get(prev).add(getHead(anEdge));
    }
    
    /**
     * Gets the node in which the edge originates.
     * @param anEdge
     * @return
     */
    public SugiyamaNode getHead(SugiyamaEdge anEdge) {
        ERDLink e = anEdge.getEdge();
        if (e != null) {
            return figNodes.get(e.getTargetNode());
        } else {
            return anEdge.getHead();
        }
    }
    
    /**
     * Gets the node in which the edge terminates.
     */
    public SugiyamaNode getTail(SugiyamaEdge anEdge) {
        ERDLink e = anEdge.getEdge();
        if (e != null) {
            return figNodes.get(e.getSourceNode());
        } else {
            return anEdge.getTail();
        }
    }
    
    /**
     * Get the incoming neighbours of a node.
     */
    public List<SugiyamaNode> getIncomingNeighbours(SugiyamaNode aNode) {
        List<SugiyamaNode> incoming = new ArrayList<SugiyamaNode>();
        for (SugiyamaNode node : adjList.keySet()) {
            if (adjList.get(node).contains(aNode)) {
                incoming.add(node);
            }
        }
        return incoming;
    }
    
    /**
     * Get the outgoing neighbours of a node.
     * @param aNode
     * @return
     */
    public List<SugiyamaNode> getOutgoingNeighbours(SugiyamaNode aNode) {
        return adjList.get(aNode);
    }
    
    /**
     * Get the incoming and outgoing neighbours of a node.
     * @param aNode
     * @return
     */
    public List<SugiyamaNode> getNeighbours(SugiyamaNode aNode) {
        List<SugiyamaNode> neighbours = new ArrayList<SugiyamaNode>();
        neighbours.addAll(getIncomingNeighbours(aNode));
        neighbours.addAll(getOutgoingNeighbours(aNode));
        return neighbours;
    }
    
    /**
     * Get the number of incoming neighbours of a node.
     * @param aNode
     * @return
     */
    public int getIndegree(SugiyamaNode aNode) {
        return getIncomingNeighbours(aNode).size();
    }
    
    /**
     * Get the number of incoming neighbours of a node.
     * @param aNode
     * @return
     */
    public int getOutdegree(SugiyamaNode aNode) {
        return getOutgoingNeighbours(aNode).size();
    }
    
    /**
     * Get the number of incoming and outgoing neighbours of a node.
     * @param aNode
     * @return
     */
    public int getDegree(SugiyamaNode aNode) {
        return getIndegree(aNode) + getOutdegree(aNode);
    }
    
    /**
     * Get all the nodes (including dummies).
     * @return
     */
    public List<SugiyamaNode> getNodes() {
        return new ArrayList<SugiyamaNode>(adjList.keySet());
    }
    
    /**
     * Get the number of nodes in the graph.
     * @return
     */
    public int getNodeCount() {
        return adjList.keySet().size();
    }
    
    /**
     * Get the number of edges in the graph.
     * @return
     */
    public int getEdgeCount() {
        int count = 0;
        for (SugiyamaNode n : adjList.keySet()) {
            List<SugiyamaNode> nodes = adjList.get(n);
            count += nodes.size();
        }
        return count;
    }
    
    /**
     * Get all nodes whose indegree is zero.
     * @return
     */
    public List<SugiyamaNode> getSources() {
        List<SugiyamaNode> sources = new ArrayList<SugiyamaNode>();
        for (SugiyamaNode node : adjList.keySet()) {
            if (getIndegree(node) == 0) {
                sources.add(node);
            }
        }
        return sources;
    }
    
    /**
     * Get all nodes whose outdegree is zero.
     * @return
     */
    public List<SugiyamaNode> getSinks() {
        List<SugiyamaNode> sinks = new ArrayList<SugiyamaNode>();
        for (SugiyamaNode node : adjList.keySet()) {
            if (getOutdegree(node) == 0) {
                sinks.add(node);
            }
        }
        return sinks;
    }
    
    /**
     * Reverse an edge.
     * @param tail
     * @param head
     */
    public void reverseEdge(SugiyamaNode tail, SugiyamaNode head) {
        adjList.get(head).add(tail);
        adjList.get(tail).remove(head);
    }
    
    /**
     * Get nodes on the given level.
     * @return
     */
    public List<SugiyamaNode> getLevel(int level) {
        int order = 1;
        List<SugiyamaNode> nodes = new ArrayList<SugiyamaNode>();
        for (SugiyamaNode node : adjList.keySet()) {
            if (node.getLevel() == level) {
                nodes.add(node);
                if (node.getOrder() == 0) {
                    node.setOrder(order++);
                }
            }
        }
        Collections.sort(nodes, new SugiyamaNodeComparator());
        return nodes;
    }
    
    /**
     * Mark all nodes the same way.
     * @param marked
     */
    public void setMarked(boolean marked) {
        for (SugiyamaNode node : adjList.keySet()) {
            node.setMarked(marked);
        }
    }
}
