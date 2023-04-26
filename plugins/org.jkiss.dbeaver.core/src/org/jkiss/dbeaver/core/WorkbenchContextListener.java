/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceToolbarHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.perspective.DBeaverPerspective;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * WorkbenchContextListener.
 * Listens workbench parts activations/deactivation and activates contexts for navigator and SQL editors.
 *
 * TODO: add multipage editor listener and folder listener. Maybe use focus listener on control
 */
public class WorkbenchContextListener implements IWindowListener, IPageListener, IPartListener {

    //private static final Log log = Log.getLog(WorkbenchContextListener.class);

    public static final String PERSPECTIVE_CONTEXT_ID = "org.jkiss.dbeaver.ui.perspective";

    private CommandExecutionListener commandExecutionListener;
    private final Set<IWorkbenchWindow> registeredWindows = new HashSet<>();

    public WorkbenchContextListener() {
        IWorkbench workbench = PlatformUI.getWorkbench();

        // Register in already created windows and pages
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            listenWindowEvents(window);
        }
        workbench.addWindowListener(this);

        {
            final ICommandService commandService = workbench.getService(ICommandService.class);
            if (commandService != null) {
                commandExecutionListener = new CommandExecutionListener();
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
    }

    private void listenWindowEvents(IWorkbenchWindow window) {
        if (!registeredWindows.contains(window)) {
            // Register ds toolbar handler
            DataSourceToolbarHandler toolbarHandler = new DataSourceToolbarHandler(window);
            window.getShell().addDisposeListener(e -> toolbarHandler.dispose());
            registeredWindows.add(window);
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
        activatePartContexts(part);
//        log.info(part.getClass().getSimpleName() + " ACTIVATED: " + contextService.getActiveContextIds());
    }

    void activatePartContexts(IWorkbenchPart part) {
        IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
        if (contextService == null) {
            return;
        }
        try {
            contextService.deferUpdates(true);
//            if (part instanceof INavigatorModelView) {
                // We check for instanceof (do not use adapter) because otherwise it become active
                // for all entity editor and clashes with SQL editor and other complex stuff.
//                if (activationNavigator != null) {
//                    //log.debug("Double activation of navigator context");
//                    contextService.deactivateContext(activationNavigator);
//                }
//                activationNavigator = contextService.activateContext(INavigatorModelView.NAVIGATOR_CONTEXT_ID);
//            }

            // What the point of setting SQL editor context here? It is set by editor itself
//            if (part instanceof SQLEditorBase || part.getAdapter(SQLEditorBase.class) != null) {
//                if (activationSQL != null) {
//                    //log.debug("Double activation of SQL context");
//                    contextService.deactivateContext(activationSQL);
//                }
//                activationSQL = contextService.activateContext(SQLEditorContributions.SQL_EDITOR_CONTEXT);
//            }
            // Refresh auto-commit element state (#3315)
            // Refresh OpenSeparateConnection
            ActionUtils.fireCommandRefresh(ConnectionCommands.CMD_TOGGLE_AUTOCOMMIT, SQLEditorCommands.CMD_TOGGLE_SEPARATE_CONNECTION);
        }
        finally {
            contextService.deferUpdates(false);
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
        deactivatePartContexts(part);
//        log.info(part.getClass().getSimpleName() + " DEACTIVATED: " + contextService.getActiveContextIds());
    }

    void deactivatePartContexts(IWorkbenchPart part) {
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
        fireOnNewSqlEditorListener(part);
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
    
    private static final Object editorListenersSyncRoot = new Object();
    private static final Set<Consumer<SQLEditor>> editorListeners = new HashSet<>();
    
    public static void addOnNewSqlEditorListener(Consumer<SQLEditor> listener) {
        synchronized (editorListenersSyncRoot) {
            editorListeners.add(listener);
        }
    }
    
    private static void fireOnNewSqlEditorListener(IWorkbenchPart part) {
        if (part instanceof SQLEditor) {
            SQLEditor editor = (SQLEditor) part;
            synchronized (editorListenersSyncRoot) {
                for (Consumer<SQLEditor> consumer : editorListeners) {
                    consumer.accept(editor);
                }
            }
        }
    }
    
}
