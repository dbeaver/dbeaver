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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.code.NotNull;

/**
 * Abstract presentation.
 */
public abstract class AbstractPresentation implements IResultSetPresentation {

    protected IResultSetController controller;

    public IResultSetController getController() {
        return controller;
    }

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        this.controller = controller;
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
}
