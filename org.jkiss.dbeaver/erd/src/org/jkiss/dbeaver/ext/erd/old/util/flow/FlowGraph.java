/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */ntEdges(FlowNode node) {
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

