/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.layout.Layouter;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterContainer;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterFactory;
import org.jkiss.dbeaver.ext.erd.old.model.ERDNode;

/**
 * A SugiyamaContainer is a specialization of SugiyamaNode that can contain its
 * own subset of node and edges to lay out.
 */
public class SugiyamaContainer extends SugiyamaNode implements LayouterContainer {

    private static final Log log = LogFactory.getLog(SugiyamaContainer.class);
    
    private Layouter layouter;
    
    private int topBorder;
    private int bottomBorder; 
    private int leftBorder;
    private int rightBorder;

    public SugiyamaContainer(
            ERDNode figNode,
            LayouterFactory 
            layouterFactory, 
            int topBorder, 
            int bottomBorder, 
            int leftBorder, 
            int rightBorder) {
        super(figNode);
        layouter = layouterFactory.createLayouter(figNode.getEnclosedNodes());
        log.debug("Laying out package contents (" + layouter.getObjects().size() + ") with " +layouter.getClass().getName());
        layouter.layout();
        this.topBorder = topBorder;
        this.bottomBorder = bottomBorder; 
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
        resize();
    }

    /**
     * @see org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode#translate(int, int)
     */
    public void translate(int dx, int dy) {
        super.translate(dx, dy);
        layouter.translate(dx, dy);
    }
    
    /**
     * @see org.jkiss.dbeaver.ext.erd.old.sugiyama.SugiyamaNode#translate(int, int)
     */
    public void setLocation(Point point) {
        Point oldPoint = getLocation();
        int dx = point.x - oldPoint.x;
        int dy = point.y - oldPoint.y;
        super.setLocation(point);
        layouter.translate(dx, dy);
    }

    public List getObjects() {
        return layouter.getObjects();
    }

    public void resize() {
        Rectangle rect = layouter.getBounds();
        if (rect != null) {
            rect.x -= leftBorder;
            rect.y -= topBorder;
            rect.width += leftBorder + rightBorder;
            rect.height += topBorder + bottomBorder;
            getNode().setBounds(rect);
        }
    }
}
