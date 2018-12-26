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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class NavigatorHandlerToggleView extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final String viewId = event.getParameter("viewId");
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IViewPart view = UIUtils.findView(workbenchWindow, viewId);
        if (view != null) {
            workbenchWindow.getActivePage().hideView(view);
        } else {
            try {
                workbenchWindow.getActivePage().showView(viewId);
            } catch (PartInitException e) {
                DBWorkbench.getPlatformUI().showError("Toggle view", "Cannot open view " + viewId, e);
            }
        }
        return null;
    }


    @Override
    public void updateElement(UIElement element, Map parameters) {
        final String viewId = (String) parameters.get("viewId");
        final IViewPart view = UIUtils.getActiveWorkbenchWindow().getActivePage().findView(viewId);
        element.setChecked(view != null);
    }
}