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

import java.time.Duration;
import java.util.List;

public record AIExtendedUsage(
    int totalInputTokens,
    int cachedTokens,
    int totalOutputTokens,
    int reasoningTokens,
    int embeddingTokens,
    Duration totalTime
) {
    public static AIExtendedUsage from(@NotNull List<AIMessageMeta> messageMetas) {
        int totalInputTokens = 0;
        int cachedTokens = 0;
        int totalOutputTokens = 0;
        int reasoningTokens = 0;
        int embeddingTokens = 0;
        Duration totalTime = Duration.ZERO;

        for (AIMessageMeta messageMeta : messageMetas) {
            totalTime = totalTime.plus(messageMeta.timeSpent());

            if (messageMeta.usage() == null) {
                continue;
            }
            if (messageMeta.type().equals(AIMetaTypes.EMBEDDING)) {
                embeddingTokens += messageMeta.usage().totalInputTokens();
            } else {
                totalInputTokens += messageMeta.usage().totalInputTokens();
                cachedTokens += messageMeta.usage().cachedTokens();
                totalOutputTokens += messageMeta.usage().totalOutputTokens();
                reasoningTokens += messageMeta.usage().reasoningTokens();
            }
        }

        return new AIExtendedUsage(
            totalInputTokens,
            cachedTokens,
            totalOutputTokens,
            reasoningTokens,
            embeddingTokens,
            totalTime
        );
    }
}
