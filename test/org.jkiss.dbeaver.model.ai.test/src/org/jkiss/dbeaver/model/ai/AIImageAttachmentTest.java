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

import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotClientChat;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotClientResponses;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotMessage;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessageFactory;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy.ChatMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy.ChatMessageContent;
import org.jkiss.dbeaver.model.ai.impl.ChatTruncator;
import org.jkiss.dbeaver.model.ai.impl.DummyTokenCounter;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptGenerateSql;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatHistoryMapper;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatMessage;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AIImageAttachmentTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN1cAAAAASUVORK5CYII=";

    @Test
    public void detectsImageFormatFromBytes() {
        AIImageAttachment image = image();
        Assertions.assertEquals("image/png", image.mediaType());
        Assertions.assertArrayEquals(Base64.getDecoder().decode(PNG), image.getBytes());
        Assertions.assertEquals("data:image/png;base64," + PNG, image.toDataUrl());
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> AIImageAttachment.fromBytes("fake.png", "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> AIImageAttachment.fromBytes("empty.png", new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> AIImageAttachment.fromBytes("large.png", new byte[AIImageAttachment.MAX_IMAGE_BYTES + 1]));
    }

    @Test
    public void retainsImagesWhenTruncatingText() {
        List<AIImageAttachment> attachments = new ArrayList<>(List.of(image()));
        AIMessage message = AIMessage.userMessage("Describe this image").withImages(attachments);
        attachments.clear();
        Assertions.assertEquals(1, message.getImages().size());
        Assertions.assertEquals(message.getImages(), message.withContent("Describe").getImages());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> message.getImages().clear());
    }

    @Test
    public void retainsImageOnlyPromptsInTruncatedHistory() {
        AIMessage message = AIMessage.userMessage("").withImages(List.of(image()));
        ChatTruncator truncator = ChatTruncator.builder().maxTokens(100).reserveForSystem(20)
            .reserveForReply(20).reserveForOverhead(10).tokenCounter(new DummyTokenCounter()).build();
        List<AIMessage> truncated = truncator.tryTruncate(List.of(
            AIMessage.systemMessage("System context ".repeat(100)), message));
        Assertions.assertNotNull(truncated);
        Assertions.assertTrue(truncated.stream().anyMatch(item -> item.getImages().equals(message.getImages())));
    }

    @Test
    public void namesImageOnlyConversationsAfterTheFirstAttachment() {
        for (String content : List.of("", " \t\n")) {
            AIChatConversation conversation = new AIChatConversation(
                UUID.randomUUID(), "New conversation", new AIPromptGenerateSql(), List.of(), null, 0, null);
            AIImageAttachment firstImage = image();
            AIImageAttachment secondImage = AIImageAttachment.fromBytes("second.png", firstImage.getBytes());
            AIMessage message = AIMessage.userMessage(content).withImages(List.of(firstImage, secondImage));
            conversation.addMessage(message);
            Assertions.assertEquals(firstImage.name(), conversation.getCaption());
            Assertions.assertEquals(content, message.getContent());
            conversation.addMessage(AIMessage.userMessage("Follow-up question"));
            Assertions.assertEquals(firstImage.name(), conversation.getCaption());
        }
    }

    @Test
    public void prefersPromptTextOverImageNameForConversationCaption() {
        AIChatConversation conversation = new AIChatConversation(
            UUID.randomUUID(), "New conversation", new AIPromptGenerateSql(), List.of(), null, 0, null);
        conversation.addMessage(AIMessage.userMessage("Describe this image").withImages(List.of(image())));
        Assertions.assertEquals("Describe this image", conversation.getCaption());
    }

    @Test
    public void serializesBothOpenAiApiFormats() {
        AIMessage message = AIMessage.userMessage("Describe").withImages(List.of(image()));
        var responses = OAIMessageFactory.fromAIMessage(message);
        var content = JSONUtils.GSON.toJsonTree(responses).getAsJsonObject().getAsJsonArray("content");
        Assertions.assertEquals("input_text", content.get(0).getAsJsonObject().get("type").getAsString());
        Assertions.assertEquals("input_image", content.get(1).getAsJsonObject().get("type").getAsString());
        Assertions.assertEquals(image().toDataUrl(), content.get(1).getAsJsonObject().get("image_url").getAsString());
        Assertions.assertEquals("Describe", responses.getFullText());
        var chat = new ChatMessage("user", ChatMessageContent.from(responses), null);
        var chatContent = JSONUtils.GSON.toJsonTree(chat).getAsJsonObject().getAsJsonArray("content");
        Assertions.assertEquals("image_url", chatContent.get(1).getAsJsonObject().get("type").getAsString());
        Assertions.assertEquals(image().toDataUrl(), chatContent.get(1).getAsJsonObject()
            .getAsJsonObject("image_url").get("url").getAsString());
        Assertions.assertEquals("text only", ChatMessageContent.from(AIMessage.userMessage("text only")));
    }

    @Test
    public void serializesCopilotImagesWithoutChangingTextMessages() {
        var message = CopilotMessage.from(AIMessage.userMessage("Describe").withImages(List.of(image()))).getFirst();
        var content = JSONUtils.GSON.toJsonTree(message).getAsJsonObject().getAsJsonArray("content");
        Assertions.assertEquals(image().toDataUrl(), content.get(1).getAsJsonObject()
            .getAsJsonObject("image_url").get("url").getAsString());
        Assertions.assertEquals("text only", CopilotMessage.from(AIMessage.userMessage("text only")).getFirst().content());
    }

    @Test
    public void addsCopilotVisionHeaderForBothApisOnlyWhenImagesArePresent() throws Exception {
        var session = new CopilotSessionToken("test-token", null);
        var chatMethod = CopilotClientChat.class.getDeclaredMethod(
            "createCompletionRequest", CopilotChatRequest.class, CopilotSessionToken.class);
        chatMethod.setAccessible(true);
        var responsesConstructor = CopilotClientResponses.class.getDeclaredConstructor(String.class);
        responsesConstructor.setAccessible(true);
        var responsesClient = responsesConstructor.newInstance("https://github.com");
        var responsesMethod = CopilotClientResponses.class.getDeclaredMethod(
            "createCompletionRequest", OAIResponsesRequest.class, CopilotSessionToken.class);
        responsesMethod.setAccessible(true);
        var chatClient = new CopilotClientChat("https://github.com");
        try {
            for (boolean withImages : List.of(false, true)) {
                AIMessage message = AIMessage.userMessage("Describe");
                if (withImages) {
                    message = message.withImages(List.of(image()));
                }
                var chat = CopilotChatRequest.builder().withModel("test-model")
                    .withMessages(CopilotMessage.from(message)).withTools(List.of()).build();
                var responses = new OAIResponsesRequest();
                responses.model = "test-model";
                responses.input = List.of(OAIMessageFactory.fromAIMessage(message));
                for (HttpRequest request : List.of(
                    (HttpRequest) chatMethod.invoke(chatClient, chat, session),
                    (HttpRequest) responsesMethod.invoke(responsesClient, responses, session))) {
                    Assertions.assertEquals(withImages ? List.of("true") : List.of(),
                        request.headers().allValues("Copilot-Vision-Request"));
                }
            }
        } finally {
            chatClient.close();
            responsesClient.close();
        }
    }

    @Test
    public void restoresImagesFromQueryManagerHistory() {
        AIMessage message = AIMessage.userMessage("Describe").withImages(List.of(image()));
        QMAIChatMessage stored = QMAIChatHistoryMapper.toQMAIChatMessages(List.of(new AIChatMessage(1, message))).getFirst();
        QMAIChatMessage restored = JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(stored), QMAIChatMessage.class);
        AIMessage restoredMessage = QMAIChatHistoryMapper.toAIMessages(unusedAssistant(), List.of(restored))
            .getFirst().message();
        Assertions.assertEquals(message.getImages(), restoredMessage.getImages());
        Assertions.assertEquals(message.getContent(), restoredMessage.getContent());
        QMAIChatMessage legacy = new QMAIChatMessage(stored.id(), stored.content(), stored.displayMessage(), stored.role(),
            null, null, stored.timestamp(), false, null);
        Assertions.assertTrue(QMAIChatHistoryMapper.toAIMessages(unusedAssistant(), List.of(legacy))
            .getFirst().message().getImages().isEmpty());
    }

    @Test
    public void enablesImageInputByDefaultAndAllowsExplicitOptOut() {
        Assertions.assertTrue(new AIModel("model", 100, Set.of(AIModelFeature.CHAT)).imageInputSupported());
        Assertions.assertFalse(new AIModel("text-model", 100, Set.of(AIModelFeature.CHAT), 0, false).imageInputSupported());
    }

    private static AIAssistant unusedAssistant() {
        return (AIAssistant) java.lang.reflect.Proxy.newProxyInstance(AIAssistant.class.getClassLoader(),
            new Class<?>[]{AIAssistant.class}, (proxy, method, arguments) -> {
                throw new AssertionError("Unexpected assistant call: " + method.getName());
            });
    }

    private static AIImageAttachment image() {
        return AIImageAttachment.fromBytes("test.png", Base64.getDecoder().decode(PNG));
    }
}
