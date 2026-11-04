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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

public class AIExtendedUsageTest {
    @Test
    public void distinguishesUnknownUsageFromZero() {
        AIExtendedUsage unknown = AIExtendedUsage.from(List.of(meta(null)));
        AIExtendedUsage zero = AIExtendedUsage.from(List.of(meta(new AIUsage(0, 0, 0, 0))));

        Assertions.assertFalse(unknown.hasUsage());
        Assertions.assertTrue(zero.hasUsage());
        Assertions.assertEquals(Duration.ofSeconds(1), unknown.totalTime());
    }

    @Test
    public void retainsKnownTotalsWhenSomeRequestsHaveNoUsage() {
        AIExtendedUsage usage = AIExtendedUsage.from(List.of(meta(new AIUsage(10, 2, 5, 1)), meta(null)));

        Assertions.assertTrue(usage.hasUsage());
        Assertions.assertEquals(10, usage.totalInputTokens());
        Assertions.assertEquals(5, usage.totalOutputTokens());
    }

    private static AIMessageMeta meta(AIUsage usage) {
        return new AIMessageMeta(AIMetaTypes.PROMPT, "test", "model", usage, Duration.ofSeconds(1), 0);
    }
}
