/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

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

    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider)
    {
        partSite.registerContextMenu(menuId, menuManager, selectionProvider);
    }

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

    public IWorkbenchPage getPage()
    {
        return partSite.getPage();
    }

    public ISelectionProvider getSelectionProvider()
    {
        return partSite.getSelectionProvider();
    }

    public Shell getShell()
    {
        return partSite.getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow()
    {
        return partSite.getWorkbenchWindow();
    }

    public void setSelectionProvider(ISelectionProvider provider)
    {
        partSite.setSelectionProvider(provider);
    }

    public Object getAdapter(Class adapter)
    {
        return partSite.getAdapter(adapter);
    }

    public Object getService(Class api)
    {
        return partSite.getService(api);
    }

    public boolean hasService(Class api)
    {
        return partSite.hasService(api);
    }
}
