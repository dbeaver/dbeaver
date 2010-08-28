/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.util.flow;

/**
 * This reduces the general maximum flow problem to the st-maximum flow 
 * problem by joining all the sources to a single source with infinite 
 * capacity and doing likewise with the sinks.
 * @author harrigan
 * 
 */

public class MaxFlow {
    
    /**
     * Computes a maximum flow.
     * @param graph
     * @param sd
     */
    public MaxFlow(FlowGraph graph) { 
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
            FlowNode n = graph.getNode(i);
            if (graph.isSource(n)) {
                stGraph.insert(new FlowEdge(s, i, FlowEdge.INFINITY));
            } else if (graph.isSink(n)) {
                stGraph.insert(new FlowEdge(i, t, FlowEdge.INFINITY));
            }
           
        }
        
        STMaxFlow flow = new STMaxFlow(stGraph, s, t);
    }
}