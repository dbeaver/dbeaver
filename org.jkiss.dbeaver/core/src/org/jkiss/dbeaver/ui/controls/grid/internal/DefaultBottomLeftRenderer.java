/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    chris.gross@us.ibm.com - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.AbstractRenderer;
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
