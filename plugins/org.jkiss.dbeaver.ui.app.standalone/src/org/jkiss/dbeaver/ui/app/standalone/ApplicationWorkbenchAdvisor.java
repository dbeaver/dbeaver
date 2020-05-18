/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.app.standalone.update.DBeaverVersionChecker;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.perspective.DBeaverPerspective;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
    private static final Log log = Log.getLog(ApplicationWorkbenchAdvisor.class);

    private static final String PERSPECTIVE_ID = DBeaverPerspective.PERSPECTIVE_ID;
    public static final String DBEAVER_SCHEME_NAME = "org.jkiss.dbeaver.defaultKeyScheme"; //$NON-NLS-1$

    protected static final String WORKBENCH_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Workbench";
    protected static final String APPEARANCE_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Views";
    //protected static final String MYLYN_PREF_PAGE_ID = "org.eclipse.mylyn.preferencePages.Mylyn";

    private static final String[] EXCLUDE_PREF_PAGES = {
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Globalization",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Perspectives",
        //"org.eclipse.ui.preferencePages.FileEditors",
        WORKBENCH_PREF_PAGE_ID + "/" + APPEARANCE_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Decorators",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Workspace",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.ContentTypes",
        //WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Startup",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.General.LinkHandlers",

        // Disable Install/Update
        //"org.eclipse.equinox.internal.p2.ui.sdk.ProvisioningPreferencePage",

        // Team preferences - not needed in CE
        //"org.eclipse.team.ui.TeamPreferences",
    };
    //private DBPPreferenceListener settingsChangeListener;

    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    @Override
    public String getInitialWindowPerspectiveId() {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer) {
        super.initialize(configurer);
        // make sure we always save and restore workspace state
        configurer.setSaveAndRestore(true);

        // register workspace IDE adapters
        IDE.registerAdapters();

        declareWorkbenchImages(configurer);

        //TrayDialog.setDialogHelpAvailable(true);

/*
        // Set default resource encoding to UTF-8
        String defEncoding = DBWorkbench.getPlatform().getPreferenceStore().getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING);
        if (CommonUtils.isEmpty(defEncoding)) {
            defEncoding = GeneralUtils.UTF8_ENCODING;
        }
        ResourcesPlugin.getPlugin().getPluginPreferences().setValue(ResourcesPlugin.PREF_ENCODING, defEncoding);
*/
    }

    /**
     * This is a bit hacky. Copied from IDEWorkbenchAdvisor.
     * Adds standard Eclipse icons mappings
     */
    private void declareWorkbenchImages(IWorkbenchConfigurer configurer) {

        Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);
        final String ICONS_PATH = "$nl$/icons/full/";//$NON-NLS-1$
        final String PATH_OBJECT = ICONS_PATH + "obj16/"; // Model object //$NON-NLS-1$
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT,
            PATH_OBJECT + "prj_obj.png", true); //$NON-NLS-1$
    }

    private void declareWorkbenchImage(IWorkbenchConfigurer configurer, Bundle ideBundle, String symbolicName, String path, boolean shared) {
        URL url = FileLocator.find(ideBundle, new Path(path), null);
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        configurer.declareImage(symbolicName, desc, shared);
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

/*
        settingsChangeListener = event -> {
            if (isPropertyChangeRequiresRestart(event.getProperty())) {
                if (UIUtils.confirmAction(null,
                    "System preference change",
                    "System setting '" + event.getProperty() + "' has been changed. You will need to restart workbench to complete the change. Restart now?"))
                {
                    PlatformUI.getWorkbench().restart();
                }
            }
        };
        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(settingsChangeListener);
*/

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
    }

    @NotNull
    protected String[] getExcludedPreferencePageIds() {
        return EXCLUDE_PREF_PAGES;
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
                for (IEditorReference editor : editors) {
                    IEditorPart editorPart = editor.getEditor(false);
                    if (editorPart != null && editorPart.getEditorInput() instanceof ContentEditorInput) {
                        workbenchPage.closeEditor(editorPart, false);
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
}
