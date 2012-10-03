/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
			@Override
            public void controlMoved(ControlEvent e) {
				grid.getDisplay().asyncExec(new Runnable() {

					@Override
                    public void run() {
						layout();
					}

				});
			}

			@Override
            public void controlResized(ControlEvent e) {
				layout();
			}
		};

		resizeListener = new Listener() {
			@Override
            public void handleEvent(Event event) {
				layout();
			}
		};

		scrollListener = new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				layout();
			}

			@Override
            public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		// Reset the mouse cursor when the mouse hovers the control
		mouseOverListener = new Listener() {

			@Override
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
	@Override
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
	@Override
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

	@Override
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
