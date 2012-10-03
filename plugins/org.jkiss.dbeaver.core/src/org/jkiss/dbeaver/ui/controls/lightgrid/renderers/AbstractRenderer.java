/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA 
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p> 
 * Base implementation of IRenderer. Provides management of a few values.
 * 
 * @author chris.gross@us.ibm.com
 */
public abstract class AbstractRenderer implements IGridRenderer
{
    protected final LightGrid grid;
    /** Display used to create GC to perform painting. */
    private final Display display;

    private int column = -1;
    private int row = -1;

    /** Hover state. */
    private boolean hover;

    /** Renderer has focus. */
    private boolean focus;

    /** Mouse down on the renderer area. */
    private boolean mouseDown;

    /** Selection state. */
    private boolean selected;

    /** The bounds the renderer paints on. */
    private Rectangle bounds = new Rectangle(0, 0, 0, 0);

    protected AbstractRenderer(LightGrid grid) {
        this.grid = grid;
        this.display = grid.getDisplay();
    }

    public int getColumn()
    {
        return column;
    }

    @Override
    public void setColumn(int column)
    {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    @Override
    public void setRow(int row) {
        this.row = row;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        setBounds(new Rectangle(x, y, width, height));
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    public void setBounds(Rectangle bounds)
    {
        this.bounds = bounds;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocation(int x, int y)
    {
        setBounds(new Rectangle(x, y, bounds.width, bounds.height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocation(Point location)
    {
        setBounds(new Rectangle(location.x, location.y, bounds.width, bounds.height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSize(int width, int height)
    {
        setBounds(new Rectangle(bounds.x, bounds.y, width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSize(Point size)
    {
        setBounds(new Rectangle(bounds.x, bounds.y, size.x, size.y));
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

    /** 
     * {@inheritDoc}
     */
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

    /** 
     * {@inheritDoc}
     */
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

    /** 
     * {@inheritDoc}
     */
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

    /** 
     * {@inheritDoc}
     */
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
        return display;
    }

    public Cursor getHoverCursor() {
        return null;
    }
}
