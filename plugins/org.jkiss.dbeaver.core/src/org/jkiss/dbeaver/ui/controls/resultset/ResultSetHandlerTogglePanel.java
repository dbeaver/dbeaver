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
import org.eclipse.swt.widgets.Shell;
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
    private static final String PARAM_PANEL_ID = "panelId";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_TOGGLE_PANEL:
                toggleResultsPanel(resultSet, HandlerUtil.getActiveShell(event), event.getParameter(PARAM_PANEL_ID));
                break;
        }
        return null;
    }

    private static void toggleResultsPanel(IResultSetController resultSet, Shell shell, String panelId) {
        boolean isVisible = false;
        IResultSetPanel visiblePanel = resultSet.getVisiblePanel();
        if (visiblePanel != null) {
            String activePanelId = ((ResultSetViewer) resultSet).getActivePanelId();
            isVisible = CommonUtils.equalObjects(activePanelId, panelId);
        }
        if (isVisible) {
            ((ResultSetViewer)resultSet).closeActivePanel();
        } else {
            resultSet.activatePanel(panelId, true, true);
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        // Put panel name in command label
    }
}
