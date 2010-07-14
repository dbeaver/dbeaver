//$Id: LongestPath.java 124 2006-08-13 15:06:12Z harrigan $
//Copyright (c) 2006 The Regents of the University of California. All
//Rights Reserved. Permission to use, copy, modify, and distribute this
//software and its documentation without fee, and without a written
//agreement is hereby granted, provided that the above copyright notice
//and this paragraph appear in all copies. This software program and
//documentation are copyrighted by The Regents of the University of
//California. The software program and documentation are supplied "AS
//IS", without any accompanying services from The Regents. The Regents
//does not warrant that the operation of the program will be
//uninterrupted or error-free. The end-user understands that the program
//was developed for research purposes and is advised not to rely
//exclusively on the program for any reason. IN NO EVENT SHALL THE
//UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
//SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
//ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
//THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
//SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
//WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
//PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
//CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
//UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.sugiyama.layer_assignment;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;

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
