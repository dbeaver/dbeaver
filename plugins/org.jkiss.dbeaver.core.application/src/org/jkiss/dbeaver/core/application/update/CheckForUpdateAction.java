/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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