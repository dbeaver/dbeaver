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
package org.jkiss.dbeaver.model.datadam.transport;

import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpAiFunctionCatalog;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Bounded, in-memory diagnostics for the current or most recent WebSocket connection.
 * Traffic counts application payload bytes, excluding WebSocket framing, control frames and TLS overhead.
 */
public final class DDConnectionMonitor {
    public static final String MCP_RESULT_OK = "OK";
    private static final int MAX_EVENTS = 10_000;
    private static final int MAX_DETAILS_LENGTH = 100_000;
    private static final int MAX_RETAINED_TEXT_LENGTH = 10_000_000;

    public enum EventType {
        CONNECTING, CONNECTED, DISCONNECTED, CLOSED, SIGNED_IN, SIGNED_OUT, INVALID_TOKEN, ERROR,
        MCP_REQUEST, MCP_RESPONSE
    }

    public record Event(
        @NotNull Instant time, @NotNull EventType type, @NotNull String details, @Nullable McpRequest request,
        long requestId, @Nullable McpResponse response, long startedNanos
    ) {
        public Event(
            @NotNull Instant time, @NotNull EventType type, @NotNull String details, @Nullable McpRequest request,
            long requestId, @Nullable McpResponse response
        ) {
            this(time, type, details, request, requestId, response, 0);
        }

        public Event(@NotNull Instant time, @NotNull EventType type, @NotNull String details, @Nullable McpRequest request) {
            this(time, type, details, request, 0, null);
        }

        public Event(@NotNull Instant time, @NotNull EventType type, @NotNull String details) {
            this(time, type, details, null);
        }

        @NotNull
        public String fullDetails() {
            return response == null ? details : details + "\n\n" + response.code()
                + (response.error() == null ? "" : "\n" + response.error())
                + "\n" + response.processingTime() + "\n\n" + response.body();
        }

        public boolean isPending() {
            return requestId != 0 && response == null;
        }

        public boolean isError() {
            return type == EventType.ERROR || response != null && (response.error() != null || !MCP_RESULT_OK.equals(response.code()));
        }
    }

    public record McpResponse(@NotNull String code, @Nullable String error, @NotNull String body, @NotNull Duration processingTime) {
        public McpResponse(@NotNull String code, @Nullable String error, @NotNull String body) {
            this(code, error, body, Duration.ZERO);
        }

        @NotNull
        public String valueText() {
            try {
                var json = JsonParser.parseString(body);
                var value = json.isJsonObject() && json.getAsJsonObject().has("value")
                    ? json.getAsJsonObject().get("value") : json;
                return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                    ? value.getAsString() : JSONUtils.PRETTY_GSON.toJson(value);
            } catch (JsonParseException e) {
                return body;
            }
        }
    }

    public record McpRequest(
        @NotNull String operation,
        @Nullable String projectId,
        @Nullable String datasourceId,
        @NotNull Map<String, String> parameters
    ) {
        public McpRequest {
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }

    public record Snapshot(
        @NotNull DDRemoteDamChannelState state,
        @Nullable Instant connectedAt,
        @NotNull Duration duration,
        long bytesSent,
        long bytesReceived,
        @Nullable String error
    ) {
    }

    private final Map<Long, Event> events = new LinkedHashMap<>();
    private final LongSupplier nanoTime;
    private DDRemoteDamChannelState state = DDRemoteDamChannelState.DISCONNECTED;
    @Nullable
    private Instant connectedAt;
    @Nullable
    private String error;
    private long startedNanos;
    private long durationNanos;
    private boolean open;
    private long bytesSent;
    private long bytesReceived;
    private long nextRequestId;
    private long nextStandaloneEventId;
    private long retainedTextLength;

    public DDConnectionMonitor() {
        this(System::nanoTime);
    }

    DDConnectionMonitor(@NotNull LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    synchronized void opened() {
        connectedAt = Instant.now();
        startedNanos = nanoTime.getAsLong();
        durationNanos = 0;
        bytesSent = 0;
        bytesReceived = 0;
        open = true;
    }

    synchronized void disconnected() {
        if (open) {
            durationNanos = nanoTime.getAsLong() - startedNanos;
            open = false;
        }
    }

    synchronized void stateChanged(@NotNull DDRemoteDamChannelStatus status) {
        state = status.state();
        if (status.isReady()) {
            error = null;
        }
        record(EventType.valueOf(state.name()), status.session() == null ? "" : status.session().damSessionId());
    }

    synchronized void sent(long bytes) {
        bytesSent += bytes;
    }

    synchronized void received(long bytes) {
        bytesReceived += bytes;
    }

    public synchronized void failed(@NotNull String message) {
        error = limit(message);
        record(EventType.ERROR, error);
    }

    public synchronized void clearError(@NotNull String expectedError) {
        if (expectedError.equals(error)) {
            error = null;
        }
    }

    public synchronized void record(@NotNull EventType type, @NotNull String details) {
        addEvent(new Event(Instant.now(), type, limit(details)));
    }

    public synchronized void recordMcpRequest(@NotNull String operation, @Nullable Map<String, Object> arguments) {
        addEvent(createMcpRequest(operation, arguments, 0));
    }

    @NotNull
    private Event createMcpRequest(@NotNull String operation, @Nullable Map<String, Object> arguments, long requestId) {
        if (arguments == null) {
            return new Event(Instant.now(), EventType.MCP_REQUEST, limit(operation), null, requestId, null, nanoTime.getAsLong());
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        int remaining = MAX_DETAILS_LENGTH;
        long valueLength = 0;
        for (var parameter : arguments.entrySet()) {
            if (parameter.getKey().length() >= remaining) {
                break;
            }
            remaining -= parameter.getKey().length();
            String value = parameter.getValue() instanceof String string ? string : JSONUtils.PRETTY_GSON.toJson(parameter.getValue());
            parameters.put(parameter.getKey(), value);
            valueLength += value.length();
        }
        if (valueLength > remaining) {
            // Trim the largest values first, preserving short parameters such as connection identifiers.
            long excess = valueLength - remaining;
            var largestFirst = parameters.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, String>>comparingInt(entry -> entry.getValue().length()).reversed()).toList();
            for (var parameter : largestFirst) {
                int removed = (int) Math.min(excess, parameter.getValue().length());
                parameter.setValue(parameter.getValue().substring(0, parameter.getValue().length() - removed));
                excess -= removed;
                if (excess == 0) {
                    break;
                }
            }
        }
        // Keep connection identifiers separately so a long preceding argument cannot truncate them away.
        var request = new McpRequest(limit(operation),
            argumentId(arguments, DDMcpAiFunctionCatalog.PROJECT_ID_PARAMETER),
            argumentId(arguments, DDMcpAiFunctionCatalog.DATASOURCE_ID_PARAMETER), parameters);
        return new Event(Instant.now(), EventType.MCP_REQUEST,
            limit(operation + "\n\n" + JSONUtils.PRETTY_GSON.toJson(arguments)), request, requestId, null, nanoTime.getAsLong());
    }

    @NotNull
    public synchronized Event beginMcpRequest(@NotNull String operation, @Nullable Map<String, Object> arguments) {
        Event request = createMcpRequest(operation, arguments, ++nextRequestId);
        addEvent(request);
        return request;
    }

    public synchronized void recordMcpResponse(
        @NotNull Event request, @NotNull String code, @Nullable String errorMessage, @NotNull String body
    ) {
        Event pending = events.get(request.requestId());
        if (pending == null || !pending.isPending()) {
            return;
        }
        String responseError = errorMessage == null ? null : limit(errorMessage);
        Duration processingTime = Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - pending.startedNanos()));
        addEvent(new Event(pending.time(), pending.type(), pending.details(), pending.request(), pending.requestId(),
            new McpResponse(code, responseError, body, processingTime), pending.startedNanos()));
    }

    @Nullable
    private static String argumentId(@NotNull Map<String, Object> arguments, @NotNull String name) {
        return arguments.get(name) instanceof String id ? limit(id) : null;
    }

    private void addEvent(@NotNull Event event) {
        // Positive keys identify requests; negative keys keep standalone events in the same ordered history.
        long key = event.requestId() == 0 ? --nextStandaloneEventId : event.requestId();
        Event previous = events.put(key, event);
        retainedTextLength += textLength(event) - (previous == null ? 0 : textLength(previous));
        // A single oversized response remains available in full, but cannot accumulate with other events.
        while (events.size() > 1 && (events.size() > MAX_EVENTS || retainedTextLength > MAX_RETAINED_TEXT_LENGTH)) {
            var iterator = events.entrySet().iterator();
            var oldest = iterator.next();
            if (oldest.getKey() == key) {
                oldest = iterator.next();
            }
            retainedTextLength -= textLength(oldest.getValue());
            iterator.remove();
        }
    }

    private static long textLength(@NotNull Event event) {
        long length = event.details().length();
        if (event.request() != null) {
            for (var parameter : event.request().parameters().entrySet()) {
                length += parameter.getKey().length() + parameter.getValue().length();
            }
        }
        if (event.response() != null) {
            length += event.response().body().length();
            if (event.response().error() != null) {
                length += event.response().error().length();
            }
        }
        return length;
    }

    @NotNull
    public synchronized Snapshot snapshot() {
        return new Snapshot(state, connectedAt, Duration.ofNanos(open ? nanoTime.getAsLong() - startedNanos : durationNanos),
            bytesSent, bytesReceived, error);
    }

    @NotNull
    public synchronized List<Event> getEvents() {
        return List.copyOf(events.values());
    }

    @Nullable
    public synchronized Event getRequestEvent(long requestId) {
        return events.get(requestId);
    }

    public synchronized void clearEvents() {
        events.clear();
        retainedTextLength = 0;
    }

    @NotNull
    private static String limit(@NotNull String text) {
        return text.length() <= MAX_DETAILS_LENGTH ? text : text.substring(0, MAX_DETAILS_LENGTH);
    }
}
