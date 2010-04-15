/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import  org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import  org.jkiss.dbeaver.ui.controls.lightgrid.GridItem;

/**
 * The renderer for a cell in Grid.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultCellRenderer extends GridCellRenderer
{

	int leftMargin = 4;

    int rightMargin = 4;

    int topMargin = 0;

    int bottomMargin = 0;

    int textTopMargin = 1;

    int textBottomMargin = 2;

    private int insideMargin = 3;

    int treeIndent = 20;

    private TextLayout textLayout;

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        GridItem item = (GridItem)value;

        gc.setFont(item.getFont(getColumn()));

        boolean drawAsSelected = isSelected();

        boolean drawBackground = true;

        if (isCellSelected())
        {
            drawAsSelected = true;//(!isCellFocus());
        }

        if (drawAsSelected)
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            if (item.getParent().isEnabled())
            {
                Color back = item.getBackground(getColumn());

                if (back != null)
                {
                    gc.setBackground(back);
                }
                else
                {
                    drawBackground = false;
                }
            }
            else
            {
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
            gc.setForeground(item.getForeground(getColumn()));
        }

        if (drawBackground)
            gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
                         getBounds().height);


        int x = leftMargin;

        Image image = item.getImage(getColumn());
        if (image != null)
        {
            int y = getBounds().y;

            y += (getBounds().height - image.getBounds().height)/2;

            gc.drawImage(image, getBounds().x + x, y);

            x += image.getBounds().width + insideMargin;
        }

        int width = getBounds().width - x - rightMargin;

        if (drawAsSelected)
        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            gc.setForeground(item.getForeground(getColumn()));
        }

        if (!isWordWrap())
        {
            String text = TextUtils.getShortString(gc, item.getText(getColumn()), width);

            if (getAlignment() == SWT.RIGHT)
            {
                int len = gc.stringExtent(text).x;
                if (len < width)
                {
                    x += width - len;
                }
            }
            else if (getAlignment() == SWT.CENTER)
            {
                int len = gc.stringExtent(text).x;
                if (len < width)
                {
                    x += (width - len) / 2;
                }
            }

            gc.drawString(text, getBounds().x + x, getBounds().y + textTopMargin + topMargin, true);
        }
        else
        {
            if (textLayout == null)
            {
                textLayout = new TextLayout(gc.getDevice());
                item.getParent().addDisposeListener(new DisposeListener()
                {
                    public void widgetDisposed(DisposeEvent e)
                    {
                        textLayout.dispose();
                    }
                });
            }
            textLayout.setFont(gc.getFont());
            textLayout.setText(item.getText(getColumn()));
            textLayout.setAlignment(getAlignment());
            textLayout.setWidth(width < 1 ? 1 : width);
            if (item.getParent().isAutoHeight())
            {
              // Look through all columns (except this one) to get the max height needed for this item
            int columnCount = item.getParent().getColumnCount();
            int maxHeight = textLayout.getBounds().height + textTopMargin + textBottomMargin;
            for (int i=0; i<columnCount; i++)
            {
              GridColumn column = item.getParent().getColumn(i);
              if (i != getColumn() && column.getWordWrap())
              {
                int height = column.getCellRenderer().computeSize(gc, column.getWidth(), SWT.DEFAULT, item).y;
                maxHeight = Math.max(maxHeight, height);
              }
            }

            // Also look at the row header if necessary
            if (item.getParent().isWordWrapHeader())
            {
            int height = item.getParent().getRowHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, item).y;
          maxHeight = Math.max(maxHeight, height);
            }

            if (maxHeight != item.getHeight())
            {
              item.setHeight(maxHeight);
            }
            }
            textLayout.draw(gc, getBounds().x + x, getBounds().y + textTopMargin + topMargin);
        }


        if (item.getParent().getLinesVisible())
        {
            if (isCellSelected())
            {
                //XXX: should be user definable?
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            }
            else
            {
                gc.setForeground(item.getParent().getLineColor());
            }
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height, getBounds().x
                                                                           + getBounds().width -1,
                        getBounds().y + getBounds().height);
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y,
                        getBounds().x + getBounds().width - 1, getBounds().y + getBounds().height);
        }

        if (isCellFocus())
        {
            Rectangle focusRect = new Rectangle(getBounds().x, getBounds().y, getBounds().width - 1,
                                                getBounds().height);

            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
            gc.drawRectangle(focusRect);

            if (isFocus())
            {
                focusRect.x ++;
                focusRect.width -= 2;
                focusRect.y ++;
                focusRect.height -= 2;

                gc.drawRectangle(focusRect);
            }
        }
    }

	/**
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        GridItem item = (GridItem)value;

        gc.setFont(item.getFont(getColumn()));

        int x = 0;

        x += leftMargin;

        int y = 0;

        Image image = item.getImage(getColumn());
        if (image != null)
        {
            y = topMargin + image.getBounds().height + bottomMargin;

            x += image.getBounds().width + insideMargin;
        }

// MOPR-DND
// MOPR: replaced this code (to get correct preferred height for cells in word-wrap columns)
//
//       x += gc.stringExtent(item.getText(column)).x + rightMargin;
//
//        y = Math.max(y,topMargin + gc.getFontMetrics().getHeight() + bottomMargin);
//
// with this code:

        int textHeight = 0;
        if(!isWordWrap())
        {
            x += gc.textExtent(item.getText(getColumn())).x + rightMargin;

            textHeight = topMargin + textTopMargin + gc.getFontMetrics().getHeight() + textBottomMargin + bottomMargin;
        }
        else
        {
        	int plainTextWidth;
        	if (wHint == SWT.DEFAULT)
        	  plainTextWidth = getBounds().width - x - rightMargin;
        	else
        		plainTextWidth = wHint - x - rightMargin;

            TextLayout currTextLayout = new TextLayout(gc.getDevice());
            currTextLayout.setFont(gc.getFont());
            currTextLayout.setText(item.getText(getColumn()));
            currTextLayout.setAlignment(getAlignment());
            currTextLayout.setWidth(plainTextWidth < 1 ? 1 : plainTextWidth);

            x += plainTextWidth + rightMargin;

            textHeight += topMargin + textTopMargin;
            for(int cnt=0;cnt<currTextLayout.getLineCount();cnt++)
                textHeight += currTextLayout.getLineBounds(cnt).height;
            textHeight += textBottomMargin + bottomMargin;

            currTextLayout.dispose();
        }

        y = Math.max(y, textHeight);

        return new Point(x, y);
    }

    /**
     * {@inheritDoc}
     */
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getTextBounds(GridItem item, boolean preferred)
    {
        int x = leftMargin;

        Image image = item.getImage(getColumn());
        if (image != null)
        {
            x += image.getBounds().width + insideMargin;
        }

        Rectangle bounds = new Rectangle(x,topMargin + textTopMargin,0,0);

        GC gc = new GC(item.getParent());
        gc.setFont(item.getFont(getColumn()));
        Point size = gc.stringExtent(item.getText(getColumn()));

        bounds.height = size.y;

        if (preferred)
        {
            bounds.width = size.x - 1;
        }
        else
        {
            bounds.width = getBounds().width - x - rightMargin;
        }

        gc.dispose();

        return bounds;
    }

}
