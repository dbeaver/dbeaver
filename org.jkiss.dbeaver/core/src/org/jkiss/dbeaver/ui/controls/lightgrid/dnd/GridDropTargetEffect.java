/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.dnd;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEffect;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import  org.jkiss.dbeaver.ui.controls.lightgrid.GridItem;
import  org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;

/**
 * This class provides a default drag under effect (eg. select, insert, scroll and expand) 
 * when a drag occurs over a <code>Grid</code>.
 * 
 * <p>Classes that wish to provide their own drag under effect for a <code>Grid</code>
 * can extend the <code>DropTargetAdapter</code> class and override any applicable methods 
 * in <code>DropTargetAdapter</code> to display their own drag under effect.</p>
 *
 * Subclasses that override any methods of this class must call the corresponding
 * <code>super</code> method to get the default drag under effect implementation.
 *
 * <p>The feedback value is either one of the FEEDBACK constants defined in 
 * class <code>DND</code> which is applicable to instances of this class, 
 * or it must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>DND</code> effect constants. 
 * </p>
 * <p>
 * <dl>
 * <dt><b>Feedback:</b></dt>
 * <dd>FEEDBACK_SELECT, FEEDBACK_INSERT_BEFORE, FEEDBACK_INSERT_AFTER, FEEDBACK_EXPAND, FEEDBACK_SCROLL</dd>
 * </dl>
 * </p><p>
 * Note: Only one of the styles FEEDBACK_SELECT, FEEDBACK_INSERT_BEFORE or
 * FEEDBACK_INSERT_AFTER may be specified.
 * </p>
 * 
 * @see DropTargetAdapter
 * @see DropTargetEvent
 * 
 * @since 3.3
 * @author mark-oliver.reiser
 */
public class GridDropTargetEffect extends DropTargetEffect {
	static final int SCROLL_HYSTERESIS = 200; // milli seconds
	static final int EXPAND_HYSTERESIS = 1000; // milli seconds

	private LightGrid grid;
	private boolean		ignoreCellSelection = false;
	
	private GridItem scrollItem;
	private long		scrollBeginTime;
	private GridItem	expandItem;
	private long		expandBeginTime;
	
	private Point		insertCell;
	private boolean		insertBefore;
	private Point		selectedCell;
	
	/**
	 * Creates a new <code>GridDropTargetEffect</code> to handle the drag under effect on the specified 
	 * <code>Grid</code>.
	 * 
	 * @param grid the <code>Grid</code> over which the user positions the cursor to drop the data
	 */
	public GridDropTargetEffect(LightGrid grid) {
		super(grid);
		this.grid = grid;
	}
	
	
	/**
	 * Set this value to true to make drop feedback in <code>Grid</code>
	 * always behave like the Grid was <em>not</em> in cell selection mode.
	 * The default is false.
	 * 
	 * <p>A value of true, means that for {@link DND#FEEDBACK_SELECT} full rows will
	 * be selected instead of cells and for {@link DND#FEEDBACK_INSERT_AFTER} and
	 * {@link DND#FEEDBACK_INSERT_BEFORE} the insert mark will span all columns.
	 * 
	 * @param ignore
	 */
	public void setIgnoreCellSelection(boolean ignore) {
		ignoreCellSelection = ignore;
	}
	
	/**
	 * 
	 * @return true if cell selection mode is ignored
	 * @see #setIgnoreCellSelection(boolean)
	 */
	public boolean getIgnoreCellSelection() {
		return ignoreCellSelection;
	}
	
	
	int checkEffect(int effect) {
		// Some effects are mutually exclusive.  Make sure that only one of the mutually exclusive effects has been specified.
		if ((effect & DND.FEEDBACK_SELECT) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER & ~DND.FEEDBACK_INSERT_BEFORE;
		if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER;
		return effect;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Widget getItem(int x, int y) {
		Point coordinates = new Point(x, y);
		coordinates = grid.toControl(coordinates);
		//return grid.getItem(coordinates);
        return null;
	}

	/**
	 * This implementation of <code>dragEnter</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragEnter</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragEnter(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag enter event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	public void dragEnter(DropTargetEvent event) {
		expandBeginTime = 0;
		expandItem = null;
		scrollBeginTime = 0;
		scrollItem = null;
		
		insertCell = null;
		selectedCell = null;
	}
	
	/**
	 * This implementation of <code>dragLeave</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragLeave</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragLeave(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag leave event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	public void dragLeave(DropTargetEvent event) {
		if (selectedCell != null)
		{
			deselect(selectedCell);
			selectedCell = null;
		}
		if (insertCell != null)
		{
			setInsertMark(null, false);
			insertCell = null;
		}
		expandBeginTime = 0;
		expandItem = null;
		scrollBeginTime = 0;
		scrollItem = null;
	}

	/**
	 * This implementation of <code>dragOver</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragOver</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragOver(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag over event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 * @see DND#FEEDBACK_SELECT
	 * @see DND#FEEDBACK_INSERT_BEFORE
	 * @see DND#FEEDBACK_INSERT_AFTER
	 * @see DND#FEEDBACK_SCROLL
	 * @see DND#FEEDBACK_EXPAND
	 */
	public void dragOver(DropTargetEvent event) {
		int effect = checkEffect(event.feedback);
		Point coordinates = new Point(event.x, event.y);
		coordinates = grid.toControl(coordinates);
		GridItem hoverItem = grid.getItem(coordinates);
		GridColumn hoverColumn = grid.getColumn(coordinates);
		Point hoverCell = grid.getCell(coordinates);
		
		if (hoverItem == null || hoverColumn == null || hoverCell == null)
		{
			hoverItem = null;
			hoverColumn = null;
			hoverCell = null;
		}
		
		// handle scrolling
		if ((effect & DND.FEEDBACK_SCROLL) == 0)
		{
			scrollBeginTime = 0;
			scrollItem = null;
		}
		else
		{
			if (hoverItem != null && scrollItem == hoverItem && scrollBeginTime != 0)
			{
				if (System.currentTimeMillis() >= scrollBeginTime)
				{
                    int hoverRow = grid.indexOf(hoverItem);
					int topItem = grid.getTopIndex();
					int nextItem = hoverRow == topItem ? hoverRow - 1 : hoverRow + 1;
					boolean scroll = nextItem != -1 && grid.isInDragScrollArea(coordinates);
					if (scroll)
					{
						grid.showItem(nextItem);
					}
					scrollBeginTime = 0;
					scrollItem = null;
				}
			}
			else
			{
				scrollBeginTime = System.currentTimeMillis() + SCROLL_HYSTERESIS;
				scrollItem = hoverItem;
			}
		}
		
		
		// handle expand
		if ((effect & DND.FEEDBACK_EXPAND) == 0) {
			expandBeginTime = 0;
			expandItem = null;
		} else {
			if (hoverItem != null && expandItem == hoverItem && expandBeginTime != 0) {
				if (System.currentTimeMillis() >= expandBeginTime) {
					expandBeginTime = 0;
					expandItem = null;
				}
			} else {
				expandBeginTime = System.currentTimeMillis() + EXPAND_HYSTERESIS;
				expandItem = hoverItem;
			}
		}
		
		
		// handle select
		if ((effect & DND.FEEDBACK_SELECT) != 0 && hoverCell != null)
		{
			if (!hoverCell.equals(selectedCell))
			{
				if (selectedCell != null)
					deselect(selectedCell);
				select(hoverCell);
				selectedCell = new Point(hoverCell.x,hoverCell.y);
			}
		}
		if ((effect & DND.FEEDBACK_SELECT) == 0 && selectedCell != null)
		{
			deselect(selectedCell);
			selectedCell = null;
		}
		
		
		// handle insert mark
		if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0 || (effect & DND.FEEDBACK_INSERT_AFTER) != 0) {
			boolean before = (effect & DND.FEEDBACK_INSERT_BEFORE) != 0;
			if (hoverCell != null)
			{
				if (!hoverCell.equals(insertCell) || before != insertBefore)
				{
					setInsertMark(hoverCell, before);
				}
				insertCell = new Point(hoverCell.x,hoverCell.y);
				insertBefore = before;
			}
			else
			{
				if (insertCell != null)
				{
					setInsertMark(null, false);
				}
				insertCell = null;
			}
		}
		else
		{
			if (insertCell != null)
			{
				setInsertMark(null, false);
			}
			insertCell = null;
		}
	}
	
	private void select(Point cell) {
        grid.select(cell.y);
		
	}
	private void deselect(Point cell) {
        grid.deselect(cell.y);
		
	}
	private void setInsertMark(Point cell, boolean before) {
		if(cell!=null)
		{
            grid.setInsertMark(cell.y, grid.getColumn(cell.x), before);
		}
		else
		{
			grid.setInsertMark(-1, null, false);
		}
	}
}
