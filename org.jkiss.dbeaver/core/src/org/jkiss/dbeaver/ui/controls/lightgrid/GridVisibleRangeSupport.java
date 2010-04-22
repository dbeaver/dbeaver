/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import java.util.*;

/**
 * This support class adds the possibility to get informed when the visual range
 * in the grid is modified. E.g. to implement clever resource management
 * <p>
 * <b>This support is provisional and may change</b>
 * </p>
 */
public class GridVisibleRangeSupport {
	private List<VisibleRangeChangedListener> rangeChangeListener;
	private LightGrid grid;
	private GridVisibleRange oldRange = new GridVisibleRange();

    /**
	 * Listener notified when the visible range changes
	 */
	public interface VisibleRangeChangedListener {
		/**
		 * Method called when range is changed
		 * 
		 * @param event
		 *            the event holding informations about the change
		 */
		public void rangeChanged(RangeChangedEvent event);
	}

	/**
	 * Event informing about the change
	 */
	public static class RangeChangedEvent extends EventObject {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Rows new in the visible range
		 */
		public Integer[] addedRows;

		/**
		 * Rows removed from the range
		 */
		public Integer[] removedRows;

		/**
		 * Columns added to the range
		 */
		public GridColumn[] addedColumns;

		/**
		 * Columns removed from the range
		 */
		public GridColumn[] removedColumns;

		/**
		 * The current visible range
		 */
		public GridVisibleRange visibleRange;

		RangeChangedEvent(LightGrid grid, GridVisibleRange visibleRange) {
			super(grid);
			this.visibleRange = visibleRange;
		}

	}

	private GridVisibleRangeSupport(LightGrid grid) {
		this.grid = grid;
		// FIXME Maybe better to listen to resize, ... ?
        Listener paintListener = new Listener() {

            public void handleEvent(Event event) {
                calculateChange();
            }

        };
        grid.addListener(SWT.Paint, paintListener);
	}

	/**
	 * Add a listener who is informed when the range is changed
	 * 
	 * @param listener
	 *            the listener to add
	 */
	public void addRangeChangeListener(VisibleRangeChangedListener listener) {
		if (rangeChangeListener == null) {
			rangeChangeListener = new ArrayList<VisibleRangeChangedListener>();
		}
		rangeChangeListener.add(listener);
	}

	/**
	 * Remove the listener from the ones informed when the range is changed
	 * 
	 * @param listener
	 */
	public void removeRangeChangeListener(VisibleRangeChangedListener listener) {
		if (rangeChangeListener != null) {
			rangeChangeListener.remove(listener);
			if (rangeChangeListener.size() == 0) {
				rangeChangeListener = null;
			}
		}
	}

	private void calculateChange() {
		// FIXME Add back
		if (rangeChangeListener != null) {
			GridVisibleRange range = grid.getVisibleRange();

			List<Integer> lOrigItems = new ArrayList<Integer>();
            for (int row : oldRange.getItems()) {
			    lOrigItems.add(row);
            }

			List<Integer> lNewItems = new ArrayList<Integer>();
            for (int row : range.getItems()) {
                lOrigItems.add(row);
            }

			Iterator<Integer> it = lNewItems.iterator();
			while (it.hasNext()) {
				if (lOrigItems.remove(it.next())) {
					it.remove();
				}
			}

			List<GridColumn> lOrigColumns = new ArrayList<GridColumn>();
			lOrigColumns.addAll(Arrays.asList(oldRange.getColumns()));

			List<GridColumn> lNewColumns = new ArrayList<GridColumn>();
			lNewColumns.addAll(Arrays.asList(range.getColumns()));

			Iterator<GridColumn> it2 = lNewColumns.iterator();
			while (it2.hasNext()) {
				if (lOrigColumns.remove(it2.next())) {
					it2.remove();
				}
			}

			if (lOrigItems.size() != 0 || lNewItems.size() != 0
					|| lOrigColumns.size() != 0 || lNewColumns.size() != 0) {
				RangeChangedEvent evt = new RangeChangedEvent(grid, range);
				evt.addedRows = new Integer[lNewItems.size()];
				lNewItems.toArray(evt.addedRows);

				evt.removedRows = new Integer[lOrigItems.size()];
				lOrigItems.toArray(evt.removedRows);

				evt.addedColumns = new GridColumn[lNewColumns.size()];
				lNewColumns.toArray(evt.addedColumns);

				evt.removedColumns = new GridColumn[lOrigColumns.size()];
				lNewColumns.toArray(evt.removedColumns);
                for (VisibleRangeChangedListener aRangeChangeListener : rangeChangeListener) {
                    aRangeChangeListener.rangeChanged(evt);
                }
			}

			oldRange = range;
		}
	}

	/**
	 * Create a range support for the given grid instance
	 * 
	 * @param grid
	 *            the grid instance the range support is created for
	 * @return the created range support
	 */
	public static GridVisibleRangeSupport createFor(LightGrid grid) {
		return new GridVisibleRangeSupport(grid);
	}

}
