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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;

import java.util.Map;

public class DisableSQLSyntaxParserHandler extends AbstractHandler implements IElementUpdater {
    
    public static final String COMMAND_ID = "org.jkiss.dbeaver.ui.editors.sql.disableSQLSyntaxParser";

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        final IEditorPart editor = HandlerUtil.getActiveEditor(event);

        if (editor instanceof SQLEditor) {
            IEditorInput editorInput = editor.getEditorInput();
            SQLEditorUtils.setSQLSyntaxParserEnabled(editorInput, !SQLEditorUtils.isSQLSyntaxParserEnabled(editorInput));
        }
        return null;
    }
    
    @Override
    public boolean isEnabled() {
        IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
        if (activePage != null) {
            IEditorPart editor = activePage.getActiveEditor();
            return editor != null && !SQLEditorBase.isBigScript(editor.getEditorInput());
        } else {
            return false;
        }
    }

    @Override
    public void updateElement(@NotNull UIElement element, @Nullable Map parameters) {
        IWorkbenchPage activePage = element.getServiceLocator().getService(IWorkbenchWindow.class).getActivePage();
        if (activePage != null) {
            IEditorPart editor = activePage.getActiveEditor();
            if (editor instanceof SQLEditor) {
                element.setChecked(!SQLEditorUtils.isSQLSyntaxParserApplied(((SQLEditor) editor).getEditorInput()));
            }
        }
    }
}
