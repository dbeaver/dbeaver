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
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.WorkbenchContextListener;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

public class SQLConsoleViewStartup implements IStartup {

    @Override
    public void earlyStartup() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        
        WorkbenchContextListener.addOnNewSqlEditorListener(editor -> watchForPart(editor));

        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorRef : page.getEditorReferences()) {
                    watchForPart(editorRef.getEditor(false));
                }
            }
        }
    }

    private void watchForPart(IWorkbenchPart part) {
        if (part instanceof SQLEditor) {
            ConsoleViewSwitchHandler.watchForEditor((SQLEditor) part);
        }
    }
}
