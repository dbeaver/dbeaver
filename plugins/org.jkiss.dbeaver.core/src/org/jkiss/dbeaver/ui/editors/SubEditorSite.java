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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
* Sub editor site
*/
public class SubEditorSite implements IEditorSite {
    private final IWorkbenchPartSite parentSite;
    private final IActionBars actionBars;
    @SuppressWarnings("deprecation")
	private final IKeyBindingService keyBindingService;
    private final ISelectionProvider selectionProvider;

    public SubEditorSite(IWorkbenchPartSite parentSite)
    {
        this.parentSite = parentSite;
        if (parentSite instanceof IEditorSite) {
            this.actionBars = new SubActionBars(((IEditorSite)parentSite).getActionBars());
        } else if (parentSite instanceof IViewSite) {
            this.actionBars = new SubActionBars(((IViewSite)parentSite).getActionBars());
        } else {
            this.actionBars = new FakeActionBars();
        }
        this.keyBindingService = new FakeKeyBindingService();
        this.selectionProvider = new FakeSelectionProvider();
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor()
    {
        if (parentSite instanceof IEditorSite) {
            return ((IEditorSite)parentSite).getActionBarContributor();
        } else {
            return new FakeEditorActionBarContributor();
        }
    }

    @Override
    public IActionBars getActionBars()
    {
        return actionBars;
    }

    @Override
    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput)
    {
    }

    @Override
    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput)
    {
    }

    @Override
    public String getId()
    {
        return parentSite.getId() + ".sub";
    }

    @Override
    public String getPluginId()
    {
        return DBeaverCore.PLUGIN_ID;
    }

    @Override
    public String getRegisteredName()
    {
        return parentSite.getRegisteredName();
    }

    @Override
    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider)
    {
    }

    @Override
    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider)
    {
    }

    @Override
    @Deprecated
    public IKeyBindingService getKeyBindingService()
    {
        return keyBindingService;
    }

    @Override
    public IWorkbenchPart getPart()
    {
        return parentSite.getPart();
    }

    @Override
    public IWorkbenchPage getPage()
    {
        return parentSite.getPage();
    }

    @Override
    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    @Override
    public Shell getShell()
    {
        return parentSite.getShell();
    }

    @Override
    public IWorkbenchWindow getWorkbenchWindow()
    {
        return parentSite.getWorkbenchWindow();
    }

    @Override
    public void setSelectionProvider(ISelectionProvider provider)
    {
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        return parentSite.getAdapter(adapter);
    }

    @Override
    public Object getService(Class api)
    {
        return parentSite.getService(api);
    }

    @Override
    public boolean hasService(Class api)
    {
        return parentSite.hasService(api);
    }

    @SuppressWarnings("deprecation")
	private static class FakeKeyBindingService implements IKeyBindingService {
        @Override
        public String[] getScopes()
        {
            return new String[0];
        }

        @Override
        public void registerAction(IAction action)
        {
        }

        @Override
        public void setScopes(String[] scopes)
        {
        }

        @Override
        public void unregisterAction(IAction action)
        {
        }
    }

    private static class FakeSelectionProvider implements ISelectionProvider {
        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener)
        {
        }

        @Override
        public ISelection getSelection()
        {
            return new StructuredSelection();
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener)
        {
        }

        @Override
        public void setSelection(ISelection selection)
        {
        }
    }

    private static class FakeEditorActionBarContributor implements IEditorActionBarContributor {

        @Override
        public void init(IActionBars bars, IWorkbenchPage page)
        {
        }

        @Override
        public void setActiveEditor(IEditorPart targetEditor)
        {
        }

        @Override
        public void dispose()
        {
        }
    }

    private static class FakeActionBars implements IActionBars {

        @Override
        public void clearGlobalActionHandlers()
        {
        }
        @Override
        public IAction getGlobalActionHandler(String actionId)
        {
            return null;
        }
        @Override
        public IMenuManager getMenuManager()
        {
            return null;
        }
        @Override
        public IServiceLocator getServiceLocator()
        {
            return null;
        }
        @Override
        public IStatusLineManager getStatusLineManager()
        {
            return null;
        }
        @Override
        public IToolBarManager getToolBarManager()
        {
            return null;
        }
        @Override
        public void setGlobalActionHandler(String actionId, IAction handler)
        {
        }
        @Override
        public void updateActionBars()
        {
        }
    }
}
