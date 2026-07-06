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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatView;
import org.jkiss.dbeaver.ui.ai.chat.controls.AIChatControl;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.io.File;
import java.util.Arrays;

public class AIChatAttachHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IViewPart view = UIUtils.findView(HandlerUtil.getActiveWorkbenchWindow(event), IActionConstants.CHAT_VIEW_ID);
        if (view instanceof AIChatView chatView) {
            AIChatControl chat = chatView.getChat();
            if (!chat.isDisposed()) {
                File[] files = DialogUtils.openFileList(chat.getShell(), "Attach files", null);
                if (files != null && files.length > 0) {
                    chat.attachFiles(Arrays.stream(files).map(File::toPath).toList());
                }
            }
        }
        return null;
    }
}
