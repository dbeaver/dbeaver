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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;

/**
 * The column header sort arrow renderer.
 */
class ResultSetSortRenderer extends AbstractRenderer {
    private Image asterisk;
    private Image arrowUp;
    private Image arrowDown;
    private GridColumn column;
    private Cursor hoverCursor;

    ResultSetSortRenderer(GridColumn column)
    {
        super(column.getParent());
        this.column = column;
        this.asterisk = DBIcon.SORT_UNKNOWN.getImage();
        this.arrowUp = DBIcon.SORT_DECREASE.getImage();
        this.arrowDown = DBIcon.SORT_INCREASE.getImage();
        this.hoverCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
        Rectangle imgBounds = arrowUp.getBounds();
        setSize(imgBounds.width, imgBounds.height);
    }

    @Override
    public void paint(GC gc)
    {
        Rectangle bounds = getBounds();
        switch (column.getSort()) {
            case SWT.DEFAULT:
                gc.drawImage(asterisk, bounds.x, bounds.y);
                break;
            case SWT.UP:
                gc.drawImage(arrowUp, bounds.x, bounds.y);
                break;
            case SWT.DOWN:
                gc.drawImage(arrowDown, bounds.x, bounds.y);
                break;
        }
/*
        if (isSelected()) {
            gc.drawLine(bounds.x, bounds.y, bounds.x + 6, bounds.y);
            gc.drawLine(bounds.x + 1, bounds.y + 1, bounds.x + 5, bounds.y + 1);
            gc.drawLine(bounds.x + 2, bounds.y + 2, bounds.x + 4, bounds.y + 2);
            gc.drawPoint(bounds.x + 3, bounds.y + 3);
        } else {
            gc.drawPoint(bounds.x + 3, bounds.y);
            gc.drawLine(bounds.x + 2, bounds.y + 1, bounds.x + 4, bounds.y + 1);
            gc.drawLine(bounds.x + 1, bounds.y + 2, bounds.x + 5, bounds.y + 2);
            gc.drawLine(bounds.x, bounds.y + 3, bounds.x + 6, bounds.y + 3);
        }
*/
    }

    @Override
    public Cursor getHoverCursor() {
        return hoverCursor;
    }
}
