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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchWindowAdvisor;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.WorkbenchHandlerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.StringJoiner;

public class ApplicationWorkbenchWindowAdvisor extends IDEWorkbenchWindowAdvisor implements DBPProjectListener, IResourceChangeListener {
    private static final Log log = Log.getLog(ApplicationWorkbenchWindowAdvisor.class);
    
    // Eclipse fonts
    
    /**
     * Compare text font
     */
    public static String COMPARE_TEXT_FONT = "org.eclipse.compare.contentmergeviewer.TextMergeViewer";
    
    /**
     * Detail pane text font
     */
    public static String DETAIL_PANE_TEXT_FONT = "org.eclipse.debug.ui.DetailPaneFont";
    
    /**
     * Memory view table font
     */
    public static String MEMORY_VIEW_TABLE_FONT = "org.eclipse.debug.ui.MemoryViewTableFont";

    /**
     * Variable text font
     */
    public static String VARIABLE_TEXT_FONT = "org.eclipse.debug.ui.VariableTextFont";
 
    /**
     * Console font
     */
    public static String CONSOLE_FONT = "org.eclipse.debug.ui.consoleFont";

    /**
     * Part title font
     */
    public static String PART_TITLE_FONT = "org.eclipse.ui.workbench.TAB_TEXT_FONT";

    /**
     * Tree and Table font for views
     */
    public static String TREE_AND_TABLE_FONT_FOR_VIEWS = "org.eclipse.ui.workbench.TREE_TABLE_FONT";

    /**
     * Header Font
     */
    public static String HEADER_FONT = "org.eclipse.jface.headerfont";

    /**
     * Text Font
     */
    public static String TEXT_FONT = "org.eclipse.jface.textfont";

    /**
     * Text Editor Block Selection Font
     */
    public static String TEXT_EDITOR_BLOCK_SELECTION_FONT = "org.eclipse.ui.workbench.texteditor.blockSelectionModeFont";

    /**
     * Banner font
     */
    public static String BANNER_FONT = JFaceResources.BANNER_FONT;

    /**
     * Dialog font
     */
    public static String DIALOG_FONT = JFaceResources.DIALOG_FONT;

    private IEditorPart lastActiveEditor = null;
    private IPerspectiveDescriptor lastPerspective = null;

    private IWorkbenchPage lastActivePage;
    private IAdaptable lastInput;
    private IPropertyChangeListener propertyChangeListener;
    private final IPropertyListener editorPropertyListener = (source, propId) -> {
        if (propId == IWorkbenchPartConstants.PROP_TITLE) {
            if (lastActiveEditor != null) {
                recomputeTitle();
            }
        }
    };

    public ApplicationWorkbenchWindowAdvisor(ApplicationWorkbenchAdvisor advisor, IWorkbenchWindowConfigurer configurer) {
        super(advisor, configurer);

        if (DBeaverApplication.WORKSPACE_MIGRATED) {
            refreshProjects();
        }

        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(this);

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
        if (propertyChangeListener != null) {
            IDEWorkbenchPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        // Remove project listener
        DBPPlatform platform = DBWorkbench.getPlatform();
        if (platform != null) {
            DBPWorkspaceEclipse workspace = DBPPlatformDesktop.getInstance().getWorkspace();
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
    public boolean preWindowShellClose() {
        return true;
    }

    @Override
    public void preWindowOpen() {
        log.debug("Configure workbench window");
        //super.preWindowOpen();
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

        // Show heap usage
        //PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
        hookTitleUpdateListeners(configurer);

        // Initialize drivers in the very beginning
        DataSourceProviderRegistry.getInstance();
    }

    /**
     * Hooks the listeners needed on the window
     */
    private void hookTitleUpdateListeners(IWorkbenchWindowConfigurer configurer) {
        // hook up the listeners to update the window title
        configurer.getWindow().addPageListener(new IPageListener() {
            @Override
            public void pageActivated(IWorkbenchPage page) {
                updateTitle(false);
            }

            @Override
            public void pageClosed(IWorkbenchPage page) {
                updateTitle(false);
            }

            @Override
            public void pageOpened(IWorkbenchPage page) {
                // do nothing
            }
        });
        configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {
            @Override
            public void perspectiveActivated(IWorkbenchPage page,
                                             IPerspectiveDescriptor perspective) {
                updateTitle(false);
            }

            @Override
            public void perspectiveSavedAs(IWorkbenchPage page,
                                           IPerspectiveDescriptor oldPerspective,
                                           IPerspectiveDescriptor newPerspective) {
                updateTitle(false);
            }

            @Override
            public void perspectiveDeactivated(IWorkbenchPage page,
                                               IPerspectiveDescriptor perspective) {
                updateTitle(false);
            }
        });
        configurer.getWindow().getPartService().addPartListener(
            new IPartListener2() {
                @Override
                public void partActivated(IWorkbenchPartReference ref) {
                    if (ref instanceof IEditorReference) {
                        updateTitle(false);
                    }
                }

                @Override
                public void partBroughtToTop(IWorkbenchPartReference ref) {
                    if (ref instanceof IEditorReference) {
                        updateTitle(false);
                    }
                }

                @Override
                public void partClosed(IWorkbenchPartReference ref) {
                    updateTitle(false);
                    handlePartClosed(ref);
                }

                @Override
                public void partDeactivated(IWorkbenchPartReference ref) {
                    // do nothing
                }

                @Override
                public void partOpened(IWorkbenchPartReference ref) {
                    // do nothing
                }

                @Override
                public void partHidden(IWorkbenchPartReference ref) {
                    if (ref.getPart(false) == lastActiveEditor
                        && lastActiveEditor != null) {
                        updateTitle(true);
                    }
                }

                @Override
                public void partVisible(IWorkbenchPartReference ref) {
                    if (ref.getPart(false) == lastActiveEditor
                        && lastActiveEditor != null) {
                        updateTitle(false);
                    }
                }

                @Override
                public void partInputChanged(IWorkbenchPartReference ref) {
                    // do nothing
                }
            });

        // Listen for changes of the workspace name.
        propertyChangeListener = event -> {
            String property = event.getProperty();
            if (IDEInternalPreferences.WORKSPACE_NAME.equals(property)
                || IDEInternalPreferences.SHOW_LOCATION.equals(property)
                || IDEInternalPreferences.SHOW_LOCATION_NAME.equals(property)
                || IDEInternalPreferences.SHOW_PERSPECTIVE_IN_TITLE.equals(property)
                || IDEInternalPreferences.SHOW_PRODUCT_IN_TITLE.equals(property)) {
                // Make sure the title is actually updated by
                // setting last active page.
                lastActivePage = null;
                updateTitle(false);
            }
        };
        IDEWorkbenchPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);
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

    protected void initWorkbenchWindows() {
        UIUtils.asyncExec(() -> {
            for (IWorkbenchWindowInitializer wwInit : WorkbenchHandlerRegistry.getInstance().getWorkbenchWindowInitializers()) {
                wwInit.initializeWorkbenchWindow(getWindowConfigurer().getWindow());
            }
        });
    }

    @Override
    public void postWindowOpen() {
        log.debug("Finish initialization");
        super.postWindowOpen();

        closeEmptyEditors();

        try {
            ApplicationCSSManager.updateApplicationCSS(Display.getCurrent());
        } catch (Throwable e) {
            log.warn(e);
        }
        if (isRunWorkbenchInitializers()) {
            // Open New Connection wizard
                initWorkbenchWindows();
        }

        UIUtils.asyncExec(() -> {
            // FIXME: dirty hack of standard commands handle (e.g. CTRL+C)
            // Re-activate active part to trigger keybindings refresh for it
            IWorkbenchPage activePage = getWindowConfigurer().getWindow().getActivePage();
            IWorkbenchPart activePart = activePage.getActivePart();
            if (activePart != null) {
                for (IViewReference viewReference : activePage.getViewReferences()) {
                    IViewPart view = viewReference.getView(false);
                    if (view != null && view != activePart) {
                        activePage.activate(view);
                        activePage.activate(activePart);
                        break;
                    }
                }
            }
        });
    }

    protected boolean isRunWorkbenchInitializers() {
        return true;
    }

    @Override
    public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue) {
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

    /**
     * Closes all empty editors that has no persisted state associated with it.
     * <p>
     * This can be achieved by causing the workbench to persist its state without
     * actually closing the application (e.g. using workbench auto-save feature),
     * and then force-closing the application (via task manager, etc.), so exit
     * hooks are not called. In fact, we do manually close non-persistable editors
     * using such hook, so empty editors do not appear under normal circumstances.
     * <p>
     * Since such editors lack any data, Eclipse doesn't know what editor it is,
     * and marks it with {@link EditorRegistry#EMPTY_EDITOR_ID}.
     */
    private void closeEmptyEditors() {
        for (IWorkbenchPage page : getWindowConfigurer().getWindow().getPages()) {
            for (IEditorReference reference : page.getEditorReferences()) {
                if (EditorRegistry.EMPTY_EDITOR_ID.equals(reference.getId())) {
                    page.closeEditors(new IEditorReference[]{reference}, false);
                }
            }
        }
    }

    private void handlePartClosed(@NotNull IWorkbenchPartReference ref) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DatabaseEditorPreferences.PROP_DISCONNECT_ON_EDITORS_CLOSE)) {
            return;
        }
        if (!(ref instanceof IEditorReference editor)) {
            // Not an editor
            return;
        }
        DBPDataSourceContainer container;
        try {
            container = EditorUtils.getInputDataSource(editor.getEditorInput());
        } catch (PartInitException ignored) {
            container = null;
        }
        if (container != null && !hasEditorsForDataSource(container)) {
            log.debug("Last editor for '" + container.getName() + "' was closed. Closing connection");
            DataSourceHandler.disconnectDataSource(container, null);
        }
    }

    private boolean hasEditorsForDataSource(@NotNull DBPDataSourceContainer container) {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    if (isEditorForDataSource(ref, container)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isEditorForDataSource(@NotNull IEditorReference ref, @NotNull DBPDataSourceContainer container) {
        try {
            return EditorUtils.getInputDataSource(ref.getEditorInput()) == container;
        } catch (PartInitException ignored) {
            return false;
        }
    }

    private void updateTitle(boolean editorHidden) {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        IWorkbenchWindow window = configurer.getWindow();
        IEditorPart activeEditor = null;
        IWorkbenchPage currentPage = window.getActivePage();
        IPerspectiveDescriptor persp = null;
        IAdaptable input = null;

        if (currentPage != null) {
            activeEditor = currentPage.getActiveEditor();
            persp = currentPage.getPerspective();
            input = currentPage.getInput();
        }

        if (editorHidden) {
            activeEditor = null;
        }

        // Nothing to do if the editor hasn't changed
        if (activeEditor == lastActiveEditor && currentPage == lastActivePage
            && persp == lastPerspective && input == lastInput) {
            return;
        }

        if (lastActiveEditor != null) {
            lastActiveEditor.removePropertyListener(editorPropertyListener);
        }

        if (window.isClosing()) {
            return;
        }

        lastActiveEditor = activeEditor;
        lastActivePage = currentPage;
        lastPerspective = persp;
        lastInput = input;

        if (activeEditor != null) {
            activeEditor.addPropertyListener(editorPropertyListener);
        }

        recomputeTitle();
    }

    private void recomputeTitle() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        String oldTitle = configurer.getTitle();
        String newTitle = computeTitle();
        if (!newTitle.equals(oldTitle)) {
            configurer.setTitle(newTitle);
        }
    }

    protected String computeTitle() {
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
            sj.add(computeProductTitle());
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

    @NotNull
    protected String computeProductTitle() {
        return GeneralUtils.getProductTitle();
    }

}

