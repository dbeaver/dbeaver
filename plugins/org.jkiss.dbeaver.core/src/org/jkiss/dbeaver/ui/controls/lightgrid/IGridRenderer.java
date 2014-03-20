/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;

public interface IGridRenderer
{

    /**
     * Paints the visual representation of the given value on the given GC. The
     * actual class of the value object is determined by the use of the
     * implementing class.
     * <p>
     * Implementors need to respect the bounds values that may have been
     * specified. The bounds values may affect the x and y values for all
     * drawing operations as well as the width and heights. Implementors may use
     * a <code>Transform</code> to translate the coordinates of all the
     * drawing operations, otherwise they will need to offset each draw.
     * </p>
     * 
     * @param gc GC to paint with
     */
    void paint(GC gc);

    /**
     * Sets the bounds of the drawing.
     * 
     * @param bounds Bounds.
     */
    void setBounds(Rectangle bounds);

    /**
     * Sets the bounds of the drawing.
     * 
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param width Width.
     * @param height Height.
     */
    void setBounds(int x, int y, int width, int height);

    /**
     * Sets the location of the drawing.
     * 
     * @param location Location.
     */
    void setLocation(Point location);

    /**
     * Sets the location of the drawing.
     * 
     * @param x X.
     * @param y Y.
     */
    void setLocation(int x, int y);

    /**
     * Sets focus state.
     * 
     * @param focus focus state.
     */
    void setFocus(boolean focus);

    /**
     * Sets the hover state.
     * 
     * @param hover Hover state.
     */
    void setHover(boolean hover);

    /**
     * Sets the hover state.
     * 
     * @param mouseDown Mouse state.
     */
    void setMouseDown(boolean mouseDown);

    /**
     * Sets the selected state.
     * 
     * @param selected Selection state.
     */
    void setSelected(boolean selected);

    /**
     * Sets the area of the drawing.
     * 
     * @param width Width.
     * @param height Height.
     */
    void setSize(int width, int height);

    /**
     * Sets the area of the drawing.
     * 
     * @param size Size.
     */
    void setSize(Point size);

    public void setCell(GridCell cell);

}