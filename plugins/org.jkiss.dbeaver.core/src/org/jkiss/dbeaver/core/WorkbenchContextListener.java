/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

/**
 * WorkbenchContextListener.
 * Listens workbench parts activations/deactivation and activates contexts for navigator and SQL editors.
 *
 * TODO: add multipage editor listener and folder listener. Maybe use focus listener on control
 */
class WorkbenchContextListener implements IWindowListener, IPageListener, IPartListener {

    static final Log log = Log.getLog(WorkbenchContextListener.class);

    public static final String NAVIGATOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.navigator";
    public static final String SQL_EDITOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.editors.sql";

    private IContextActivation activationNavigator;
    private IContextActivation activationSQL;

    public WorkbenchContextListener() {
        // Register in already created windows and pages
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            window.addPageListener(this);
            for (IWorkbenchPage page : window.getPages()) {
                page.addPartListener(this);
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
        window.addPageListener(this);
        for (IWorkbenchPage page : window.getPages()) {
            page.addPartListener(this);
        }
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
                while (!PlatformUI.isWorkbenchRunning()) {
                    RuntimeUtils.pause(50);
                }
                PlatformUI.getWorkbench().addWindowListener(new WorkbenchContextListener());
                return Status.OK_STATUS;
            }
        }.schedule();
    }
}
