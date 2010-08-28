/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Any layouter for any diagram type should implement this
 * interface.
 */
public interface Layouter {

    /**
     * Add another object to the diagram.
     *
     * @param obj represents the object to be part of the diagram.
     */
    public void add(LayouterObject obj);

    /**
     * Get all the layouted objects from this diagram.
     *
     * @return An array with the layouted objects of this Layouter.
     */
    public List getObjects();

    /**
     * This operation starts the actual layout process.
     */
    public void layout();

    /**
     * Get the total bounds of all objects in the layouter
     * @return the bounds of the layouter
     */
    public Rectangle getBounds();
    
    public void translate(int dx, int dy);
    
    public void setLocation(Point point);
}
