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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetHandlerTogglePanel extends AbstractHandler implements IElementUpdater {

    public static final String CMD_TOGGLE_PANEL = "org.jkiss.dbeaver.core.resultset.grid.togglePanel";
    public static final String PARAM_PANEL_ID = "panelId";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        String panelId = event.getParameter(PARAM_PANEL_ID);
        if (panelId == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_TOGGLE_PANEL:
                toggleResultsPanel(resultSet, panelId);
                break;
        }
        return null;
    }

    private static void toggleResultsPanel(IResultSetController resultSet, String panelId) {
        boolean isVisible = ((ResultSetViewer)resultSet).isPanelVisible(panelId);

        if (isVisible) {
            ((ResultSetViewer)resultSet).closePanel(panelId);
        } else {
            resultSet.activatePanel(panelId, true, true);
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        // Put panel name in command label
        String panelId = (String) parameters.get(PARAM_PANEL_ID);
        if (panelId != null) {
            ResultSetPanelDescriptor panel = ResultSetPresentationRegistry.getInstance().getPanel(panelId);
            if (panel != null) {
                element.setText(panel.getLabel());
                if (!CommonUtils.isEmpty(panel.getDescription())) {
                    element.setTooltip(panel.getDescription());
                }
            }
            IWorkbenchPart workbenchPart = element.getServiceLocator().getService(IWorkbenchPart.class);
            if (workbenchPart != null) {
                IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(workbenchPart);
                if (resultSet != null) {
                    element.setChecked(((ResultSetViewer)resultSet).isPanelVisible(panelId));
                }
            }
        }
    }
}
