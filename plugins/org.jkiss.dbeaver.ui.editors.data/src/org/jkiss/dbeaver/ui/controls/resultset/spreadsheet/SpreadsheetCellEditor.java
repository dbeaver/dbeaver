/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * Spreadsheet cell editor
 *
 * @author serge@jkiss.org
 */
public class SpreadsheetCellEditor extends ControlEditor
{
    private LightGrid grid;

    private int column = -1;
    private int row = -1;

    private ControlListener columnListener;
    private Listener resizeListener;
    private SelectionListener scrollListener;

    /**
     * Creates a TableEditor for the specified Table.
     * 
     * @param grid the Table Control above which this editor will be displayed
     */
    public SpreadsheetCellEditor(final LightGrid grid)
    {
        super(grid);
        this.grid = grid;
        
        columnListener = new ControlListener()
        {
            @Override
            public void controlMoved(ControlEvent e)
            {
                layout();
            }

            @Override
            public void controlResized(ControlEvent e)
            {
                layout();
            }
        };
        
        resizeListener = event -> layout();
        
        scrollListener = new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                layout();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }        
        };
        
        // The following three listeners are workarounds for
        // Eclipse bug 105764
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=105764
        grid.addListener(SWT.Resize, resizeListener);
        
        if (grid.getVerticalScrollBarProxy() != null)
        {
            grid.getVerticalScrollBarProxy().addSelectionListener(scrollListener);
        }
        if (grid.getHorizontalScrollBarProxy() != null)
        {
            grid.getHorizontalScrollBarProxy().addSelectionListener(scrollListener);
        }

        // To be consistent with older versions of SWT, grabVertical defaults to
        // true
        grabVertical = true;
    }

    /**
     * Returns the bounds of the editor.
     * 
     * @return bounds of the editor.
     */
    private Rectangle computeEditorBounds()
    {
        if (row == -1 || column == -1)
            return new Rectangle(0, 0, 0, 0);
        Rectangle cell = grid.getCellBounds(column, row);
        Rectangle area = grid.getClientArea();
        if (cell.x < area.x + area.width)
        {
            if (cell.x + cell.width > area.x + area.width)
            {
                cell.width = area.x + area.width - cell.x;
            }
        }
        Rectangle editorRect = new Rectangle(cell.x, cell.y, minimumWidth, minimumHeight);

        if (grabHorizontal)
        {
            editorRect.width = Math.max(cell.width, minimumWidth);
        }

        if (grabVertical)
        {
            editorRect.height = Math.max(cell.height, minimumHeight);
        }

        if (horizontalAlignment == SWT.RIGHT)
        {
            editorRect.x += cell.width - editorRect.width;
        }
        else if (horizontalAlignment == SWT.LEFT)
        {
            // do nothing - cell.x is the right answer
        }
        else
        { // default is CENTER
            editorRect.x += (cell.width - editorRect.width) / 2;
        }

        if (verticalAlignment == SWT.BOTTOM)
        {
            editorRect.y += cell.height - editorRect.height;
        }
        else if (verticalAlignment == SWT.TOP)
        {
            // do nothing - cell.y is the right answer
        }
        else
        { // default is CENTER
            editorRect.y += (cell.height - editorRect.height) / 2;
        }
        
        return editorRect;
    }

    /**
     * Removes all associations between the TableEditor and the cell in the
     * table. The Table and the editor Control are <b>not</b> disposed.
     */
    @Override
    public void dispose()
    {
        if (!grid.isDisposed())
        {
            grid.removeListener(SWT.Resize, resizeListener);
            
            if (grid.getVerticalScrollBarProxy() != null)
                grid.getVerticalScrollBarProxy().removeSelectionListener(scrollListener);
            
            if (grid.getHorizontalScrollBarProxy() != null)
                grid.getHorizontalScrollBarProxy().removeSelectionListener(scrollListener);
        }
        
        columnListener = null;
        resizeListener = null;
        grid = null;
        row = -1;
        column = -1;        
        super.dispose();
    }

    /**
     * Returns the zero based index of the column of the cell being tracked by
     * this editor.
     * 
     * @return the zero based index of the column of the cell being tracked by
     * this editor
     */
    public int getColumn()
    {
        return column;
    }

    /**
     * Returns the TableItem for the row of the cell being tracked by this
     * editor.
     * 
     * @return the TableItem for the row of the cell being tracked by this
     * editor
     */
    public int getRow()
    {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
        layout();
    }

    /**
     * Sets the zero based index of the column of the cell being tracked by this
     * editor.
     * 
     * @param column the zero based index of the column of the cell being
     * tracked by this editor
     */
    public void setColumn(int column)
    {
        int columnCount = grid.getColumnCount();
        // Separately handle the case where the grid has no TableColumns.
        // In this situation, there is a single default column.
        if (columnCount == 0)
        {
            this.column = (column == 0) ? 0 : -1;
            layout();
            return;
        }
        if (this.column > -1 && this.column < columnCount)
        {
            this.column = -1;
        }

        if (column < 0 || column >= grid.getColumnCount())
            return;

        this.column = column;
        layout();
    }

    /**
     * Specify the Control that is to be displayed and the cell in the table
     * that it is to be positioned above.
     * <p>
     * Note: The Control provided as the editor <b>must</b> be created with its
     * parent being the Table control specified in the TableEditor constructor.
     * 
     * @param editor the Control that is displayed above the cell being edited
     * @param row the row of the cell being tracked by this editor
     * @param column the zero based index of the column of the cell being
     * tracked by this editor
     */
    public void setEditor(Control editor, int column, int row)
    {
        setRow(row);
        setColumn(column);
        setEditor(editor);

        layout();
    }

    @Override
    public void layout()
    {

        if (grid.isDisposed())
            return;
        if (row == -1)
            return;
        int columnCount = grid.getColumnCount();
        if (columnCount == 0 && column != 0)
            return;
        if (columnCount > 0 && (column < 0 || column >= columnCount))
            return;

        boolean hadFocus = false;

        if (getEditor() == null || getEditor().isDisposed())
            return;
        if (getEditor().getVisible())
        {
            hadFocus = getEditor().isFocusControl();
        } // this doesn't work because
        // resizing the column takes the focus away
        // before we get here
        getEditor().setBounds(computeEditorBounds());
        if (hadFocus)
        {
            if (getEditor() == null || getEditor().isDisposed())
                return;
            getEditor().setFocus();
        }

    }

}
