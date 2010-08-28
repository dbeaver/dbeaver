/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.util.flow;

/**
 * This reduces the feasible flow problem where there is a supply/demand at 
 * each vertex to the st-flow problem.
 * @author harrigan
 *
 */

public class FeasibleFlow {
    
    /**
     * Computes a feasible flow.
     * @param graph
     * @param sd
     */
    public FeasibleFlow(FlowGraph graph, int[] sd) { 
        FlowGraph stGraph = new FlowGraph(graph.getNodeCount() + 2);
        for (int v = 0; v < graph.getNodeCount(); v++) {
            FlowNode n = graph.getNode(v);
            while (n != null) {
                stGraph.insert(n.getEdge());
                n = n.getNext();
            }
        } 
        
        int s = graph.getNodeCount();
        int t = graph.getNodeCount() + 1;
        for (int i = 0; i < graph.getNodeCount(); i++) {
            if (sd[i] >= 0) {
                stGraph.insert(new FlowEdge(s, i, sd[i]));
            } else {
                stGraph.insert(new FlowEdge(i, t, -sd[i]));
            }
        }
        
        STMaxFlow flow = new STMaxFlow(stGraph, s, t);
    }
}