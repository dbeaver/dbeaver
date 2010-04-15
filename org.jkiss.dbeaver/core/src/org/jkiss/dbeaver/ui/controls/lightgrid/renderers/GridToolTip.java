/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * An in-place tooltip.
 *
 * @author cgross
 */
public class GridToolTip extends Widget
{
    private Shell shell;

    private String text;

    private int ymargin = 2;

    private int xmargin = 3;

    /**
     * Creates an inplace tooltip.
     *
     * @param parent parent control.
     */
    public GridToolTip(final Control parent)
    {
        super(parent, SWT.NONE);

        shell = new Shell(parent.getShell(), SWT.NO_TRIM | SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
        shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        shell.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        parent.addListener(SWT.Dispose, new Listener()
        {
			public void handleEvent(Event arg0)
			{
				shell.dispose();
				dispose();
			}
		});

        shell.addListener(SWT.Paint, new Listener()
        {
            public void handleEvent(Event e)
            {
                onPaint(e.gc);
            }
        });
    }

    /**
     * Paints the tooltip.
     *
     * @param gc
     */
    private void onPaint(GC gc)
    {
        gc.drawRectangle(0, 0, shell.getSize().x - 1, shell.getSize().y - 1);

        gc.drawString(text, xmargin, ymargin, true);
    }

    /**
     * Sets the location of the tooltip.
     *
     * @param location
     */
    public void setLocation(Point location)
    {
        shell.setLocation(location.x - xmargin, location.y - ymargin);
    }

    /**
     * Shows or hides the tooltip.
     *
     * @param visible
     */
    public void setVisible(boolean visible)
    {
        if (visible && shell.getVisible())
        {
            shell.redraw();
        }
        else
        {
            shell.setVisible(visible);
        }
    }

    /**
     * @param font
     */
    public void setFont(Font font)
    {
        shell.setFont(font);
    }

    /**
     * @return the text
     */
    public String getText()
    {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text)
    {
        this.text = text;

        GC gc = new GC(shell);
        Point size = gc.stringExtent(text);
        gc.dispose();

        size.x += xmargin + xmargin;
        size.y += ymargin + ymargin;

        shell.setSize(size);

    }

    /**
     * {@inheritDoc}
     */
    protected void checkSubclass()
    {

    }

}
