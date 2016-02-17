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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class EmptyPresentation implements IResultSetPresentation {

    private Composite placeholder;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        UIUtils.createHorizontalLine(parent);
        placeholder = new Canvas(parent, SWT.NONE);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        placeholder.setBackground(controller.getDefaultBackground());

        final Font normalFont = parent.getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(18);
        final Font largeFont = new Font(normalFont.getDevice(), fontData[0]);
        placeholder.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.dispose(largeFont);
            }
        });

        placeholder.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                if (controller.isRefreshInProgress()) {
                    return;
                }
                e.gc.setFont(largeFont);
                UIUtils.drawMessageOverControl(placeholder, e, "No Data", -10);
                e.gc.setFont(normalFont);
                if (controller.getDataContainer() instanceof SQLEditor.QueryResultsContainer) {
                    String execQuery = ActionUtils.findCommandDescription(ICommandIds.CMD_EXECUTE_STATEMENT, controller.getSite(), true);
                    String execScript = ActionUtils.findCommandDescription(ICommandIds.CMD_EXECUTE_SCRIPT, controller.getSite(), true);
                    UIUtils.drawMessageOverControl(placeholder, e, "Execute query (" + execQuery + ") or script (" + execScript + ") to see results", 20);
                }
            }

        });
    }

    @Override
    public Control getControl() {
        return placeholder;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append) {

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
    public String copySelectionToString(boolean copyHeader, boolean copyRowNumbers, boolean cut, String delimiter, DBDDisplayFormat format) {
        return null;
    }


}
