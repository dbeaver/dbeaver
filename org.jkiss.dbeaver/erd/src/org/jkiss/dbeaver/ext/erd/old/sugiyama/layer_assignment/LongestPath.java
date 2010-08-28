/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.layer_assignment;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;

/**
 * This step assigns the nodes to levels so that all edges are directed
 * uniformly. It uses the Longest-Path algorithm (minimal heigh
 *
 * @author harrigan
 */

public class LongestPath {

    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(LongestPath.class);

    private int depth;

    public LongestPath(SugiyamaGraph graph)
    {
        log.debug("Longest Path Layer Assignment.");

        depth = 0;

        List<SugiyamaNode> toLevel = new ArrayList<SugiyamaNode>();
        List<SugiyamaNode> sinks = graph.getSinks();

        log.debug("Assigning " + sinks.size() + " sinks to the bottom level");
        for (SugiyamaNode node : sinks) {
            node.setLevel(1);
            depth = 1;

            List<SugiyamaNode> incoming = graph.getIncomingNeighbours(node);
            for (SugiyamaNode inc : incoming) {
                if (!toLevel.contains(inc)) {
                    toLevel.add(inc);
                }
            }
        }

        log.debug("Assigning the remaining nodes to levels");
        while (!toLevel.isEmpty()) {
            SugiyamaNode node = toLevel.remove(toLevel.size() - 1);
            if (node.getLevel() > 0) {
                continue;
            }

            int max = 0;
            List<SugiyamaNode> outgoing = graph.getOutgoingNeighbours(node);
            for (SugiyamaNode next : outgoing) {
                if (next.getLevel() > max) {
                    max = next.getLevel();
                }
            }

            node.setLevel(max + 1);
            if (max + 1 > depth) {
                depth = max + 1;
            }

            List<SugiyamaNode> incoming = graph.getIncomingNeighbours(node);
            for (SugiyamaNode inc : incoming) {
                toLevel.add(inc);
            }
        }
    }

    public int getDepth()
    {
        return depth;
    }

}
