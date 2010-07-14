// $Id: FlowModel.java 129 2006-08-29 16:21:02Z harrigan $
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

package org.jkiss.dbeaver.ext.erd.sugiyama.layer_assignment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.util.flow.FlowGraph;

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
