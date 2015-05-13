/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
