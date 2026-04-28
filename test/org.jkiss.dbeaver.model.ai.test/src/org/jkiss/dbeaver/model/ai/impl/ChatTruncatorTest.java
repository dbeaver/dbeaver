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

import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ChatTruncatorTest {

    /**
     * Counts every whitespace-separated word as a single token.
     */
    private static final TokenCounter WORD_COUNTER = new TokenCounter() {
        @Override
        public int count(String message) {
            if (message == null || message.isBlank()) {
                return 0;
            }
            return message.trim().split("\\s+").length;
        }

        @Override
        public String truncateToTokenLimit(String message, int tokenLimit) {
            if (message == null || message.isBlank()) {
                return message;
            }
            String[] words = message.trim().split("\\s+");
            if (words.length <= tokenLimit) {
                return message;
            }
            return String.join(" ", List.of(words).subList(0, tokenLimit));
        }
    };

    private ChatTruncator truncator;

    @Before
    public void setUp() {
        truncator = ChatTruncator.builder()
            .maxTokens(60)
            .reserveForSystem(15)
            .reserveForReply(10)
            .reserveForOverhead(5)
            .tokenCounter(WORD_COUNTER)
            .build();
    }

    @Test
    public void emptyInput() {
        assertNull("Expecting null for empty input", truncator.tryTruncate(List.of()));
    }


    @Test
    public void filtersBlank() {
        assertNull(truncator.tryTruncate(List.of(msg(AIMessageType.USER, "   "))));
        assertNull(truncator.tryTruncate(List.of(msg(AIMessageType.USER, words(3)), msg(AIMessageType.USER, "   "))));
    }

    @Test
    public void systemMerge() {
        AIMessage sys1 = msg(AIMessageType.SYSTEM, words(2));
        AIMessage sys2 = msg(AIMessageType.SYSTEM, words(3));
        AIMessage userOld = msg(AIMessageType.USER, words(1));
        AIMessage userNew = msg(AIMessageType.USER, words(39));

        List<AIMessage> out = truncator.tryTruncate(List.of(sys1, userOld, userNew, sys2));

        assertNotNull("Truncation must have occurred", out);
        assertEquals("First must be SYSTEM", AIMessageType.SYSTEM, out.getFirst().getRole());
        assertTrue("Merged system must contain sys1 content", out.getFirst().getContent().contains(sys1.getContent()));
        assertTrue("Merged system must contain sys2 content", out.getFirst().getContent().contains(sys2.getContent()));
    }

    @Test
    public void greedySelection() {
        AIMessage m1 = msg(AIMessageType.USER, words(10));
        AIMessage m2 = msg(AIMessageType.ASSISTANT, words(20));
        AIMessage m3 = msg(AIMessageType.USER, words(30));

        List<AIMessage> out = truncator.tryTruncate(List.of(m1, m2, m3));

        assertNotNull("Truncation must have occurred", out);
        assertEquals("Expected m2-truncated + m3", 2, out.size());
        assertNotEquals("m1 should have been truncated out", m1, out.getFirst());
        assertSame("Newest message should be last", m3, out.getLast());
        int tokens = out.stream().mapToInt(m -> WORD_COUNTER.count(m.getContent())).sum();
        assertTrue("Total tokens must fit within maxTokens window", tokens <= 60);
    }

    @Test
    public void oversizedSystemTrim() {
        AIMessage bigSystem = msg(AIMessageType.SYSTEM, words(40));
        AIMessage user = msg(AIMessageType.USER, words(30));

        List<AIMessage> out = truncator.tryTruncate(List.of(bigSystem, user));

        assertNotNull("Truncation must have occurred", out);
        assertTrue("System content must carry the truncation marker", out.getFirst().getContent().contains("[...truncated"));

        String sysContent = out.getFirst().getContent();
        int suffixTokens = WORD_COUNTER.count("\n[...truncated, don't try again]");
        int sysTokens = WORD_COUNTER.count(sysContent);
        assertTrue("System content must be within reserve + suffix overhead", sysTokens <= 15 + suffixTokens);

        int totalTokens = out.stream().mapToInt(m -> WORD_COUNTER.count(m.getContent())).sum();
        assertTrue("Total tokens must fit within maxTokens window", totalTokens <= 60);
    }

    @Test
    public void noTruncationReturnsNull() {
        List<AIMessage> out = truncator.tryTruncate(List.of(
            msg(AIMessageType.SYSTEM, words(5)),
            msg(AIMessageType.USER, words(10)),
            msg(AIMessageType.ASSISTANT, words(5)),
            msg(AIMessageType.USER, words(5))
        ));
        assertNull("Everything fits — no truncation should occur", out);
    }

    @Test
    public void pinnedUserTruncatedWhenTooLarge() {
        AIMessage bigUser = msg(AIMessageType.USER, words(50));

        List<AIMessage> out = truncator.tryTruncate(List.of(bigUser));

        assertNotNull("Oversized pinned message must still be returned (truncated)", out);
        assertEquals(1, out.size());
        assertTrue("Truncated content must carry the marker", out.getFirst().getContent().contains("[...truncated"));
        assertTrue("Truncated message must fit within maxTokens", WORD_COUNTER.count(out.getFirst().getContent()) <= 60);
    }

    @Test
    public void chronologicalOrderPreserved() {
        AIMessage m1 = msg(AIMessageType.USER, words(21));
        AIMessage m2 = msg(AIMessageType.ASSISTANT, words(15));
        AIMessage m3 = msg(AIMessageType.USER, words(10));

        List<AIMessage> out = truncator.tryTruncate(List.of(m1, m2, m3));

        assertNotNull(out);
        assertEquals(3, out.size());
        assertTrue("Oldest (truncated) message must be first", out.get(0).getContent().contains("[...truncated"));
        assertSame("m2 must be in the middle", m2, out.get(1));
        assertSame("Newest m3 must be last", m3, out.get(2));
    }

    @Test
    public void lastAssistantIsPinnedWhenNoUser() {
        AIMessage m1 = msg(AIMessageType.ASSISTANT, words(30));
        AIMessage m2 = msg(AIMessageType.ASSISTANT, words(20));

        List<AIMessage> out = truncator.tryTruncate(List.of(m1, m2));

        assertNotNull(out);
        assertEquals(2, out.size());
        assertSame("Pinned newest assistant must be last", m2, out.getLast());
        assertTrue("Older assistant must have been truncated", out.getFirst().getContent().contains("[...truncated"));
    }

    private static AIMessage msg(AIMessageType role, String content) {
        return new AIMessage(role, content, null);
    }

    /**
     * Build a string of n one-token words: "w0 w1 …"
     */
    private static String words(int n) {
        return IntStream.range(0, n).mapToObj(i -> "w" + i).collect(Collectors.joining(" "));
    }
}
