/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.common.ContextSearchAction;
import org.jkiss.dbeaver.utils.ViewUtils;

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

    public boolean isObjectEditable()
    {
        if (curEditor == null) {
            return false;
        }
        DBPDataSource dataSource = curEditor.getEditorInput().getDataSource();
        if (dataSource == null) {
            return false;
        }
        if (dataSource.getInfo().isReadOnlyMetaData()) {
            return false;
        }
        DBSObject databaseObject = curEditor.getEditorInput().getDatabaseObject();
        return databaseObject != null && DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(databaseObject.getClass(), DBEObjectManager.class) != null;
    }


    @Override
    public void contributeToMenu(IMenuManager menuManager)
    {
        super.contributeToMenu(menuManager);
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        final IWorkbenchWindow workbenchWindow = getPage().getWorkbenchWindow();
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.FILE_SAVE, "Save", DBIcon.SAVE_TO_DATABASE.getImageDescriptor(), "View/Persist Changes", true));
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.FILE_REVERT, "Revert", DBIcon.RESET.getImageDescriptor(), "Revert changes", true));
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.EDIT_UNDO));
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.EDIT_REDO));

        manager.add(new Separator());
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, ICommandIds.CMD_OBJECT_CREATE));
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.EDIT_DELETE));

        manager.add(new Separator());
        manager.add(ViewUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.FILE_REFRESH));
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