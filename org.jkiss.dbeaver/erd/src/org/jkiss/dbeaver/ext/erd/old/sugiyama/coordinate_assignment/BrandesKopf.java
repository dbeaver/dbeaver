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
 * This step assigns the final x-y coordinates to the nodes. Based on 
 * "Fast and Simple Horizontal Coordinate Assignment", by Brandes and Ko
 *
 * @author harrigan
 */

public class BrandesKopf {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(BrandesKopf.class);

    private static int BK_DOWN = 1;
    
    private static int BK_UP = -1;
    
    private static int BK_LEFT = -1;
    
    private static int BK_RIGHT = -1;
    
    private SugiyamaGraph graph;
    
    private int depth;

    public BrandesKopf(SugiyamaGraph g, int d, int xGap, int yGap) {
        log.debug("Brandes and Kopf Coordinate Assignment");
        
        graph = g;
        depth = d;
        
        // initiate the x-y coordinates
        int y = yGap;

        for (int level = 1; level <= depth; level++) {
            int x = xGap;
            int yIncrement = 0;
            List<SugiyamaNode> nodes = graph.getLevel(level);
            for (SugiyamaNode node : nodes) {
                node.setX(x);
                node.setY(y);
                node.setXDL(x);
                node.setXDR(x);
                node.setXUL(x);
                node.setXUR(x);
                x += node.getSize().width + xGap; 
                if (node.getSize().height > yIncrement) {
                    yIncrement = node.getSize().height;
                }
            }
            y += yIncrement + yGap;
        }
        
        preprocessing();
        
        verticalAlignment(BK_DOWN, BK_LEFT);
        horizontalCompaction(BK_RIGHT);
        
        verticalAlignment(BK_DOWN, BK_RIGHT);
        horizontalCompaction(BK_RIGHT);
        
        verticalAlignment(BK_UP, BK_LEFT);
        horizontalCompaction(BK_RIGHT);
        
        verticalAlignment(BK_UP, BK_RIGHT);
        horizontalCompaction(BK_RIGHT);
        
        alignToSmallestWidth();
        
        balance();
        
        List<SugiyamaNode> nodes = graph.getNodes();
        for (SugiyamaNode node : nodes) {
            if (node.isDummyNode()) {
                node.getEdge().addBend(new Point(node.getX(), node.getY()));
            } else {
                node.setLocation(new Point(node.getX(), node.getY()));
            }
        }
    }
    
    private void preprocessing() {
        // mark type 1 conflicts
        for (int level = 2; level < depth - 1; level++) {
            List<SugiyamaNode> nodes = graph.getLevel(level);
            for (SugiyamaNode node : nodes) {

            }

        }
        
    }
    
    private void verticalAlignment(int vertical, int horizontal) {
        
    }
    
    private void horizontalCompaction (int horizontal) {
        
    }
    
    private void placeBlock() {
        
    }
    
    private void alignToSmallestWidth() {
        
    }
    
    private void balance() {
        
    }

}
