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
 * A checkbox renderer.  
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class CheckBoxRenderer extends AbstractRenderer
{

    private boolean checked = false;

    private boolean grayed = false;

    /**
     * 
     */
    public CheckBoxRenderer()
    {
        super();
        this.setSize(13, 13);
    }

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {

        if (isGrayed())
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
        else
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        }

        gc.fillRectangle(getBounds());

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        gc.drawRectangle(getBounds().x, getBounds().y, getBounds().width - 1,
                         getBounds().height - 1);
        gc.drawRectangle(getBounds().x + 1, getBounds().y + 1, getBounds().width - 3,
                         getBounds().height - 3);

        if (isGrayed())
        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        }

        if (isChecked())
        {

            gc.drawLine(getBounds().x + 3, getBounds().y + 5, getBounds().x + 6, getBounds().y + 8);
            gc.drawLine(getBounds().x + 3, getBounds().y + 6, getBounds().x + 5, getBounds().y + 8);
            gc.drawLine(getBounds().x + 3, getBounds().y + 7, getBounds().x + 5, getBounds().y + 9);
            gc.drawLine(getBounds().x + 9, getBounds().y + 3, getBounds().x + 6, getBounds().y + 6);
            gc.drawLine(getBounds().x + 9, getBounds().y + 4, getBounds().x + 6, getBounds().y + 7);
            gc.drawLine(getBounds().x + 9, getBounds().y + 5, getBounds().x + 7, getBounds().y + 7);

        }
    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return getSize();
    }

    /**
     * @return Returns the checked.
     */
    public boolean isChecked()
    {
        return checked;
    }

    /**
     * @param checked The checked to set.
     */
    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }

    /**
     * @return Returns the grayed.
     */
    public boolean isGrayed()
    {
        return grayed;
    }

    /**
     * @param grayed The grayed to set.
     */
    public void setGrayed(boolean grayed)
    {
        this.grayed = grayed;
    }
}
