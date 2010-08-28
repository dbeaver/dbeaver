/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.layer_assignment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.old.util.flow.FlowGraph;

import java.util.ArrayList;
import java.util.List;

public class FlowModel {

    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(FlowModel.class);

    private int depth;

    public FlowModel(SugiyamaGraph graph)
    {
        log.debug("Flow Based Layer Assignment.");

        int numFundamentalCycles = 0;

        // find a set of fundamental cycles
        graph.setMarked(false);
        List<SugiyamaNode> toVisit = new ArrayList<SugiyamaNode>();
        SugiyamaNode prevNode;
        SugiyamaNode currNode = null;
        toVisit.add(graph.getNodes().get(0));

        while (!toVisit.isEmpty()) {
            prevNode = currNode;
            currNode = toVisit.remove(toVisit.size() - 1);
            currNode.setMarked(true);
            List<SugiyamaNode> neighbours = graph.getNeighbours(currNode);
            for (SugiyamaNode nextNode : neighbours) {
                if (nextNode != prevNode) {
                    if (nextNode.isMarked()) {
                        log.debug("I have found a fundamental cycle.");
                        numFundamentalCycles++;
                    } else {
                        if (!toVisit.contains(nextNode)) {
                            toVisit.add(nextNode);
                        }
                    }
                }
            }
        }

        // create the flow graph
        FlowGraph g = new FlowGraph(graph.getEdgeCount() + numFundamentalCycles + 2);

        // add edges from 'edge nodes' to 'fundamental cycle nodes'
        // orient depend on their direction within each cycle

        // add external flow

        // complete the cirulcation

        depth = 0;
    }

    public int getDepth()
    {
        return depth;
    }

}
