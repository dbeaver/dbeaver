/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.model;

import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * ERDNode
 */
public abstract class ERDNode extends DefaultGraphCell {

    protected ERDNode(Object userObject)
    {
        super(userObject);
    }

    public Point getLocation()
    {
        Rectangle2D bounds = GraphConstants.getBounds(getAttributes());
        return bounds == null ? null : new Point((int)bounds.getX(), (int)bounds.getY());
    }

    public void setLocation(Point newLocation)
    {
        Rectangle2D bounds = GraphConstants.getBounds(getAttributes());
        bounds.setRect(newLocation.getX(), newLocation.getY(), bounds.getWidth(), bounds.getHeight());
        GraphConstants.setBounds(getAttributes(), bounds);
    }

    public int getX()
    {
        return (int)GraphConstants.getBounds(getAttributes()).getX();
    }

    public int getY()
    {
        return (int)GraphConstants.getBounds(getAttributes()).getY();
    }

    public int getWidth()
    {
        return (int)GraphConstants.getBounds(getAttributes()).getWidth();
    }

    public int getHeight()
    {
        return (int)GraphConstants.getBounds(getAttributes()).getHeight();
    }

    public Dimension getSize()
    {
        Rectangle2D bounds = GraphConstants.getBounds(getAttributes());
        return bounds == null ?
            new Dimension(1, 1) :
            new Dimension((int)bounds.getWidth(), (int)bounds.getHeight());
    }

    public void setBounds(Rectangle rect)
    {
        GraphConstants.setBounds(getAttributes(), rect);
    }

    public void translate(int dx, int dy)
    {
        Point location = getLocation();
        location.x += dx;
        location.y += dy;
        setLocation(location);
    }

    public abstract String getTipString();

    public abstract String getId();

    public List getEnclosedNodes()
    {
        return null;
    }
}
