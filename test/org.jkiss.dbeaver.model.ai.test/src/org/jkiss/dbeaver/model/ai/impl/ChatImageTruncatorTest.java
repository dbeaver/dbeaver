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
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

class ChatImageTruncatorTest {
    private static final AIImageAttachment IMAGE = AIImageAttachment.fromBytes("image.png", Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN1cAAAAASUVORK5CYII="));

    @Test
    void dropsOlderImageMessagesToFitTheBudget() throws DBException {
        ChatTruncator truncator = builder(100).imageTokenCounter(image -> 30).build();
        List<AIMessage> messages = IntStream.range(0, 100)
            .mapToObj(index -> AIMessage.userMessage("").withImages(List.of(IMAGE))).toList();
        List<AIMessage> truncated = truncator.tryTruncate(messages);
        Assertions.assertNotNull(truncated);
        Assertions.assertEquals(2, truncated.size());
        Assertions.assertSame(messages.getLast(), truncated.getLast());
        Assertions.assertEquals(2, truncated.stream().mapToInt(message -> message.getImages().size()).sum());
    }

    @Test
    void rejectsLatestImagesThatCannotFitInsteadOfDroppingThem() {
        ChatTruncator truncator = builder(100).imageTokenCounter(image -> 40).build();
        AIMessage message = AIMessage.userMessage("").withImages(List.of(IMAGE, IMAGE));
        DBException error = Assertions.assertThrows(DBException.class, () -> truncator.tryTruncate(List.of(message)));
        Assertions.assertTrue(error.getMessage().contains("context window"));
        Assertions.assertEquals(2, message.getImages().size());
    }

    @Test
    void truncatesTextAfterReservingImageAndSuffixTokens() throws DBException {
        ChatTruncator truncator = builder(100).imageTokenCounter(image -> 30).build();
        AIMessage latest = AIMessage.userMessage("describe ".repeat(100)).withImages(List.of(IMAGE));
        List<AIMessage> truncated = truncator.tryTruncate(List.of(AIMessage.systemMessage("system ".repeat(100)), latest));
        Assertions.assertNotNull(truncated);
        Assertions.assertEquals(List.of(IMAGE), truncated.getLast().getImages());
        int total = truncated.stream().mapToInt(message -> new DummyTokenCounter().count(message.getContent())
            + message.getImages().size() * 30).sum();
        Assertions.assertTrue(total <= 70, "Must leave 20 reply and 10 overhead tokens");
    }

    @Test
    void givesLatestImagesPriorityOverSystemText() throws DBException {
        ChatTruncator truncator = builder(100).imageTokenCounter(image -> 60).build();
        AIMessage latest = AIMessage.userMessage("").withImages(List.of(IMAGE));
        List<AIMessage> truncated = truncator.tryTruncate(List.of(AIMessage.systemMessage("system ".repeat(100)), latest));
        Assertions.assertNotNull(truncated);
        Assertions.assertSame(latest, truncated.getLast());
        Assertions.assertTrue(new DummyTokenCounter().count(truncated.getFirst().getContent()) <= 10);
    }

    @Test
    void countsImagesByDefaultAndLeavesAFittingRequestUnchanged() throws DBException {
        AIMessage latest = AIMessage.userMessage("").withImages(List.of(IMAGE));
        Assertions.assertThrows(DBException.class, () -> builder(100).build().tryTruncate(List.of(latest)));
        Assertions.assertNull(builder(5000).build().tryTruncate(List.of(latest)));
    }

    @NotNull
    private static ChatTruncator.Builder builder(int maxTokens) {
        return ChatTruncator.builder().maxTokens(maxTokens).reserveForSystem(20).reserveForReply(20)
            .reserveForOverhead(10).tokenCounter(new DummyTokenCounter());
    }
}
