package org.jkiss.dbeaver.ui.controls.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;

import org.jkiss.dbeaver.ui.controls.grid.Grid.GridVisibleRange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * This support class adds the possibility to get informed when the visual range
 * in the grid is modified. E.g. to implement clever resource management
 * <p>
 * <b>This support is provisional and may change</b>
 * </p>
 */
public class GridVisibleRangeSupport {
	private Collection rangeChangeListener;
	private Grid grid;
	private GridVisibleRange oldRange = new GridVisibleRange();

	private Listener paintListener = new Listener() {

		public void handleEvent(Event event) {
			calculateChange();
		}

	};

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
		public GridItem[] addedRows;

		/**
		 * Rows removed from the range
		 */
		public GridItem[] removedRows;

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

		RangeChangedEvent(Grid grid, GridVisibleRange visibleRange) {
			super(grid);
			this.visibleRange = visibleRange;
		}

	}

	private GridVisibleRangeSupport(Grid grid) {
		this.grid = grid;
		this.grid.setSizeOnEveryItemImageChange(true);
		// FIXME Maybe better to listen to resize, ... ?
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
			rangeChangeListener = new ArrayList();
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

			ArrayList lOrigItems = new ArrayList();
			lOrigItems.addAll(Arrays.asList(oldRange.getItems()));

			ArrayList lNewItems = new ArrayList();
			lNewItems.addAll(Arrays.asList(range.getItems()));

			Iterator it = lNewItems.iterator();
			while (it.hasNext()) {
				if (lOrigItems.remove(it.next())) {
					it.remove();
				}
			}

			ArrayList lOrigColumns = new ArrayList();
			lOrigColumns.addAll(Arrays.asList(oldRange.getColumns()));

			ArrayList lNewColumns = new ArrayList();
			lNewColumns.addAll(Arrays.asList(range.getColumns()));

			it = lNewColumns.iterator();
			while (it.hasNext()) {
				if (lOrigColumns.remove(it.next())) {
					it.remove();
				}
			}

			if (lOrigItems.size() != 0 || lNewItems.size() != 0
					|| lOrigColumns.size() != 0 || lNewColumns.size() != 0) {
				RangeChangedEvent evt = new RangeChangedEvent(grid, range);
				evt.addedRows = new GridItem[lNewItems.size()];
				lNewItems.toArray(evt.addedRows);

				evt.removedRows = new GridItem[lOrigItems.size()];
				lOrigItems.toArray(evt.removedRows);

				evt.addedColumns = new GridColumn[lNewColumns.size()];
				lNewColumns.toArray(evt.addedColumns);

				evt.removedColumns = new GridColumn[lOrigColumns.size()];
				lNewColumns.toArray(evt.removedColumns);
				it = rangeChangeListener.iterator();
				while (it.hasNext()) {
					((VisibleRangeChangedListener) it.next()).rangeChanged(evt);
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
	public static GridVisibleRangeSupport createFor(Grid grid) {
		return new GridVisibleRangeSupport(grid);
	}

}
