/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.texteditor.templates.TemplatesView;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.application.about.AboutBoxAction;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.actions.common.CheckForUpdateAction;
import org.jkiss.dbeaver.ui.actions.common.EmergentExitAction;
import org.jkiss.dbeaver.ui.actions.common.ToggleViewAction;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.dbeaver.ui.views.qm.QueryManagerView;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the
 * actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor
{

    // Actions - important to allocate these only in makeActions, and then use them
    // in the fill methods.  This ensures that the actions aren't recreated
    // when fillActionBars is called with FILL_PROXY.
    //private IWorkbenchAction findAction;
    private IActionDelegate emergentExitAction;
    private IActionDelegate aboutAction;
    private IActionDelegate checkUpdatesAction;
    private IWorkbenchAction showHelpAction;
//    private IWorkbenchAction searchHelpAction;
//    private IWorkbenchAction dynamicHelpAction;
    private IWorkbenchAction newWindowAction;
//    private IWorkbenchAction historyBackAction;
//    private IWorkbenchAction historyForwardAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

    @Override
    protected void makeActions(final IWorkbenchWindow window)
    {
        register(ActionFactory.SAVE.create(window));
        register(ActionFactory.SAVE_AS.create(window));
        register(ActionFactory.SAVE_ALL.create(window));
        register(ActionFactory.CLOSE.create(window));
        register(ActionFactory.PRINT.create(window));

        //aboutAction = ActionFactory.ABOUT.create(window);
        //register(aboutAction);
        aboutAction = new AboutBoxAction(window);
        emergentExitAction = new EmergentExitAction(window);
        register(showHelpAction = ActionFactory.HELP_CONTENTS.create(window));
//        register(searchHelpAction = ActionFactory.HELP_SEARCH.create(window));
//        register(dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window));
        checkUpdatesAction = new CheckForUpdateAction();

        newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        register(newWindowAction);

//        historyBackAction = ActionFactory.BACKWARD_HISTORY.create(window);
//        register(historyBackAction);
//        historyForwardAction = ActionFactory.FORWARD_HISTORY.create(window);
//        register(historyForwardAction);
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar)
    {
        MenuManager fileMenu = new MenuManager(CoreMessages.actions_menu_file, IWorkbenchActionConstants.M_FILE);
        MenuManager editMenu = new MenuManager(CoreMessages.actions_menu_edit, IWorkbenchActionConstants.M_EDIT);
        MenuManager navigateMenu = new MenuManager(CoreMessages.actions_menu_navigate, IWorkbenchActionConstants.M_NAVIGATE);
        MenuManager windowMenu = new MenuManager(CoreMessages.actions_menu_window, IWorkbenchActionConstants.M_WINDOW);
        // do not use standard help menu to avoid junk provided by platform (like cheat sheets)
        MenuManager helpMenu = new MenuManager(CoreMessages.actions_menu_help, "dbhelp"); //IWorkbenchActionConstants.M_HELP

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(navigateMenu);

        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        // File
        //MenuManager recentMenu = new MenuManager("Recent editors");
        //recentMenu.add(ContributionItemFactory.REOPEN_EDITORS.create(getActionBarConfigurer().getWindowConfigurer().getWindow()));

        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        fileMenu.add(ContributionItemFactory.REOPEN_EDITORS.create(getActionBarConfigurer().getWindowConfigurer().getWindow()));
        fileMenu.add(new Separator());

        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        fileMenu.add(ActionUtils.makeAction(emergentExitAction, null, null, CoreMessages.actions_menu_exit_emergency, null, null));

        // Edit
/*
        editMenu.add(new Separator("undoredo"));
        editMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        editMenu.add(new Separator(IWorkbenchActionConstants.FIND_EXT));
        editMenu.add(findAction);
*/

        // Navigate
        navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.NAV_START));
        navigateMenu.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
        navigateMenu.add(new Separator(IWorkbenchActionConstants.SHOW_EXT));
        navigateMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.NAV_END));
        navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.GROUP_APP));
//        navigateMenu.add(historyBackAction);
//        navigateMenu.add(historyForwardAction);

        //editMenu.add(ActionFactory.PROPERTIES);
        //editMenu.add(viewPropertiesAction);

        // Window
        windowMenu.add(newWindowAction);
        windowMenu.add(new Separator());
        windowMenu.add(new ToggleViewAction(DatabaseNavigatorView.VIEW_ID));
        windowMenu.add(new ToggleViewAction(ProjectNavigatorView.VIEW_ID));
        windowMenu.add(new ToggleViewAction(ProjectExplorerView.VIEW_ID));
        windowMenu.add(new Separator());
        windowMenu.add(new ToggleViewAction(IPageLayout.ID_PROP_SHEET));
        windowMenu.add(new ToggleViewAction(QueryManagerView.VIEW_ID));
        windowMenu.add(new ToggleViewAction(TemplatesView.ID));
        windowMenu.add(new ToggleViewAction(IPageLayout.ID_OUTLINE));
        windowMenu.add(new ToggleViewAction(IPageLayout.ID_PROGRESS_VIEW));
        windowMenu.add(new ToggleViewAction(IActionConstants.LOG_VIEW_ID));
        windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        windowMenu.add(new Separator());
/*
        {
            MenuManager showViewMenuMgr = new MenuManager(IDEWorkbenchMessages.Workbench_showView, "showView"); //$NON-NLS-1$
            IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
            showViewMenuMgr.add(showViewMenu);
            windowMenu.add(showViewMenuMgr);
        }
*/

        // Help
        helpMenu.add(ActionUtils.makeAction(aboutAction, null, null, CoreMessages.actions_menu_about, null, null));
        helpMenu.add(showHelpAction);
        //helpMenu.add(searchHelpAction);
        //helpMenu.add(dynamicHelpAction);
        helpMenu.add(new Separator());
        helpMenu.add(ActionUtils.makeCommandContribution(getActionBarConfigurer().getWindowConfigurer().getWindow(), "org.eclipse.ui.help.installationDialog"));
        helpMenu.add(ActionUtils.makeAction(checkUpdatesAction, null, null, CoreMessages.actions_menu_check_update, null, null));
    }

    @Override
    protected void fillCoolBar(ICoolBarManager coolBar)
    {
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.LEFT), IActionConstants.TOOLBAR_DATABASE));

/*
        // Use CommandAction here as a workaround. Otherwise FORCE_TEXT mode just ignored by Eclipse 4.2+
        // TODO: remove all manual mapping when it will be fixed by Eclipse - https://bugs.eclipse.org/bugs/show_bug.cgi?id=399065
        ToolBarManager txnToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        txnToolbar.add(ActionUtils.makeActionContribution(new CommandAction(PlatformUI.getWorkbench(), ICommandIds.CMD_COMMIT), true));
        txnToolbar.add(ActionUtils.makeActionContribution(new CommandAction(PlatformUI.getWorkbench(), ICommandIds.CMD_ROLLBACK), true));
        coolBar.add(new ToolBarContributionItem(txnToolbar, IActionConstants.TOOLBAR_TXN));
*/
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_TXN));
        //coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_DATASOURCE));

    }

}
