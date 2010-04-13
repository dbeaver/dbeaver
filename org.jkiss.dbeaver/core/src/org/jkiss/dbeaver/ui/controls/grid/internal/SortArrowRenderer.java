/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    chris.gross@us.ibm.com - initial API and implementation
 *******************************************************************************/ 
package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.AbstractRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The column header sort arrow renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class SortArrowRenderer extends AbstractRenderer
{

    /**
     * Default constructor.
     */
    public SortArrowRenderer()
    {
        super();

        setSize(7, 4);
    }

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        if (isSelected())
        {
            gc
                .drawLine(getBounds().x + 0, getBounds().y + 0, getBounds().x + 6,
                          getBounds().y + 00);
            gc.drawLine(getBounds().x + 1, getBounds().y + 01, getBounds().x + 5,
                        getBounds().y + 01);
            gc.drawLine(getBounds().x + 2, getBounds().y + 02, getBounds().x + 4,
                        getBounds().y + 02);
            gc.drawPoint(getBounds().x + 3, getBounds().y + 03);
        }
        else
        {
            gc.drawPoint(getBounds().x + 3, getBounds().y + 0);
            gc.drawLine(getBounds().x + 2, getBounds().y + 01, getBounds().x + 4,
                        getBounds().y + 01);
            gc.drawLine(getBounds().x + 1, getBounds().y + 02, getBounds().x + 5,
                        getBounds().y + 02);
            gc.drawLine(getBounds().x + 0, getBounds().y + 03, getBounds().x + 6,
                        getBounds().y + 03);
        }

    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(7, 4);
    }

}
