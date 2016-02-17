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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageSite;

/**
* ProxyPageSite
*/
public class ProxyPageSite implements IPageSite {

    private final IWorkbenchPartSite partSite;

    public ProxyPageSite(IWorkbenchPartSite partSite)
    {
        this.partSite = partSite;
    }

    @Override
    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider)
    {
        partSite.registerContextMenu(menuId, menuManager, selectionProvider);
    }

    @Override
    public IActionBars getActionBars()
    {
        if (partSite instanceof IEditorSite) {
            return ((IEditorSite)partSite).getActionBars();
        } else if (partSite instanceof IViewSite) {
            return ((IViewSite)partSite).getActionBars();
        } else {
            return null;
        }
    }

    @Override
    public IWorkbenchPage getPage()
    {
        return partSite.getPage();
    }

    @Override
    public ISelectionProvider getSelectionProvider()
    {
        return partSite.getSelectionProvider();
    }

    @Override
    public Shell getShell()
    {
        return partSite.getShell();
    }

    @Override
    public IWorkbenchWindow getWorkbenchWindow()
    {
        return partSite.getWorkbenchWindow();
    }

    @Override
    public void setSelectionProvider(ISelectionProvider provider)
    {
        partSite.setSelectionProvider(provider);
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        return partSite.getAdapter(adapter);
    }

    @Override
    public Object getService(Class api)
    {
        return partSite.getService(api);
    }

    @Override
    public boolean hasService(Class api)
    {
        return partSite.hasService(api);
    }
}
