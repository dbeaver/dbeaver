/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The renderer for the empty top left area when both column and row headers are visible.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultTopLeftRenderer extends AbstractRenderer {
    private Button cfgButton;

    public DefaultTopLeftRenderer(LightGrid grid) {
        super(grid);
        //cfgButton = new Button(grid, SWT.FLAT | SWT.ARROW | SWT.DOWN);
        //cfgButton.setText("...");
    }

    @Override
    public void setBounds(Rectangle bounds) {
        //cfgButton.setBounds(bounds);

        super.setBounds(bounds);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc) {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(
            getBounds().x,
            getBounds().y,
            getBounds().width - 1,
            getBounds().height + 1);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

        gc.drawLine(
            getBounds().x + getBounds().width - 1,
            getBounds().y,
            getBounds().x + getBounds().width - 1,
            getBounds().y + getBounds().height);

        gc.drawLine(
            getBounds().x,
            getBounds().y + getBounds().height - 1,
            getBounds().x + getBounds().width,
            getBounds().y + getBounds().height - 1);

        //cfgButton.redraw();

    }

}
