/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

public class SQLEditorHandlerToggleEditorLayout extends AbstractHandler {
    static protected final Log log = Log.getLog(SQLEditorHandlerToggleEditorLayout.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            String curPresentationName = DBWorkbench.getPlatform().getPreferenceStore().getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION);
            SQLEditor.ResultSetOrientation curOrientation = CommonUtils.valueOf(SQLEditor.ResultSetOrientation.class, curPresentationName, SQLEditor.ResultSetOrientation.HORIZONTAL);
            if (curOrientation == SQLEditor.ResultSetOrientation.HORIZONTAL) {
                curOrientation = SQLEditor.ResultSetOrientation.VERTICAL;
            } else {
                curOrientation = SQLEditor.ResultSetOrientation.HORIZONTAL;
            }
            DBWorkbench.getPlatform().getPreferenceStore().setValue(SQLPreferenceConstants.RESULT_SET_ORIENTATION, curOrientation.name());
            try {
                editor.getActivePreferenceStore().save();
            } catch (IOException e) {
                log.error("Error saving editor preferences", e);
            }
        }
        return null;
    }

}