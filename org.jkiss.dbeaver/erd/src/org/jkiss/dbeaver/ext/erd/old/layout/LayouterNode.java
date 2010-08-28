/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

import java.awt.*;

/**
 * This interface has to be implemented by layouted nodes in
 * diagrams (i.e. classes or interfaces in a classdiagram).
 */
public interface LayouterNode extends LayouterObject {

    /**
     * Operation getSize returns the size of this node.
     *
     * @return The size of this node.
     */
    public Dimension getSize();

    /**
     * Operation getLocation returns the current location of
     * this node.
     *
     * @return The location of this node.
     */
    public Point getLocation();

    /**
     * Operation getBounds returns the current bounds of
     * this node.
     *
     * @return The location of this node.
     */
    public Rectangle getBounds();
    
    /**
     * Operation setLocation sets a new location for this
     * node.
     *
     * @param newLocation represents the new location for this node.
     */
    public void setLocation(Point newLocation);
}
