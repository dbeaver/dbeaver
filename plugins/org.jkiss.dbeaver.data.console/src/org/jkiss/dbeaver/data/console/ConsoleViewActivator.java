/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.console;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.osgi.framework.BundleContext;
import org.jkiss.code.NotNull;

public class ConsoleViewActivator extends Plugin {

    public ConsoleViewActivator() {
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);
        watchForActiveEditor();
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        super.stop(context);
    }

    private static void watchForActiveEditor() {
        PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
            @Override
            public void windowOpened(IWorkbenchWindow window) {
                watchForWindow(window);
            }
            @Override
            public void windowDeactivated(IWorkbenchWindow window) { }
            @Override
            public void windowClosed(IWorkbenchWindow window) { }
            @Override
            public void windowActivated(IWorkbenchWindow window) { }
        });
        
        for (IWorkbenchWindow window: PlatformUI.getWorkbench().getWorkbenchWindows()) {
            watchForWindow(window);
        }
    }
    
    private static void watchForWindow(@NotNull IWorkbenchWindow window) {
        window.getPartService().addPartListener(new IPartListener2() {
            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
                if (partRef.getPart(false) instanceof SQLEditor) {
                    triggerRefreshCommandState();
                }
            }
        });
    }
    

    public static void triggerRefreshCommandState() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements("org.jkiss.dbeaver.ui.editors.sql.show.consoleView", null);
        }
    }
}