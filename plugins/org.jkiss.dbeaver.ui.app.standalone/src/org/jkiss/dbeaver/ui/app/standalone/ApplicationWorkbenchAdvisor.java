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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.SaveableHelper;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.app.standalone.update.DBeaverVersionChecker;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.perspective.DBeaverPerspective;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseEditors;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseUserInterface;

import java.util.ArrayList;
import java.util.List;

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
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.ContentTypes",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Startup",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.General.LinkHandlers",

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
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID,
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.AutoSave",
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.FileEditors" //"File Associations"
    };

    // Move to Editors
    private static final String[] EDITORS_PREF_PAGES = {
            WORKBENCH_PREF_PAGE_ID + "/" + EDITORS_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.GeneralTextEditor"
    };

    // Move to General
    private static final String[] GENERAL_PREF_PAGES = {
        "org.eclipse.equinox.internal.p2.ui.sdk.ProvisioningPreferencePage",    // Install-Update
        "org.eclipse.debug.ui.DebugPreferencePage"                              // Debugger
    };

    //processor must be created before we start event loop
    private final DelayedEventsProcessor processor;

    protected ApplicationWorkbenchAdvisor() {
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
        super.initialize(configurer);

        // Initialize app preferences
        DefaultScope.INSTANCE.getNode(CoreApplicationActivator.getDefault().getBundle().getSymbolicName());

        // Don't show Help button in JFace dialogs
        TrayDialog.setDialogHelpAvailable(false);

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
    }

    @Override
    public void postStartup() {
        super.postStartup();

        filterPreferencePages();

        startVersionChecker();
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
        // Remove unneeded pref pages
        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();

        for (String epp : getExcludedPreferencePageIds()) {
            pm.remove(epp);
        }
        patchPreferencePages(pm, EDITORS_PREF_PAGES, PrefPageDatabaseEditors.PAGE_ID);
        patchPreferencePages(pm, UI_PREF_PAGES, PrefPageDatabaseUserInterface.PAGE_ID);
        patchPreferencePages(pm, GENERAL_PREF_PAGES, WORKBENCH_PREF_PAGE_ID);
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

    private void startVersionChecker() {
        DBeaverVersionChecker checker = new DBeaverVersionChecker(false);
        checker.schedule(3000);
    }

    @Override
    public boolean preShutdown() {
        //DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(settingsChangeListener);

        if (!saveAndCleanup()) {
            // User rejected to exit
            return false;
        } else {
            return super.preShutdown();
        }
    }

    @Override
    public void postShutdown() {
        super.postShutdown();
    }

    private boolean saveAndCleanup() {
        try {
            IWorkbenchWindow window = getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                if (!MessageDialogWithToggle.NEVER.equals(ConfirmationDialog.getSavedPreference(DBeaverPreferences.CONFIRM_EXIT))) {
                    // Workaround of #703 bug. NEVER doesn't make sense for Exit confirmation. It is the same as ALWAYS.
                    if (!ConfirmationDialog.confirmAction(DBeaverActivator.getCoreResourceBundle(), window.getShell(), DBeaverPreferences.CONFIRM_EXIT)) {
                        return false;
                    }
                }
                // Close al content editors
                // They are locks resources which are shared between other editors
                // So we need to close em first
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
                // Standard workbench finalizer works in the very end when it is too late
                // (all connections are closed at that moment)
                for (IEditorReference editor : editors) {
                    IEditorPart editorPart = editor.getEditor(false);
                    if (editorPart instanceof ISaveablePart2) {
                        if (!SaveableHelper.savePart(editorPart, editorPart, window, true)) {
                            return false;
                        }
                        editorsToRevert.add(editorPart);
                    }
                }

                // Revert all open editors to  avoid double confirmation
                for (IEditorPart editorPart : editorsToRevert) {
                    try {
                        EditorUtils.revertEditorChanges(editorPart);
                    } catch (Exception e) {
                        log.debug(e);
                    }
                }
            }

            return closeActiveTransactions();
        } catch (Throwable e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean closeActiveTransactions() {
        for (DBPDataSourceContainer dataSourceDescriptor : DataSourceRegistry.getAllDataSources()) {
            if (!DataSourceHandler.checkAndCloseActiveTransaction(dataSourceDescriptor)) {
                return false;
            }
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
