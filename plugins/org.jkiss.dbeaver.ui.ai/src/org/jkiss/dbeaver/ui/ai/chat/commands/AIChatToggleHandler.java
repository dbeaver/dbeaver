/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.chat.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

import java.util.Map;

public class AIChatToggleHandler extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IViewPart view = UIUtils.findView(window, IActionConstants.CHAT_VIEW_ID);

        try {
            if (view != null && page.isPartVisible(view)) {
                page.hideView(view);
            } else if (view != null) {
                page.bringToTop(view);
            } else {
                page.showView(IActionConstants.CHAT_VIEW_ID);
            }

            ActionUtils.fireCommandRefresh(SQLEditorCommands.CMD_AI_CHAT_TOGGLE);
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Toggle AI chat", "Cannot open AI chat", e);
        }

        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        IViewPart view = UIUtils.findView(window, IActionConstants.CHAT_VIEW_ID);
        element.setChecked(view != null);
    }
}
