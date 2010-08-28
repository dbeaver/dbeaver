/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */rithm to detect
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