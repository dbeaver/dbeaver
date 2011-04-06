/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.action.*;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.DBeaverConstants;
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
        curEditor = (EntityEditor) part;
    }

    @Override
    public void setActivePage(IEditorPart activeEditor) {
        if (curPage != activeEditor) {
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
                System.out.println("PROVIDER: " + provider);
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
            System.out.println("RESET");
            actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, null);
            actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_NEXT, null);
            actionBars.setGlobalActionHandler(IWorkbenchActionDefinitionIds.FIND_PREVIOUS, null);
        }
        actionBars.updateActionBars();
    }

}