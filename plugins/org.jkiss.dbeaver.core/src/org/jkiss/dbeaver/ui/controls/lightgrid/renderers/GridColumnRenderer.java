/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 *
 * @author serge@jkiss.org
 */
public abstract class GridColumnRenderer extends AbstractGridWidget
{
    protected GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    public Rectangle getControlBounds(Object value, boolean preferred)
    {
        return null;
    }

    public Rectangle getTextBounds(Object value, boolean preferred) {
        return null;
    }

    protected Image getColumnImage() {
        return grid.getColumnLabelProvider() == null ? null : grid.getColumnLabelProvider().getImage(getColumn());
    }

    protected String getColumnText()
    {
        String text = grid.getColumnLabelProvider() == null ? null : grid.getColumnLabelProvider().getText(getColumn());
        if (text == null)
        {
            text = String.valueOf(getColumn());
        }
        return text;
    }
    
    protected Color getColumnBackground() {
        if (grid.getColumnLabelProvider() instanceof IColorProvider) {
    	    return ((IColorProvider)grid.getColumnLabelProvider()).getBackground(getColumn());
        } else {
            return null;
        }
    }
    
    protected Color getColumnForeground() {
        if (grid.getColumnLabelProvider() instanceof IColorProvider) {
    	    return ((IColorProvider)grid.getColumnLabelProvider()).getForeground(getColumn());
        } else {
            return null;
        }
    }

    protected Font getColumnFont() {
        Font font = null;
        if (grid.getColumnLabelProvider() instanceof IFontProvider) {
            font = ((IFontProvider) grid.getColumnLabelProvider()).getFont(getColumn());
        }
        return font != null ? font : grid.getFont();
    }
}
