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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ChatTruncator {

    private final int maxTokens;
    private final int reserveForSystem;
    private final int reserveForReply;
    private final int reserveForOverhead;
    private final TokenCounter counter;

    private ChatTruncator(Builder b) {
        this.maxTokens = b.maxTokens;
        this.reserveForReply = b.reserveForReply;
        this.reserveForOverhead = b.reserveForOverhead;
        this.counter = Objects.requireNonNull(b.counter, "TokenCounter is required");
        this.reserveForSystem = b.reserveForSystem;
        if (maxTokens <= reserveForReply + reserveForOverhead) {
            throw new IllegalArgumentException("maxDbSnapshotTokens too small for the reserves");
        }
    }

    /**
     * Truncate the conversation to fit into (maxDbSnapshotTokens - reserves).
     * Ordering in the result is chronological (SYSTEM first, then oldest->newest).
     */
    @NotNull
    public List<AIMessage> truncate(@NotNull List<AIMessage> input) {
        List<AIMessage> messages = filterNonEmpty(input);
        if (messages.isEmpty()) {
            return List.of();
        }

        // 1) Extract and merge SYSTEM messages
        List<AIMessage> systems = new ArrayList<>();
        ArrayList<AIMessage> rest = new ArrayList<>(messages.size());
        for (AIMessage m : messages) {
            if (m.getRole() == AIMessageType.SYSTEM) {
                systems.add(m);
            } else {
                rest.add(m);
            }
        }

        AIMessage mergedSystem = systems.isEmpty() ? null : mergeSystems(systems);

        int systemTokens = mergedSystem != null ? counter.count(mergedSystem.getContent()) : 0;
        int systemCap = Math.min(systemTokens, reserveForSystem);
        int headroom = maxTokens - reserveForReply - reserveForOverhead;
        int budget = Math.max(0, headroom - systemCap);

        // 2) Walk from newest to oldest
        ArrayList<AIMessage> pickedReverse = new ArrayList<>(rest.size());
        int used = 0;

        // Simple greedy: newest -> oldest
        for (int i = rest.size() - 1; i >= 0; i--) {
            AIMessage m = rest.get(i);
            int t = counter.count(m.getContent());
            if (used + t <= budget) {
                pickedReverse.add(m);
                used += t;
            } else {
                int remaining = budget - used;
                if (remaining <= 0) {
                    break;
                }
                AIMessage cut = truncateToTokens(m, remaining);
                int cutTokens = counter.count(cut.getContent());
                if (cutTokens > 0) {
                    pickedReverse.add(cut);
                    used += cutTokens;
                }
                break;
            }
        }

        // 3) Chronological order
        Collections.reverse(pickedReverse);

        // 4) Place SYSTEM in front
        ArrayList<AIMessage> result = new ArrayList<>(pickedReverse.size() + 1);
        if (mergedSystem != null) {
            int remainingForSystem = Math.max(0, headroom - used);
            result.add(truncateToTokens(mergedSystem, remainingForSystem));
        }

        result.addAll(pickedReverse);
        return result;
    }

    private static AIMessage mergeSystems(List<AIMessage> systems) {
        assert !systems.isEmpty() : "At least one SYSTEM message is required";

        // Preserve chronological order; newest last has the highest precedence for humans reading.
        String mergedMessage = systems.stream()
            .map(AIMessage::getContent)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining("\n\n---\n\n"));

        return systems.getFirst().withContent(mergedMessage);
    }

    private static List<AIMessage> filterNonEmpty(List<AIMessage> in) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        ArrayList<AIMessage> out = new ArrayList<>(in.size());
        for (AIMessage m : in) {
            if (m != null && !m.getContent().isBlank()) {
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Truncate message content to <= maxDbSnapshotTokens using binary search over substring length.
     * Keeps the BEGINNING of the content (most stable/system prompts, etc).
     */
    private AIMessage truncateToTokens(AIMessage message, int maxTokens) {
        if (maxTokens <= 0) {
            return message.withContent("");
        }
        if (counter.count(message.getContent()) <= maxTokens) {
            return message;
        }

        String content = message.getContent();
        int lo = 0;
        int hi = content.length();

        AIMessage best = message.withContent("");
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String slice = safeHead(content, mid); // <-- keep the start
            AIMessage candidate = message.withContent(slice);
            int t = counter.count(candidate.getContent());
            if (t <= maxTokens) {
                best = candidate;   // can fit more from the start
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    private static String safeHead(String s, int headLen) {
        if (headLen <= 0) {
            return "";
        }
        if (headLen >= s.length()) {
            return s;
        }
        return s.substring(0, headLen);
    }

    // ----- builder -----

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxTokens;
        private int reserveForSystem;
        private int reserveForReply;
        private int reserveForOverhead;
        private TokenCounter counter;

        public Builder maxTokens(int v) {
            this.maxTokens = v;
            return this;
        }

        public Builder reserveForSystem(int v) {
            this.reserveForSystem = v;
            return this;
        }

        public Builder reserveForReply(int v) {
            this.reserveForReply = v;
            return this;
        }

        public Builder reserveForOverhead(int v) {
            this.reserveForOverhead = v;
            return this;
        }

        public Builder tokenCounter(TokenCounter c) {
            this.counter = c;
            return this;
        }

        public ChatTruncator build() {
            assert maxTokens > reserveForReply + reserveForOverhead + reserveForSystem
                : "maxDbSnapshotTokens must be greater than the sum of reserves";

            return new ChatTruncator(this);
        }
    }

}
