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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.colors.ResetAllColorAction;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.SpreadsheetCommandHandler;

class FilterResetAllSettingsAction extends Action {
    private final ResultSetViewer viewer;

    FilterResetAllSettingsAction(ResultSetViewer resultSetViewer) {
        super(ResultSetMessages.controls_resultset_viewer_action_reset_all_settings);
        this.viewer = resultSetViewer;
    }

    @Override
    public void run() {
        ActionUtils.runCommand(ResultSetHandlerMain.CMD_FILTER_CLEAR_SETTING, viewer.getSite());
        if (viewer.getDataFilter().hasHiddenAttributes()) {
            ActionUtils.runCommand(SpreadsheetCommandHandler.CMD_SHOW_COLUMNS, viewer.getSite());
        }
        if (viewer.hasColorOverrides()) {
            new ResetAllColorAction(viewer).execute(false);
        }
        if (viewer.hasColumnTransformers()) {
            new FilterResetAllTransformersAction(viewer).execute(false);
        }
        if (viewer.getDataFilter().hasPinnedAttributes()) {
            new FilterResetAllPinsAction(viewer).execute(false);
        }
        viewer.refreshData(null);
    }
}
