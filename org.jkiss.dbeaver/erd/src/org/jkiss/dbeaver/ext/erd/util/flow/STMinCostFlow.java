// $Id: STMinCostFlow.java 113 2006-08-08 18:13:29Z harrigan $
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

/**
 * Given a flow network with edge costs, find a maximum flow such that no 
 * other maximum flow has lower cost.
 * The cycle-canceling algorithm:
 * 	1. Find a maxflow.
 * 	2. Augment the flow along any negative-cost cycle in the residual network, 
 * 		continuing until none remain.
 * @author harrigan
 *
 */

public class STMinCostFlow {
    
    private FlowGraph graph;
    
    private FlowEdge[] parents;
    
    public STMinCostFlow(FlowGraph graph, int s, int t) {
        this.graph = graph;
        parents = new FlowEdge[graph.getNodeCount()];
        STMaxFlow flow = new STMaxFlow(this.graph, s, t);
        for (int v = negativeCycle(); v != -1; v = negativeCycle()) {
            augment(v, v);
        }
    }
    
    private int getParent(int v) {
        return parents[v].getTo(v);
    }
    
    private void augment(int s, int t) {
        // The arguments s and t intentionally shadow the private members.
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
    
    private int negativeCycle() {
    	// A simple implementation of the Bellman-Ford algorithm to detect 
    	// negative cycles
    	for (int v = 0; v < graph.getNodeCount(); v++) {
    		int sp[] = new int[graph.getNodeCount()]; // sp[w] = sp from v to w
    		for (int w = 0; w < graph.getNodeCount(); w++) {
    			sp[w] = FlowEdge.INFINITY;
    			parents[w] = null;
    		}

    		// Consider the edges in any order, relax along each edge.
    		// Make graph.getNodeCount() such passes.

    		sp[v] = 0;
    		for (int i = 0; i < graph.getNodeCount(); i++) {
    			for (int w = 0; w < graph.getNodeCount(); w++) {
    				if (w != v && parents[w] == null) {
    					continue;
    				}

    				FlowNode n = graph.getNode(w);
    				for (FlowEdge e : graph.getOutgoingEdges(n)) {
    					int x = e.getOther(w);
    					int d = e.getCapacityResidual(x) * e.getCost(x);
    					if (sp[x] > sp[x] + d) { // relax this edge
    						sp[x] = sp[x] + d;
    						parents[x] = e;
    						if (i == graph.getNodeCount() - 1) {
    							return v;
    						}
    					}
    				}
    			}
    		}
    	}
    	return -1;
    }
}