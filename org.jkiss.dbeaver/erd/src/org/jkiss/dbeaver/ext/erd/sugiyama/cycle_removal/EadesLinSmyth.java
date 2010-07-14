// $Id: EadesLinSmyth.java 128 2006-08-29 13:59:33Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
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

package org.jkiss.dbeaver.ext.erd.sugiyama.cycle_removal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.util.PairInt;

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
