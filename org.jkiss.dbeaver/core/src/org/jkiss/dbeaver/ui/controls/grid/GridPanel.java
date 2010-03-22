package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;

/**
 * GridPanel
 */
class GridPanel extends Composite implements Listener
{
    private GridControl grid;
    private int panelWidth = 30;

    public GridPanel(Composite parent, int style, GridControl grid)
    {
        super(parent, style);
        this.addListener(SWT.Paint, this);
        this.addListener(SWT.MouseDown, this);
        this.grid = grid;
    }

    GridControl getGrid()
    {
        return grid;
    }

    public void handleEvent(Event event)
    {
        switch (event.type) {
            case SWT.Paint:
                this.paint(event);
                break;
            case SWT.MouseDown:
                this.onMouseDown(event);
                break;
        }
    }

    private void onMouseDown(Event event)
    {
        grid.getTable().setFocus();
        if (event.button != 1) {
            return;
        }
        int clickAt = event.y;
        int headerHeight = grid.getTable().getHeaderHeight();
        if (clickAt <= headerHeight) {
            grid.selectAllRows();
            return;
        }
        int itemHeight = grid.getTable().getItemHeight();
        int rowPos = grid.getTable().getTopIndex() + (clickAt - headerHeight) / itemHeight;
        grid.selectRow(rowPos, (event.stateMask & SWT.SHIFT) != 0);
    }

    public int getPanelWidth()
    {
        return panelWidth;
    }

    public void setPanelWidth(int panelWidth)
    {
        this.panelWidth = panelWidth;
    }

    public Point computeSize (int wHint, int hHint, boolean changed)
    {
        return new Point(panelWidth, hHint);
    }

    private void paint(Event event)
    {
        Rectangle tableBounds = grid.getTable().getBounds();

        event.gc.setBackground(grid.getBackgroundControl());
        event.gc.fillRectangle(0, 0, panelWidth, tableBounds.height);
        event.gc.setForeground(grid.getForegroundLines());
        int headerHeight = grid.getTable().getHeaderHeight();
        //event.gc.drawLine(0, 0, 20, 0);
        event.gc.drawLine(panelWidth - 1, 0, panelWidth - 1, tableBounds.height);

        int itemHeight = grid.getTable().getItemHeight();
        int rowsCount = grid.getVisibleRowsCount();
        int offset = headerHeight - 1;
        int topPos = grid.getTable().getTopIndex();
        int itemsCount = grid.getTable().getItemCount();
        for (int i = 0 ; i <= rowsCount; i++) {
            event.gc.setForeground(grid.getForegroundLines());
            event.gc.drawLine(0, offset, panelWidth, offset);
            if (topPos >= itemsCount) {
                break;
            }

            String text = grid.getRowTitle(topPos);
            Point textSize = event.gc.textExtent(text);
            event.gc.setForeground(grid.getForegroundNormal());
            event.gc.drawText(text, panelWidth - textSize.x - 2, offset + 2);
            offset += itemHeight;
            topPos++;
        }
    }
}
