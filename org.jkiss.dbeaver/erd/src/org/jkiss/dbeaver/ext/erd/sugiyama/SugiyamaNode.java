// $Id: SugiyamaNode.java 129 2006-08-29 16:21:02Z harrigan $
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.layout.LayouterNode;
import org.jkiss.dbeaver.ext.erd.model.ERDNode;

public class SugiyamaNode implements LayouterNode {
    
    /**
     * The Logger.
     */
    private static final Log log = LogFactory.getLog(SugiyamaNode.class);
       
    /**
     * A reference to the actual node we are laying out.
     */
    private ERDNode node;
    
    /**
     * A reference to the actual edge the dummy node corresponds to.
     */
    private SugiyamaEdge edge;
    
    /**
     * The level of the node.
     */
    private int level;
    
    /**
     * The order of the node, i.e. within its level.
     */
    private int order;
    
    private boolean marked;
    
    /**
     * Brandes and Kopf variables.
     */
    private int x;
    private int y;
    private int xDL;
    private int XDR;
    private int xUL;
    private int xUR;
    private SugiyamaNode root;
    private SugiyamaNode align;
    private SugiyamaNode sink;
    private double shift;
    
     
    /**
     * Constructs a SugiyamaNode corresponding to an actual node.
     */
    public SugiyamaNode(ERDNode aNode) {
        node = aNode;
        edge = null;
        level = 0;
        order = 0;
    }
    
    /**
     * Constructs a SugiyamaNode that does not exist in the diagram,
     * i.e. a dummy node belonging to an original edge
     */
    public SugiyamaNode(SugiyamaEdge anEdge) {
        node = null;
        edge = anEdge;
        level = 0;
        order = 0;
    }
    
    /**
     * Constructs an empty SugiyamaNode.
     */
    public SugiyamaNode() {
        node = null;
        edge = null;
        level = 0;
        order = 0;
    }
    
    /**
     * Get the actual node.
     */
    public ERDNode getNode() {
        return node;
    }

    /**
     * Get the actual FigEdge.
     */
    public Object getContent() {
        return node;
    }
    
    /**
     * Get the actual edge the dummy node corresponds to.
     */
    public SugiyamaEdge getEdge() {
        return edge;
    }
    
    /**
     * Is this a dummy node added to make the graph proper?
     */
    public boolean isDummyNode() {
        return (node == null);
    }

    
    /**
     * Set level to some integer greater than zero (0 = no level).
     * @param aLevel
     */
    public void setLevel(int aLevel) {
        level = aLevel;
    }
    
    /**
     * Get level.
     * @return
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Set order to some integer greater than zero (0 = no position)
     */
    public void setOrder(int anOrder) {
        order = anOrder;
    }
    
    /**
     * Get order.
     * @return
     */
    public int getOrder() {
        return order;
    }

    /**
     * Get the size of the corresponding FigNode.
     */
    public Dimension getSize() {
        if (node == null) {
            return new Dimension(1, 1);
        }
        return node.getSize();
    }

    /**
     * Get the location of the corresponding FigNode.
     */
    public Point getLocation() {
        return node.getLocation();
    }
    
    public Rectangle getBounds() {
        return new Rectangle(getLocation(), getSize());
    }

    /**
     * Set the location of the corresponding FigNode.
     */
    public void setLocation(Point newLocation) {
        if (node == null) {
            log.warn("Attemtped to translate a dummy node");
        } else {
            node.setLocation(newLocation);
        }
    }
    
    public void translate(int dx, int dy) {
        if (node == null) {
            log.warn("Attemtped to translate a dummy node");
        } else {
            node.translate(dx, dy);
        }
    }
    
    public String toString() {
        if (node == null) {
            return "dummy";
        } else {
            return node.getTipString();
        }
    }

    public SugiyamaNode getAlign() {
        return align;
    }

    public void setAlign(SugiyamaNode align) {
        this.align = align;
    }

    public SugiyamaNode getRoot() {
        return root;
    }

    public void setRoot(SugiyamaNode root) {
        this.root = root;
    }

    public double getShift() {
        return shift;
    }

    public void setShift(double shift) {
        this.shift = shift;
    }

    public SugiyamaNode getSink() {
        return sink;
    }

    public void setSink(SugiyamaNode sink) {
        this.sink = sink;
    }

    public int getXDL() {
        return xDL;
    }

    public void setXDL(int xdl) {
        xDL = xdl;
    }

    public int getXDR() {
        return XDR;
    }

    public void setXDR(int xdr) {
        XDR = xdr;
    }

    public int getXUL() {
        return xUL;
    }

    public void setXUL(int xul) {
        xUL = xul;
    }

    public int getXUR() {
        return xUR;
    }

    public void setXUR(int xur) {
        xUR = xur;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }
}
