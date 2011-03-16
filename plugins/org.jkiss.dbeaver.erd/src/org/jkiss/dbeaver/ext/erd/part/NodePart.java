/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.NodeEditPart;

/**
 * Abstract node part
 */
public abstract class NodePart extends PropertyAwarePart implements NodeEditPart {

    private Rectangle bounds;


    /**
     * @return Returns the bounds.
     */
    public Rectangle getBounds()
    {
        return bounds;
    }

    /**
     * Sets bounds without firing off any event notifications
     *
     * @param bounds
     *            The bounds to set.
     */
    public void setBounds(Rectangle bounds)
    {
        this.bounds = bounds;
    }

    /**
     * If modified, sets bounds and fires off event notification
     *
     * @param bounds
     *            The bounds to set.
     */
    public void modifyBounds(Rectangle bounds)
    {
        Rectangle oldBounds = this.bounds;
        if (!bounds.equals(oldBounds))
        {
            this.bounds = bounds;

            Figure entityFigure = (Figure) getFigure();
            DiagramPart parent = (DiagramPart) getParent();
            parent.setLayoutConstraint(this, entityFigure, bounds);
        }
    }

}
