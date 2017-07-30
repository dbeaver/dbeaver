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
package org.jkiss.dbeaver.core;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

/**
 * WorkbenchContextListener.
 * Listens workbench parts activations/deactivation and activates contexts for navigator and SQL editors.
 *
 * TODO: add multipage editor listener and folder listener. Maybe use focus listener on control
 */
class WorkbenchContextListener implements IWindowListener, IPageListener, IPartListener {

    private static final Log log = Log.getLog(WorkbenchContextListener.class);

    public static final String NAVIGATOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.navigator";
    public static final String SQL_EDITOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.editors.sql";
    public static final String RESULTS_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.resultset";

    private IContextActivation activationNavigator;
    private IContextActivation activationSQL;
    private IContextActivation activationResults;
    private CommandExecutionListener commandExecutionListener;

    public WorkbenchContextListener() {
        // Register in already created windows and pages
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            listenWindowEvents(window);
        }
    }

    private void listenWindowEvents(IWorkbenchWindow window) {
        window.addPageListener(this);
        for (IWorkbenchPage page : window.getPages()) {
            page.addPartListener(this);
        }
        if (commandExecutionListener == null) {
            final ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            if (commandService != null) {
                commandExecutionListener = new CommandExecutionListener();
                commandService.addExecutionListener(commandExecutionListener);
            }
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
        IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
        if (contextService == null) {
            return;
        }
        try {
            contextService.deferUpdates(true);
            if (part instanceof INavigatorModelView) {
                if (activationNavigator != null) {
                    //log.debug("Double activation of navigator context");
                    contextService.deactivateContext(activationNavigator);
                }
                activationNavigator = contextService.activateContext(NAVIGATOR_CONTEXT_ID);
            }
            if (part instanceof SQLEditorBase) {
                if (activationSQL != null) {
                    //log.debug("Double activation of SQL context");
                    contextService.deactivateContext(activationSQL);
                }
                activationSQL = contextService.activateContext(SQL_EDITOR_CONTEXT_ID);
            }
            if (part instanceof EntityEditor || part.getAdapter(ResultSetViewer.class) != null) {
                if (activationResults != null) {
                    contextService.deactivateContext(activationResults);
                }
                activationResults = contextService.activateContext(RESULTS_CONTEXT_ID);
            }
        }
        finally {
            contextService.deferUpdates(false);
        }
//        log.info(part.getClass().getSimpleName() + " ACTIVATED: " + contextService.getActiveContextIds());
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
        IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
        if (contextService == null) {
            return;
        }
        try {
            contextService.deferUpdates(true);
            if (activationNavigator != null && part instanceof INavigatorModelView) {
                contextService.deactivateContext(activationNavigator);
                activationNavigator = null;
            }
            if (activationSQL != null && part instanceof SQLEditorBase) {
                contextService.deactivateContext(activationSQL);
                activationSQL = null;
            }
            if (activationResults != null) {
                contextService.deactivateContext(activationResults);
                activationResults = null;
            }
        }
        finally {
            contextService.deferUpdates(false);
        }
//        log.info(part.getClass().getSimpleName() + " DEACTIVATED: " + contextService.getActiveContextIds());
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {

    }

    @Override
    public void partClosed(IWorkbenchPart part) {

    }

    @Override
    public void partOpened(IWorkbenchPart part) {

    }

    static void registerInWorkbench() {
        new Job("Workbench listener") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                if (!PlatformUI.isWorkbenchRunning()) {
                    schedule(50);
                } else {
                    PlatformUI.getWorkbench().addWindowListener(new WorkbenchContextListener());
                }
                return Status.OK_STATUS;
            }
        }.schedule();
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
            final DBRFeature commandFeature = DBRFeatureRegistry.getInstance().findCommandFeature(commandId);
            if (commandFeature != null) {
                commandFeature.use();
            }
        }

        @Override
        public void preExecute(String commandId, ExecutionEvent event) {

        }
    }
}
