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
package org.jkiss.dbeaver.model.ai.qm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIChatMessage;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIMetaTypes;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class QMAIChatHistoryMapperTest {

    @Test
    @ResourceLock("java.util.TimeZone.default")
    public void preservesMessageTimeAcrossHistoryRoundTrip() {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("UTC", "GMT+03:00", "Europe/Berlin", "America/New_York", "Asia/Kolkata")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                for (String instant : List.of("2026-01-15T17:06:00Z", "2026-07-15T17:06:00Z")) {
                    Instant timestamp = Instant.parse(instant);
                    LocalDateTime time = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
                    AIMessage message = new AIMessage(AIMessageType.USER, "test", null, time, null);
                    List<QMAIChatMessage> stored = QMAIChatHistoryMapper.toQMAIChatMessages(
                        List.of(new AIChatMessage(1, message))
                    );

                    Assertions.assertEquals(timestamp, stored.getFirst().timestamp(), zone);
                    List<AIChatMessage> restored = QMAIChatHistoryMapper.toAIMessages(Mockito.mock(AIAssistant.class), stored);
                    Assertions.assertEquals(time, restored.getFirst().message().getTime(), zone);
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void skipsUnavailableUsageInHistory() {
        List<AIMessageMeta> original = List.of(
            new AIMessageMeta(AIMetaTypes.PROMPT, "test", "model", null, Duration.ofSeconds(2), 10)
        );

        Assertions.assertTrue(QMAIChatHistoryMapper.toQMAIMessageMeta(original).isEmpty());
    }

    @Test
    public void preservesMetadataWithoutInventingReportedUsage() {
        List<AIMessageMeta> original = List.of(new AIMessageMeta(AIMetaTypes.PROMPT, "test", "model", null,
            Duration.ofSeconds(2), 10, Map.of("openai.response.output", "opaque-state")));
        List<QMAIMessageMeta> stored = QMAIChatHistoryMapper.toQMAIMessageMeta(original);
        Assertions.assertEquals(1, stored.size());
        QMAIMessageMeta restored = JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(stored.getFirst()), QMAIMessageMeta.class);
        Assertions.assertEquals(original, QMAIChatHistoryMapper.toAIMessageMeta(List.of(restored)));
    }

    @Test
    public void preservesReportedZeroUsageInHistory() {
        List<AIMessageMeta> original = List.of(
            new AIMessageMeta(AIMetaTypes.PROMPT, "test", "model", new AIUsage(0, 0, 0, 0), Duration.ofSeconds(2), 10)
        );
        List<QMAIMessageMeta> stored = QMAIChatHistoryMapper.toQMAIMessageMeta(original);

        Assertions.assertEquals(1, stored.size());
        String json = JSONUtils.GSON.toJson(stored.getFirst());
        QMAIMessageMeta restored = JSONUtils.GSON.fromJson(json, QMAIMessageMeta.class);
        Assertions.assertEquals(original, QMAIChatHistoryMapper.toAIMessageMeta(List.of(restored)));
    }

    @Test
    public void serializesFunctionResultWithoutTraversingValues() {
        AIFunctionResult result = new AIFunctionResult(
            AIFunctionType.INFORMATION,
            new CyclicValue(),
            null,
            new DBException("Database error")
        );
        AIMessage message = new AIMessage(
            new AIFunctionCall("test", Map.of()),
            result,
            LocalDateTime.now(),
            null
        );

        String json = QMAIChatHistoryMapper.toQMAIChatMessages(List.of(new AIChatMessage(1, message)))
            .getFirst()
            .functionResult();
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        Assertions.assertEquals("INFORMATION", object.get("type").getAsString());
        Assertions.assertEquals("Function result", object.get("value").getAsString());
        Assertions.assertEquals(
            "Database error",
            object.getAsJsonObject("exception").get("detailMessage").getAsString()
        );

        LegacyFunctionResult legacyResult = JSONUtils.GSON.fromJson(json, LegacyFunctionResult.class);
        Assertions.assertEquals("Function result", legacyResult.value());
        Assertions.assertEquals("Database error", legacyResult.exception().detailMessage());
    }

    @Test
    public void restoresFunctionResultError() {
        AIFunctionResult result = QMAIChatHistoryMapper.fromFunctionResultJson(
            "{\"type\":\"INFORMATION\",\"value\":\"Failed\",\"exception\":{\"detailMessage\":\"Database error\"}}"
        );

        Assertions.assertEquals(AIFunctionType.INFORMATION, result.getType());
        Assertions.assertEquals("Failed", result.getValue());
        Assertions.assertNotNull(result.getException());
        Assertions.assertEquals("Database error", result.getException().getMessage());
        Assertions.assertNull(result.getCallback());
    }

    @Test
    public void restoresStringFunctionResultError() {
        AIFunctionResult result = QMAIChatHistoryMapper.fromFunctionResultJson(
            "{\"type\":\"INFORMATION\",\"value\":\"Failed\",\"exception\":\"Database error\"}"
        );

        Assertions.assertNotNull(result.getException());
        Assertions.assertEquals("Database error", result.getException().getMessage());
    }

    private static class CyclicValue {
        @SuppressWarnings("unused")
        private final Object self = this;

        @Override
        public String toString() {
            return "Function result";
        }
    }

    private record LegacyFunctionResult(
        AIFunctionType type,
        String value,
        LegacyException exception
    ) {
    }

    private record LegacyException(String detailMessage) {
    }
}
