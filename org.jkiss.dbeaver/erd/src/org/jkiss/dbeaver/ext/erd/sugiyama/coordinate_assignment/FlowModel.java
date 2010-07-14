// $Id: FlowModel.java 128 2006-08-29 13:59:33Z harrigan $
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

package org.jkiss.dbeaver.ext.erd.sugiyama.coordinate_assignment;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.util.flow.FlowEdge;
import org.jkiss.dbeaver.ext.erd.util.flow.FlowGraph;

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
