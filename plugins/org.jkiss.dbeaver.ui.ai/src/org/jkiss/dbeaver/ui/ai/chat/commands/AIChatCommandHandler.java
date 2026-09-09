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
package org.jkiss.dbeaver.ui.ai.chat.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatController;
import org.jkiss.dbeaver.ui.ai.chat.AIChatView;
import org.jkiss.dbeaver.ui.ai.chat.controls.AIChatControl;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.nio.file.Path;
import java.util.List;

public class AIChatCommandHandler extends AbstractHandler {

    private static final Log log = Log.getLog(AIChatCommandHandler.class);

    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        IViewPart view = UIUtils.findView(HandlerUtil.getActiveWorkbenchWindow(event), IActionConstants.CHAT_VIEW_ID);
        if (view instanceof AIChatView chatView) {
            AIChatControl chat = chatView.getChat();
            if (!chat.isDisposed()) {
                handleChat(chat, event.getCommand().getId());
            }
        }
        return null;
    }

    private static void handleChat(@NotNull AIChatControl chat, @NotNull String commandId) {
        switch (commandId) {
            case AIChatController.CMD_SEND_PROMPT -> chat.sendPrompt();
            case AIChatController.CMD_ATTACH -> attachFiles(chat);
            case AIChatController.CMD_FOCUS_PROMPT -> chat.setFocusOnPrompt();
            case AIChatController.CMD_FOCUS_CHAT -> chat.setFocusOnMessages();
            case AIChatController.CMD_NEW_CONVERSATION -> chat.createNewConversation();
            case AIChatController.CMD_DELETE_CONVERSATION -> chat.deleteActiveConversationWithConfirmation();
            case AIChatController.CMD_OPEN_SETTINGS -> chat.openChatSettings();
            case AIChatController.CMD_OPEN_FILTERS -> chat.openScopeDropDown();
            default -> log.warn("Unexpected AI chat command: " + commandId);
        }
    }

    private static void attachFiles(@NotNull AIChatControl chat) {
        Path[] files = DialogUtils.openFileList(chat.getShell(), "Attach files", null);
        if (files != null && files.length > 0) {
            chat.attachFiles(List.of(files));
        }
    }
}
