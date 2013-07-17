/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
 * @author serge@jkiss.org
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultTopLeftRenderer extends AbstractRenderer {
    //private Button cfgButton;

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
    @Override
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
