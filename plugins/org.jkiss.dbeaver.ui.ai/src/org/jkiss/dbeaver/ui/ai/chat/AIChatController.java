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
package org.jkiss.dbeaver.ui.ai.chat;

import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIContextSettings;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * AI chat controller. Provides info about chat host environment.
 */
public interface AIChatController extends DBPContextProvider {

    String CMD_ATTACH = "com.dbeaver.ai.chat.attach";
    String CMD_SEND_PROMPT = "com.dbeaver.ai.chat.sendPrompt";
    String CMD_RECORD_AUDIO = "com.dbeaver.ai.chat.recordAudio";

    int FEATURE_CONTEXT_VIEW            = 1 << 1;
    int FEATURE_PROMPT_VIEW             = 1 << 2;
    int FEATURE_AUDIO_TRANSCRIPT        = 1 << 3;

    int getChatFeatures();

    void executeInEditor(@NotNull String text);

    void openInEditor(@NotNull String text, @Nullable AIChatConversation conversation);

    @Nullable
    DBCExecutionContext getExecutionContext();

    @NotNull
    IWorkbenchPartSite getSite();

    @Nullable
    AIContextSettings getContextSettings();

}
