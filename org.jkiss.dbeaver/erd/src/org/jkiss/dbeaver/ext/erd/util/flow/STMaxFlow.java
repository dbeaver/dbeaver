// $Id: STMaxFlow.java 108 2006-08-05 10:30:24Z harrigan $
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

import org.jkiss.dbeaver.ext.erd.util.PriorityQueueInt;

/**
 * Given a FlowGraph with a source s and a sink t, find a flow such that no 
 * other flow from s to t has larger value.
 * The Ford-Fulkerson or augmenting-path algorithm:
 *  1. Start with zero flow everywhere.
 *  2. Increase the flow along the maximum-capacity-augmenting-path.
 *  3. Continue until there are no such paths in the network.
 */

public class STMaxFlow {
    
    /**
     * The flow graph.
     */
    private FlowGraph graph;
    
    /**
     * The source vertex.
     */
    private int s;
    
    /**
     * The sink vertex.
     */
    private int t;
    
    /**
     * The weight of each vertex - should we use this vertex in the augmenting 
     * path?
     */
    private int[] weights;
    
    /**
     * The edge that led to this vertex.
     */
    private FlowEdge[] parents;
    
    /**
     * Computes the maximum flow.
     * @param graph
     * @param s
     * @param t
     */
    public STMaxFlow(FlowGraph graph, int s, int t) {
        this.graph = graph;
        this.s = s;
        this.t = t;
        weights = new int[graph.getNodeCount()];
        parents = new FlowEdge[graph.getNodeCount()];
        while (priorityFirstSearch()) {
            augment();
        }
    }
    
    /**
     * Get the vertex that led to this vertex.
     * @param v
     * @return
     */
    private int getParent(int v) {
        return parents[v].getTo(v);
    }
    
    /**
     * Augment the path along the spanning tree.
     * @param s
     * @param t
     */
    private void augment() {
        int min = parents[t].getCapacityResidual(t);
        for (int v = getParent(t); v != s; v = getParent(v))
            if (parents[v].getCapacityResidual(v) < min) {
                min = parents[v].getCapacityResidual(v);
            }
        
        parents[t].addFlowToResidual(t, min);
        for (int v = getParent(t); v != s; v = getParent(v)) {
            parents[v].addFlowToResidual(v, min);
        }
    }
    
    /**
     * A priority first search to find the maximum-capacity-augmenting-path.
     * @return
     */
    private boolean priorityFirstSearch() {
        PriorityQueueInt pQ = new PriorityQueueInt(graph.getNodeCount(), weights);
        for (int v = 0; v < graph.getNodeCount(); v++) {
            weights[v] = 0;
            parents[v] = null;
            pQ.insert(v);
        }
        weights[s] = -FlowEdge.INFINITY;
        pQ.lower(s);  
        while (!pQ.isEmpty()) {
            int v = pQ.getMinimum();
            if (v == t) {
                break;  
            }
            if (v != s && parents[v] == null) {
                break;  
            }
            
            FlowNode n = graph.getNode(v);
            for (FlowEdge e : graph.getIncidentEdges(n)) {
                int w = e.getTo(v);
                int cap = e.getCapacityResidual(w);
                int p = cap < -weights[v] ? cap : -weights[v]; // p = min(cap, cost)
                if (cap > 0 && -p < weights[w]) {
                    weights[w] = -p;
                    pQ.lower(w);
                    parents[w] = e;
                }
            }
        }
        return parents[t] != null;
    }
}
