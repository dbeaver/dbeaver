/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

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
     * @param value the value being painted
     */
    void paint(GC gc, Object value);

    /**
     * Returns the size of the given value's visual representation.
     * 
     * @param gc convenience GC for string and text extents
     * @param wHint given width (or SWT.DEFAULT)
     * @param hHint given height (or SWT.DEFAULT)
     * @param value value to be sized
     * @return the size
     */
    Point computeSize(GC gc, int wHint, int hHint, Object value);

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

    /**
     * Sets the display.
     * 
     * @param display Display.
     */
    void setDisplay(Display display);
}