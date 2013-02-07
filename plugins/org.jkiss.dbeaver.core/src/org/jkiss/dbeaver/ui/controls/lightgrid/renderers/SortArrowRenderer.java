/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The column header sort arrow renderer.
 *
 * @author serge@jkiss.org
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
    @Override
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
