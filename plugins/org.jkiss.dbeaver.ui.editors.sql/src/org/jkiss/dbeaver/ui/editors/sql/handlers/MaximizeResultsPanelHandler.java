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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class MaximizeResultsPanelHandler extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            editor.toggleEditorMaximize();
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }
        SQLEditor editor = RuntimeUtils.getObjectAdapter(activeEditor, SQLEditor.class);
        if (editor != null) {
            if (editor.hasMaximizedControl()) {
                element.setText(SQLEditorMessages.action_menu_sqleditor_restoreResultsPanel);
            } else {
                element.setText(SQLEditorMessages.action_menu_sqleditor_maximizeResultsPanel);
            }

        }
    }

}