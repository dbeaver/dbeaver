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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.registry.WorkbenchHandlerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.StringJoiner;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements DBPProjectListener, IResourceChangeListener {
    private static final Log log = Log.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);

        if (DBeaverApplication.WORKSPACE_MIGRATED) {
            refreshProjects();
        }

        DBWorkbench.getPlatform().getWorkspace().addProjectListener(this);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
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
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        // Remove project listener
        DBPPlatform platform = DBWorkbench.getPlatform();
        if (platform != null) {
            DBPWorkspace workspace = platform.getWorkspace();
            workspace.removeProjectListener(this);
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
        super.preWindowOpen();
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
        hookTitleUpdateListeners(configurer);

        DBeaverUI.getInstance();
    }

    /**
     * Hooks the listeners needed on the window
     */
    private void hookTitleUpdateListeners(IWorkbenchWindowConfigurer configurer) {
        // hook up the listeners to update the window title
        configurer.getWindow().addPageListener(new AbstractPageListener() {
            @Override
            public void pageActivated(IWorkbenchPage page) {
                recomputeTitle();
            }
            @Override
            public void pageClosed(IWorkbenchPage page) {
                recomputeTitle();
            }
        });
        configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {
            @Override
            public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
                recomputeTitle();
            }
            @Override
            public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor oldPerspective, IPerspectiveDescriptor newPerspective) {
                recomputeTitle();
            }
            @Override
            public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
                recomputeTitle();
            }
        });
        configurer.getWindow().getPartService().addPartListener(
            new IPartListener2() {
                @Override
                public void partActivated(IWorkbenchPartReference ref) {
                    if (ref instanceof IEditorReference) {
                        recomputeTitle();
                    }
                }
                @Override
                public void partBroughtToTop(IWorkbenchPartReference ref) {
                    if (ref instanceof IEditorReference) {
                        recomputeTitle();
                    }
                }
                @Override
                public void partClosed(IWorkbenchPartReference ref) {
                    recomputeTitle();
                }

                @Override
                public void partDeactivated(IWorkbenchPartReference partRef) {
                }

                @Override
                public void partOpened(IWorkbenchPartReference partRef) {
                }

                @Override
                public void partHidden(IWorkbenchPartReference ref) {
                    recomputeTitle();
                }
                @Override
                public void partVisible(IWorkbenchPartReference ref) {
                    recomputeTitle();
                }

                @Override
                public void partInputChanged(IWorkbenchPartReference partRef) {
                    recomputeTitle();
                }
            });
    }

    @Override
    public void postWindowCreate() {
        log.debug("Initialize workbench window");
        super.postWindowCreate();
        recomputeTitle();


        try {
            DBeaverCommandLine.executeCommandLineCommands(
                DBeaverCommandLine.getCommandLine(),
                DBeaverApplication.getInstance().getInstanceServer(),
                true);
        } catch (Exception e) {
            log.error("Error processing command line", e);
        }
    }

    @Override
    public void postWindowOpen() {
        log.debug("Finish initialization");
        super.postWindowOpen();

        UIUtils.asyncExec(() -> {

        });
        try {
            ApplicationCSSManager.updateApplicationCSS(Display.getCurrent());
        } catch (Throwable e) {
            log.warn(e);
        }
        if (isRunWorkbenchInitializers()) {
            // Open New Connection wizard
            UIUtils.asyncExec(() -> {
                for (IWorkbenchWindowInitializer wwInit : WorkbenchHandlerRegistry.getInstance().getWorkbenchWindowInitializers()) {
                    wwInit.initializeWorkbenchWindow(getWindowConfigurer().getWindow());
                }
            });
        }
    }

    protected boolean isRunWorkbenchInitializers() {
        return true;
    }

    @Override
    public void handleProjectAdd(DBPProject project) {

    }

    @Override
    public void handleProjectRemove(DBPProject project) {

    }

    @Override
    public void handleActiveProjectChange(DBPProject oldValue, DBPProject newValue) {
        UIUtils.asyncExec(this::recomputeTitle);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            // Checker for active editor's file change
            try {
                event.getDelta().accept(delta -> {
                    IResource resource = delta.getResource();
                    if (resource instanceof IFile) {
                        if ((IResourceDelta.MOVED_TO & delta.getFlags()) != 0) {
                            IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                            if (workbenchWindow != null) {
                                IWorkbenchPage activePage = workbenchWindow.getActivePage();
                                if (activePage != null) {
                                    IEditorPart activeEditor = activePage.getActiveEditor();
                                    if (activeEditor != null) {
                                        IFile file = EditorUtils.getFileFromInput(activeEditor.getEditorInput());
                                        if (file != null && file.equals(resource)) {
                                            UIUtils.asyncExec(this::recomputeTitle);
                                        }
                                    }
                                }
                            }

                        }
                        return false;
                    }
                    return true;
                });
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public class EditorAreaDropAdapter extends DropTargetAdapter {
    }

    private void recomputeTitle() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        String oldTitle = configurer.getTitle();
        String newTitle = computeTitle();
        if (!newTitle.equals(oldTitle)) {
            configurer.setTitle(newTitle);
        }
    }

    private String computeTitle() {
        // Use hardcoded pref constants to avoid E4.7 compile dependency
        IPreferenceStore ps = IDEWorkbenchPlugin.getDefault().getPreferenceStore();
        StringJoiner sj = new StringJoiner(" - "); //$NON-NLS-1$

        if (ps.getBoolean("SHOW_LOCATION_NAME")) {
            String workspaceName = ps.getString("WORKSPACE_NAME");
            if (workspaceName != null && workspaceName.length() > 0) {
                sj.add(workspaceName);
            }
        }
        if (ps.getBoolean("SHOW_LOCATION")) {
            String workspaceLocation = Platform.getLocation().toOSString();
            sj.add(workspaceLocation);
        }

        if (ps.getBoolean("SHOW_PERSPECTIVE_IN_TITLE")) {
            DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
            if (activeProject != null) {
                sj.add(activeProject.getName()); //$NON-NLS-1$
            }
        }
        if (ps.getBoolean("SHOW_PRODUCT_IN_TITLE")) {
            sj.add(GeneralUtils.getProductTitle());
        }
        IWorkbenchWindow window = getWindowConfigurer().getWindow();
        if (window != null) {
            IWorkbenchPage activePage = window.getActivePage();
            if (activePage != null) {
                IEditorPart activeEditor = activePage.getActiveEditor();
                if (activeEditor != null) {
                    sj.add(activeEditor.getTitle());
                }
            }
        }
        return sj.toString();
    }

}

