/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
