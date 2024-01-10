/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

import java.util.Map;


public class SQLEditorHandlerToggleOutlineView extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            IWorkbenchWindow activeWindow = HandlerUtil.getActiveWorkbenchWindow(event);
            if (activeWindow != null) {
                IWorkbenchPage activePage = activeWindow.getActivePage();
                if (activePage != null) {
                    IViewReference viewReference = activePage.findViewReference(IPageLayout.ID_OUTLINE);
                    if (viewReference != null && viewReference.getView(false) != null) {
                        activePage.hideView(viewReference);
                    } else {
                        IViewPart outlineView = activePage.showView(IPageLayout.ID_OUTLINE);
                        if (outlineView != null) {
                            outlineView.setFocus();
                        }
                    }
                    refreshCommandState(activeWindow);
                }
            }
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Toggle outline", "Can't open outline view", e);
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        IWorkbenchPage page = element.getServiceLocator().getService(IWorkbenchPage.class);
        IViewReference viewReference = page.findViewReference(IPageLayout.ID_OUTLINE);
        boolean isVisible = viewReference != null && viewReference.getView(false) != null;
        element.setChecked(isVisible);
    }

    public static void refreshCommandState(@NotNull IServiceLocator serviceLocator) {
        ICommandService commandService = serviceLocator.getService(ICommandService.class);
        commandService.refreshElements(SQLEditorCommands.CMD_SQL_SHOW_OUTLINE, null);  
    }
}