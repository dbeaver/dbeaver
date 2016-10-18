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

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.actions.common.ContextSearchAction;

/**
 * Entity Editor contributor
 */
public class EntityEditorContributor extends MultiPageEditorActionBarContributor
{
    private EntityEditor curEditor;
    private IEditorPart curPage;

    @Override
    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        if (part instanceof EntityEditor) {
            curEditor = (EntityEditor) part;
        }
    }

    @Override
    public void setActivePage(IEditorPart activeEditor) {
        /*if (curPage != activeEditor) */{
            curPage = activeEditor;
            registerSearchActions(activeEditor);
        }
    }

    @Override
    public void contributeToMenu(IMenuManager menuManager)
    {
        super.contributeToMenu(menuManager);
    }

    public static void registerSearchActions(IEditorPart activeEditor)
    {
        if (activeEditor == null) {
            return;
        }
        IActionBars actionBars = activeEditor.getEditorSite().getActionBars();

        if (activeEditor instanceof ISearchContextProvider) {
            ISearchContextProvider provider = (ISearchContextProvider)activeEditor;
            if (provider.isSearchPossible()) {
                ContextSearchAction findAction = new ContextSearchAction(provider, ISearchContextProvider.SearchType.NONE);
                findAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
                actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, findAction);

                ContextSearchAction findNextAction = new ContextSearchAction(provider, ISearchContextProvider.SearchType.NEXT);
                findNextAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_NEXT);
                actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_NEXT, findNextAction);

                ContextSearchAction findPrevAction = new ContextSearchAction(provider, ISearchContextProvider.SearchType.PREVIOUS);
                findPrevAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
                actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_PREVIOUS, findPrevAction);
            }
        } else {
            actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, null);
            actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_NEXT, null);
            actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_PREVIOUS, null);
        }
        actionBars.updateActionBars();
    }

}