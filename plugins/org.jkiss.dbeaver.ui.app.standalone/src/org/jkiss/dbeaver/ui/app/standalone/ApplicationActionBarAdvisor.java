/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.commands.CommandImageManager;
import org.eclipse.ui.internal.commands.CommandImageService;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.app.standalone.about.AboutBoxAction;
import org.jkiss.dbeaver.ui.app.standalone.actions.EmergentExitAction;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.app.standalone.update.CheckForUpdateAction;
import org.jkiss.dbeaver.ui.controls.StatusLineContributionItemEx;
import org.jkiss.dbeaver.ui.navigator.actions.ToggleViewAction;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Bundle;

import java.lang.reflect.Field;
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

    protected IActionDelegate aboutAction;
    protected CheckForUpdateAction checkUpdatesAction;
    protected IWorkbenchAction showHelpAction;
    protected IWorkbenchAction newWindowAction;
    private IWorkbenchAction openWorkspaceAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

    private static final String[] REDUNTANT_ACTIONS_SETS = new String[] {
        "org.eclipse.ui.WorkingSetActionSet", //$NON-NLS-1$
        //"org.eclipse.ui.edit.text.actionSet.navigation", //$NON-NLS-1$
        //"org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo", //$NON-NLS-1$
        //"org.eclipse.ui.actionSet.openFiles", //$NON-NLS-1$
        "org.eclipse.ui.edit.text.actionSet.annotationNavigation", //$NON-NLS-1$
        //"org.eclipse.ui.NavigateActionSet", //$NON-NLS-1$
        //"org.eclipse.search.searchActionSet" //$NON-NLS-1$
        "org.eclipse.mylyn.tasks.ui.navigation",

        // Disable files actionset to redefine OpenLocalFileAction
        "org.eclipse.ui.actionSet.openFiles"
    };


    private void removeUnWantedActions() {
        ActionSetRegistry asr = WorkbenchPlugin.getDefault().getActionSetRegistry();
        IActionSetDescriptor[] actionSets = asr.getActionSets();

        for (IActionSetDescriptor actionSet : actionSets) {
            if ("org.eclipse.search.searchActionSet".equals(actionSet.getId())) {
                patchSearchIcons(actionSet);
            } else {
                if (ArrayUtils.contains(REDUNTANT_ACTIONS_SETS, actionSet.getId())) {
                    log.debug("Disable Eclipse action set '" + actionSet.getId() + "'");
                    IExtension ext = actionSet.getConfigurationElement().getDeclaringExtension();
                    asr.removeExtension(ext, new Object[]{actionSet});
                }
            }
        }
    }

    private void patchSearchIcons(IActionSetDescriptor actionSet) {
        // Patch search icons. Directly change icon reference in config registry
        // FIXME: This is a very dirty hack but I didn't find any better way to patch search action icons
        for (IConfigurationElement searchActionItem : actionSet.getConfigurationElement().getChildren()) {
            String saId = searchActionItem.getAttribute("id");
            if ("org.eclipse.search.OpenSearchDialog".equals(saId) || "org.eclipse.search.OpenSearchDialogPage".equals(saId)) {
                patchActionSetIcon(searchActionItem, "platform:/plugin/" + CoreApplicationActivator.PLUGIN_ID + "/icons/eclipse/search.png");
            } else if ("org.eclipse.search.OpenFileSearchPage".equals(saId)) {
                patchActionSetIcon(searchActionItem, UIIcon.FIND_TEXT.getLocation());
            }
        }
    }

    private void patchActionSetIcon(IConfigurationElement searchActionItem, String iconPath) {
        try {
            Object cfgElement = BeanUtils.invokeObjectDeclaredMethod(searchActionItem, "getConfigurationElement", new Class[0], new Object[0]);
            if (cfgElement  != null) {
                Field pavField = cfgElement.getClass().getDeclaredField("propertiesAndValue");
                pavField.setAccessible(true);
                String[] pav = (String[]) pavField.get(cfgElement);
                for (int i = 0; i < pav.length; i += 2) {
                    if (pav[i].equals("icon")) {
                        pav[i + 1] = iconPath;
                    }
                }
            }
        } catch (Throwable e) {
            // ignore
            log.debug("Failed to patch search actions", e);
        }
    }

    protected boolean isShowAltHelp() {
        return true;
    }

    @Override
    protected void makeActions(final IWorkbenchWindow window)
    {
        removeUnWantedActions();
        log.debug("Create workbench actions");

        register(ActionFactory.SAVE.create(window));
        register(ActionFactory.SAVE_AS.create(window));
        register(ActionFactory.SAVE_ALL.create(window));
        register(ActionFactory.CLOSE.create(window));
        register(ActionFactory.PRINT.create(window));

        //aboutAction = ActionFactory.ABOUT.create(window);
        //register(aboutAction);
        aboutAction = new AboutBoxAction(window);
        register(showHelpAction = ActionFactory.HELP_CONTENTS.create(window));
//        register(searchHelpAction = ActionFactory.HELP_SEARCH.create(window));
//        register(dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window));
        checkUpdatesAction = new CheckForUpdateAction();

        newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        register(newWindowAction);

        if (DBWorkbench.getPlatform().getApplication().isWorkspaceSwitchingAllowed()) {
            openWorkspaceAction = IDEActionFactory.OPEN_WORKSPACE.create(window);
            register(openWorkspaceAction);
        }

//        historyBackAction = ActionFactory.BACKWARD_HISTORY.create(window);
//        register(historyBackAction);
//        historyForwardAction = ActionFactory.FORWARD_HISTORY.create(window);
//        register(historyForwardAction);

        CheckForUpdateAction.deactivateStandardHandler(window);
        ApplicationPolicyService.getInstance().disableStandardProductModification(window.getService(ICommandService.class));
    }


    private void patchImages() {
        // We have to patch images manually because using commandImages extension point doesn't guarantee order
        //WorkbenchImages.declareImage(IWorkbenchGraphicConstants.IMG_WIZBAN_IMPORT_WIZ, DBeaverIcons.getImageDescriptor(UIIcon.IMPORT), true);
        //WorkbenchImages.declareImage(IWorkbenchGraphicConstants.IMG_WIZBAN_EXPORT_WIZ, DBeaverIcons.getImageDescriptor(UIIcon.EXPORT), true);

        IWorkbenchWindow workbenchWindow = getActionBarConfigurer().getWindowConfigurer().getWindow();
        if (workbenchWindow != null) {
            ICommandImageService service = workbenchWindow.getService(ICommandImageService.class);
            if (service instanceof CommandImageService) {
                CommandImageService cis = (CommandImageService)service;
                bindImage(cis, IWorkbenchCommandConstants.FILE_SAVE, UIIcon.SAVE);
                bindImage(cis, IWorkbenchCommandConstants.FILE_SAVE_AS, UIIcon.SAVE_AS);
                bindImage(cis, IWorkbenchCommandConstants.FILE_SAVE_ALL, UIIcon.SAVE_ALL);

/*
                bindImage(cis, IWorkbenchCommandConstants.EDIT_COPY, UIIcon.EDIT_COPY);
                bindImage(cis, IWorkbenchCommandConstants.EDIT_COPY, UIIcon.EDIT_COPY);
                bindImage(cis, IWorkbenchCommandConstants.EDIT_COPY, UIIcon.EDIT_COPY);
                bindImage(cis, IWorkbenchCommandConstants.EDIT_COPY, UIIcon.EDIT_COPY);
*/

                bindImage(cis, IWorkbenchCommandConstants.FILE_IMPORT, UIIcon.IMPORT);
                bindImage(cis, IWorkbenchCommandConstants.FILE_EXPORT, UIIcon.EXPORT);
                bindImage(cis, IWorkbenchCommandConstants.FILE_REFRESH, UIIcon.REFRESH);
            }
        }
    }

    private void bindImage(CommandImageService cis, String commandId, DBIcon icon) {
        ImageDescriptor id = DBeaverIcons.getImageDescriptor(icon);
        cis.bind(commandId, CommandImageManager.TYPE_DEFAULT, null, id);
        cis.bind(commandId, CommandImageManager.TYPE_HOVER, null, id);
        cis.bind(commandId, CommandImageManager.TYPE_DISABLED, null, (ImageDescriptor) null);
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar) {
        patchImages();
        menuBar.updateAll(true);

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
            MenuManager recentEditors = new MenuManager(CoreApplicationMessages.actions_menu_recent_editors);
            recentEditors.add(ContributionItemFactory.REOPEN_EDITORS.create(workbenchWindow));
            recentEditors.add(new GroupMarker(IWorkbenchActionConstants.MRU));
            fileMenu.add(recentEditors);

            if (!DBWorkbench.isDistributed()) {
                // Local FS operations are not needed
                fileMenu.add(ActionUtils.makeCommandContribution(workbenchWindow, "org.eclipse.ui.edit.text.openLocalFile"));
                fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
                fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
            }
            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

            if (openWorkspaceAction != null) {
                fileMenu.add(openWorkspaceAction);
            }

            fileMenu.add(new Separator());
            fileMenu.add(new EmergentExitAction(workbenchWindow));

            fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        }

        if (false) {
            // Edit
            // Disabled because new Eclipse adds this to the File menu by default
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
                            convertLDMenu.add((IAction)actionClass.getConstructor().newInstance());
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
            //windowMenu.add(new ToggleViewAction(DatabaseTasksView.VIEW_ID));
            windowMenu.add(new GroupMarker("primary.views"));
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
            helpMenu.add(ActionUtils.makeAction(aboutAction, null, null, "about-box", CoreMessages.actions_menu_about, null, null));
            helpMenu.add(showHelpAction);
            helpMenu.add(new Separator());
            helpMenu.add(ActionUtils.makeCommandContribution(workbenchWindow, "org.eclipse.ui.help.installationDialog"));
            helpMenu.add(new Separator());
            helpMenu.add(new GroupMarker("installation_help"));
            helpMenu.add(new Separator());
            helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
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

    private void updateTimezoneItem(StatusLineContributionItemEx tzItem) {
        TimeZone tzDefault = TimeZone.getDefault();
        tzItem.setText(tzDefault.getDisplayName(false, TimeZone.SHORT));
        tzItem.setToolTip(tzDefault.getDisplayName(false, TimeZone.LONG));
    }

    @Override
    protected void fillStatusLine(IStatusLineManager statusLine) {
        {
            StatusLineContributionItemEx tzItem = new StatusLineContributionItemEx("Time Zone");
            updateTimezoneItem(tzItem);

            DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(event -> {
                if (event.getProperty().equals(ModelPreferences.CLIENT_TIMEZONE)) {
                    UIUtils.syncExec(() -> updateTimezoneItem(tzItem));
                }
            });

            tzItem.setDoubleClickListener(() -> {
                UIUtils.showMessageBox(
                    null,
                    CoreApplicationMessages.timezone_change_info_title,
                    NLS.bind(
                        CoreApplicationMessages.timezone_change_info_message,
                        StandardConstants.ENV_USER_TIMEZONE,
                        DBWorkbench.getPlatform().getApplicationConfiguration().toAbsolutePath()),
                    SWT.ICON_INFORMATION
                );
            });
            statusLine.add(tzItem);
        }
        {
            StatusLineContributionItemEx localeItem =  new StatusLineContributionItemEx("Locale");
            localeItem.setText(Locale.getDefault().toString());
            localeItem.setToolTip(Locale.getDefault().getDisplayName());
            localeItem.setDoubleClickListener(() -> {
                UIUtils.showMessageBox(
                    null,
                    CoreApplicationMessages.locale_change_info_title,
                    NLS.bind(
                        CoreApplicationMessages.locale_change_info_message,
                        DBWorkbench.getPlatform().getApplicationConfiguration().toAbsolutePath()),
                    SWT.ICON_INFORMATION);
            });
            statusLine.add(localeItem);
        }
    }

}
