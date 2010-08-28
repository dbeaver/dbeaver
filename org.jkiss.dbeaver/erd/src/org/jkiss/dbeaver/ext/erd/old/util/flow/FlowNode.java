/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.util.flow;

public class FlowNode {
    
    private FlowEdge e;
    
    private FlowNode next;
    
    public FlowNode(FlowEdge e, FlowNode n) {
        this.e = e;
        next = n;
    }
    
    public FlowNode getNext() {
        return next;
    }
    
    public FlowEdge getEdge() {
        return e;
    }
}
