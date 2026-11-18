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

import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

class McpServerStatisticsTest {
    @Test
    void excludesUnrecognizedClientDetails() {
        Map<String, Object> parameters = McpServerStatistics.toolCallParameters(
            McpServerStatistics.AccessPath.LOCAL, "execute_query", "private@example.invalid", "private-host/build");
        Assertions.assertEquals(Map.of(
            "action", "toolCall", "enabled", true, "accessPath", "local", "toolName", "execute_query", "clientName", "other"
        ), parameters);
    }

    @Test
    void keepsKnownClientAndNumericVersionOnly() {
        Map<String, Object> parameters = McpServerStatistics.toolCallParameters(
            McpServerStatistics.AccessPath.LOCAL, "execute_query", "Cursor", "2.1.3");
        Assertions.assertEquals("cursor", parameters.get("clientName"));
        Assertions.assertEquals("2.1.3", parameters.get("clientVersion"));
        Assertions.assertFalse(McpServerStatistics.toolCallParameters(
            McpServerStatistics.AccessPath.LOCAL, "execute_query", "Cursor", "2.1-private@example.invalid"
        ).containsKey("clientVersion"));
    }

    @Test
    void doesNotInventMissingClientDetails() {
        Map<String, Object> parameters = McpServerStatistics.toolCallParameters(
            McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
        Assertions.assertFalse(parameters.containsKey("clientName"));
        Assertions.assertFalse(parameters.containsKey("clientVersion"));
    }

    @Test
    void deduplicatesSharedRequestIdWithoutSendingIt() {
        UUID requestId = UUID.randomUUID();
        UUID secondRequestId = UUID.randomUUID();
        Map<String, Object> parameters = Map.of(
            "action", "toolCall", "enabled", true, "accessPath", "datadam", "toolName", "execute_query");
        try (var registry = Mockito.mockStatic(DBRFeatureRegistry.class)) {
            McpServerStatistics.toolCalled(requestId, McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
            McpServerStatistics.toolCalled(requestId, McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
            McpServerStatistics.toolCalled(secondRequestId, McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
            registry.verify(() -> DBRFeatureRegistry.useFeature(McpServerStatistics.MCP_SERVER, parameters), Mockito.times(2));
            registry.verifyNoMoreInteractions();
        }
    }

    @Test
    void countsLegacyCallsWithoutAnIdSeparately() {
        try (var registry = Mockito.mockStatic(DBRFeatureRegistry.class)) {
            McpServerStatistics.toolCalled(null, McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
            McpServerStatistics.toolCalled(null, McpServerStatistics.AccessPath.DATADAM, "execute_query", null, null);
            registry.verify(() -> DBRFeatureRegistry.useFeature(Mockito.eq(McpServerStatistics.MCP_SERVER), Mockito.anyMap()),
                Mockito.times(2));
        }
    }
}
