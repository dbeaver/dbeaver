/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.*;

import java.util.*;

/**
 * Spreadsheet presentation.
 * Visualizes results as grid.
 */
public class SpreadsheetPresentation implements IResultSetPresentation, ISelectionProvider, IAdaptable  {

    static final Log log = Log.getLog(SpreadsheetPresentation.class);

    private IResultSetController controller;
    private Spreadsheet spreadsheet;
    private SpreadsheetFindReplaceTarget findReplaceTarget;
    private final List<ISelectionChangedListener> selectionChangedListenerList = new ArrayList<ISelectionChangedListener>();

    public SpreadsheetPresentation() {
        findReplaceTarget = new SpreadsheetFindReplaceTarget(this);
    }

    public IResultSetController getController() {
        return controller;
    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    @Override
    public void createPresentation(IResultSetController controller, Composite parent) {
        this.controller = controller;
        this.spreadsheet = ((ResultSetViewer)controller).getSpreadsheet();

        this.spreadsheet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                fireSelectionChanged(new SpreadsheetSelectionImpl());
            }
        });

    }

    @Override
    public Control getControl() {
        return spreadsheet;
    }

    @Override
    public void refreshData(boolean refreshMetadata) {
        spreadsheet.refreshData(refreshMetadata);
    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void fillToolbar(IToolBarManager toolBar) {

    }

    public Control showCellEditor(boolean inline) {
        return ((ResultSetViewer)controller).showCellEditor(inline);
    }

    public void resetCellValue(@NotNull Object col, @NotNull Object row, boolean delete) {
        ((ResultSetViewer)controller).resetCellValue(col, row, delete);
    }

    public void fillContextMenu(@Nullable Object col, @Nullable Object row, @NotNull IMenuManager manager) {
        ((ResultSetViewer)controller).fillContextMenu(col, row, manager);
    }

    public void changeSorting(Object columnElement, int state) {
        ((ResultSetViewer)controller).changeSorting(columnElement, state);
    }

    public void navigateLink(@NotNull GridCell cell, int state) {
        ((ResultSetViewer)controller).navigateLink(cell, state);
    }

    public IPreferenceStore getPreferenceStore() {
        return controller.getPreferenceStore();
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IFindReplaceTarget.class) {
            return findReplaceTarget;
        }
        return null;
    }

    public boolean isRecordMode() {
        return ((ResultSetViewer)controller).isRecordMode();
    }

    ///////////////////////////////////////////////
    // Misc

    @Nullable
    public DBDAttributeBinding getFocusAttribute()
    {
        return isRecordMode() ?
            (DBDAttributeBinding) spreadsheet.getFocusRowElement() :
            (DBDAttributeBinding) spreadsheet.getFocusColumnElement();
    }

    @Nullable
    public ResultSetRow getFocusRow()
    {
        return isRecordMode() ?
            (ResultSetRow) spreadsheet.getFocusColumnElement() :
            (ResultSetRow) spreadsheet.getFocusRowElement();
    }

    ///////////////////////////////////////////////
    // Selection provider

    @Override
    public IResultSetSelection getSelection() {
        return new SpreadsheetSelectionImpl();
    }

    @Override
    public void setSelection(ISelection selection) {
        if (selection instanceof IResultSetSelection && ((IResultSetSelection) selection).getController() == getController()) {
            // It may occur on simple focus change so we won't do anything
            return;
        }
        spreadsheet.deselectAll();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            List<GridPos> cellSelection = new ArrayList<GridPos>();
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object cell = iter.next();
                if (cell instanceof GridPos) {
                    cellSelection.add((GridPos) cell);
                } else {
                    log.warn("Bad selection object: " + cell);
                }
            }
            spreadsheet.selectCells(cellSelection);
            spreadsheet.showSelection();
        }
        fireSelectionChanged(selection);
    }

    private void fireSelectionChanged(ISelection selection) {
        SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
        for (ISelectionChangedListener listener : selectionChangedListenerList) {
            listener.selectionChanged(event);
        }
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.remove(listener);
    }

    private class SpreadsheetSelectionImpl implements IResultSetSelection {

        @Nullable
        @Override
        public GridPos getFirstElement()
        {
            Collection<GridPos> ssSelection = spreadsheet.getSelection();
            if (ssSelection.isEmpty()) {
                return null;
            }
            return ssSelection.iterator().next();
        }

        @Override
        public Iterator<GridPos> iterator()
        {
            return spreadsheet.getSelection().iterator();
        }

        @Override
        public int size()
        {
            return spreadsheet.getSelection().size();
        }

        @Override
        public Object[] toArray()
        {
            return spreadsheet.getSelection().toArray();
        }

        @Override
        public List toList()
        {
            return new ArrayList<GridPos>(spreadsheet.getSelection());
        }

        @Override
        public boolean isEmpty()
        {
            return spreadsheet.getSelection().isEmpty();
        }

        @Override
        public IResultSetController getController()
        {
            return SpreadsheetPresentation.this.getController();
        }

        @Override
        public Collection<ResultSetRow> getSelectedRows()
        {
            if (isRecordMode()) {
                ResultSetRow currentRow = controller.getCurrentRow();
                if (currentRow == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(currentRow);
            } else {
                List<ResultSetRow> rows = new ArrayList<ResultSetRow>();
                Collection<Integer> rowSelection = spreadsheet.getRowSelection();
                for (Integer row : rowSelection) {
                    rows.add(controller.getModel().getRow(row));
                }
                return rows;
            }
        }
    }
}
