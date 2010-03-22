// $Id: MinCostFlow.java 115 2006-08-08 18:27:36Z harrigan $
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
 * This reduces the general minimum cost flow problem to the st-minimum cost  
 * flow problem by joining all the sources to a single source with infinite 
 * capacity and zero cost and doing likewise with the sinks.
 * @author harrigan
 * 
 */

public class MinCostFlow {
	
    /**
     * Computes a minimum cost flow.
     * @param graph
     * @param sd
     */
    public MinCostFlow(FlowGraph graph) { 
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
                stGraph.insert(new FlowEdge(s, i, FlowEdge.INFINITY, 0));
            } else if (graph.isSink(n)) {
                stGraph.insert(new FlowEdge(i, t, FlowEdge.INFINITY, 0));
            }
           
        }
        
        STMinCostFlow flow = new STMinCostFlow(stGraph, s, t);
    }

}
