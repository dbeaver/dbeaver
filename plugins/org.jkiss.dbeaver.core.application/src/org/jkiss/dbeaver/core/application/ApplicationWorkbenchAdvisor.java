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
package org.jkiss.dbeaver.core.application;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.application.update.DBeaverVersionChecker;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
    private static final String PERSPECTIVE_ID = "org.jkiss.dbeaver.core.perspective"; //$NON-NLS-1$
    public static final String DBEAVER_SCHEME_NAME = "org.jkiss.dbeaver.defaultKeyScheme"; //$NON-NLS-1$

    private static final String WORKBENCH_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Workbench";
    private static final String APPEARANCE_PREF_PAGE_ID = "org.eclipse.ui.preferencePages.Views";
    private static final String[] EXCLUDE_PREF_PAGES = {
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Globalization",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Perspectives",
        //"org.eclipse.ui.preferencePages.FileEditors",
        WORKBENCH_PREF_PAGE_ID + "/" + APPEARANCE_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Decorators",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Workspace",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.ContentTypes",
        WORKBENCH_PREF_PAGE_ID + "/org.eclipse.ui.preferencePages.Startup"

    };

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

        TrayDialog.setDialogHelpAvailable(true);
    }

    @Override
    public void preStartup() {
        super.preStartup();
    }

    @Override
    public void postStartup() {
        super.postStartup();

        // Remove unneeded pref pages
        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
        for (String epp : EXCLUDE_PREF_PAGES) {
            pm.remove(epp);
        }

        startVersionChecker();
    }

    private void startVersionChecker() {
        DBeaverVersionChecker checker = new DBeaverVersionChecker(false);
        checker.schedule(3000);
    }

    @Override
    public boolean preShutdown() {
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
                    if (!ConfirmationDialog.confirmAction(window.getShell(), DBeaverPreferences.CONFIRM_EXIT)) {
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
        for (DataSourceDescriptor dataSourceDescriptor : DataSourceRegistry.getAllDataSources()) {
            if (!DataSourceHandler.checkAndCloseActiveTransaction(dataSourceDescriptor)) {
                return false;
            }
        }
        return true;
    }

}
