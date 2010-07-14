// $Id: SweepingMethod.java 128 2006-08-29 13:59:33Z harrigan $
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

package org.jkiss.dbeaver.ext.erd.sugiyama.crossing_reduction;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNodeComparator;

/**
 * This step reduces the number of crossings between edges using a bottom
 * to top level by level sweep and the barycenter heuristic. In future, we
 * may have a choice of such heuristics.
 *
 * @author harrigan
 */

public class SweepingMethod {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SweepingMethod.class);
    
    /**
     * Maximum number of sweeps of any heuristic to reduce crossings.
     */
    private static int MAX_SWEEPS = 5;
    
    private SugiyamaGraph graph;
    
    public SweepingMethod(SugiyamaGraph g, int depth) {
        log.debug("Sweeping crossing reduction.");
        
        this.graph = g;
        
        List<SugiyamaNode> nodes = g.getNodes();
        
        // sweep from bottom to top
        for (int sweep = 0; sweep < MAX_SWEEPS; sweep++) {
            for(int level = 1; level <= depth; level++) {
                int order = 1;
                barycenterHeuristic(graph.getLevel(level));
                Collections.sort(nodes, new SugiyamaNodeComparator());
                for (SugiyamaNode node : nodes) {
                    node.setOrder(order++);
                }
            }
            
            // sweep from top to bottom
            for(int level = depth; level >= 1; level--) {
                int order = 1;
                barycenterHeuristic(graph.getLevel(level));
                Collections.sort(nodes, new SugiyamaNodeComparator());
                for (SugiyamaNode node : nodes) {
                    node.setOrder(order++);
                }
            }
        }
    }
    
    /**
     * The barycenter heuristic reorders the nodes on the changeable level 
     * according to their barycenter weight. Based on
     * "Methods for visual understanding of hierarchical systems", by
     * Sugiyama, Tagawa and Toda, IEEE Trans. Syst. Man Cybern., 
     * SMC-11(2):109-125, 1981.
     * @param level
     */
    private void barycenterHeuristic(List<SugiyamaNode> level) {
        for (SugiyamaNode node : level) {
            List<SugiyamaNode> neighbours = new ArrayList<SugiyamaNode>();
            neighbours.addAll(graph.getIncomingNeighbours(node));
            neighbours.addAll(graph.getOutgoingNeighbours(node));
            
            int total = 0;
            for (SugiyamaNode inner : neighbours) {
                total += inner.getOrder();
            }
            
            if (neighbours.size() > 0) {
                node.setOrder(total / neighbours.size());
            }
        }
    }

}
