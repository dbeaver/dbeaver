/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
* Sub editor site
*/
public class SubEditorSite implements IEditorSite {
    private final IWorkbenchPartSite parentSite;
    private final IActionBars actionBars;
    @SuppressWarnings("deprecation")
	private final IKeyBindingService keyBindingService;
    private final ISelectionProvider selectionProvider;

    public SubEditorSite(IWorkbenchPartSite parentSite, ISelectionProvider selectionProvider)
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
        this.selectionProvider = selectionProvider;
    }

    public SubEditorSite(IWorkbenchPartSite parentSite)
    {
        this(parentSite, new FakeSelectionProvider());
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

    public IWorkbenchPartSite getParentSite() {
        return parentSite;
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
        return parentSite.getPluginId();
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
        registerContextMenu(getId(), menuManager, selectionProvider);
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
    public <T> T getAdapter(Class<T> adapter)
    {
        return parentSite.getAdapter(adapter);
    }

    @Override
    public <T> T getService(Class<T> api)
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
