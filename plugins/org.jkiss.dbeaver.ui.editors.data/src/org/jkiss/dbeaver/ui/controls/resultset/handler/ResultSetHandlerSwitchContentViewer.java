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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer.ValueViewerPanel;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class ResultSetHandlerSwitchContentViewer extends AbstractHandler implements IElementUpdater {
    public static final String COMMAND_ID = "org.jkiss.dbeaver.core.resultset.grid.switchContentViewer";
    public static final String PARAM_STREAM_MANAGER_ID = "managerId";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final String managerId = event.getParameter(PARAM_STREAM_MANAGER_ID);
        final StreamValueManagerDescriptor manager = ValueManagerRegistry.getInstance().getStreamManager(managerId);
        final ContentPanelEditor editor = getEditor(HandlerUtil.getActivePart(event));

        if (manager != null && editor != null) {
            editor.setCurrentStreamManager(manager);
        }

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        final String managerId = (String) parameters.get(PARAM_STREAM_MANAGER_ID);
        final StreamValueManagerDescriptor manager = ValueManagerRegistry.getInstance().getStreamManager(managerId);
        final ContentPanelEditor editor = getEditor(element.getServiceLocator().getService(IWorkbenchPart.class));

        if (manager != null) {
            element.setText(manager.getLabel());
            element.setTooltip(manager.getDescription());
            element.setIcon(DBeaverIcons.getImageDescriptor(manager.getIcon()));
            element.setChecked(editor != null && editor.getCurrentStreamManager() == manager);
        }
    }

    @Nullable
    private static ContentPanelEditor getEditor(@NotNull IWorkbenchPart workbenchPart) {
        final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(workbenchPart);

        if (rsv != null && rsv.isPanelVisible(ValueViewerPanel.PANEL_ID)) {
            return GeneralUtils.adapt(rsv.getVisiblePanel(), ContentPanelEditor.class);
        }

        return null;
    }

    public static class StreamManagerIdParameterValues implements IParameterValues {
        @Override
        public Map<String, String> getParameterValues() {
            return ValueManagerRegistry.getInstance().getAllStreamManagers().stream()
                .collect(Collectors.toMap(
                    StreamValueManagerDescriptor::getLabel,
                    StreamValueManagerDescriptor::getId
                ));
        }
    }
}
