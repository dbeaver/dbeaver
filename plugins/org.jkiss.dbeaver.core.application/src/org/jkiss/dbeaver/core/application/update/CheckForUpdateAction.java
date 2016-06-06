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
package org.jkiss.dbeaver.core.application.update;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.jkiss.dbeaver.core.CoreMessages;


public class CheckForUpdateAction extends Action {

    public static final String P2_PLUGIN_ID = "org.eclipse.equinox.p2.ui.sdk";
    public static final String P2_UPDATE_COMMAND = "org.eclipse.equinox.p2.ui.sdk.update";

    private static IHandlerActivation p2UpdateHandlerActivation;

    public CheckForUpdateAction() {
        super(CoreMessages.actions_menu_check_update);
        setId("org.jkiss.dbeaver.action.checkForUpdate");
    }

    @Override
    public void run() {
        new DBeaverVersionChecker(true).schedule();
    }

    public static void deactivateStandardHandler(IWorkbenchWindow window) {
        if (p2UpdateHandlerActivation != null) {
            return;
        }
        IHandlerService srv = window.getService(IHandlerService.class);
        p2UpdateHandlerActivation = srv.activateHandler(CheckForUpdateAction.P2_UPDATE_COMMAND, new AbstractHandler() {
            @Override
            public Object execute(ExecutionEvent event) throws ExecutionException {
                new CheckForUpdateAction().run();
                return null;
            }
        });
    }

    public static void activateStandardHandler(IWorkbenchWindow window) {
        if (p2UpdateHandlerActivation == null) {
            return;
        }
        IHandlerService srv = window.getService(IHandlerService.class);
        srv.deactivateHandler(p2UpdateHandlerActivation);
        p2UpdateHandlerActivation = null;
    }
}