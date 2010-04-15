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

	private LightGrid table;

	private GridColumn column;

	ControlListener columnListener;

	Listener resizeListener;

	private final Listener columnVisibleListener;

	private final Listener columnGroupListener;

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

		this.table = column.getParent();
		this.column = column;

		columnListener = new ControlListener() {
			public void controlMoved(ControlEvent e) {
				table.getDisplay().asyncExec(new Runnable() {

					public void run() {
						layout();
					}

				});
			}

			public void controlResized(ControlEvent e) {
				layout();
			}
		};

		columnVisibleListener = new Listener() {
			public void handleEvent(Event event) {
				getEditor().setVisible(column.isVisible());
				// if (getEditor().isVisible())
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

		columnGroupListener = new Listener() {
			public void handleEvent(Event event) {
				if (getEditor() == null || getEditor().isDisposed())
					return;
				getEditor().setVisible(column.isVisible());
				// if (getEditor().isVisible())
				layout();
			}
		};

		// Reset the mouse cursor when the mouse hovers the control
		mouseOverListener = new Listener() {

			public void handleEvent(Event event) {
				if (table.getCursor() != null) {
					// We need to reset because it could be that when we left the resizer was active
					table.hoveringOnColumnResizer=false;
					table.setCursor(null);
				}
			}

		};

		// The following three listeners are workarounds for
		// Eclipse bug 105764
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=105764
		table.addListener(SWT.Resize, resizeListener);

		if (table.getVerticalScrollBarProxy() != null) {
			table.getVerticalScrollBarProxy().addSelectionListener(
					scrollListener);
		}
		if (table.getHorizontalScrollBarProxy() != null) {
			table.getHorizontalScrollBarProxy().addSelectionListener(
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
	 * table. The Table and the editor Control are <b>not</b> disposed.
	 */
	public void dispose() {
		if (!table.isDisposed() && !column.isDisposed()) {
			column.removeControlListener(columnListener);
			if (column.getColumnGroup() != null) {
				column.getColumnGroup().removeListener(SWT.Expand,
						columnGroupListener);
				column.getColumnGroup().removeListener(SWT.Collapse,
						columnGroupListener);
			}
		}

		if (!table.isDisposed()) {
			table.removeListener(SWT.Resize, resizeListener);

			if (table.getVerticalScrollBarProxy() != null)
				table.getVerticalScrollBarProxy().removeSelectionListener(
						scrollListener);

			if (table.getHorizontalScrollBarProxy() != null)
				table.getHorizontalScrollBarProxy().removeSelectionListener(
						scrollListener);
		}

		columnListener = null;
		resizeListener = null;
		table = null;
		super.dispose();
	}

	/**
	 * Sets the zero based index of the column of the cell being tracked by this
	 * editor.
	 * 
	 * @param column
	 *            the zero based index of the column of the cell being tracked
	 *            by this editor
	 */
	void initColumn() {

		column.addControlListener(columnListener);
		column.addListener(SWT.Show, columnVisibleListener);
		column.addListener(SWT.Hide, columnVisibleListener);

		if (column.getColumnGroup() != null) {
			column.getColumnGroup()
					.addListener(SWT.Expand, columnGroupListener);
			column.getColumnGroup().addListener(SWT.Collapse,
					columnGroupListener);
		}
		layout();
	}

	/**
	 * {@inheritDoc}
	 */
	public void layout() {
		if (table.isDisposed())
			return;

		boolean hadFocus = false;
		if (getEditor() == null || getEditor().isDisposed()
				|| !column.isVisible()) {
			return;
		}

		if (getEditor().getVisible()) {
			hadFocus = getEditor().isFocusControl();
		}

		Rectangle rect = internalComputeBounds();
		if (rect == null || rect.x < 0) {
			getEditor().setVisible(false);
			return;
		} else if(table.getItemHeaderWidth()>0&&table.getItemHeaderWidth()>rect.x){
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
