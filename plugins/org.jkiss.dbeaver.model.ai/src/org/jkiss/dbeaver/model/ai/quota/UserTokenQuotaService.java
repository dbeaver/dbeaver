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
package org.jkiss.dbeaver.model.ai.quota;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.qm.QMAIMessageMeta;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserTokenQuotaService implements AIChatListener {
    private static final Duration QUOTA_REFRESH_INTERVAL = Duration.ofDays(1);
    private static final ChronoUnit QUOTA_REFRESH_INTERVAL_UNIT = ChronoUnit.DAYS;

    @NotNull
    private final Clock clock;
    @NotNull
    private final AIChatStorage chatStorage;
    private final Map<String, CachedValue> totalInputTokenCountCache = new ConcurrentHashMap<>();

    public UserTokenQuotaService(
        @NotNull Clock clock,
        @NotNull AIChatStorage chatStorage
    ) {
        this.clock = clock;
        this.chatStorage = chatStorage;
    }

    @NotNull
    public QuotaStatus getUserQuotaStatus(@NotNull String sessionId, @NotNull String engineId) throws DBException {
        UserQuotaSettings config = getConfig();

        if (config == null || !config.enabled()) {
            return QuotaStatus.EMPTY;
        }

        Instant now = clock.instant();
        CachedValue value;
        try {
            value = totalInputTokenCountCache.compute(
                engineId, (k, v) -> {
                    if (v == null || v.getCacheTime().isBefore(now.minus(QUOTA_REFRESH_INTERVAL))) {
                        TokenCount tokenCount = computeTotalInputTokenCount(sessionId, engineId, now);
                        return new CachedValue(tokenCount.prompt, tokenCount.embedding);
                    } else {
                        return v;
                    }
                }
            );
        } catch (UncheckedDBException e) {
            throw e.getCause();
        }

        long tokenLimit = config.engineToMaxTokens().getOrDefault(engineId, -1L);
        return new QuotaStatus(
            true,
            value.getPromptTokenCount(),
            tokenLimit,
            tokenLimit >= 0 ? Math.max(0, tokenLimit - value.getPromptTokenCount()) : Long.MAX_VALUE,
            tokenLimit >= 0 && value.getPromptTokenCount() >= tokenLimit,
            now.plus(QUOTA_REFRESH_INTERVAL).truncatedTo(QUOTA_REFRESH_INTERVAL_UNIT),
            value.getEmbeddingTokenCount()
        );
    }

    @Override
    public void messageAdded(
        @NotNull AIChatConversation conversation,
        @NotNull AIChatMessage message
    ) {
        List<AIMessageMeta> meta = message.message().getMeta();
        if (meta == null) {
            return;
        }

        for (AIMessageMeta messageMeta : meta) {
            String engineId = messageMeta.engineId();
            AIUsage usage = messageMeta.usage();
            if (usage == null) {
                continue;
            }
            totalInputTokenCountCache.computeIfPresent(
                engineId, (k, v) -> {
                    switch (messageMeta.type()) {
                        case AIMetaTypes.EMBEDDING -> v.addAndGetEmbeddingTokenCount(usage.totalInputTokens());
                        case AIMetaTypes.PROMPT -> v.addAndGetPromptTokenCount(usage.totalInputTokens());
                        default -> {}
                    }

                    return v;
                }
            );
        }
    }

    @NotNull
    private TokenCount computeTotalInputTokenCount(
        @NotNull String sessionId,
        @NotNull String engineId,
        @NotNull Instant now
    ) {
        try {
            List<QMAIMessageMeta> historyMeta = chatStorage.getConversationHistoryMeta(
                sessionId,
                engineId,
                now.truncatedTo(QUOTA_REFRESH_INTERVAL_UNIT),
                now
            );
            long promptCount = historyMeta.stream()
                .filter(it -> it.type().equalsIgnoreCase(AIMetaTypes.PROMPT))
                .map(it -> (long) it.totalInputTokens())
                .reduce(0L, Long::sum);

            long embeddingCount = historyMeta.stream()
                .filter(it -> it.type().equalsIgnoreCase(AIMetaTypes.EMBEDDING))
                .map(it -> (long) it.totalInputTokens())
                .reduce(0L, Long::sum);

            return new TokenCount(promptCount, embeddingCount);
        } catch (DBException e) {
            throw new UncheckedDBException(e);
        }
    }

    private class CachedValue {
        @NotNull
        private final AtomicLong promptTokenCount;
        @NotNull
        private final AtomicLong embeddingTokenCount;
        @NotNull
        private final Instant cacheTime;

        CachedValue(
            long promptTokenCount,
            long embeddingTokenCount
        ) {
            this.promptTokenCount = new AtomicLong(promptTokenCount);
            this.embeddingTokenCount = new AtomicLong(embeddingTokenCount);
            this.cacheTime = clock.instant();
        }

        long getPromptTokenCount() {
            return promptTokenCount.get();
        }

        long addAndGetPromptTokenCount(long delta) {
            return promptTokenCount.addAndGet(delta);
        }

        long getEmbeddingTokenCount() {
            return embeddingTokenCount.get();
        }

        long addAndGetEmbeddingTokenCount(long delta) {
            return embeddingTokenCount.addAndGet(delta);
        }

        @NotNull
        Instant getCacheTime() {
            return cacheTime;
        }
    }

    private static class UncheckedDBException extends RuntimeException {
        public UncheckedDBException(@NotNull DBException cause) {
            super(cause);
        }

        @NotNull
        @Override
        public synchronized DBException getCause() {
            return (DBException) super.getCause();
        }
    }

    @Nullable
    private static UserQuotaSettings getConfig() {
        AISettings settings = AISettingsManager.getInstance().getSettings();
        Object props = settings.getProperty(AIConstants.USER_QUOTA_PROPERTY);
        return AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.SAVE_PROPS_GSON.toJson(props),
            UserQuotaSettings.class
        );
    }

    private record TokenCount(long prompt, long embedding) {
    }
}
