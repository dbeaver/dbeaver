/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */ v;
    }
    
    public int getTo(int v) {
        return isFrom(v) ? head : tail;
    }
    
    public int getOther(int v) {
    	return tail == v ? head : tail;
    }
    
    public int getCapacityResidual(int v) {
    	// edges in the residual network that represent backward edges have 
    	// capacity equal to flow
        return isFrom(v) ? flow : capacity - flow;
    }
    
	public int getCost(int v) {
		// edges in the residual network that represent backward edges have 
		// negative cost possibly giving negative-cost cycles
		return isFrom(v) ? -cost : cost;
	}
    
    public void addFlowToResidual(int v, int d) {
        flow += isFrom(v) ? -d : d;
    }
    
    public String toString() {
    	return "(" + getTail() + ", " + getHead() + ")";
    }
}
