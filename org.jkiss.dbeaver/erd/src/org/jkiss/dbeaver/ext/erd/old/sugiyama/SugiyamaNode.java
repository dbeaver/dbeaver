/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterNode;
import org.jkiss.dbeaver.ext.erd.old.model.ERDNode;

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
