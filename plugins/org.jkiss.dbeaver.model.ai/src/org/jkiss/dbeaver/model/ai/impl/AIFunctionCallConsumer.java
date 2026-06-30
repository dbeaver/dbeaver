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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AIFunctionCallConsumer implements Consumer<List<AIFunctionCall>> {
    private static final Log log = Log.getLog(AIFunctionCallConsumer.class);
    private final AIChatSession chatSession;
    private final AIChatResponseConsumer chatListener;
    private final AIChatConversation conversation;
    private final DBRProgressMonitor monitor;

    public AIFunctionCallConsumer(
        @NotNull AIChatSession chatSession,
        @NotNull AIChatResponseConsumer chatListener,
        @NotNull AIChatConversation conversation,
        @NotNull DBRProgressMonitor monitor
    ) {
        this.chatSession = chatSession;
        this.chatListener = chatListener;
        this.conversation = conversation;
        this.monitor = monitor;
    }

    @Override
    public void accept(@NotNull List<AIFunctionCall> aiFunctionCalls) {
        // we need to extract valid functions and add warning for invalid functions
        List<AIFunctionCall> validFunctionCalls = new ArrayList<>();
        List<AIFunctionCall> invalidFunctionCalls = new ArrayList<>();
        for (AIFunctionCall fc : aiFunctionCalls) {
            AIFunctionDescriptor function = fc.getOrResolveFunction(chatSession.getAssistant().getToolboxManager());
            if (function == null) {
                invalidFunctionCalls.add(fc);
                continue;
            }
            boolean hasInvalidParameters = false;
            for (AIFunctionParameter parameter : function.getParameters()) {
                if (parameter.isRequired()) {
                    String strValue = CommonUtils.toString(fc.getArguments().get(parameter.getName()));
                    if (CommonUtils.isEmpty(strValue)) {
                        hasInvalidParameters = true;
                        break;
                    }
                }
            }
            if (hasInvalidParameters) {
                invalidFunctionCalls.add(fc);
            } else {
                validFunctionCalls.add(fc);
            }
        }
        if (!invalidFunctionCalls.isEmpty()) {
            chatListener.warning("Invalid function call(s): " + invalidFunctionCalls.stream()
                .map(AIFunctionCall::getFunctionDisplayName)
                .collect(Collectors.joining(", ")));
        }
        // decline invalid function calls
        chatSession.declineFunctionCalls(conversation, invalidFunctionCalls);
        if (validFunctionCalls.isEmpty()) {
            // just continue streaming without confirmation
            try {
                chatSession.processAICompletion(monitor, conversation, chatListener, null, null);
            } catch (DBException e) {
                log.error(e);
            }
            return;
        }
        AIChatMessage confirmMessage = conversation.addMessage(
            AIMessage.functionConfirmation(validFunctionCalls)
        );
        chatSession.notifyMessageAdd(conversation, confirmMessage);
    }
}
