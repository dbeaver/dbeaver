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

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.jkiss.code.NotNull;

public class ConsoleViewActivator implements IStartup {
    
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.data.console";
    
    public ConsoleViewActivator() {
    }
    
    @Override
    public void earlyStartup() {
        Bundle bundle = Platform.getBundle(BUNDLE_NAME);
        try {
            bundle.start();
        } catch (BundleException e) {
            
            e.printStackTrace();
        }
        watchForActiveEditor();
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
            for (IWorkbenchPage page: window.getPages()) {
                IWorkbenchPart part = page.getActivePart();
                if (part instanceof SQLEditor) {
                    watchForEditor((SQLEditor)part);
                }
            }
        }
        UIUtils.asyncExec(() -> triggerRefreshCommandState());
    }
    
    private static void watchForWindow(@NotNull IWorkbenchWindow window) {
        window.getPartService().addPartListener(new IPartListener2() {
            @Override
            public void partOpened(IWorkbenchPartReference partRef) {
                IWorkbenchPart part = partRef.getPart(false);
                if (part instanceof SQLEditor) {
                    watchForEditor((SQLEditor)part);
                }
            }
            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
                IWorkbenchPart part = partRef.getPart(false);
                if (part instanceof SQLEditor) {
                    triggerRefreshCommandState();
                }
            }
        });
    }
    
    private static void watchForEditor(SQLEditor editor) {
        if (!ConsoleViewSwitchHandler.isLogViewerEnabledSetForEditor(editor)) {
            ConsoleViewSwitchHandler.subscribeEditorData(editor);
        }
    }

    public static void triggerRefreshCommandState() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements("org.jkiss.dbeaver.ui.editors.sql.show.consoleView", null);
        }
    }
}