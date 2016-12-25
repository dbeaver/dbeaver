/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.AbstractPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCopySettings;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

/**
 * Execution statistics presentation.
 * Special internal presentation for execution statistics visualization.
 */
public class StatisticsPresentation extends AbstractPresentation {

    private Table table;

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);
        UIUtils.createHorizontalLine(parent);
        table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createTableColumn(table, SWT.LEFT, "Name");
        UIUtils.createTableColumn(table, SWT.LEFT, "Value");
    }

    @Override
    public Control getControl() {
        return table;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {
        table.removeAll();

        ResultSetRow row = controller.getModel().getRow(0);
        java.util.List<DBDAttributeBinding> visibleAttributes = controller.getModel().getVisibleAttributes();
        for (int i = 0; i < visibleAttributes.size(); i++) {
            DBDAttributeBinding attr = visibleAttributes.get(i);
            Object value = row.getValues()[i];
            TableItem item = new TableItem(table, SWT.LEFT);
            item.setText(0, attr.getName());
            item.setText(1, DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }

        UIUtils.packColumns(table);
    }

    @Override
    public void formatData(boolean refreshData) {

    }

    @Override
    public void clearMetaData() {

    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return null;
    }

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return null;
    }

}
