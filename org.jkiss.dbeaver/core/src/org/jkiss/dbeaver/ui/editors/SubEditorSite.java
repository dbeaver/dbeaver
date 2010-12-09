/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.ui.DBeaverConstants;

/**
* Sub editor site
*/
public class SubEditorSite implements IEditorSite {
    private final IEditorSite parentSite;
    private final IActionBars actionBars;
    private final IKeyBindingService keyBindingService;
    private final ISelectionProvider selectionProvider;

    public SubEditorSite(IEditorSite parentSite)
    {
        this.parentSite = parentSite;
        this.actionBars = new SubActionBars(parentSite.getActionBars());
        this.keyBindingService = new FakeKeyBindingService();
        this.selectionProvider = new FakeSelectionProvider();
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return parentSite.getActionBarContributor();
    }

    public IActionBars getActionBars()
    {
        return actionBars;
    }

    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput)
    {
    }

    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput)
    {
    }

    public String getId()
    {
        return parentSite.getId() + ".sub";
    }

    public String getPluginId()
    {
        return DBeaverConstants.PLUGIN_ID;
    }

    public String getRegisteredName()
    {
        return parentSite.getRegisteredName();
    }

    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider)
    {
    }

    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider)
    {
    }

    public IKeyBindingService getKeyBindingService()
    {
        return keyBindingService;
    }

    public IWorkbenchPart getPart()
    {
        return parentSite.getPart();
    }

    public IWorkbenchPage getPage()
    {
        return parentSite.getPage();
    }

    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    public Shell getShell()
    {
        return parentSite.getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow()
    {
        return parentSite.getWorkbenchWindow();
    }

    public void setSelectionProvider(ISelectionProvider provider)
    {
    }

    public Object getAdapter(Class adapter)
    {
        return parentSite.getAdapter(adapter);
    }

    public Object getService(Class api)
    {
        return parentSite.getService(api);
    }

    public boolean hasService(Class api)
    {
        return parentSite.hasService(api);
    }

    private static class FakeKeyBindingService implements IKeyBindingService {
        public String[] getScopes()
        {
            return new String[0];
        }

        public void registerAction(IAction action)
        {
        }

        public void setScopes(String[] scopes)
        {
        }

        public void unregisterAction(IAction action)
        {
        }
    }

    private static class FakeSelectionProvider implements ISelectionProvider {
        public void addSelectionChangedListener(ISelectionChangedListener listener)
        {
        }

        public ISelection getSelection()
        {
            return new StructuredSelection();
        }

        public void removeSelectionChangedListener(ISelectionChangedListener listener)
        {
        }

        public void setSelection(ISelection selection)
        {
        }
    }
}
