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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class SQLEditorHandlerPlaceExtraPanels extends AbstractHandler implements IElementUpdater {
    public static final String COMMAND_ID = "org.jkiss.dbeaver.ui.editors.sql.place.extraPanels";
    public static final String LOCATION_PARAMETER = "location";

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            editor.setExtraPanelsLocation(event.getParameter(LOCATION_PARAMETER));
        }
        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
        IWorkbenchWindow window = element.getServiceLocator().getService(IWorkbenchWindow.class);
        IEditorPart activeEditor = window == null || window.getActivePage() == null ? null : window.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }
        SQLEditor editor = RuntimeUtils.getObjectAdapter(activeEditor, SQLEditor.class);
        String location = (String) parameters.get(LOCATION_PARAMETER);
        if (editor != null && location != null) {
            element.setChecked(editor.isExtraPanelsLocation(location));
            element.setIcon(getPlacementIcon(location));
        }
    }

    @NotNull
    public static ImageDescriptor getPlacementIcon(@NotNull String location) {
        boolean vertical = SQLEditor.ResultSetOrientation.VERTICAL.name().equals(
            DBWorkbench.getPlatform().getPreferenceStore().getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION));
        boolean withResults = SQLPreferenceConstants.LOCATION_RESULTS.equals(location);
        return DBeaverIcons.getImageDescriptor(vertical
            ? (withResults ? UIIcon.SQL_AUX_TABS_RIGHT_WITH_RESULTS : UIIcon.SQL_AUX_TABS_RIGHT_BESIDE_CODE)
            : (withResults ? UIIcon.SQL_AUX_TABS_BOTTOM_WITH_RESULTS : UIIcon.SQL_AUX_TABS_BOTTOM_BESIDE_CODE));
    }

    public static void refreshCommandState(@NotNull IServiceLocator serviceLocator) {
        ICommandService commandService = serviceLocator.getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(COMMAND_ID, null);
        }
    }
}
