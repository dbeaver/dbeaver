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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptGenerateSql;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AIImageHistoryTest {
    @Test
    public void loadsOnlyTheSelectedHistoryOnceAndKeepsMessageIds() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation first = conversation();
        AIChatConversation second = conversation();
        AIImageAttachment image = image();
        Mockito.when(storage.findConversationImages("session", first.getId())).thenReturn(Map.of(7, List.of(image)));
        session.loadConversationImages(new VoidProgressMonitor(), first);
        session.loadConversationImages(new VoidProgressMonitor(), first);
        Assertions.assertTrue(first.areImagesLoaded());
        Assertions.assertEquals(7, first.getMessages().getFirst().id());
        Assertions.assertEquals("Stored caption", first.getCaption());
        Assertions.assertEquals(List.of(image), first.getMessages().getFirst().message().getImages());
        Assertions.assertFalse(second.areImagesLoaded());
        Mockito.verify(storage, Mockito.times(1)).findConversationImages("session", first.getId());
        Mockito.verifyNoMoreInteractions(storage);
    }

    @Test
    public void failedLoadsRemainRetryableAndDoNotPartiallyReplaceHistory() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation conversation = conversation();
        Mockito.when(storage.findConversationImages("session", conversation.getId()))
            .thenThrow(new DBException("Unavailable"))
            .thenReturn(Map.of(7, List.of(image())));
        Assertions.assertThrows(DBException.class, () -> session.loadConversationImages(new VoidProgressMonitor(), conversation));
        Assertions.assertFalse(conversation.areImagesLoaded());
        Assertions.assertTrue(conversation.getMessages().getFirst().message().getImages().isEmpty());
        session.loadConversationImages(new VoidProgressMonitor(), conversation);
        Assertions.assertTrue(conversation.areImagesLoaded());
        Assertions.assertEquals(1, conversation.getMessages().getFirst().message().getImages().size());
    }

    @Test
    public void rejectsInvalidStoredImagesBeforeApplyingAnyOfThem() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation conversation = conversation();
        conversation.addMessage(AIMessage.assistantMessage("Reply", null));
        int replyId = conversation.getMessages().getLast().id();
        Mockito.when(storage.findConversationImages("session", conversation.getId()))
            .thenReturn(Map.of(7, List.of(image()), replyId, List.of(image())));
        Assertions.assertThrows(DBException.class, () -> session.loadConversationImages(new VoidProgressMonitor(), conversation));
        Assertions.assertFalse(conversation.areImagesLoaded());
        Assertions.assertTrue(conversation.getMessages().stream().allMatch(message -> message.message().getImages().isEmpty()));
    }

    @NotNull
    private static AIChatSession session(@NotNull AIChatStorage storage) {
        return new AIChatSession(Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> "session", Mockito.mock(UserTokenQuotaService.class));
    }

    @NotNull
    private static AIChatConversation conversation() {
        AIChatConversation conversation = new AIChatConversation(UUID.randomUUID(), "Stored caption", new AIPromptGenerateSql(),
            List.of(new AIChatMessage(7, AIMessage.userMessage("Describe"))), null, 8, null);
        conversation.setImagesLoaded(false);
        return conversation;
    }

    @NotNull
    private static AIImageAttachment image() {
        return AIImageAttachment.fromBytes("test.png", Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN1cAAAAASUVORK5CYII="));
    }
}
