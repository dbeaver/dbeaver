/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Manager for a Control that appears below the grid column header. Based on
 * {@link GridEditor}.
 */
class GridHeaderEditor extends ControlEditor {

	private LightGrid grid;
	private GridColumn column;
	private ControlListener columnListener;
	private Listener resizeListener;

	private final SelectionListener scrollListener;
	private final Listener mouseOverListener;

	/**
	 * Creates a TableEditor for the specified Table.
	 * 
	 * @param column
	 *            the Table Control above which this editor will be displayed
	 */
	GridHeaderEditor(final GridColumn column) {
		super(column.getParent());

		this.grid = column.getParent();
		this.column = column;

		columnListener = new ControlListener() {
			public void controlMoved(ControlEvent e) {
				grid.getDisplay().asyncExec(new Runnable() {

					public void run() {
						layout();
					}

				});
			}

			public void controlResized(ControlEvent e) {
				layout();
			}
		};

		resizeListener = new Listener() {
			public void handleEvent(Event event) {
				layout();
			}
		};

		scrollListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				layout();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		// Reset the mouse cursor when the mouse hovers the control
		mouseOverListener = new Listener() {

			public void handleEvent(Event event) {
				if (grid.getCursor() != null) {
					// We need to reset because it could be that when we left the resizer was active
					grid.hoveringOnColumnResizer=false;
					grid.setCursor(null);
				}
			}

		};

		// The following three listeners are workarounds for
		// Eclipse bug 105764
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=105764
		grid.addListener(SWT.Resize, resizeListener);

		if (grid.getVerticalScrollBarProxy() != null) {
			grid.getVerticalScrollBarProxy().addSelectionListener(
					scrollListener);
		}
		if (grid.getHorizontalScrollBarProxy() != null) {
			grid.getHorizontalScrollBarProxy().addSelectionListener(
					scrollListener);
		}

		// To be consistent with older versions of SWT, grabVertical defaults to
		// true
		grabVertical = true;

		// initColumn();
	}

	/**
	 * Returns the bounds of the editor.
	 * 
	 * @return bounds of the editor.
	 */
	protected Rectangle internalComputeBounds() {
		column.getHeaderRenderer().setBounds(column.getBounds());
		return column.getHeaderRenderer().getControlBounds(column, true);
	}

	/**
	 * Removes all associations between the TableEditor and the cell in the
	 * grid. The Table and the editor Control are <b>not</b> disposed.
	 */
	public void dispose() {
		if (!grid.isDisposed() && !column.isDisposed()) {
			column.removeControlListener(columnListener);
		}

		if (!grid.isDisposed()) {
			grid.removeListener(SWT.Resize, resizeListener);

			if (grid.getVerticalScrollBarProxy() != null)
				grid.getVerticalScrollBarProxy().removeSelectionListener(
						scrollListener);

			if (grid.getHorizontalScrollBarProxy() != null)
				grid.getHorizontalScrollBarProxy().removeSelectionListener(
						scrollListener);
		}

		columnListener = null;
		resizeListener = null;
		grid = null;
		super.dispose();
	}

	/**
	 * Sets the zero based index of the column of the cell being tracked by this
	 * editor.
	 */
	void initColumn() {

		column.addControlListener(columnListener);
		layout();
	}

	/**
	 * {@inheritDoc}
	 */
	public void layout() {
		if (grid.isDisposed())
			return;

		boolean hadFocus = false;
		if (getEditor() == null || getEditor().isDisposed()) {
			return;
		}

		if (getEditor().getVisible()) {
			hadFocus = getEditor().isFocusControl();
		}

		Rectangle rect = internalComputeBounds();
		if (rect == null || rect.x < 0) {
			getEditor().setVisible(false);
			return;
		} else if(grid.getItemHeaderWidth()>0&& grid.getItemHeaderWidth()>rect.x){
			getEditor().setVisible(false);
		    return;
		}else {
			getEditor().setVisible(true);
		}
		getEditor().setBounds(rect);

		if (hadFocus) {
			if (getEditor() == null || getEditor().isDisposed())
				return;
			getEditor().setFocus();
		}
	}

	public void setEditor(Control editor) {
		if (getEditor() != null) {
			getEditor().removeListener(SWT.MouseEnter, mouseOverListener);
		}
		super.setEditor(editor);

		if (editor != null) {
			getEditor().addListener(SWT.MouseEnter, mouseOverListener);
		}
	}

}
