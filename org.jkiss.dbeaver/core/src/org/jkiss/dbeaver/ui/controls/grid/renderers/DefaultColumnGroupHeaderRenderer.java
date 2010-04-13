/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.renderers;

import org.jkiss.dbeaver.ui.controls.grid.GridColumnGroup;
import org.jkiss.dbeaver.ui.controls.grid.renderers.GridHeaderRenderer;
import org.jkiss.dbeaver.ui.controls.grid.renderers.IGridWidget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * The column group header renderer in Grid.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultColumnGroupHeaderRenderer extends GridHeaderRenderer
{
    int leftMargin = 6;

    int rightMargin = 6;

    int topMargin = 3;

    int bottomMargin = 3;

    int imageSpacing = 3;

    private ExpandToggleRenderer toggleRenderer = new ExpandToggleRenderer();

    private TextLayout textLayout;

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        GridColumnGroup group = (GridColumnGroup)value;

        // set the font to be used to display the text.
        gc.setFont(group.getHeaderFont());

        if (isSelected())
        {
            gc.setBackground(group.getParent().getCellHeaderSelectionBackground());
        }
        else
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height + 1);

        int x = leftMargin;

        if (group.getImage() != null)
        {
            gc.drawImage(group.getImage(), getBounds().x + x, getBounds().y + topMargin);
            x += group.getImage().getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x - rightMargin;
        if ((group.getStyle() & SWT.TOGGLE) != 0)
        {
            width -= toggleRenderer.getSize().x;
        }

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
        if (!isWordWrap())
        {
          gc.drawString(TextUtils.getShortString(gc, group.getText(), width), getBounds().x + x,
              getBounds().y + topMargin);
        }
        else
        {
            getTextLayout(gc, group);
            textLayout.setWidth(width < 1 ? 1 : width);
            textLayout.setText(group.getText());

            if (group.getParent().isAutoHeight())
            {
            	group.getParent().recalculateHeader();
            }

            textLayout.draw(gc, getBounds().x + x, getBounds().y + topMargin);
        }

        if ((group.getStyle() & SWT.TOGGLE) != 0)
        {
            toggleRenderer.setHover(isHover() && getHoverDetail().equals("toggle"));
            toggleRenderer.setExpanded(group.getExpanded());
            toggleRenderer.setBounds(getToggleBounds());
            toggleRenderer.paint(gc, null);
        }

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

        gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                          + getBounds().width - 1,
                    getBounds().y + getBounds().height - 1);
        gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                                                                           + getBounds().width - 1,
                    getBounds().y + getBounds().height - 1);

    }

    /**
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        GridColumnGroup group = (GridColumnGroup)value;

        gc.setFont(group.getHeaderFont());

        int x = leftMargin;
        int y = topMargin + gc.getFontMetrics().getHeight() + bottomMargin;


        if (group.getImage() != null)
        {
            x += group.getImage().getBounds().width + imageSpacing;

            y = Math.max(y, topMargin + group.getImage().getBounds().height + bottomMargin);
        }

        if (!isWordWrap())
        {
          x += gc.stringExtent(group.getText()).x + rightMargin;
        }
        else
        {
          int toggleWidth = 0;
            if ((group.getStyle() & SWT.TOGGLE) != 0)
              toggleWidth = toggleRenderer.getSize().x;

          int plainTextWidth;
          if (wHint == SWT.DEFAULT)
            plainTextWidth = getBounds().width - x - rightMargin - toggleWidth;
          else
            plainTextWidth = wHint - x - rightMargin - toggleWidth;

          getTextLayout(gc, group);
            textLayout.setText(group.getText());
            textLayout.setWidth(plainTextWidth < 1 ? 1 : plainTextWidth);

            x += plainTextWidth + rightMargin;

            int textHeight = topMargin;
            textHeight += textLayout.getBounds().height;
            textHeight += bottomMargin;

            y = Math.max(y, textHeight);
        }
        return new Point(x, y);
    }

    /**
     * {@inheritDoc}
     */
    public boolean notify(int event, Point point, Object value)
    {
        GridColumnGroup group = (GridColumnGroup)value;

        if ((group.getStyle() & SWT.TOGGLE) != 0)
        {
            if (event == IGridWidget.LeftMouseButtonDown)
            {
                if (getToggleBounds().contains(point))
                {
                    group.setExpanded(!group.getExpanded());

                    if (group.getExpanded())
                    {
                        group.notifyListeners(SWT.Expand,new Event());
                    }
                    else
                    {
                        group.notifyListeners(SWT.Collapse,new Event());
                    }
                    return true;
                }
            }
            else
            {
                if (getToggleBounds().contains(point))
                {
                    setHoverDetail("toggle");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getToggleBounds()
    {
        int x = getBounds().x + getBounds().width - toggleRenderer.getBounds().width - rightMargin;
        int y = getBounds().y + (getBounds().height - toggleRenderer.getBounds().height) / 2;

        return new Rectangle(x, y, toggleRenderer.getBounds().width,
                             toggleRenderer.getBounds().height);
    }

    /**
     * {@inheritDoc}
     */
    public void setDisplay(Display display)
    {
        super.setDisplay(display);
        toggleRenderer.setDisplay(display);
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getTextBounds(Object value, boolean preferred)
    {
        GridColumnGroup group = (GridColumnGroup)value;

        int x = leftMargin;

        if (group.getImage() != null)
        {
            x += group.getImage().getBounds().width + imageSpacing;
        }

        Rectangle bounds = new Rectangle(x, topMargin, 0, 0);

        GC gc = new GC(group.getParent());
        gc.setFont(group.getHeaderFont());

        Point p = gc.stringExtent(group.getText());

        bounds.height = p.y;

        if (preferred)
        {
            bounds.width = p.x;
        }
        else
        {
            int width = getBounds().width - x - rightMargin;
            if ((group.getStyle() & SWT.TOGGLE) != 0)
            {
                width -= toggleRenderer.getSize().x;
            }
            bounds.width = width;
        }

        gc.dispose();
        return bounds;
    }

    private void getTextLayout(GC gc, GridColumnGroup group)
    {
        if (textLayout == null)
        {
            textLayout = new TextLayout(gc.getDevice());
            textLayout.setFont(gc.getFont());
            group.getParent().addDisposeListener(new DisposeListener()
            {
                public void widgetDisposed(DisposeEvent e)
                {
                    textLayout.dispose();
                }
            });
        }
    }
}
