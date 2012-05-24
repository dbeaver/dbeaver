/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * A renderer for the last empty column header.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyColumnFooterRenderer extends AbstractRenderer
{
    public DefaultEmptyColumnFooterRenderer(LightGrid grid) {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(GC gc)
    {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height + 1);
        gc.drawLine(getBounds().x, getBounds().y, getBounds().x
                + getBounds().width, getBounds().y);
    }

}
