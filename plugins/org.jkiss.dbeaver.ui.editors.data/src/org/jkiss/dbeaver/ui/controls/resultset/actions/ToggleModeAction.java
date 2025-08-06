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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

public class ToggleModeAction extends AbstractResultSetViewerAction {
    {
        setActionDefinitionId(ResultSetHandlerMain.CMD_TOGGLE_MODE);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.RS_DETAILS));
    }

    public ToggleModeAction(ResultSetViewer resultSetViewer) {
        super(resultSetViewer, ResultSetMessages.dialog_text_check_box_record, Action.AS_CHECK_BOX);
        String toolTip = ActionUtils.findCommandDescription(ResultSetHandlerMain.CMD_TOGGLE_MODE, getResultSetViewer().getSite(), false);
        if (!CommonUtils.isEmpty(toolTip)) {
            setToolTipText(toolTip);
        }
    }

    @Override
    public boolean isChecked() {
        return getResultSetViewer().isRecordMode();
    }

    @Override
    public void run() {
        getResultSetViewer().toggleMode();
    }
}
