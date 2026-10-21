/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.core;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.services.IServiceScopes;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceToolbarHandler;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.NodeEditorInput;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.perspective.DBeaverPerspective;

import java.util.*;

/**
 * WorkbenchContextListener.
 * Listens workbench parts activations/deactivation and activates contexts for navigator and SQL editors.
 *
 * TODO: add multipage editor listener and folder listener. Maybe use focus listener on control
 */
public class WorkbenchContextListener implements IWindowListener, IPageListener, IPartListener {

    public static final String PERSPECTIVE_CONTEXT_ID = "org.jkiss.dbeaver.ui.perspective";

    private final Set<IWorkbenchWindow> registeredWindows = new HashSet<>();
    private final DBPProjectListener projectListener;
    private final INullSelectionListener selectionListener = (part, selection) -> {
        if (part == null) {
            registeredWindows.forEach(this::refreshDeleteLabels);
        } else {
            refreshDeleteLabels(part.getSite().getWorkbenchWindow());
        }
    };

    private final Listener focusListener = event -> {
        if (event.widget instanceof Control control) {
            IWorkbenchWindow window = UIUtils.findWorkbenchWindow(control);
            if (window != null && registeredWindows.contains(window)) {
                refreshDeleteLabels(window);
            }
        }
    };

    public WorkbenchContextListener() {
        IWorkbench workbench = PlatformUI.getWorkbench();

        // Register in already created windows and pages
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            listenWindowEvents(window);
        }
        workbench.getDisplay().addFilter(SWT.FocusIn, focusListener);
        workbench.addWindowListener(this);

        {
            final ICommandService commandService = workbench.getService(ICommandService.class);
            if (commandService != null) {
                CommandExecutionListener commandExecutionListener = new CommandExecutionListener();
                commandService.addExecutionListener(commandExecutionListener);
            }
        }

        workbench.addWindowListener(this);
        workbench.addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                return true;
            }

            @Override
            public void postShutdown(IWorkbench workbench) {
                DesktopPlatform.setClosing(true);
            }
        });
        IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
        if (activeWindow != null) {
            windowActivated(activeWindow);
            IWorkbenchPage activePage = activeWindow.getActivePage();
            if (activePage != null) {
                pageActivated(activePage);
                IWorkbenchPart activePart = activePage.getActivePart();
                if (activePart != null) {
                    partActivated(activePart);
                }
            }
        }

        projectListener = new DBPProjectListener() {
            @Override
            public void handleProjectRemove(@NotNull DBPProject project) {
                UIUtils.asyncExec(() -> closeRemovedProjectsEditors(project));
            }
        };
        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(projectListener);
    }

    private void closeRemovedProjectsEditors(DBPProject project) {
        Arrays.stream(PlatformUI.getWorkbench().getWorkbenchWindows())
            .flatMap(window -> Arrays.stream(window.getPages()))
            .forEach(page -> closePageProjectEditors(page, project));
    }

    private void closePageProjectEditors(IWorkbenchPage page, DBPProject project) {
        for (IEditorReference editorReference : page.getEditorReferences()) {
            IEditorInput input;
            try {
                input = editorReference.getEditorInput();
            } catch (PartInitException e) {
                return;
            }

            if (input instanceof NodeEditorInput nodeEditorInput) {
                DBNNode node = nodeEditorInput.getNavigatorNode();
                if (node != null && Objects.equals(project, node.getOwnerProject())) {
                    IEditorPart editor = editorReference.getEditor(false);
                    page.closeEditor(editor, false);
                    editor.dispose();
                }
            }
        }
    }

    private void listenWindowEvents(IWorkbenchWindow window) {
        if (!registeredWindows.contains(window)) {
            // Register ds toolbar handler
            DataSourceToolbarHandler toolbarHandler = new DataSourceToolbarHandler(window);
            window.getShell().addDisposeListener(e -> toolbarHandler.dispose());
            registeredWindows.add(window);
            window.getSelectionService().addSelectionListener(selectionListener);
        }

        IPerspectiveListener perspectiveListener = new IPerspectiveListener() {
            private IContextActivation perspectiveActivation;

            @Override
            public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
                IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
                if (contextService == null) {
                    return;
                }
                if (perspective.getId().equals(DBeaverPerspective.PERSPECTIVE_ID)) {
                    perspectiveActivation = contextService.activateContext(PERSPECTIVE_CONTEXT_ID);
                } else if (perspectiveActivation != null) {
                    contextService.deactivateContext(perspectiveActivation);
                    perspectiveActivation = null;
                }

                CoreFeatures.GENERAL_SHOW_PERSPECTIVE.use(Map.of("perspective", perspective.getId()));
            }

            @Override
            public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {

            }
        };
        window.addPerspectiveListener(perspectiveListener);
        IWorkbenchPage activePage = window.getActivePage();
        if (activePage != null) {
            perspectiveListener.perspectiveActivated(activePage, activePage.getPerspective());
        }

        window.addPageListener(this);
        for (IWorkbenchPage page : window.getPages()) {
            for (IViewReference vr : page.getViewReferences()) {
                if (vr.getView(false) != null) {
                    CoreFeatures.GENERAL_VIEW_OPEN.use(Map.of("view", vr.getId()));
                }
            }
            page.addPartListener(this);
        }
    }

    /////////////////////////////////////////////////
    // IWindowListener

    @Override
    public void windowActivated(IWorkbenchWindow window) {
        refreshDeleteLabels(window);
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {

    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        listenWindowEvents(window);
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        window.removePageListener(this);
        window.getSelectionService().removeSelectionListener(selectionListener);
        registeredWindows.remove(window);
    }

    /////////////////////////////////////////////////
    // IPageListener

    @Override
    public void pageActivated(IWorkbenchPage page) {
    }

    @Override
    public void pageOpened(IWorkbenchPage page) {
        page.addPartListener(this);
    }

    @Override
    public void pageClosed(IWorkbenchPage page) {
        page.removePartListener(this);
    }

    /////////////////////////////////////////////////
    // IPartListener

    @Override
    public void partActivated(IWorkbenchPart part) {
        EditorUtils.activatePartContexts(part);
        refreshDeleteLabels(part.getSite().getWorkbenchWindow());
//        log.info(part.getClass().getSimpleName() + " ACTIVATED: " + contextService.getActiveContextIds());
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
        EditorUtils.deactivatePartContexts(part);
//        log.info(part.getClass().getSimpleName() + " DEACTIVATED: " + contextService.getActiveContextIds());
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {

    }

    @Override
    public void partClosed(IWorkbenchPart part) {
        if (part instanceof IViewPart) {
            CoreFeatures.GENERAL_VIEW_CLOSE.use(Map.of(
                "view", ((IViewPart) part).getViewSite().getId()
            ));
        }
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
        if (part instanceof IViewPart) {
            CoreFeatures.GENERAL_VIEW_OPEN.use(Map.of(
                "view", ((IViewPart) part).getViewSite().getId()
            ));
        }
    }

    private void refreshDeleteLabels(@NotNull IWorkbenchWindow window) {
        UIUtils.asyncExec(() -> {
            Shell shell = window.getShell();
            if (shell == null || shell.isDisposed()) {
                return;
            }
            ICommandService commandService = window.getService(ICommandService.class);
            MWindow modelWindow = window.getService(MWindow.class);
            EModelService modelService = window.getService(EModelService.class);
            if (commandService == null || modelWindow == null || modelWindow.getMainMenu() == null || modelService == null) {
                return;
            }
            String commandId = IWorkbenchCommandConstants.EDIT_DELETE;
            String defaultLabel = ActionUtils.findCommandName(commandId);
            // the next handler may not supply a label when focus leaves the navigator
            for (MHandledMenuItem item : modelService.findElements(modelWindow.getMainMenu(), null, MHandledMenuItem.class)) {
                if (item.getCommand() != null && commandId.equals(item.getCommand().getElementId())) {
                    item.setLabel(defaultLabel);
                }
            }
            Map<String, Object> filter = Map.of(IServiceScopes.WINDOW_SCOPE, window);
            commandService.refreshElements(commandId, filter);
            commandService.refreshElements(NavigatorCommands.CMD_OBJECT_DELETE, filter);
        });
    }

    public void dispose() {
        PlatformUI.getWorkbench().getDisplay().removeFilter(SWT.FocusIn, focusListener);
        for (IWorkbenchWindow window : registeredWindows) {
            window.getSelectionService().removeSelectionListener(selectionListener);
        }
        registeredWindows.clear();
        DBPPlatformDesktop.getInstance().getWorkspace().removeProjectListener(projectListener);
    }

    static WorkbenchContextListener registerInWorkbench() {
        return new WorkbenchContextListener();
    }

    private static class CommandExecutionListener implements IExecutionListener {
        @Override
        public void notHandled(String commandId, NotHandledException exception) {

        }

        @Override
        public void postExecuteFailure(String commandId, ExecutionException exception) {

        }

        @Override
        public void postExecuteSuccess(String commandId, Object returnValue) {
        }

        @Override
        public void preExecute(String commandId, ExecutionEvent event) {
            final DBRFeature commandFeature = DBRFeatureRegistry.getInstance().findCommandFeature(commandId);
            if (commandFeature != null) {
                commandFeature.use(event.getParameters());
            }
        }
    }

}
