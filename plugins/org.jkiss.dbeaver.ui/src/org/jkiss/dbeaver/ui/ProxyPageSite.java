/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
    public <T> T getAdapter(Class<T> adapter)
    {
        return partSite.getAdapter(adapter);
    }

    @Override
    public <T> T getService(Class<T> api)
    {
        return partSite.getService(api);
    }

    @Override
    public boolean hasService(Class api)
    {
        return partSite.hasService(api);
    }
}
