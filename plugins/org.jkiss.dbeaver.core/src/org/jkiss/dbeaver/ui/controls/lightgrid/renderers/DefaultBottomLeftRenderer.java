/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The renderer for the empty top left area when both column and row headers are
 * visible.
 * 
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultBottomLeftRenderer extends AbstractRenderer {

    public DefaultBottomLeftRenderer(LightGrid grid) {
        super(grid);
    }

    /**
	 * {@inheritDoc}
	 */
	public void paint(GC gc) {
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
				getBounds().height + 1);

		gc.setForeground(getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_DARK_SHADOW));

		gc.drawLine(getBounds().x, getBounds().y, getBounds().x
				+ getBounds().width, getBounds().y);

	}

}
