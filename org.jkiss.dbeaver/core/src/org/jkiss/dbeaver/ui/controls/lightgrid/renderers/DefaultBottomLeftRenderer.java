/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The renderer for the empty top left area when both column and row headers are
 * visible.
 * 
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultBottomLeftRenderer extends AbstractRenderer {

	/**
	 * {@inheritDoc}
	 */
	public Point computeSize(GC gc, int wHint, int hHint, Object value) {
		return new Point(wHint, hHint);
	}

	/**
	 * {@inheritDoc}
	 */
	public void paint(GC gc, Object value) {
		gc.setBackground(getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));

		gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
				getBounds().height + 1);

		gc.setForeground(getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_DARK_SHADOW));

		gc.drawLine(getBounds().x, getBounds().y, getBounds().x
				+ getBounds().width, getBounds().y);

	}

}
