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
import org.jkiss.dbeaver.ui.controls.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The focus item renderer - renders over the completely drawn table.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultFocusRenderer extends AbstractRenderer
{

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        GridItem item = (GridItem)value;

        if (item.getParent().isSelected(item))
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            gc.setBackground(item.getBackground());
            gc.setForeground(item.getForeground());
        }

        gc.drawFocus(getBounds().x, getBounds().y, getBounds().width + 1, getBounds().height + 1);

    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return null;
    }

}
