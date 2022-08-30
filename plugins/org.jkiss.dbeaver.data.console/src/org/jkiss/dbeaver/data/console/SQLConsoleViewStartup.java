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

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class SQLConsoleViewStartup implements IStartup {

    private IWindowListener windowListener = new IWindowListener() {
        @Override
        public void windowOpened(IWorkbenchWindow window) {
            watchForWindow(window);
        }

        @Override
        public void windowDeactivated(IWorkbenchWindow window) {
        }

        @Override
        public void windowClosed(IWorkbenchWindow window) {
        }

        @Override
        public void windowActivated(IWorkbenchWindow window) {
        }
    };

    private IPageListener pageListener = new IPageListener() {
        @Override
        public void pageOpened(IWorkbenchPage page) {
            watchForPage(page);
        }

        @Override
        public void pageClosed(IWorkbenchPage page) {
        }

        @Override
        public void pageActivated(IWorkbenchPage page) {
        }
    };

    private final IPartListener partListener = new IPartListener() {
        @Override
        public void partOpened(IWorkbenchPart part) {
            watchForPart(part);
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
        }

        @Override
        public void partActivated(IWorkbenchPart part) {
        }
    };

    @Override
    public void earlyStartup() {
        IWorkbench workbench = PlatformUI.getWorkbench();

        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            watchForWindow(window);
        }
        workbench.addWindowListener(windowListener);
    }

    private void watchForWindow(IWorkbenchWindow window) {
        for (IWorkbenchPage page : window.getPages()) {
            watchForPage(page);
        }
        window.addPageListener(pageListener);
    }

    private void watchForPage(IWorkbenchPage page) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
            watchForPart(editorRef.getEditor(false));
        }

        page.addPartListener(partListener);
    }

    private void watchForPart(IWorkbenchPart part) {
        if (part instanceof SQLEditor) {
            ConsoleViewSwitchHandler.watchForEditor((SQLEditor) part);
        }
    }

}
