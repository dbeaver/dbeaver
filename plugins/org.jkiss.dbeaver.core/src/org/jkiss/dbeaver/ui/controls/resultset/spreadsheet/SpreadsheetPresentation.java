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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

/**
 * Spreadsheet presentation.
 * Visualizes results as grid.
 */
public class SpreadsheetPresentation implements IResultSetPresentation {

    private IResultSetController controller;
    private Spreadsheet spreadsheet;

    public SpreadsheetPresentation() {
    }

    @Override
    public void createPresentation(IResultSetController controller, Composite parent) {
        this.controller = controller;
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
}
