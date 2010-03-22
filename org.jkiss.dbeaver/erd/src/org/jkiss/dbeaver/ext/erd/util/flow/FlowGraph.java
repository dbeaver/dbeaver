// $Id: FlowGraph.java 111 2006-08-07 11:00:28Z harrigan $
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

package org.jkiss.dbeaver.ext.erd.util.flow;

import java.util.ArrayList;
import java.util.List;

public class FlowGraph {
    
    private int nodeCount;
    
    private int edgeCount;
    
    private FlowNode adj[];
    
    public FlowGraph(int nodeCount)
    {
        this.nodeCount = nodeCount;
        edgeCount = 0;
        adj = new FlowNode[nodeCount];
    }
    
    public int getNodeCount() {
        return nodeCount;
    }
    
    public int getEdgeCount() {
        return edgeCount;
    }
    
    public void insert(FlowEdge e) {
        adj[e.getTail()] = new FlowNode(e, adj[e.getTail()]);
        edgeCount++;
    }
    
    public FlowNode getNode(int v) {
        return adj[v];
    }
    
    public List<FlowEdge> getIncomingEdges(FlowNode node) {
        List<FlowEdge> edges = new ArrayList<FlowEdge>();
        for (int i = 0; i < nodeCount; i++) {
            FlowNode n = getNode(i);
            while (n != null) {
                FlowEdge e = n.getEdge();
                if (getNode(e.getHead()) == node) {
                    edges.add(e);
                }
                n = n.getNext();
            }
        }
        return edges;
    }
    
    public List<FlowEdge> getOutgoingEdges(FlowNode node) {
        List<FlowEdge> edges = new ArrayList<FlowEdge>();
        FlowNode n = node;
        while (n != null) {
            edges.add(n.getEdge());
            n = n.getNext();
        }
        return edges;
    }
    
    public List<FlowEdge> getIncidentEdges(FlowNode node) {
        List<FlowEdge> edges = new ArrayList<FlowEdge>();
        edges.addAll(getOutgoingEdges(node));
        edges.addAll(getIncomingEdges(node));
        return edges; 
    }
    
    /*
     * TODO: Use counters to keep track of in- and out-degrees.
     */
    
    public int getInDegree(FlowNode node) {
        return getIncomingEdges(node).size();
    }
    
    public int getOutDegree(FlowNode node) {
        return getOutgoingEdges(node).size();
    }
    
    public int getDegree(FlowNode node) {
        return getIncomingEdges(node).size() + getOutgoingEdges(node).size();
    }
    
    public boolean isSource(FlowNode node) {
        return getInDegree(node) == 0;
    }
    
    public boolean isSink(FlowNode node) {
        return getOutDegree(node) == 0;
    }
    
    /**
     * Checks whether the flow satisfies the equilibrium condition.
     * @param s
     * @param t
     * @return
     */
    public boolean checkFlow(int s, int t) {
        for (int v = 0; v < getNodeCount(); v++) {
            if ((v != s) && (v != t)) {
                if (flow(v) != 0) {
                    return false;
                }
            }
        }
        
        if (flow(s) < 0 || flow(s) + flow(t) != 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Compute the net amount of flow at a vertex.
     * @param v
     * @return
     */
    public int flow(int v) {
        int flowValue = 0;
        FlowNode n = adj[v];
        for (FlowEdge e : getIncidentEdges(n)) {
            flowValue += e.isFrom(v) ? e.getFlow() : -e.getFlow();
        }
        return flowValue;
    }
    
    /**
     * Computes the total cost of the flow.
     * @return
     */
    public int getCost() {
        int costValue = 0;
        for (int i = 0; i < getNodeCount(); i++) {
            FlowNode n = adj[i];
            for (FlowEdge e : getOutgoingEdges(n)) {
                costValue += e.getFlow() * e.getCost();
            }
        }
        return costValue;
    }
}

