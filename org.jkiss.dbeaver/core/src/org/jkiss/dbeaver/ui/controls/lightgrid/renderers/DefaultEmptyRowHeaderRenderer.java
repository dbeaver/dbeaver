/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The empty row header renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyRowHeaderRenderer extends AbstractRenderer
{
    public DefaultEmptyRowHeaderRenderer(LightGrid grid) {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc)
    {

        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width, getBounds().height + 1);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                                                                               + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
        }

    }

}
