/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

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
    public SortArrowRenderer(LightGrid grid)
    {
        super(grid);

        setSize(7, 4);
    }

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc)
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

}
