/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.coordinate_assignment;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.old.util.flow.FlowEdge;
import org.jkiss.dbeaver.ext.erd.old.util.flow.FlowGraph;

/**
 * A flow based method.
 *
 * @author harrigan
 */

public class FlowModel {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(BrandesKopf.class);

    public FlowModel(SugiyamaGraph graph, int depth, int xGap, int yGap) {
        log.debug("Flow Based Coordinate Assignment");
        
        FlowGraph g = new FlowGraph(graph.getNodeCount() + graph.getEdgeCount());
        
        int edgeCount = graph.getNodeCount(); // start of the 'edge nodes'
        List<SugiyamaNode> nodes = graph.getNodes();
        for (SugiyamaNode tail : nodes) {
            List<SugiyamaNode> outgoing = graph.getOutgoingNeighbours(tail);
            for (SugiyamaNode head : outgoing) {
                g.insert(new FlowEdge(nodes.indexOf(tail), edgeCount, 0));
                g.insert(new FlowEdge(edgeCount, nodes.indexOf(head), 0));
                edgeCount++;
            }
            
        }

    }

}
