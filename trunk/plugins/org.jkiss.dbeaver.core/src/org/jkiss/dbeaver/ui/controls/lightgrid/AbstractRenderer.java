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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Abstract grid renderer
 */
public abstract class AbstractRenderer implements IGridRenderer
{
    protected final LightGrid grid;

    protected GridCell cell;

    /** Hover state. */
    private boolean hover;

    /** Renderer has focus. */
    private boolean focus;

    /** Mouse down on the renderer area. */
    private boolean mouseDown;

    /** Selection state. */
    private boolean selected;

    /** The bounds the renderer paints on. */
    protected final Rectangle bounds = new Rectangle(0, 0, 0, 0);

    protected AbstractRenderer(LightGrid grid) {
        this.grid = grid;
    }

    public GridCell getCell()
    {
        return cell;
    }

    @Override
    public void setCell(GridCell cell)
    {
        this.cell = cell;
    }

    /**
     * Returns the bounds.
     * 
     * @return Rectangle describing the bounds.
     */
    public Rectangle getBounds()
    {
        return bounds;
    }

    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        this.bounds.x = x;
        this.bounds.y = y;
        this.bounds.width = width;
        this.bounds.height = height;
    }


    @Override
    public void setBounds(Rectangle bounds)
    {
        this.bounds.x = bounds.x;
        this.bounds.y = bounds.y;
        this.bounds.width = bounds.width;
        this.bounds.height = bounds.height;
    }

    /**
     * Returns the size.
     * 
     * @return size of the renderer.
     */
    public Point getSize()
    {
        return new Point(bounds.width, bounds.height);
    }

    @Override
    public void setLocation(int x, int y)
    {
        setBounds(x, y, bounds.width, bounds.height);
    }

    @Override
    public void setLocation(Point location)
    {
        setBounds(location.x, location.y, bounds.width, bounds.height);
    }

    @Override
    public void setSize(int width, int height)
    {
        setBounds(bounds.x, bounds.y, width, height);
    }

    @Override
    public void setSize(Point size)
    {
        setBounds(bounds.x, bounds.y, size.x, size.y);
    }

    /**
     * Returns a boolean value indicating if this renderer has focus.
     * 
     * @return True/false if has focus.
     */
    public boolean isFocus()
    {
        return focus;
    }

    @Override
    public void setFocus(boolean focus)
    {
        this.focus = focus;
    }

    /**
     * Returns the hover state.
     * 
     * @return Is the renderer in the hover state.
     */
    public boolean isHover()
    {
        return hover;
    }

    @Override
    public void setHover(boolean hover)
    {
        this.hover = hover;
    }

    /**
     * Returns the boolean value indicating if the renderer has the mouseDown
     * state.
     * 
     * @return mouse down state.
     */
    public boolean isMouseDown()
    {
        return mouseDown;
    }

    @Override
    public void setMouseDown(boolean mouseDown)
    {
        this.mouseDown = mouseDown;
    }

    /**
     * Returns the boolean state indicating if the selected state is set.
     * 
     * @return selected state.
     */
    public boolean isSelected()
    {
        return selected;
    }

    @Override
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    /**
     * Sets the display for the renderer.
     * 
     * @return Returns the display.
     */
    public Display getDisplay()
    {
        return grid.getDisplay();
    }

}
