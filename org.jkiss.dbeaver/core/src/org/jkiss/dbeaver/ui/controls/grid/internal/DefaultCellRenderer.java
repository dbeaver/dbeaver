/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    chris.gross@us.ibm.com - initial API and implementation
 *    Chuck.Mastrandrea@sas.com - wordwrapping in bug 222280
 *    smcduff@hotmail.com - wordwrapping in bug 222280
 *    Claes Rosell<claes.rosell@solme.se> - rowspan in bug 272384
 *******************************************************************************/
package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.Grid;
import org.jkiss.dbeaver.ui.controls.grid.GridCellRenderer;
import org.jkiss.dbeaver.ui.controls.grid.GridColumn;
import org.jkiss.dbeaver.ui.controls.grid.GridItem;
import org.jkiss.dbeaver.ui.controls.grid.IInternalWidget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;

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

    private ToggleRenderer toggleRenderer;

	private BranchRenderer branchRenderer;

    private CheckBoxRenderer checkRenderer;

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

        if (isTree())
        {
        	boolean renderBranches = item.getParent().getTreeLinesVisible();
        	if(renderBranches) {
        		branchRenderer.setBranches(getBranches(item));
        		branchRenderer.setIndent(treeIndent);
        		branchRenderer.setBounds(
        				getBounds().x + x, getBounds().y,
        				getToggleIndent(item), getBounds().height + 1); // Take into account border
        	}

            x += getToggleIndent(item);

            toggleRenderer.setExpanded(item.isExpanded());

            toggleRenderer.setHover(getHoverDetail().equals("toggle"));

            toggleRenderer.setLocation(getBounds().x + x, (getBounds().height - toggleRenderer
                .getBounds().height)
                                                          / 2 + getBounds().y);
            if (item.hasChildren())
                toggleRenderer.paint(gc, null);

            if (renderBranches) {
                branchRenderer.setToggleBounds(toggleRenderer.getBounds());
                branchRenderer.paint(gc, null);
            }

            x += toggleRenderer.getBounds().width + insideMargin;

        }

        if (isCheck())
        {
            checkRenderer.setChecked(item.getChecked(getColumn()));
            checkRenderer.setGrayed(item.getGrayed(getColumn()));
            if (!item.getParent().isEnabled())
            {
                checkRenderer.setGrayed(true);
            }
            checkRenderer.setHover(getHoverDetail().equals("check"));

        	if (isCenteredCheckBoxOnly(item))
        	{
        		//Special logic if this column only has a checkbox and is centered
                checkRenderer.setBounds(getBounds().x + ((getBounds().width - checkRenderer.getBounds().width) /2),
                		                (getBounds().height - checkRenderer.getBounds().height)
                                            / 2 + getBounds().y, checkRenderer
                                          .getBounds().width, checkRenderer.getBounds().height);
        	}
        	else
        	{
                checkRenderer.setBounds(getBounds().x + x, (getBounds().height - checkRenderer
                        .getBounds().height)
                                                               / 2 + getBounds().y, checkRenderer
                        .getBounds().width, checkRenderer.getBounds().height);

                    x += checkRenderer.getBounds().width + insideMargin;
        	}

        	checkRenderer.paint(gc, null);
        }

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
     * Calculates the sequence of branch lines which should be rendered for the provided item
     * @param item
     * @return an array of integers composed using the constants in {@link BranchRenderer}
     */
    private int[] getBranches(GridItem item) {
		int[] branches = new int[item.getLevel() + 1];
		GridItem[] roots = item.getParent().getRootItems();

		// Is this a node or a leaf?
		if (item.getParentItem() == null) {
			// Add descender if not last item
			if (!item.isExpanded() && roots[roots.length-1].equals(item)) {
				if (item.hasChildren())
					branches[item.getLevel()] = BranchRenderer.LAST_ROOT;
				else
					branches[item.getLevel()] = BranchRenderer.SMALL_L;
			}
			else {
				if (item.hasChildren())
					branches[item.getLevel()] = BranchRenderer.ROOT;
				else
					branches[item.getLevel()] = BranchRenderer.SMALL_T;
			}

		}
		else if (item.hasChildren())
			if (item.isExpanded())
				branches[item.getLevel()] = BranchRenderer.NODE;
			else
				branches[item.getLevel()] = BranchRenderer.NONE;
		else
			branches[item.getLevel()] = BranchRenderer.LEAF;

		// Branch for current item
		GridItem parent = item.getParentItem();
		if (parent == null)
			return branches;

		// Are there siblings below this item?
		if (parent.indexOf(item) < parent.getItemCount() - 1)
			branches[item.getLevel() - 1] = BranchRenderer.T;

		// Is the next node a root?
		else if (parent.getParentItem() == null && !parent.equals(roots[roots.length - 1]))
			branches[item.getLevel() - 1] = BranchRenderer.T;

		// This must be the last element at this level
		else
			branches[item.getLevel() - 1] = BranchRenderer.L;

		Grid grid = item.getParent();
		item = parent;
		parent = item.getParentItem();

		// Branches for parent items
		while(item.getLevel() > 0) {
			if (parent.indexOf(item) == parent.getItemCount() - 1) {
				if (parent.getParentItem() == null && !grid.getRootItem(grid.getRootItemCount() - 1).equals(parent))
					branches[item.getLevel() - 1] = BranchRenderer.I;
				else
					branches[item.getLevel() - 1] = BranchRenderer.NONE;
			}
			else
				branches[item.getLevel() - 1] = BranchRenderer.I;
			item = parent;
			parent = item.getParentItem();
		}
		// item should be null at this point
		return branches;
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

        if (isTree())
        {
            x += getToggleIndent(item);

            x += toggleRenderer.getBounds().width + insideMargin;

        }

        if (isCheck())
        {
            x += checkRenderer.getBounds().width + insideMargin;
        }

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

        GridItem item = (GridItem)value;

        if (isCheck())
        {
            if (event == IInternalWidget.MouseMove)
            {
                if (overCheck(item, point))
                {
                    setHoverDetail("check");
                    return true;
                }
            }

            if (event == IInternalWidget.LeftMouseButtonDown)
            {
                if (overCheck(item, point))
                {
                    if (!item.getCheckable(getColumn()))
                    {
                        return false;
                    }

                    item.setChecked(getColumn(), !item.getChecked(getColumn()));
                    item.getParent().redraw();

                    item.fireCheckEvent(getColumn());

                    return true;
                }
            }
        }

        if (isTree() && item.hasChildren())
        {
            if (event == IInternalWidget.MouseMove)
            {
                if (overToggle(item, point))
                {
                    setHoverDetail("toggle");
                    return true;
                }
            }

            if (event == IInternalWidget.LeftMouseButtonDown)
            {
                if (overToggle(item, point))
                {
                    item.setExpanded(!item.isExpanded());
                    item.getParent().redraw();

                    if (item.isExpanded())
                    {
                        item.fireEvent(SWT.Expand);
                    }
                    else
                    {
                        item.fireEvent(SWT.Collapse);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean overCheck(GridItem item, Point point)
    {
    	if (isCenteredCheckBoxOnly(item))
    	{
	        point = new Point(point.x, point.y);
	        point.x -= getBounds().x;
	        point.y -= getBounds().y;

	        Rectangle checkBounds = new Rectangle(0,0,0,0);
	        checkBounds.x = (getBounds().width - checkRenderer.getBounds().width)/2;
	        checkBounds.y = ((getBounds().height - checkRenderer.getBounds().height) / 2);
	        checkBounds.width = checkRenderer.getBounds().width;
	        checkBounds.height = checkRenderer.getBounds().height;

	        return checkBounds.contains(point);
    	}
    	else
    	{
	        point = new Point(point.x, point.y);
	        point.x -= getBounds().x;
	        point.y -= getBounds().y;

	        int x = leftMargin;
	        if (isTree())
	        {
	            x += getToggleIndent(item);
	            x += toggleRenderer.getSize().x + insideMargin;
	        }

	        if (point.x >= x && point.x < (x + checkRenderer.getSize().x))
	        {
	            int yStart = ((getBounds().height - checkRenderer.getBounds().height) / 2);
	            if (point.y >= yStart && point.y < yStart + checkRenderer.getSize().y)
	            {
	                return true;
	            }
	        }

	        return false;
    	}
    }

    private int getToggleIndent(GridItem item)
    {
        return item.getLevel() * treeIndent;
    }

    private boolean overToggle(GridItem item, Point point)
    {

        point = new Point(point.x, point.y);

        point.x -= getBounds().x - 1;
        point.y -= getBounds().y - 1;

        int x = leftMargin;
        x += getToggleIndent(item);

        if (point.x >= x && point.x < (x + toggleRenderer.getSize().x))
        {
            // return true;
            int yStart = ((getBounds().height - toggleRenderer.getBounds().height) / 2);
            if (point.y >= yStart && point.y < yStart + toggleRenderer.getSize().y)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setTree(boolean tree)
    {
        super.setTree(tree);

        if (tree)
        {
            toggleRenderer = new ToggleRenderer();
            toggleRenderer.setDisplay(getDisplay());

            branchRenderer = new BranchRenderer();
            branchRenderer.setDisplay(getDisplay());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCheck(boolean check)
    {
        super.setCheck(check);

        if (check)
        {
            checkRenderer = new CheckBoxRenderer();
            checkRenderer.setDisplay(getDisplay());
        }
        else
        {
            checkRenderer = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getTextBounds(GridItem item, boolean preferred)
    {
        int x = leftMargin;

        if (isTree())
        {
            x += getToggleIndent(item);

            x += toggleRenderer.getBounds().width + insideMargin;

        }

        if (isCheck())
        {
            x += checkRenderer.getBounds().width + insideMargin;
        }

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

    private boolean isCenteredCheckBoxOnly(GridItem item)
    {
    	return !isTree() && item.getImage(getColumn()) == null && item.getText(getColumn()).equals("")
		&& getAlignment() == SWT.CENTER;
    }
}
