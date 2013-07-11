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
package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The empty cell renderer.
 *
 * @author serge@jkiss.org
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyCellRenderer extends GridCellRenderer
{
    public DefaultEmptyCellRenderer(LightGrid grid)
    {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(GC gc)
    {

        boolean drawBackground = true;
        
        if (isSelected())
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            if (grid.isEnabled())
            {
                drawBackground = false;
            }
            else
            {
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
            gc.setForeground(grid.getForeground());
        }

        if (drawBackground)
            gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height);

        if (grid.getLinesVisible())
        {
            gc.setForeground(grid.getLineColor());
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height, getBounds().x
                                                                           + getBounds().width,
                        getBounds().y + getBounds().height);
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width
                                                                              - 1,
                        getBounds().y + getBounds().height);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }
    
}
