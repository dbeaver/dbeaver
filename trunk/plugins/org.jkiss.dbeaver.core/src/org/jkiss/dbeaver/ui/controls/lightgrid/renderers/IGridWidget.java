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
