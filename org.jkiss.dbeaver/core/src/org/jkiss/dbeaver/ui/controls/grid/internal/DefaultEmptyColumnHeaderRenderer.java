/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.AbstractRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * A renderer for the last empty column header.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyColumnHeaderRenderer extends AbstractRenderer
{

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(wHint, hHint);
    }

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height + 1);

    }

}
