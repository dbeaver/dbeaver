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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class DisableSQLSyntaxParserHandler extends AbstractHandler implements IElementUpdater {
    
    public static final String COMMAND_ID = "org.jkiss.dbeaver.ui.editors.sql.disableSQLSyntaxParser";

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        final SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);

        if (editor != null && editor.getDocument() != null) {
            editor.setSQLSyntaxParserEnabled(!editor.isSQLSyntaxParserEnabled());
        }
        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @Nullable Map parameters) {
        IEditorPart editor = element.getServiceLocator().getService(IWorkbenchWindow.class).getActivePage().getActiveEditor();
        if (editor instanceof SQLEditor) {
            element.setChecked(!((SQLEditor) editor).isSQLSyntaxParserEnabled());
        }
    }
}
