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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.registry.WorkbenchHandlerRegistry;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements DBPProjectListener {
    private static final Log log = Log.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);

        if (DBeaverApplication.WORKSPACE_MIGRATED) {
            refreshProjects();
        }

        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    private void refreshProjects() {

        // Refresh all projects
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            try {
                project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Error refreshing project '" + project.getName() + "'", e);
            }
        }
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
        Rectangle displaySize = Display.getCurrent().getPrimaryMonitor().getBounds();
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(displaySize.width * 3 / 4, displaySize.height * 3 / 4));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowPerspectiveBar(true);
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

        if (isRunWorkbenchInitializers()) {
            // Open New Connection wizard
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    for (IWorkbenchWindowInitializer wwInit : WorkbenchHandlerRegistry.getInstance().getWorkbenchWindowInitializers()) {
                        wwInit.initializeWorkbenchWindow(getWindowConfigurer().getWindow());
                    }
                }
            });
        }
    }

    protected boolean isRunWorkbenchInitializers() {
        return true;
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue) {
        UIUtils.updateMainWindowTitle(getWindowConfigurer().getWindow());
    }

    public class EditorAreaDropAdapter extends DropTargetAdapter {
    }

}

