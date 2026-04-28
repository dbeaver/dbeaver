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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;

import java.util.*;
import java.util.stream.Collectors;

public final class ChatTruncator {

    private static final String DEFAULT_TRUNCATED_SUFFIX  = "\n[...truncated, don't try again]";

    private final int maxTokens;
    private final int reserveForSystem;
    private final int reserveForReply;
    private final int reserveForOverhead;
    private final TokenCounter counter;

    private ChatTruncator(@NotNull Builder b) {
        this.maxTokens = b.maxTokens;
        this.reserveForReply = b.reserveForReply;
        this.reserveForOverhead = b.reserveForOverhead;
        this.counter = Objects.requireNonNull(b.counter, "TokenCounter is required");
        this.reserveForSystem = b.reserveForSystem;
        if (maxTokens <= reserveForReply + reserveForOverhead) {
            throw new IllegalArgumentException("This AI request does not fit into the selected context window");
        }
    }

    /**
     * Attempts to truncate the conversation so it fits within the configured token budget.
     *
     * @param input the full conversation history (may be unordered with respect to SYSTEM messages)
     * @return the truncated message list if anything was cut or dropped,
     *         or {@code null} if the entire input already fits within the budget unchanged
     */
    @Nullable
    public List<AIMessage> tryTruncate(@NotNull List<AIMessage> input) {
        List<AIMessage> messages = filterNonEmpty(input);
        if (messages.isEmpty()) {
            return null;
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

        // 2) Walk from newest to oldest.
        List<AIMessage> pickedReverse = new ArrayList<>(rest.size());
        int used = 0;
        boolean truncated = false;

        if (!rest.isEmpty()) {
            int[] pinnedIndexes = getLastUserAndAssistantMessage(rest);
            Map<Integer, AIMessage> pinned = new LinkedHashMap<>();
            for (int idx : pinnedIndexes) {
                if (idx < 0) {
                    continue;
                }
                AIMessage m = rest.get(idx);
                AIMessage truncatedMessage = tryTruncateMessage(m, Math.max(0, budget - used));
                AIMessage pickedMessage = truncatedMessage == null ? m : truncatedMessage;
                pinned.put(idx, pickedMessage);
                used += counter.count(pickedMessage.getContent());
                truncated = truncated | truncatedMessage != null;
            }

            Map<Integer, AIMessage> extra = new LinkedHashMap<>();
            for (int i = rest.size() - 1; i >= 0; i--) {
                if (pinned.containsKey(i)) {
                    continue;
                }
                AIMessage m = rest.get(i);
                AIMessage truncatedMessage = tryTruncateMessage(m, budget - used);
                if (truncatedMessage == null) {
                    extra.put(i, m);
                    used += counter.count(m.getContent());
                } else {
                    truncated = true;
                    int contentTokens = countTokensWithoutTruncatedSuffix(truncatedMessage);
                    if (contentTokens <= 0) {
                        break;
                    }
                    extra.put(i, truncatedMessage);
                    used += contentTokens;
                    break;
                }
            }

            TreeMap<Integer, AIMessage> ordered = new TreeMap<>();
            ordered.putAll(pinned);
            ordered.putAll(extra);
            pickedReverse.addAll(ordered.values()); // TreeMap iterates in ascending key order
        }

        // 3) Place SYSTEM in front; optionally prepend omission notice
        ArrayList<AIMessage> result = new ArrayList<>(pickedReverse.size() + 2);
        if (mergedSystem != null) {
            int remainingForSystem = Math.max(0, headroom - used);
            AIMessage truncatedSystem = tryTruncateMessage(mergedSystem, remainingForSystem);
            truncated = truncated || truncatedSystem != null;
            result.add(truncatedSystem == null ? mergedSystem : truncatedSystem);
        }

        result.addAll(pickedReverse);
        return truncated ? result : null;
    }

    @NotNull
    private static int[] getLastUserAndAssistantMessage(@NotNull List<AIMessage> rest) {
        int pinnedUserIdx = -1;
        int pinnedAssistantIdx = -1;
        for (int i = rest.size() - 1; i >= 0; i--) {
            AIMessageType role = rest.get(i).getRole();
            if (pinnedUserIdx < 0 && role == AIMessageType.USER) {
                pinnedUserIdx = i;
            }
            if (pinnedAssistantIdx < 0 && role == AIMessageType.ASSISTANT) {
                pinnedAssistantIdx = i;
            }
            if (pinnedUserIdx >= 0 && pinnedAssistantIdx >= 0) {
                break;
            }
        }
        return new int[]{pinnedUserIdx, pinnedAssistantIdx};
    }

    @NotNull
    private static AIMessage mergeSystems(@NotNull List<AIMessage> systems) {
        assert !systems.isEmpty() : "At least one SYSTEM message is required";

        // Preserve chronological order; newest last has the highest precedence for humans reading.
        String mergedMessage = systems.stream()
            .map(AIMessage::getContent)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining("\n\n---\n\n"));

        return systems.getFirst().withContent(mergedMessage);
    }

    @NotNull
    private static List<AIMessage> filterNonEmpty(@Nullable List<AIMessage> in) {
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
    @NotNull
    private AIMessage truncateToTokens(@NotNull AIMessage message, int maxTokens) {
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

    @Nullable
    private AIMessage tryTruncateMessage(@NotNull AIMessage message, int maxTokens) {
        if (maxTokens >= counter.count(message.getContent())) {
            return null;
        }
        AIMessage truncatedMessage = truncateToTokens(message, maxTokens);
        return truncatedMessage.withContent(truncatedMessage.getContent() + DEFAULT_TRUNCATED_SUFFIX);
    }

    private int countTokensWithoutTruncatedSuffix(@NotNull AIMessage message) {
        String content = message.getContent();
        if (!content.endsWith(DEFAULT_TRUNCATED_SUFFIX)) {
            return counter.count(content);
        }
        return counter.count(content.substring(0, content.length() - DEFAULT_TRUNCATED_SUFFIX.length()));
    }

    @NotNull
    private static String safeHead(@NotNull String s, int headLen) {
        if (headLen <= 0) {
            return "";
        }
        if (headLen >= s.length()) {
            return s;
        }
        return s.substring(0, headLen);
    }

    // ----- builder -----

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxTokens;
        private int reserveForSystem;
        private int reserveForReply;
        private int reserveForOverhead;
        private TokenCounter counter;

        @NotNull
        public Builder maxTokens(int v) {
            this.maxTokens = v;
            return this;
        }

        @NotNull
        public Builder reserveForSystem(int v) {
            this.reserveForSystem = v;
            return this;
        }

        @NotNull
        public Builder reserveForReply(int v) {
            this.reserveForReply = v;
            return this;
        }

        @NotNull
        public Builder reserveForOverhead(int v) {
            this.reserveForOverhead = v;
            return this;
        }

        @NotNull
        public Builder tokenCounter(@NotNull TokenCounter c) {
            this.counter = c;
            return this;
        }

        @NotNull
        public ChatTruncator build() {
            assert maxTokens > reserveForReply + reserveForOverhead + reserveForSystem
                : "This AI request does not fit into the selected context window";

            return new ChatTruncator(this);
        }
    }

}
