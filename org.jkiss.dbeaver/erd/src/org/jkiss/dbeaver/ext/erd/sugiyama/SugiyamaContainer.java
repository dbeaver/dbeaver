// $Id: SugiyamaContainer.java 106 2006-08-05 03:32:23Z bobtarling $
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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.layout.Layouter;
import org.jkiss.dbeaver.ext.erd.layout.LayouterContainer;
import org.jkiss.dbeaver.ext.erd.layout.LayouterFactory;
import org.jkiss.dbeaver.ext.erd.model.ERDNode;

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
     * @see org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode#translate(int, int)
     */
    public void translate(int dx, int dy) {
        super.translate(dx, dy);
        layouter.translate(dx, dy);
    }
    
    /**
     * @see org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode#translate(int, int)
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
