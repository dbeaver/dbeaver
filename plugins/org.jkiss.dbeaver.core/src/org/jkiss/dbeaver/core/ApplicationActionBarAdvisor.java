/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.actions.common.AboutBoxAction;
import org.jkiss.dbeaver.ui.actions.common.EmergentExitAction;
import org.jkiss.dbeaver.ui.actions.common.ToggleViewAction;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectNavigatorView;
import org.jkiss.dbeaver.ui.views.qm.QueryManagerView;
import org.jkiss.dbeaver.utils.ViewUtils;

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
    private IWorkbenchAction showHelpAction;
    private IWorkbenchAction searchHelpAction;
    private IWorkbenchAction dynamicHelpAction;
    private IWorkbenchAction newWindowAction;
    private ApplicationToolbarDataSources dataSourceToolbar;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

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
        register(searchHelpAction = ActionFactory.HELP_SEARCH.create(window));
        register(dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window));

        newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        register(newWindowAction);
    }

    protected void fillMenuBar(IMenuManager menuBar)
    {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        MenuManager navigateMenu = new MenuManager("&Navigate", IWorkbenchActionConstants.M_NAVIGATE);
        MenuManager databaseMenu = new MenuManager("&Database", IActionConstants.M_DATABASE);
        MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(navigateMenu);
        menuBar.add(databaseMenu);
        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        // File
        fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        fileMenu.add(new Separator(IWorkbenchActionConstants.FILE_END));
        fileMenu.add(ViewUtils.makeAction(emergentExitAction, null, null, "Emergency Exit", null, null));

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

        // Database
        databaseMenu.add(new Separator(IActionConstants.M_DRIVER_GROUP));
        databaseMenu.add(new Separator(IActionConstants.M_CONNECTION_GROUP));
        databaseMenu.add(new Separator(IActionConstants.M_TOOLS_GROUP));
        databaseMenu.add(new Separator(IActionConstants.M_SESSION_GROUP));
        databaseMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

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
        helpMenu.add(ViewUtils.makeAction(aboutAction, null, null, "About", null, null));
        helpMenu.add(showHelpAction);
        helpMenu.add(searchHelpAction);
        helpMenu.add(dynamicHelpAction);
    }

    protected void fillCoolBar(ICoolBarManager coolBar)
    {
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_DATABASE));
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_TXN));

        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        dataSourceToolbar = new ApplicationToolbarDataSources(getActionBarConfigurer().getWindowConfigurer().getWindow());
        dataSourceToolbar.fillToolBar(toolbar);
        coolBar.add(new ToolBarContributionItem(toolbar, IActionConstants.TOOLBAR_DATASOURCE));
    }

    @Override
    public void dispose()
    {
        if (dataSourceToolbar != null) {
            dataSourceToolbar.dispose();
            dataSourceToolbar = null;
        }
    }
}
