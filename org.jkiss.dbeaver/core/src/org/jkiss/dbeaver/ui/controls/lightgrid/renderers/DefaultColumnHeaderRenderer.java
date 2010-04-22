/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The column header renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultColumnHeaderRenderer extends GridHeaderRenderer {

    int leftMargin = 6;

    int rightMargin = 6;

    int topMargin = 3;

    int bottomMargin = 3;

    int arrowMargin = 6;

    int imageSpacing = 3;

    private SortArrowRenderer arrowRenderer;

    private TextLayout textLayout;

    public DefaultColumnHeaderRenderer(LightGrid grid) {
        super(grid);
        arrowRenderer = new SortArrowRenderer(grid);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc) {
        GridColumn column = grid.getColumn(getColumn());

        // set the font to be used to display the text.
        gc.setFont(column.getHeaderFont());

        boolean flat = !column.getMoveable();

        boolean drawSelected = ((isMouseDown() && isHover()));

        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        if (flat && isSelected()) {
            gc.setBackground(column.getParent().getCellHeaderSelectionBackground());
        }

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
            getBounds().height);

        int pushedDrawingOffset = 0;
        if (drawSelected) {
            pushedDrawingOffset = 1;
        }

        int x = leftMargin;

        if (column.getImage() != null) {
            int y = bottomMargin;

            if (column.getHeaderControl() == null) {
                y = getBounds().y + pushedDrawingOffset + getBounds().height - bottomMargin - column.getImage().getBounds().height;
            }

            gc.drawImage(column.getImage(), getBounds().x + x + pushedDrawingOffset, y);
            x += column.getImage().getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x;

        if (column.getSort() == SWT.NONE) {
            width -= rightMargin;
        } else {
            width -= arrowMargin + arrowRenderer.getSize().x + arrowMargin;
        }

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y;

        if (column.getHeaderControl() == null) {
            y = getBounds().y + getBounds().height - bottomMargin
                - gc.getFontMetrics().getHeight();
        } else {
            y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight() - computeControlSize(column).y;
        }

        String text = column.getText();

        if (!isWordWrap()) {
            text = TextUtils.getShortString(gc, text, width);
            //y -= gc.getFontMetrics().getHeight();
        }

        if (column.getAlignment() == SWT.RIGHT) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += width - len;
            }
        } else if (column.getAlignment() == SWT.CENTER) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += (width - len) / 2;
            }
        }


        if (!isWordWrap()) {
            gc.drawString(text, getBounds().x + x + pushedDrawingOffset,
                y + pushedDrawingOffset, true);
        } else {
            getTextLayout(gc, column);
            textLayout.setWidth(width < 1 ? 1 : width);
            textLayout.setText(text);
            y -= textLayout.getBounds().height;

            textLayout.draw(gc, getBounds().x + x + pushedDrawingOffset, y + pushedDrawingOffset);
        }

        if (column.getSort() != SWT.NONE) {
            if (column.getHeaderControl() == null) {
                y = getBounds().y
                    + ((getBounds().height - arrowRenderer.getBounds().height) / 2)
                    + 1;
            } else {
                y = getBounds().y
                    + ((getBounds().height - computeControlSize(column).y - arrowRenderer.getBounds().height) / 2)
                    + 1;
            }

            arrowRenderer.setSelected(column.getSort() == SWT.UP);
            if (drawSelected) {
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width + 1, y
                    );
            } else {
                if (column.getHeaderControl() == null) {
                    y = getBounds().y
                        + ((getBounds().height - arrowRenderer.getBounds().height) / 2);
                } else {
                    y = getBounds().y
                        + ((getBounds().height - computeControlSize(column).y - arrowRenderer.getBounds().height) / 2);
                }
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width, y);
            }
            arrowRenderer.paint(gc);
        }

        if (!flat) {

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            }

            gc.drawLine(getBounds().x, getBounds().y, getBounds().x + getBounds().width - 1,
                getBounds().y);
            gc.drawLine(getBounds().x, getBounds().y, getBounds().x, getBounds().y + getBounds().height
                - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                gc.drawLine(getBounds().x + 1, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + 1);
                gc.drawLine(getBounds().x + 1, getBounds().y + 1, getBounds().x + 1,
                    getBounds().y + getBounds().height - 2);
            }

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            }
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.drawLine(getBounds().x + getBounds().width - 2, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
                gc.drawLine(getBounds().x + 1, getBounds().y + getBounds().height - 2,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
            }

        } else {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
        }


    }

    /**
     * {@inheritDoc}
     */
    public boolean notify(int event, Point point, Object value) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getTextBounds(Object value, boolean preferred) {
        GridColumn column = (GridColumn) value;

        int x = leftMargin;

        if (column.getImage() != null) {
            x += column.getImage().getBounds().width + imageSpacing;
        }


        GC gc = new GC(column.getParent());
        gc.setFont(column.getParent().getFont());
        int y = getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();

        Rectangle bounds = new Rectangle(x, y, 0, 0);

        Point p = gc.stringExtent(column.getText());

        bounds.height = p.y;

        if (preferred) {
            bounds.width = p.x;
        } else {
            int width = getBounds().width - x;
            if (column.getSort() == SWT.NONE) {
                width -= rightMargin;
            } else {
                width -= arrowMargin + arrowRenderer.getSize().x + arrowMargin;
            }
            bounds.width = width;
        }


        gc.dispose();

        return bounds;
    }

    /**
     * @return the bounds reserved for the control
     */
    public Rectangle getControlBounds(Object value, boolean preferred) {
        Rectangle bounds = getBounds();
        GridColumn column = (GridColumn) value;
        Point controlSize = computeControlSize(column);

        int y = getBounds().y + getBounds().height - bottomMargin - controlSize.y;

        return new Rectangle(bounds.x + 3, y, bounds.width - 6, controlSize.y);
    }

    private Point computeControlSize(GridColumn column) {
        if (column.getHeaderControl() != null) {
            return column.getHeaderControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        }
        return new Point(0, 0);
    }

    private void getTextLayout(GC gc, GridColumn column) {
        if (textLayout == null) {
            textLayout = new TextLayout(gc.getDevice());
            textLayout.setFont(gc.getFont());
            column.getParent().addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e) {
                    textLayout.dispose();
                }
            });
        }
        textLayout.setAlignment(column.getAlignment());
    }
}
