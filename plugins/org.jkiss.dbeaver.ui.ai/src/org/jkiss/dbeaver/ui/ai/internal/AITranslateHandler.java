/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ai.legacy.AILegacyTranslator;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

public class AITranslateHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        Command command = ActionUtils.findCommand(SQLEditorCommands.CMD_AI_CHAT_TOGGLE);
        if (command != null && command.isEnabled() && command.getHandler() != null) {
            // PRO products, sorry
            ActionUtils.runCommand(SQLEditorCommands.CMD_AI_CHAT_TOGGLE, HandlerUtil.getActiveWorkbenchWindow(event));
            return null;
        }

        // CE legacy popup
        new AILegacyTranslator().performAiTranslation(event);
        return null;
    }

}
