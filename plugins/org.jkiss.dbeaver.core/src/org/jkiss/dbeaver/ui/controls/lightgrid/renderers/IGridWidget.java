/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

/**

 */
public interface IGridWidget extends IGridRenderer
{
    // Event type constants
    /** Hover State. */
    static final int MouseMove = SWT.MouseMove;

    /** Mouse down state. */
    static final int LeftMouseButtonDown = SWT.MouseDown;

    /**
     * Mechanism used to notify the light weight widgets that an event occurred
     * that it might be interested in.
     * 
     * @param event Event type.
     * @param point Location of event.
     * @param value New value.
     * @return widget handled the event.
     */
    boolean notify(int event, Point point, Object value);

    /**
     * Returns the hover detail object. This detail is used by the renderer to
     * determine which part or piece of the rendered image is hovered over.
     * 
     * @return string identifying which part of the image is being hovered over.
     */
    String getHoverDetail();

    /**
     * Sets a string object that represents which part of the rendered image is currently under the
     * mouse pointer.
     * 
     * @param detail identifying string.
     */
    void setHoverDetail(String detail);
}
