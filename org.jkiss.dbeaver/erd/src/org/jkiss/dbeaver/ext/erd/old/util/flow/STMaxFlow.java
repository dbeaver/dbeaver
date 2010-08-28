/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */);
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
