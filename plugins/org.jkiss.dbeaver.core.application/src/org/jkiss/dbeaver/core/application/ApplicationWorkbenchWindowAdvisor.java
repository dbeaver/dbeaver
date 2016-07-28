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

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.CreateConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements DBPProjectListener {
    private static final Log log = Log.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);

        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    @Override
    public void dispose() {
        // Remove project listener
        DBeaverCore core = DBeaverCore.getInstance();
        if (core != null) {
            ProjectRegistry projectRegistry = core.getProjectRegistry();
            if (projectRegistry != null) {
                projectRegistry.removeProjectListener(this);
            }
        }

        super.dispose();
    }

    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        log.debug("Create actions");
        return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
    public void preWindowOpen() {
        log.debug("Configure workbench window");
        // Set timeout for short jobs (like SQL queries)
        // Jobs longer than this will show progress dialog
        ProgressManagerUtil.SHORT_OPERATION_TIME = 100;

        // Configure window
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(800, 600));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.configureEditorAreaDropListener(new EditorAreaDropAdapter());

        configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());
        configurer.addEditorAreaTransfer(ResourceTransfer.getInstance());
        configurer.addEditorAreaTransfer(FileTransfer.getInstance());
        configurer.addEditorAreaTransfer(MarkerTransfer.getInstance());
        configurer.configureEditorAreaDropListener(new org.eclipse.ui.internal.ide.EditorAreaDropAdapter(
            configurer.getWindow()));

        //PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
        //preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Perspectives");
        //preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Workspace");

        // Show heap usage
        //PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
    }

    @Override
    public void postWindowCreate() {
        log.debug("Initialize workbench window");
        final IWorkbenchWindow window = getWindowConfigurer().getWindow();
        UIUtils.updateMainWindowTitle(window);
/*
        try {
            ApplicationCSSManager.updateApplicationCSS(window.getShell().getDisplay());
        } catch (Throwable e) {
            log.warn(e);
        }
*/

        try {
            DBeaverApplication.executeCommandLineCommands(
                DBeaverApplication.getCommandLine(),
                DBeaverApplication.getInstance().getInstanceServer());
        } catch (Exception e) {
            log.error("Error processing command line", e);
        }
    }

    @Override
    public void postWindowOpen() {
        log.debug("Finish initialization");
        super.postWindowOpen();

        if (DataSourceDescriptor.getActiveDataSources().isEmpty()) {
            // Open New Connection wizard
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchWindow window = getWindowConfigurer().getWindow();
                    CreateConnectionDialog dialog = new CreateConnectionDialog(window
                        , new NewConnectionWizard(DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry()));
                    dialog.open();
                }
            });
        }
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue) {
        UIUtils.updateMainWindowTitle(getWindowConfigurer().getWindow());
    }

    public class EditorAreaDropAdapter extends DropTargetAdapter {
    }

}

