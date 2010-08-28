/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.crossing_reduction;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNodeComparator;

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
