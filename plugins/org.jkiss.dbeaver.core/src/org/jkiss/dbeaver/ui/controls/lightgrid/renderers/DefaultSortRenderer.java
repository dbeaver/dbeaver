package org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;

/**
 * The column header sort arrow renderer.
 */
public class DefaultSortRenderer extends AbstractRenderer {
    private Image asterisk;
    private Image arrowUp;
    private Image arrowDown;
    private GridColumn column;
    private Cursor hoverCursor;

    public DefaultSortRenderer(GridColumn column)
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
