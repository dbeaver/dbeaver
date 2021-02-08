/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class NavigatorHandlerToggleView extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final String viewId = event.getParameter("viewId");
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        final IViewPart view = UIUtils.findView(workbenchWindow, viewId);
        if (view != null && workbenchWindow.getActivePage().isPartVisible(view)) {
            workbenchWindow.getActivePage().hideView(view);
        } else {
            try {
                if (view != null) {
                    workbenchWindow.getActivePage().bringToTop(view);
                } else {
                    workbenchWindow.getActivePage().showView(viewId);
                }
            } catch (PartInitException e) {
                DBWorkbench.getPlatformUI().showError("Toggle view", "Cannot open view " + viewId, e);
            }
        }

        ActionUtils.fireCommandRefresh("org.jkiss.dbeaver.core.view.toggle");

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        final String viewId = (String) parameters.get("viewId");

        IViewDescriptor viewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(viewId);
        if (viewDescriptor != null) {
            element.setText(viewDescriptor.getLabel());
            element.setIcon(viewDescriptor.getImageDescriptor());
            if (!CommonUtils.isEmpty(viewDescriptor.getDescription())) {
                element.setTooltip(viewDescriptor.getDescription());
            }

            IViewPart view = null;
            IViewReference viewReference = UIUtils.getActiveWorkbenchWindow().getActivePage().findViewReference(viewId);
            if (viewReference != null) {
                view = viewReference.getView(false);
            }
            element.setChecked(view != null);
        }
    }

    public static class ViewValues implements IParameterValues {

        @Override
        public Map<String, String> getParameterValues() {
            final Map<String, String> values = new HashMap<>();
            for (IViewDescriptor view : PlatformUI.getWorkbench().getViewRegistry().getViews()) {
                values.put(view.getLabel(), view.getId());
            }
            return values;
        }

    }

}