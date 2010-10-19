/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;

/**
 * The column header sort arrow renderer.
 */
class ResultSetSortRenderer extends AbstractRenderer {
    private Color arrowColor;

    ResultSetSortRenderer(LightGrid grid)
    {
        super(grid);

        arrowColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        setSize(7, 4);
    }

    public void paint(GC gc)
    {
        gc.setForeground(arrowColor);
        Rectangle bounds = getBounds();
        if (isSelected()) {
            gc.drawLine(bounds.x, bounds.y, bounds.x + 6, bounds.y);
            gc.drawLine(bounds.x + 1, bounds.y + 1, bounds.x + 5, bounds.y + 1);
            gc.drawLine(bounds.x + 2, bounds.y + 2, bounds.x + 4, bounds.y + 2);
            gc.drawPoint(bounds.x + 3, bounds.y + 3);
        } else {
            gc.drawPoint(bounds.x + 3, bounds.y);
            gc.drawLine(bounds.x + 2, bounds.y + 1, bounds.x + 4, bounds.y + 1);
            gc.drawLine(bounds.x + 1, bounds.y + 2, bounds.x + 5, bounds.y + 2);
            gc.drawLine(bounds.x, bounds.y + 3, bounds.x + 6, bounds.y + 3);
        }
    }

}
