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

package org.jkiss.dbeaver.ui.controls.resultset.plaintext;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class PlainTextPresentation implements IResultSetPresentation {

    private IResultSetController controller;
    private StyledText text;
    private int[] colWidths;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        this.controller = controller;

        UIUtils.createHorizontalLine(parent);
        text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        text.setBlockSelection(true);
        text.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_IBEAM));
        text.setMargins(4, 4, 4, 4);
        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    @Override
    public Control getControl() {
        return text;
    }

    @Override
    public void refreshData(boolean refreshMetadata) {
        StringBuilder grid = new StringBuilder(512);
        ResultSetModel model = controller.getModel();
        DBDAttributeBinding[] attrs = model.getAttributes();
        colWidths = new int[attrs.length];

        for (int i = 0; i < attrs.length; i++) {
            DBDAttributeBinding attr = attrs[i];
            colWidths[i] = attr.getName().length();
            for (ResultSetRow row : model.getAllRows()) {
                String displayString = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, row), DBDDisplayFormat.EDIT);
                colWidths[i] = Math.max(colWidths[i], displayString.length());
            }
        }
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i]++;
        }

        // Print header
        for (int i = 0; i < attrs.length; i++) {
            DBDAttributeBinding attr = attrs[i];
            String attrName = attr.getName();
            grid.append(attrName);
            for (int k = colWidths[i] - attrName.length(); k >0 ; k--) {
                grid.append(" ");
            }
            grid.append("|");
        }
        grid.append("\n");

        // Print divider
        // Print header
        for (int i = 0; i < attrs.length; i++) {
            for (int k = colWidths[i]; k >0 ; k--) {
                grid.append("-");
            }
            grid.append("|");
        }
        grid.append("\n");

        // Print rows
        for (ResultSetRow row : model.getAllRows()) {
            for (int i = 0; i < attrs.length; i++) {
                DBDAttributeBinding attr = attrs[i];
                String displayString = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, row), DBDDisplayFormat.EDIT);
                grid.append(displayString);
                for (int k = colWidths[i] - displayString.length(); k >0 ; k--) {
                    grid.append(" ");
                }
                grid.append("|");
            }
            grid.append("\n");
        }

        text.setText(grid.toString());
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
