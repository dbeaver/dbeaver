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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.SaveableHelper;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.dialogs.WorkbenchWizardElement;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.eclipse.ui.internal.wizards.AbstractExtensionWizardRegistry;
import org.eclipse.ui.wizards.IWizardCategory;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreFeatures;
import org.jkiss.dbeaver.core.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.OperationSystemState;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIFonts;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.app.standalone.update.DBeaverVersionChecker;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.perspective.DBeaverPerspective;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionsGeneral;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseEditors;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseUserInterface;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.awt.*;
import java.awt.desktop.SystemEventListener;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.util.List;
import java.util.*;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends IDEWorkbenchAdvisor {
    private static final Log log = Log.getLog(ApplicationWorkbenchAdvisor.class);

    private static final String PERSPECTIVE_ID = DBeaverPerspective.PERSPECTIVE_ID;
    public static final String DBEAVER_SCHEME_NAME = "org.jkiss.dbeaver.defaultKeyScheme"; //$NON-NLS-1$

    protected static final String WORKBENCH_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Workbench";
    protected static final String APPEARANCE_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Views";
    private static final String EDITORS_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Editors";

    private static final String[] EXCLUDE_PREF_PAGES = {
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Globalization",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Perspectives",
        //"org.eclipse.ui.preferencePages.FileEditors",
        WORKBENCH_PREF_PAGE_ID + "/" + APPEARANCE_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Decorators",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Workspace",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Workspace/org.eclipse.ui.preferencePages.BuildOrder",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.ContentTypes",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.General.LinkHandlers",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Startup",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.trace.tracingPage",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.epp.mpc.projectnatures",
        "org.eclipse.ui.internal.console.ansi.preferences.AnsiConsolePreferencePage",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.browser.preferencePage",
        "org.eclipse.jsch.ui.SSHPreferences",

        WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID,
        WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.AutoSave",

        "org.eclipse.equinox.internal.p2.ui.sdk.ProvisioningPreferencePage",    // Install-Update

        // Team preferences - not needed in CE
        //"org.eclipse.team.ui.TeamPreferences",
    };

    // Move to UI
    private static final String[] UI_PREF_PAGES = {
            WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Views",
            WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Keys",
            WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.browser.preferencePage",
            WORKBENCH_PREF_PAGE_ID + "/org.eclipse.search.preferences.SearchPreferencePage",
            WORKBENCH_PREF_PAGE_ID + "/org.eclipse.text.quicksearch.PreferencesPage",
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.FileEditors" //"File Associations"
    };

    // Move to Editors
    private static final String[] EDITORS_PREF_PAGES = {
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.GeneralTextEditor"
    };

    // Move to General
    private static final String[] GENERAL_PREF_PAGES = {
        "org.eclipse.debug.ui.DebugPreferencePage"                              // Debugger
    };

    // Move to Connections
    private static final String[] NETWORK_PREF_PAGES = {
        WORKBENCH_PREF_PAGE_ID + "/" + "org.eclipse.ui.net.NetPreferences",    // Network Connections
    };


    private static final Set<String> fontPrefIdsToHide = Set.of(
        ApplicationWorkbenchWindowAdvisor.TEXT_EDITOR_BLOCK_SELECTION_FONT,
        ApplicationWorkbenchWindowAdvisor.TEXT_FONT,
        ApplicationWorkbenchWindowAdvisor.CONSOLE_FONT,
        ApplicationWorkbenchWindowAdvisor.DETAIL_PANE_TEXT_FONT,
        ApplicationWorkbenchWindowAdvisor.MEMORY_VIEW_TABLE_FONT,
        ApplicationWorkbenchWindowAdvisor.COMPARE_TEXT_FONT,
        ApplicationWorkbenchWindowAdvisor.DIALOG_FONT,
        ApplicationWorkbenchWindowAdvisor.VARIABLE_TEXT_FONT,
        ApplicationWorkbenchWindowAdvisor.PART_TITLE_FONT,
        ApplicationWorkbenchWindowAdvisor.TREE_AND_TABLE_FONT_FOR_VIEWS
    );
    
    private static final Map<String, List<String>> fontOverrides = Map.of(
        UIFonts.DBEAVER_FONTS_MONOSPACE, List.of(
            ApplicationWorkbenchWindowAdvisor.TEXT_EDITOR_BLOCK_SELECTION_FONT,
            ApplicationWorkbenchWindowAdvisor.TEXT_FONT,
            ApplicationWorkbenchWindowAdvisor.CONSOLE_FONT,
            ApplicationWorkbenchWindowAdvisor.DETAIL_PANE_TEXT_FONT,
            ApplicationWorkbenchWindowAdvisor.MEMORY_VIEW_TABLE_FONT,
            ApplicationWorkbenchWindowAdvisor.COMPARE_TEXT_FONT
        ),
        UIFonts.DBEAVER_FONTS_MAIN_FONT, List.of(
            ApplicationWorkbenchWindowAdvisor.DIALOG_FONT,
            ApplicationWorkbenchWindowAdvisor.VARIABLE_TEXT_FONT,
            ApplicationWorkbenchWindowAdvisor.PART_TITLE_FONT,
            ApplicationWorkbenchWindowAdvisor.TREE_AND_TABLE_FONT_FOR_VIEWS
        )
    ); 
    
    //processor must be created before we start event loop
    protected final DBPApplication application;
    private final DelayedEventsProcessor processor;

    private final SystemEventListener systemSleepListener = new SystemSleepListener() {
        @Override
        public void systemAboutToSleep(SystemSleepEvent e) {
            OperationSystemState.toggleSleepMode(true);
        }

        @Override
        public void systemAwoke(SystemSleepEvent e) {
            OperationSystemState.toggleSleepMode(false);
        }
    };

    protected ApplicationWorkbenchAdvisor(DBPApplication application) {
        this.application = application;
        this.processor = new DelayedEventsProcessor(Display.getCurrent());
    }

    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(this, configurer);
    }

    @Override
    public String getInitialWindowPerspectiveId() {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer) {
        if (RuntimeUtils.isMacOS()) {
            // Disable URI handlers auto registration.
            // They modify plist file on MacOS - this breaks sealed application
            try {
                new BundlePreferenceStore("org.eclipse.urischeme").setValue("skipAutoRegistration", true);
            } catch (Exception e) {
                log.debug("Error disabling urischeme auto registration", e);
            }
        }

        super.initialize(configurer);

        // Initialize app preferences
        DefaultScope.INSTANCE.getNode(CoreApplicationActivator.getDefault().getBundle().getSymbolicName());

        // Don't show Help button in JFace dialogs
        TrayDialog.setDialogHelpAvailable(false);

        // Replace Eclipse error icon shown in the "Problems" view with our own
        WorkbenchImages.getImageRegistry().remove(IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH);
        WorkbenchImages.getImageRegistry().put(IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, DBeaverIcons.getImageDescriptor(DBIcon.SMALL_ERROR));
        WorkbenchImages.getDescriptors().put(IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, DBeaverIcons.getImageDescriptor(DBIcon.SMALL_ERROR));

        FontPreferenceOverrides.overrideFontPrefValues(fontOverrides);
            
/*
        // Set default resource encoding to UTF-8
        String defEncoding = DBWorkbench.getPlatform().getPreferenceStore().getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING);
        if (CommonUtils.isEmpty(defEncoding)) {
            defEncoding = GeneralUtils.UTF8_ENCODING;
        }
        ResourcesPlugin.getPlugin().getPluginPreferences().setValue(ResourcesPlugin.PREF_ENCODING, defEncoding);
*/
    }

    @Override
    public void preStartup() {
        super.preStartup();

        {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("startTime", DBWorkbench.getPlatform().getApplication().getApplicationStartTime());
            CoreFeatures.APP_OPEN.use(params);
        }
    }

    @Override
    public void postStartup() {
        super.postStartup();

        filterPreferencePages();
        filterWizards();
        patchJFaceIcons();

        if (!application.isDistributed() &&
            !ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
            startVersionChecker();
        }
        if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
            // System events
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) {
                desktop.addAppEventListener(systemSleepListener);
            }
        }
    }

    @Override
    public IAdaptable getDefaultPageInput() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    protected boolean isPropertyChangeRequiresRestart(String property) {
        return
            property.equals(DBeaverPreferences.LOGS_DEBUG_ENABLED) ||
            property.equals(DBeaverPreferences.LOGS_DEBUG_LOCATION) ||
            property.equals(ModelPreferences.PLATFORM_LANGUAGE);
    }
    
    
    private void filterPreferencePages() {
        // Remove unneeded pref pages and override font preferences page
        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
        
        FontPreferenceOverrides.hideFontPrefs(pm, fontPrefIdsToHide);
        
        patchPreferencePages(pm, EDITORS_PREF_PAGES, PrefPageDatabaseEditors.PAGE_ID);
        patchPreferencePages(pm, UI_PREF_PAGES, PrefPageDatabaseUserInterface.PAGE_ID);
        patchPreferencePages(pm, GENERAL_PREF_PAGES, WORKBENCH_PREF_PAGE_ID);
        patchPreferencePages(pm, NETWORK_PREF_PAGES, PrefPageConnectionsGeneral.PAGE_ID);

        for (String epp : getExcludedPreferencePageIds()) {
            pm.remove(epp);
        }
    }

    @NotNull
    protected String[] getExcludedPreferencePageIds() {
        return EXCLUDE_PREF_PAGES;
    }

    protected void patchPreferencePages(PreferenceManager pm, String[] preferencePages, String preferencePageId) {
        for (String pageId : preferencePages)  {
            IPreferenceNode uiPage = pm.remove(pageId);
            if (uiPage != null) {
                pm.addTo(preferencePageId, uiPage);
            }
        }
    }

    protected boolean isWizardAllowed(String wizardId) {
        return !(application.isStandalone() && "org.eclipse.ui.wizards.new.project".equals(wizardId));
    }

    private void filterWizards() {
        AbstractExtensionWizardRegistry wizardRegistry = (AbstractExtensionWizardRegistry) WorkbenchPlugin.getDefault().getNewWizardRegistry();
        IWizardCategory[] categories = WorkbenchPlugin.getDefault().getNewWizardRegistry().getRootCategory().getCategories();
        for (IWizardDescriptor wizard : getAllWizards(categories)) {
            WorkbenchWizardElement wizardElement = (WorkbenchWizardElement) wizard;
            if (!isWizardAllowed(wizardElement.getId())) {
                wizardRegistry.removeExtension(wizardElement.getConfigurationElement().getDeclaringExtension(), new Object[]{wizardElement});
            }
        }
    }

    private IWizardDescriptor[] getAllWizards(IWizardCategory... categories) {
        List<IWizardDescriptor> results = new ArrayList<>();
        for(IWizardCategory wizardCategory : categories){
            Collections.addAll(results, wizardCategory.getWizards());
            Collections.addAll(results, getAllWizards(wizardCategory.getCategories()));
        }
        return results.toArray(new IWizardDescriptor[0]);
    }

    private void patchJFaceIcons() {
        final Map<String, ImageDescriptor> icons = Map.of(
            Dialog.DLG_IMG_MESSAGE_INFO, DBeaverIcons.getImageDescriptor(DBIcon.SMALL_INFO),
            Dialog.DLG_IMG_MESSAGE_WARNING, DBeaverIcons.getImageDescriptor(DBIcon.SMALL_WARNING),
            Dialog.DLG_IMG_MESSAGE_ERROR, DBeaverIcons.getImageDescriptor(DBIcon.SMALL_ERROR)
        );

        final ImageRegistry registry = JFaceResources.getImageRegistry();
        for (Map.Entry<String, ImageDescriptor> entry : icons.entrySet()) {
            registry.remove(entry.getKey());
            registry.put(entry.getKey(), entry.getValue());
        }
    }

    private void startVersionChecker() {
        DBeaverVersionChecker checker = new DBeaverVersionChecker(false);
        checker.schedule(3000);
    }

    ///////////////////////
    // Shutdown

    @Override
    public boolean preShutdown() {
        //DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(settingsChangeListener);

        if (!saveAndCleanup()) {
            // User rejected to exit
            return false;
        } else {
            CoreFeatures.APP_CLOSE.use();
            return super.preShutdown();
        }
    }

    @Override
    public void postShutdown() {
        super.postShutdown();
        if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
            // System events
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) {
                desktop.removeAppEventListener(systemSleepListener);
            }
        }

        if (DBWorkbench.getPlatform() instanceof BasePlatformImpl basePlatform) {
            // Dispose navigator model earlier because it may lock some UI resources
            // and we want to free them before application display will be disposed
            basePlatform.disposeNavigatorModel();
        }
    }

    private boolean saveAndCleanup() {
        if (getWorkbenchConfigurer().emergencyClosing()) {
            return true;
        }
        try {
            IWorkbenchWindow window = getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                if (!ApplicationWorkbenchAdvisor.closeOpenEditors(window, false, true)) {
                    return false;
                }
            }
            return ApplicationWorkbenchAdvisor.cancelRunningTasks(true) && ApplicationWorkbenchAdvisor.closeActiveTransactions(false);
        } catch (Throwable e) {
            log.error(e);
            return true;
        }
    }

    public static boolean closeOpenEditors(IWorkbenchWindow window, boolean forceRevert, boolean showConfirmation) {
        if (showConfirmation && !forceRevert &&
                !MessageDialogWithToggle.NEVER.equals(ConfirmationDialog.getSavedPreference(DBeaverPreferences.CONFIRM_EXIT))
        ) {
            // Workaround of #703 bug. NEVER doesn't make sense for Exit confirmation. It is the same as ALWAYS.
            if (ConfirmationDialog.confirmAction(window.getShell(), DBeaverPreferences.CONFIRM_EXIT, ConfirmationDialog.QUESTION)
                    != IDialogConstants.YES_ID) {
                return false;
            }
        }
        // Close all content editors
        // They are locking resources which are shared between other editors
        // So we need to close them first
        IWorkbenchPage workbenchPage = window.getActivePage();
        IEditorReference[] editors = workbenchPage.getEditorReferences();
        List<IEditorPart> editorsToRevert = new ArrayList<>();
        for (IEditorReference editor : editors) {
            IEditorPart editorPart = editor.getEditor(false);
            if (editorPart != null && editorPart.getEditorInput() instanceof ContentEditorInput) {
                workbenchPage.closeEditor(editorPart, false);
            }
        }
        // We also save all saveable parts here. Because we need to do this before transaction finializer hook.
        // Standard workbench finalizer works at the very end when it is too late
        // (all connections are closed at that moment)
        for (IEditorReference editor : editors) {
            IEditorPart editorPart = editor.getEditor(false);
            if (editorPart instanceof ISaveablePart2) {
                if (!forceRevert && !SaveableHelper.savePart(editorPart, editorPart, window, true)) {
                    return false;
                }
                editorsToRevert.add(editorPart);
            }
        }

        // Revert all open editors to avoid double confirmation
        for (IEditorPart editorPart : editorsToRevert) {
            try {
                EditorUtils.revertEditorChanges(editorPart);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        return true;
    }

    public static boolean closeActiveTransactions(boolean forceRollback) {
        for (DBPDataSourceContainer dataSourceDescriptor : DataSourceRegistry.getAllDataSources()) {
            if (!DataSourceHandler.checkAndCloseActiveTransaction(dataSourceDescriptor, false, forceRollback)) {
                return false;
            }
        }
        return true;
    }

    public static boolean cancelRunningTasks(boolean confirmCancel) {
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject == null) {
            // Probably some TE user without permissions and projects
            return true;
        }
        final DBTTaskManager manager = activeProject.getTaskManager(false);
        if (manager == null) {
            return true;
        }

        if (manager.hasRunningTasks()) {
            final boolean cancel = !confirmCancel || DBWorkbench.getPlatformUI().confirmAction(
                CoreApplicationMessages.confirmation_cancel_database_tasks_title,
                CoreApplicationMessages.confirmation_cancel_database_tasks_message
            );

            if (cancel) {
                manager.cancelRunningTasks();
            }

            return cancel;
        }

        return true;
    }

    public void eventLoopException(Throwable exception) {
        super.eventLoopException(exception);
        log.error("Event loop exception", exception);
    }

    @Override
    public void eventLoopIdle(Display display) {
        processor.catchUp(display);
        super.eventLoopIdle(display);
    }

}
