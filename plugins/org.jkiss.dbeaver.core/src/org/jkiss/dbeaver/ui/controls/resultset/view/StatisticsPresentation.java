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

package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;

/**
 * Execution statistics presentation.
 * Special internal presentation for execution statistics visualization.
 */
public class StatisticsPresentation implements IResultSetPresentation {

    private TableViewer tableViewer;

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        tableViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    @Override
    public Control getControl() {
        return tableViewer.getControl();
    }

    @Override
    public void refreshData(boolean refreshMetadata) {
        tableViewer.refresh();
    }

    @Override
    public void formatData(boolean refreshData) {

    }

    @Override
    public void clearData() {

    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void fillToolbar(@NotNull IToolBarManager toolBar) {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Override
    public void scrollToRow(@NotNull RowPosition position) {

    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return null;
    }

    @Nullable
    @Override
    public Control openValueEditor(boolean inline) {
        return null;
    }

    @Nullable
    @Override
    public String copySelectionToString(boolean copyHeader, boolean copyRowNumbers, boolean cut, String delimiter, DBDDisplayFormat format) {
        return null;
    }

    @Override
    public void pasteFromClipboard() {

    }

}
