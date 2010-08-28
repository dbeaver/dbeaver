/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama.coordinate_assignment;

import java.awt.Point;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode;

/**
 * This step assigns the final x-y coordinates to the nodes.
 *
 * @author harrigan
 */

public class SimpleMethod {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SimpleMethod.class);
    
    public SimpleMethod(SugiyamaGraph graph, int depth, int xGap, int yGap) {
            log.debug("Simple Coordinate Assignment");
            
            int y = yGap;
            
            for (int level = 1; level <= depth; level++) {
                int x = yGap;
                int yIncrement = 0;
                List<SugiyamaNode> nodes = graph.getLevel(level);
                for (SugiyamaNode node : nodes) {
                    if (node.isDummyNode()) {
                        log.debug("Routing edge along dummy node");
                        node.getEdge().addBend(new Point(x, y));
                        x += yGap;
                    } else {
                        if (node.getSize().height > yIncrement) {
                            yIncrement = node.getSize().height;
                        }
                        node.setLocation(new Point(x, y));
                        x += node.getSize().width + yGap; 
                    }
                }
                y += yIncrement + yGap;
            }
    }

}
