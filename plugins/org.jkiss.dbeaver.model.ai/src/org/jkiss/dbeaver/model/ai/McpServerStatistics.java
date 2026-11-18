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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class McpServerStatistics {
    public enum AccessPath {
        LOCAL,
        DATADAM
    }

    public static final DBRFeature MCP_SERVER = new DBRFeature("MCP_SERVER");
    public static final String REQUEST_ID = "dbeaver.mcp.requestId";

    private static final int MAX_RECENT_REQUESTS = 10000;
    private static final Set<UUID> recentRequests = new LinkedHashSet<>();

    private McpServerStatistics() {
    }

    public static void configurationChanged(@NotNull AccessPath accessPath, boolean enabled) {
        MCP_SERVER.use(Map.of(
            "action", enabled ? "enable" : "disable",
            "enabled", enabled,
            "accessPath", accessPath.name().toLowerCase(Locale.ROOT)
        ));
    }

    public static void toolCalled(
        @Nullable UUID requestId,
        @NotNull AccessPath accessPath,
        @NotNull String registeredToolName,
        @Nullable String clientName,
        @Nullable String clientVersion
    ) {
        if (requestId != null) {
            synchronized (recentRequests) {
                if (!recentRequests.add(requestId)) {
                    return;
                }
                if (recentRequests.size() > MAX_RECENT_REQUESTS) {
                    recentRequests.remove(recentRequests.iterator().next());
                }
            }
        }
        MCP_SERVER.use(toolCallParameters(accessPath, registeredToolName, clientName, clientVersion));
    }

    @NotNull
    public static Map<String, Object> toolCallParameters(
        @NotNull AccessPath accessPath,
        @NotNull String registeredToolName,
        @Nullable String clientName,
        @Nullable String clientVersion
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("action", "toolCall");
        parameters.put("enabled", true);
        parameters.put("accessPath", accessPath.name().toLowerCase(Locale.ROOT));
        parameters.put("toolName", registeredToolName);
        if (clientName != null) {
            String knownClient = switch (clientName.toLowerCase(Locale.ROOT)) {
                case "claude-ai", "claude-desktop" -> "claude-desktop";
                case "claude-code" -> "claude-code";
                case "cursor" -> "cursor";
                case "vscode", "visual-studio-code" -> "vscode";
                case "codex", "codex-mcp-client" -> "codex";
                case "zed" -> "zed";
                default -> "other";
            };
            parameters.put("clientName", knownClient);
            if (!"other".equals(knownClient) && clientVersion != null &&
                clientVersion.matches("[0-9]{1,4}(\\.[0-9]{1,4}){0,3}")) {
                parameters.put("clientVersion", clientVersion);
            }
        }
        return parameters;
    }
}
