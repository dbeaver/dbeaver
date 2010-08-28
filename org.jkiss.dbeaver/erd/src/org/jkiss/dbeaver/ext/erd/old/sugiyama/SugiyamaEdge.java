/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterEdge;
import org.jkiss.dbeaver.ext.erd.old.model.ERDLink;
import org.jkiss.dbeaver.ext.erd.old.model.ERDNode;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class SugiyamaEdge implements LayouterEdge {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SugiyamaEdge.class);
       
    /**
     * A reference to the actual edge.
     */
    private ERDLink edge;
    
    private SugiyamaNode tail;
    
    private SugiyamaNode head;
    
    /**
     * The bends along the edge.
     */
    private List<Point> bends;
    
    /**
     * Brandes and Kopf variables.
     */
    private boolean marked;
    
    /**
     * Constructs a dummy edge.
     */
    public SugiyamaEdge() {
        edge = null;
    }
        
    /**
     * Constructs an edge that wraps a ERDLink.
     * @param anEdge
     */
    public SugiyamaEdge(ERDLink anEdge) {
        edge = anEdge;
        bends = new ArrayList<Point>();
    }
    
    public SugiyamaEdge(SugiyamaNode tail, SugiyamaNode head) {
        this.tail = tail;
        this.head = head;
    }
    
    public SugiyamaNode getTail() {
        return tail;
    }
    
    public SugiyamaNode getHead() {
        return head;
    }

    /**
     * Get the actual ERDLink.
     * @return
     */
    public ERDLink getEdge() {
        return edge;
    }

    /**
     * Get the actual ERDLink.
     * @return
     */
    public Object getContent() {
        return edge;
    }
    
    /**
     * Add a bend.
     */
    public void addBend(Point bend) {
        bends.add(0, bend);
    }
    
    public void route() {
        ERDNode sourceNode = edge.getSourceNode();
        ERDNode destNode = edge.getTargetNode();
        Point[] points;
        int lastPoint;
        if (bends.size() == 0) {
            points = new Point[2];
            points[0] = sourceNode.getLocation();
            points[1] = destNode.getLocation();
            lastPoint = 1;
        } else {
            log.debug("Following edge bends");
            points = new Point[2 + bends.size()];
            points[0] = sourceNode.getLocation();
            points[1 + bends.size()] = destNode.getLocation();
            for (int i = 1; i <= bends.size(); i++) {
                points[i] = bends.get(i - 1);
            }
            lastPoint = 1 + bends.size();
        }
        edge.setPoints(points);
        edge.computeRoute();
        points = edge.getPoints();
        if (points[0].x == points[lastPoint].x) {
            int x1 = sourceNode.getX();
            if (destNode.getX() > x1) {
                x1 = destNode.getX();
            }
            int x2 = sourceNode.getX() + sourceNode.getWidth();
            if (destNode.getX() + destNode.getWidth() < x2) {
                x2 = destNode.getX() + destNode.getWidth();
            }
            points[0].x = (x1 + x2) / 2;
            points[lastPoint].x = (x1 + x2) / 2;
            edge.setPoints(points);
        } else if (points[0].y == points[lastPoint].y) {
            int y1 = sourceNode.getY();
            if (destNode.getY() > y1) {
                y1 = destNode.getY();
            }
            int y2 = sourceNode.getY() + sourceNode.getHeight();
            if (destNode.getY() + destNode.getHeight() < y2) {
                y2 = destNode.getY() + destNode.getHeight();
            }
            points[0].y = (y1 + y2) / 2;
            points[lastPoint].y = (y1 + y2) / 2;
            edge.setPoints(points);
        }
    }
    
    public void translate(int dx, int dy) {
        edge.translate(dx, dy);
    }
    
    
    public String toString() {
        if (edge == null) {
            return "dummy";
        } else {
            return edge.getTipString();
        }
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }
}
