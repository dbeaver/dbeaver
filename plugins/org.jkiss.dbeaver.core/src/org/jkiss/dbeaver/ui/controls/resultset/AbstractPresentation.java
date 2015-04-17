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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Abstract presentation.
 */
public abstract class AbstractPresentation implements IResultSetPresentation {

    private static final String PRESENTATION_CONTROL_ID = "org.jkiss.dbeaver.ui.resultset.presentation";

    protected IResultSetController controller;

    public IResultSetController getController() {
        return controller;
    }

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        this.controller = controller;
    }

    @Override
    public void fillToolbar(@NotNull IToolBarManager toolBar) {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    /**
     * Shifts current row and redraws current control.
     * In record mode refreshes data.
     * @param position    position
     */
    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        ResultSetRow currentRow = controller.getCurrentRow();
        ResultSetModel model = controller.getModel();
        switch (position) {
            case FIRST:
                if (model.getRowCount() > 0) {
                    controller.setCurrentRow(model.getRow(0));
                }
                break;
            case PREVIOUS:
                if (currentRow != null && currentRow.getVisualNumber() > 0) {
                    controller.setCurrentRow(model.getRow(currentRow.getVisualNumber() - 1));
                }
                break;
            case NEXT:
                if (currentRow != null && currentRow.getVisualNumber() < model.getRowCount() - 1) {
                    controller.setCurrentRow(model.getRow(currentRow.getVisualNumber() + 1));
                }
                break;
            case LAST:
                if (currentRow != null) {
                    controller.setCurrentRow(model.getRow(model.getRowCount() - 1));
                }
                break;
        }
        if (controller.isRecordMode()) {
            refreshData(true, false);
        } else {
            getControl().redraw();
        }
        controller.updateStatusMessage();
        controller.updateEditControls();
    }

    protected void registerContextMenu() {
        // Register context menu
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(getControl());
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                controller.fillContextMenu(
                    manager,
                    getCurrentAttribute(),
                    controller.getCurrentRow());
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        getControl().setMenu(menu);
        controller.getSite().registerContextMenu(menuMgr, null);
    }

    protected void trackPresentationControl() {
        final Control control = getControl();
        final IWorkbenchPartSite site = controller.getSite();
        UIUtils.addFocusTracker(site, PRESENTATION_CONTROL_ID, control);
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.removeFocusTracker(site, control);
            }
        });
    }
}
