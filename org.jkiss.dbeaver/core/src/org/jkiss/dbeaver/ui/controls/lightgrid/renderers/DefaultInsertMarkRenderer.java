/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * A renderer which paints the insert mark feedback during drag & drop.
 *
 * @author mark-olver.reiser
 * @since 3.3
 */
public class DefaultInsertMarkRenderer extends AbstractRenderer
{
    public DefaultInsertMarkRenderer(LightGrid grid) {
        super(grid);
    }

    /**
     * Renders the insertion mark.  The bounds of the renderer
     * need not be set.
     * 
     * @param gc
     * @param value  must be a {@link Rectangle} with height == 0.
     */
    public void paint(GC gc, Object value)
    {
    	Rectangle r = (Rectangle)value;

    	gc.setLineStyle(SWT.LINE_SOLID);
    	gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));

    	gc.drawLine(r.x, r.y-1, r.x+r.width, r.y-1);
    	gc.drawLine(r.x, r.y  , r.x+r.width, r.y  );
    	gc.drawLine(r.x, r.y+1, r.x+r.width, r.y+1);

    	gc.drawLine(r.x-1,  r.y-2,  r.x-1,   r.y+2);
    	gc.drawLine(r.x-2,  r.y-3,  r.x-2,   r.y+3);
    }

}
