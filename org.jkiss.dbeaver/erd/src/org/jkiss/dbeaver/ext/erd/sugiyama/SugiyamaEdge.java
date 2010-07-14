// $Id: SugiyamaEdge.java 129 2006-08-29 16:21:02Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
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

package org.jkiss.dbeaver.ext.erd.sugiyama;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.layout.LayouterEdge;
import org.jkiss.dbeaver.ext.erd.model.ERDLink;
import org.jkiss.dbeaver.ext.erd.model.ERDNode;

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
