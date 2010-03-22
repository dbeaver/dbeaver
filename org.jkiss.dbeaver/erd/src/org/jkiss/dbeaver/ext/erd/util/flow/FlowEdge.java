// $Id: FlowEdge.java 111 2006-08-07 11:00:28Z harrigan $
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
 * FlowEdge implements the residual flow graph.
 */
public class FlowEdge {
    
    /**
     * The highest/lowest priority allowed on the priority queue.
     */
    public static int INFINITY = Integer.MAX_VALUE;
    
    private int tail;
    
    private int head;
    
    private int capacity;
    
    private int flow;
    
    private int cost;
    
    /**
     * Constructs a FlowEdge and sets the amount of flow to zero.
     * @param tail
     * @param head
     * @param capacity
     */
    public FlowEdge(int tail, int head, int capacity) {
        this.tail = tail;
        this.head = head;
        this.capacity = capacity;
        flow = 0;
        cost = 0;
    }
    
    /**
     * Constructs a FlowEdge with a possible cost and sets the amount of flow 
     * to zero.
     * @param tail
     * @param head
     * @param capacity
     * @param cost
     */
    public FlowEdge(int tail, int head, int capacity, int cost) {
        this.tail = tail;
        this.head = head;
        this.capacity = capacity;
        flow = 0;
        this.cost = cost;
    }
    
    public int getTail() {
        return tail;
    }
    
    public int getHead() {
        return head;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public int getFlow() {
        return flow;
    }
    
    public int getCost() {
        return cost;
    }
    
    public boolean isFrom (int v) {
        return tail == v;
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
