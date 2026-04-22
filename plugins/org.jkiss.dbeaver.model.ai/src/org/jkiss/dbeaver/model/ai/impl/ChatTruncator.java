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
    private static final String DEFAULT_OMITTED_NOTICE =
        "[Note: some earlier messages were omitted due to context length limits.]";

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
            throw new IllegalArgumentException("maxDbSnapshotTokens too small for the reserves");
        }
    }

    /**
     * Result of a truncation operation.
     * {@link #wasTruncated()} returns {@code true} if any message was dropped or its content was cut.
     */
    public record TruncationResult(@NotNull List<AIMessage> messages, boolean wasTruncated) {
    }

    /**
     * Truncate the conversation to fit into (maxDbSnapshotTokens - reserves).
     * Ordering in the result is chronological (SYSTEM first, then oldest->newest).
     *
     * @return a {@link TruncationResult} that carries the resulting message list
     *         and a flag indicating whether anything was actually truncated.
     */
    @NotNull
    public TruncationResult truncate(@NotNull List<AIMessage> input) {
        List<AIMessage> messages = filterNonEmpty(input);
        if (messages.isEmpty()) {
            return new TruncationResult(List.of(), false);
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
        //    The very last (most recent) message is ALWAYS included – even if the budget is
        //    exhausted – because it carries the user's actual intent.  Without it the model
        //    has no idea what is being asked.
        ArrayList<AIMessage> pickedReverse = new ArrayList<>(rest.size());
        int used = 0;
        boolean truncated = false;
        boolean messagesOmitted = false;

        if (!rest.isEmpty()) {
            // Find indices of messages that must always be present:
            //  - last USER message  (carries the actual question)
            //  - last ASSISTANT message (gives the model conversational context)
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

            // Reserve space for pinned messages first (truncate if budget is tight, but never drop)
            // We use a small helper map: originalIndex -> message to include
            LinkedHashMap<Integer, AIMessage> pinned = new LinkedHashMap<>();
            for (int idx : new int[]{pinnedUserIdx, pinnedAssistantIdx}) {
                if (idx < 0) {
                    continue;
                }
                AIMessage m = rest.get(idx);
                int t = counter.count(m.getContent());
                if (used + t <= budget) {
                    pinned.put(idx, m);
                    used += t;
                } else {
                    int remaining = Math.max(0, budget - used);
                    AIMessage cut = truncateToTokens(m, remaining);
                    cut = cut.withContent(cut.getContent() + ChatTruncator.DEFAULT_TRUNCATED_SUFFIX);
                    pinned.put(idx, cut);
                    used += counter.count(cut.getContent());
                    truncated = true;
                }
            }

            // Greedily fill remaining budget from newest to oldest, skipping pinned indices
            LinkedHashMap<Integer, AIMessage> extra = new LinkedHashMap<>();
            for (int i = rest.size() - 1; i >= 0; i--) {
                if (pinned.containsKey(i)) {
                    continue;
                }
                AIMessage m = rest.get(i);
                int t = counter.count(m.getContent());
                if (used + t <= budget) {
                    extra.put(i, m);
                    used += t;
                } else {
                    int remaining = budget - used;
                    truncated = true;
                    if (remaining <= 0) {
                        messagesOmitted = true;
                        break;
                    }
                    AIMessage cut = truncateToTokens(m, remaining);
                    int cutTokens = counter.count(cut.getContent());
                    if (cutTokens > 0) {
                        cut = cut.withContent(cut.getContent() + ChatTruncator.DEFAULT_TRUNCATED_SUFFIX);
                        extra.put(i, cut);
                        used += cutTokens;
                    } else {
                        messagesOmitted = true;
                    }
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
            AIMessage truncatedSystem = truncateToTokens(mergedSystem, remainingForSystem);
            if (!truncatedSystem.getContent().equals(mergedSystem.getContent())) {
                truncated = true;
                truncatedSystem = truncatedSystem.withContent(truncatedSystem.getContent() + ChatTruncator.DEFAULT_TRUNCATED_SUFFIX);
            }
            result.add(truncatedSystem);
        }

        if (messagesOmitted) {
            result.add(AIMessage.systemMessage(DEFAULT_OMITTED_NOTICE));
        }

        result.addAll(pickedReverse);
        return new TruncationResult(result, truncated);
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
                : "maxDbSnapshotTokens must be greater than the sum of reserves";

            return new ChatTruncator(this);
        }
    }

}
