/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.application.about.AboutBoxAction;
import org.jkiss.dbeaver.core.application.update.CheckForUpdateAction;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.actions.common.EmergentExitAction;
import org.jkiss.dbeaver.ui.actions.common.ToggleViewAction;
import org.jkiss.dbeaver.ui.controls.StatusLineContributionItemEx;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.util.Locale;
import java.util.TimeZone;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the
 * actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor
{
    private static final Log log = Log.getLog(ApplicationActionBarAdvisor.class);

    public static final String M_ALT_HELP = "dbhelp";

    protected IActionDelegate emergentExitAction;
    protected IActionDelegate aboutAction;
    protected CheckForUpdateAction checkUpdatesAction;
    protected IWorkbenchAction showHelpAction;
    protected IWorkbenchAction newWindowAction;
    private IWorkbenchAction openWorkspaceAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

    private static final String[] actionSetId = new String[] {
        "org.eclipse.ui.WorkingSetActionSet", //$NON-NLS-1$
        //"org.eclipse.ui.edit.text.actionSet.navigation", //$NON-NLS-1$
        //"org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo", //$NON-NLS-1$
        //"org.eclipse.ui.actionSet.openFiles", //$NON-NLS-1$
        "org.eclipse.ui.edit.text.actionSet.annotationNavigation", //$NON-NLS-1$
        //"org.eclipse.ui.NavigateActionSet", //$NON-NLS-1$
        //"org.eclipse.search.searchActionSet" //$NON-NLS-1$
        "org.eclipse.mylyn.tasks.ui.navigation",
    };


    private void removeUnWantedActions() {
        ActionSetRegistry asr = WorkbenchPlugin.getDefault().getActionSetRegistry();
        IActionSetDescriptor[] actionSets = asr.getActionSets();

        for (IActionSetDescriptor actionSet : actionSets) {
            for (String element : actionSetId) {

                if (element.equals(actionSet.getId())) {
                    log.debug("Disable Eclipse action set '" + actionSet.getId() + "'");
                    IExtension ext = actionSet.getConfigurationElement().getDeclaringExtension();
                    asr.removeExtension(ext, new Object[] { actionSet });
                }
            }
        }
    }

    protected boolean isShowAltHelp() {
        return true;
    }

    @Override
    protected void makeActions(final IWorkbenchWindow window)
    {
        removeUnWantedActions();

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

        openWorkspaceAction = IDEActionFactory.OPEN_WORKSPACE.create(window);
        register(openWorkspaceAction);


//        historyBackAction = ActionFactory.BACKWARD_HISTORY.create(window);
//        register(historyBackAction);
//        historyForwardAction = ActionFactory.FORWARD_HISTORY.create(window);
//        register(historyForwardAction);

        CheckForUpdateAction.deactivateStandardHandler(window);
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar) {
        // do not use standard help menu to avoid junk provided by platform (like cheat sheets)
        final boolean showAltHelp = isShowAltHelp();

        MenuManager fileMenu = new MenuManager(CoreMessages.actions_menu_file, IWorkbenchActionConstants.M_FILE);
        MenuManager editMenu = new MenuManager(CoreMessages.actions_menu_edit, IWorkbenchActionConstants.M_EDIT);
        MenuManager navigateMenu = new MenuManager(CoreMessages.actions_menu_navigate, IWorkbenchActionConstants.M_NAVIGATE);
        MenuManager windowMenu = new MenuManager(CoreMessages.actions_menu_window, IWorkbenchActionConstants.M_WINDOW);
        MenuManager helpMenu = new MenuManager(CoreMessages.actions_menu_help, IWorkbenchActionConstants.M_HELP);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(navigateMenu);

        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        IWorkbenchWindow workbenchWindow = getActionBarConfigurer().getWindowConfigurer().getWindow();
        {
            // File
            //MenuManager recentMenu = new MenuManager("Recent editors");
            //recentMenu.add(ContributionItemFactory.REOPEN_EDITORS.create(getActionBarConfigurer().getWindowConfigurer().getWindow()));

            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
            fileMenu.add(new Separator());
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
            fileMenu.add(new Separator());
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.PRINT_EXT));
            fileMenu.add(new Separator());
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.OPEN_EXT));
            fileMenu.add(new Separator());
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.IMPORT_EXT));
            fileMenu.add(new Separator());
            //fileMenu.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
            //fileMenu.add(new Separator());

            MenuManager recentEditors = new MenuManager("Recent editors");
            recentEditors.add(ContributionItemFactory.REOPEN_EDITORS.create(workbenchWindow));
            recentEditors.add(new GroupMarker(IWorkbenchActionConstants.MRU));
            fileMenu.add(recentEditors);

            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

            fileMenu.add(openWorkspaceAction);

            fileMenu.add(new Separator());
            fileMenu.add(ActionUtils.makeAction(emergentExitAction, null, null, CoreMessages.actions_menu_exit_emergency, null, null));

            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        }

        {
            // Edit
            ActionSetRegistry asr = WorkbenchPlugin.getDefault().getActionSetRegistry();
            IActionSetDescriptor actionSet = asr.findActionSet("org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo");
            if (actionSet != null) {
                MenuManager convertLDMenu = new MenuManager(actionSet.getLabel());
                for (IConfigurationElement action : actionSet.getConfigurationElement().getChildren("action")) {
                    String actionClassName = action.getAttribute("class");
                    if (!CommonUtils.isEmpty(actionClassName)) {
                        try {
                            Bundle actionBundle = Platform.getBundle(action.getContributor().getName());
                            Class<?> actionClass = actionBundle.loadClass(actionClassName);
                            convertLDMenu.add((IAction)actionClass.newInstance());
                        } catch (Throwable e) {
                            log.error(e);
                        }
                    }
                }
                editMenu.add(convertLDMenu);
            }
        }

        {
            // Navigate
            navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.NAV_START));
            navigateMenu.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
            navigateMenu.add(new Separator(IWorkbenchActionConstants.SHOW_EXT));
            navigateMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.NAV_END));
            navigateMenu.add(new GroupMarker(IWorkbenchActionConstants.GROUP_APP));
        }

        {
            // Window
            windowMenu.add(newWindowAction);
            windowMenu.add(new Separator());
            windowMenu.add(new ToggleViewAction(DatabaseNavigatorView.VIEW_ID));
            windowMenu.add(new ToggleViewAction(ProjectNavigatorView.VIEW_ID));
            windowMenu.add(new ToggleViewAction(ProjectExplorerView.VIEW_ID));
            {
                MenuManager showViewMenuMgr = new MenuManager(CoreMessages.actions_menu_window_showView, "showView"); //$NON-NLS-1$
                IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                showViewMenuMgr.add(showViewMenu);
                windowMenu.add(showViewMenuMgr);
            }
            windowMenu.add(new Separator());
            windowMenu.add(new GroupMarker("perspective"));

            windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            windowMenu.add(new Separator());
        }

        {
            // Help
            helpMenu.add(ActionUtils.makeAction(aboutAction, null, null, CoreMessages.actions_menu_about, null, null));
            helpMenu.add(showHelpAction);
            helpMenu.add(new Separator());
            helpMenu.add(ActionUtils.makeCommandContribution(workbenchWindow, "org.eclipse.ui.help.installationDialog"));
            helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
/*
            if (showAltHelp) {
                //helpMenu.add(searchHelpAction);
                //helpMenu.add(dynamicHelpAction);
                helpMenu.add(ActionUtils.makeCommandContribution(workbenchWindow, IWorkbenchCommandConstants.WINDOW_SHOW_KEY_ASSIST, CoreMessages.action_menu_showKeyAssist, null));
                helpMenu.add(new Separator());
                helpMenu.add(ActionUtils.makeCommandContribution(workbenchWindow, "org.eclipse.equinox.p2.ui.sdk.install"));

                helpMenu.add(new Separator());
                helpMenu.add(checkUpdatesAction);

                helpMenu.add(new ExternalPageAction(
                    NLS.bind(CoreMessages.action_menu_marketplace_extensions, GeneralUtils.getProductName()),
                    UIIcon.DBEAVER_MARKETPLACE, "https://marketplace.eclipse.org/search/site/dbeaver"));
                helpMenu.add(new ExternalPageAction(CoreMessages.action_menu_enterpriseEdition, UIIcon.DBEAVER_LOGO_SMALL, "https://dbeaver.com"));
            } else {
                helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
*/
        }
    }

    @Override
    protected void fillCoolBar(ICoolBarManager coolBar)
    {
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.LEFT), IActionConstants.TOOLBAR_DATABASE));
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_TXN));
        coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_EDIT));
        //coolBar.add(new ToolBarContributionItem(new ToolBarManager(SWT.FLAT | SWT.RIGHT), IActionConstants.TOOLBAR_DATASOURCE));
    }

    @Override
    protected void fillStatusLine(IStatusLineManager statusLine) {

        {
            StatusLineContributionItemEx tzItem = new StatusLineContributionItemEx("Time Zone");
            tzItem.setText(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
            tzItem.setToolTip(TimeZone.getDefault().getDisplayName(true, TimeZone.LONG));
            statusLine.add(tzItem);
        }
        {
            StatusLineContributionItemEx localeItem = new StatusLineContributionItemEx("Locale");
            localeItem.setText(Locale.getDefault().toString());
            localeItem.setToolTip(Locale.getDefault().getDisplayName());
            statusLine.add(localeItem);
        }
    }

}
