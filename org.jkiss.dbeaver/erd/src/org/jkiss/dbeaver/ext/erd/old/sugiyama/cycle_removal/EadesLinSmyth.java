/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.cycle_removal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.old.util.PairInt;

/**
 * This step temporarily remove any directed cycles. Based on
 * "A fast and effective heuristic for the feedback arc set problem"
 * Eades, Lin, and Smyth, Inform. Process. Lett. 47 (1993) 319-323.
 *
 * @author harrigan
 */

public class EadesLinSmyth {

    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(EadesLinSmyth.class);

    public EadesLinSmyth(SugiyamaGraph graph)
    {
        log.debug("Heuristic Cycle Removal");

        List<SugiyamaNode> nodes = graph.getNodes();

        List<SugiyamaNode> start = new ArrayList<SugiyamaNode>();
        List<SugiyamaNode> end = new ArrayList<SugiyamaNode>();

        Map<SugiyamaNode, PairInt> degrees = new HashMap<SugiyamaNode, PairInt>();
        List<SugiyamaNode> toRemove = new ArrayList<SugiyamaNode>();

        for (SugiyamaNode node : nodes) {
            log.debug("Adding " + node + " (" + graph.getIndegree(node) + ", " +
                graph.getOutdegree(node) + ")");
            degrees.put(node, new PairInt(graph.getIndegree(node), graph.getOutdegree(node)));
        }

        while (degrees.size() > 0) {
            // add the sinks
            do {
                for (SugiyamaNode node : toRemove) {
                    degrees.remove(node);
                }
                toRemove.clear();
                for (SugiyamaNode node : degrees.keySet()) {
                    PairInt pair = degrees.get(node);
                    if (pair.second() == 0) {
                        end.add(0, node);
                        toRemove.add(node);

                        for (SugiyamaNode tmpNode : graph.getIncomingNeighbours(node)) {
                            if (degrees.containsKey(tmpNode)) {
                                pair = degrees.get(tmpNode);
                                pair.setSecond(pair.second() - 1);
                            }
                        }
                    }
                }
            } while (toRemove.size() > 0);


            // add the sources
            do {
                for (SugiyamaNode tmpNode : toRemove) {
                    degrees.remove(tmpNode);
                }
                toRemove.clear();
                for (SugiyamaNode node : degrees.keySet()) {
                    PairInt pair = degrees.get(node);
                    if (pair.first() == 0) {
                        start.add(node);
                        toRemove.add(node);

                        for (SugiyamaNode tmpNode : degrees.keySet()) {
                            if (degrees.containsKey(tmpNode)) {
                                pair = degrees.get(tmpNode);
                                pair.setFirst(pair.first() - 1);
                            }
                        }
                    }
                }
            } while (toRemove.size() > 0);

            // add the vertex whose outdegree - indegree is greatest
            SugiyamaNode nextNode = null;
            int delta = -degrees.size();

            for (SugiyamaNode node : degrees.keySet()) {
                PairInt pair = degrees.get(node);
                int thisDelta = pair.second() - pair.first();

                if (thisDelta > delta) {
                    delta = thisDelta;
                    nextNode = node;
                }
            }

            if (nextNode != null) {
                start.add(nextNode);
                degrees.remove(nextNode);

                for (SugiyamaNode node : graph.getIncomingNeighbours(nextNode)) {
                    if (degrees.containsKey(node)) {
                        PairInt pair = degrees.get(node);
                        pair.setSecond(pair.second() - 1);
                    }
                }

                for (SugiyamaNode node : graph.getIncomingNeighbours(nextNode)) {
                    if (degrees.containsKey(node)) {
                        PairInt pair = degrees.get(node);
                        pair.setFirst(pair.first() - 1);
                    }
                }
            }
        }

        start.addAll(end);

        List<SugiyamaNode> finished = new ArrayList<SugiyamaNode>();
        List<SugiyamaNode> toReverseTail = new ArrayList<SugiyamaNode>();
        List<SugiyamaNode> toReverseHead = new ArrayList<SugiyamaNode>();
        for (SugiyamaNode node : start) {
            for (SugiyamaNode next : graph.getOutgoingNeighbours(node)) {
                if (finished.contains(next)) {
                    log.debug("Reversing an edge");
                    toReverseTail.add(node);
                    toReverseHead.add(next);
                }
            }
            finished.add(node);
        }
        for (Iterator<SugiyamaNode> itTail = toReverseTail.iterator(),
            itHead = toReverseHead.iterator(); itTail.hasNext() && itHead.hasNext();) {
            graph.reverseEdge(itTail.next(), itHead.next());
        }

    }

}
