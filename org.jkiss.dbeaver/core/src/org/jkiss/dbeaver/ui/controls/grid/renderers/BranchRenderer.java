/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.renderers;

import org.jkiss.dbeaver.ui.controls.grid.renderers.AbstractRenderer;
import org.jkiss.dbeaver.ui.controls.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Renders the tree branch hierarchy for a {@link GridColumn}
 * @author Michael Houston <schmeeky@gmail.com>
 */
public class BranchRenderer extends AbstractRenderer {

	private static final int[] LINE_STYLE = new int[] {1,1};
	
	// Line segments
	/** A full-width horizontal bar */
	public static final int H_FULL = 1;
	
	/** A horizontal bar from the centre to the right */
	public static final int H_RIGHT = 1 << 1;
	
	/** A horizontal bar from the centre to the toggle */
	public static final int H_CENTRE_TOGGLE = 1 << 2;
	
	/** A horizontal bar from the left to the toggle */
	public static final int H_LEFT_TOGGLE = 1 << 3;
	
	/** A full-height vertical bar */
	public static final int V_FULL = 1 << 4;
	
	/** A vertical bar from the top to the middle */
	public static final int V_TOP = 1 << 5;
	
	/** A vertical bar from the toggle to the bottom  */
	public static final int DESCENDER = 1 << 6;
	
	/** A vertical bar from the top to the toggle  */
	public static final int ASCENDER = 1 << 7;

	// Predefined combinations
	/** Indicates that a branch should not be rendered. */
	public static final int NONE = 0;
	
	/** Indicates that a branch should be rendered as a 'T' shape. This
	 *   is used for normal children with following siblings	*/
	public static final int T = V_FULL | H_RIGHT;

	/** Indicates that a branch should be rendered as an 'L' shape. This
	 *   is used for the last child element */
	public static final int L = V_TOP | H_RIGHT;
	
	/** Indicates that a branch should be rendered as a 'I' shape. This
	 *   is used for connecting children when intermediate children are shown. */
	public static final int I = V_FULL;
	
	/** Indicates that the toggle decoration for an expanded parent should be drawn */
	public static final int NODE = DESCENDER;
	
	/** Indicates that the decoration for a leaf node should be drawn */
	public static final int LEAF = H_LEFT_TOGGLE;
	
	/** Indicates that the decoration for a root node should be drawn */
	public static final int ROOT = ASCENDER | DESCENDER;
	
	/** Indicates that the decoration for the last root node should be drawn */
	public static final int LAST_ROOT = ASCENDER;

	/** A half-width T used on roots with no children */
	public static final int SMALL_T = V_FULL | H_CENTRE_TOGGLE;
	
	/** A half-width L used on roots with no children */
	public static final int SMALL_L = V_TOP | H_CENTRE_TOGGLE;
	

	private int indent;
	private int[] branches;
	private Rectangle toggleBounds;
	
	/**
	 * {@inheritDoc}
	 */
	public Point computeSize(GC gc, int hint, int hint2, Object value) {
		return new Point(getBounds().width, getBounds().height);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void paint(GC gc, Object value) {
		Rectangle bounds = getBounds();
		
		int xLeft = bounds.x;
		int yTop = bounds.y - 1;
		
		int yBottom = yTop + bounds.height;
		int yMiddle = toggleBounds.y + toggleBounds.height / 2;
		int yToggleBottom = toggleBounds.y + toggleBounds.height - 1;
		int yToggleTop = toggleBounds.y;
		
		int oldStyle = gc.getLineStyle();
		Color oldForeground = gc.getForeground();
		int[] oldLineDash = gc.getLineDash();
		
		gc.setForeground(getDisplay().getSystemColor(
				isSelected() ? 
						SWT.COLOR_LIST_SELECTION_TEXT 
						: SWT.COLOR_WIDGET_NORMAL_SHADOW));

		int dy = bounds.y % 2;

		// Set line style to dotted
		gc.setLineDash(LINE_STYLE);
		
		// Adjust line positions by a few pixels to create correct effect
		yToggleTop --;
		yTop ++;
		yToggleBottom ++;
		
		// Adjust full height
		// If height is even, we shorten to an odd number of pixels, and start at the original y offset
		if (bounds.height % 2 == 0) {
			yBottom -= 1;
		}
		// If height is odd, we alternate based on the row offset
		else {
			yTop += dy;
			yBottom -= dy;
		}

		// Adjust ascender and descender
		yToggleBottom += dy;

		if ((yToggleTop - yTop + 1) % 2 == 0)
			yToggleTop -= 1;
		if ((yToggleBottom - yBottom + 1) % 2 == 0)
			yToggleBottom += dy == 1 ? -1 : 1;
		
		for (int i = 0; i < branches.length; i++) {
			// Calculate offsets for this branch
			int xRight = xLeft + indent;
			int xMiddle = xLeft + toggleBounds.width / 2;
			int xMiddleBranch = xMiddle;
			int xToggleRight = xLeft + toggleBounds.width;

			int dx = 0;
			xRight --;
			xMiddleBranch += 2;
			xToggleRight --;
			
			if (indent % 2 == 0) {
				xRight -= 1;
			}
			else {
				dx = xLeft % 2;
				xLeft += dx;
				xRight -= dx;
			}
			
			// Render line segments
			if ((branches[i] & H_FULL) == H_FULL)
				gc.drawLine(xLeft, yMiddle, xRight, yMiddle);
			if ((branches[i] & H_RIGHT) == H_RIGHT)
				gc.drawLine(xMiddleBranch, yMiddle, xRight, yMiddle);
			if ((branches[i] & H_CENTRE_TOGGLE) == H_CENTRE_TOGGLE)
				gc.drawLine(xMiddleBranch, yMiddle, xToggleRight, yMiddle);
			if ((branches[i] & H_LEFT_TOGGLE) == H_LEFT_TOGGLE)
				gc.drawLine(xLeft, yMiddle, xToggleRight, yMiddle);
			if ((branches[i] & V_FULL) == V_FULL)
				gc.drawLine(xMiddle, yTop, xMiddle, yBottom);
			if ((branches[i] & V_TOP) == V_TOP)
				gc.drawLine(xMiddle, yTop, xMiddle, yMiddle);
			if ((branches[i] & ASCENDER) == ASCENDER)
				gc.drawLine(xMiddle, yTop, xMiddle, yToggleTop);
			if ((branches[i] & DESCENDER) == DESCENDER)
				gc.drawLine(xMiddle, yToggleBottom, xMiddle, yBottom);
			
			xLeft += indent - dx;
		}

		gc.setLineDash(oldLineDash);
		gc.setLineStyle(oldStyle);
		gc.setForeground(oldForeground);
	}

	/**
	 * Sets the branches that will be used. The values are taken from the constants in this class such as
	 * I, L, T, NODE, LEAF and NONE, which represent the branch type to be used for each level.
	 * @param branches an array of branch types
	 */
	public void setBranches(int[] branches) {
		this.branches = branches;
	}

	/**
	 * Sets the indent used for rendering the tree branches
	 * @param toggleIndent the indent used for the tree
	 */
	public void setIndent(int toggleIndent) {
		this.indent = toggleIndent;
	}

	/**
	 * Sets bounds of the toggle control. This is used to position the downwards branches
	 * @param toggleBounds the bounds of the toggle control
	 */
	public void setToggleBounds(Rectangle toggleBounds) {
		this.toggleBounds = toggleBounds;
	}

}
