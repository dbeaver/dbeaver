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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class SQLEditorHandlerOpenDefaultSwitch extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(SQLEditorHandlerOpenDefaultSwitch.class);

    public SQLEditorHandlerOpenDefaultSwitch() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String editorCommand = event.getParameter("command");
        if (editorCommand == null) {
            log.error("No command for default SQL editor handler");
            return null;
        }
        DBWorkbench.getPlatform().getPreferenceStore().setValue(SQLPreferenceConstants.DEFAULT_SQL_EDITOR_OPEN_COMMAND, editorCommand);
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        String commandId = CommonUtils.toString(parameters.get("command"));
        if (CommonUtils.isEmpty(commandId)) {
            return;
        }
        String commandName = ActionUtils.findCommandName(commandId);
        if (CommonUtils.isEmpty(commandName)) {
            return;
        }
        element.setText(commandName);

        String defCommand = DBWorkbench.getPlatform().getPreferenceStore().getString(SQLPreferenceConstants.DEFAULT_SQL_EDITOR_OPEN_COMMAND);
        element.setChecked(CommonUtils.equalObjects(defCommand, commandId));
    }

}
