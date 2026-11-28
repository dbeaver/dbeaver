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

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class DDConnectionMonitorTest {
    @Test
    void loadingAnExistingNameDoesNotReportARename() {
        var monitor = new DDConnectionMonitor();
        monitor.damNameLoaded("Saved desktop");
        monitor.damNameLoaded("Saved desktop");
        monitor.stateChanged(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null), false);
        monitor.stateChanged(new DDRemoteDamChannelStatus(
            DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", "session")), false);
        monitor.damNameLoaded("Saved desktop");
        Assertions.assertTrue(monitor.getEvents().isEmpty());

        monitor.damNameChanged("Renamed desktop");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, monitor.getEvents().getFirst().type());
        Assertions.assertEquals("Renamed desktop", monitor.getEvents().getFirst().details());
    }

    @Test
    void loadingAnotherAccountsNameIsSilentButRenamingItIsRecorded() {
        var monitor = new DDConnectionMonitor();
        monitor.damNameLoaded("First account desktop");
        monitor.damNameLoaded("Second account desktop");
        Assertions.assertTrue(monitor.getEvents().isEmpty());

        monitor.damNameChanged("Renamed desktop");
        monitor.damNameLoaded("Renamed desktop");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, monitor.getEvents().getFirst().type());
        Assertions.assertEquals("Renamed desktop", monitor.getEvents().getFirst().details());
    }

    @Test
    void aNewAccountCanConfirmAssignmentOfTheSameEnvironmentName() {
        var monitor = new DDConnectionMonitor();
        monitor.damNameChanged("DBeaver: workstation");
        monitor.damNameLoaded(null);
        monitor.damNameChanged("DBeaver: workstation");
        Assertions.assertEquals(2, monitor.getEvents().size());
    }

    @Test
    void assigningTheFirstNameToAnUnnamedDamReportsARename() {
        var monitor = new DDConnectionMonitor();
        monitor.damNameChanged("Desktop");
        monitor.damNameLoaded("Desktop");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, monitor.getEvents().getFirst().type());
        Assertions.assertEquals("Desktop", monitor.getEvents().getFirst().details());
    }

    @Test
    void silentStateUpdatesKeepSnapshotAndOriginalFailureWithoutAddingEvents() {
        var monitor = new DDConnectionMonitor();
        monitor.failed("Offline");
        var events = monitor.getEvents();
        monitor.stateChanged(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CONNECTING, null), false);
        Assertions.assertEquals(DDRemoteDamChannelState.CONNECTING, monitor.snapshot().state());
        Assertions.assertEquals("Offline", monitor.snapshot().error());
        Assertions.assertEquals(events, monitor.getEvents());
        monitor.stateChanged(new DDRemoteDamChannelStatus(
            DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", "session")));
        Assertions.assertNull(monitor.snapshot().error());
        Assertions.assertEquals(DDConnectionMonitor.EventType.CONNECTED, monitor.getEvents().getLast().type());
    }

    @Test
    void recordsNameChangesWithoutRepeatingCachedOrReconnectedNames() {
        var monitor = new DDConnectionMonitor();
        monitor.damNameChanged(null);
        monitor.damNameChanged("Desktop");
        monitor.damNameChanged("Desktop");
        monitor.damNameChanged("Renamed desktop");
        monitor.clearEvents();
        monitor.damNameChanged("Renamed desktop");
        Assertions.assertTrue(monitor.getEvents().isEmpty());
        monitor.damNameChanged("New name");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, monitor.getEvents().getFirst().type());
        Assertions.assertEquals("New name", monitor.getEvents().getFirst().details());
    }

    @Test
    void responseEditorProjectionContainsOnlyTheValueAttribute() {
        String body = "{\"value\":{\"rows\":[1,2],\"code\":5},\"error\":false,\"message\":\"envelope metadata\"}";
        var response = new DDConnectionMonitor.McpResponse("OK", null, body);

        var value = JsonParser.parseString(response.valueText()).getAsJsonObject();

        Assertions.assertEquals(2, value.size());
        Assertions.assertEquals(2, value.getAsJsonArray("rows").size());
        Assertions.assertEquals(5, value.get("code").getAsInt());
        Assertions.assertFalse(value.has("message"));
        Assertions.assertEquals(body, response.body());
    }

    @Test
    void responseEditorProjectionUnwrapsStringsAndFormatsArrays() {
        var text = new DDConnectionMonitor.McpResponse("OK", null, "{\"value\":\"first\\nsecond\",\"error\":false}");
        Assertions.assertEquals("first\nsecond", text.valueText());

        var array = new DDConnectionMonitor.McpResponse("OK", null, "{\"value\":[1,{\"name\":\"sample\"}],\"error\":false}");
        Assertions.assertEquals(JsonParser.parseString("[1,{\"name\":\"sample\"}]"), JsonParser.parseString(array.valueText()));
    }

    @Test
    void responseEditorProjectionPreservesPrimitiveAndExplicitNullValues() {
        Assertions.assertEquals("42", new DDConnectionMonitor.McpResponse("OK", null, "{\"value\":42}").valueText());
        Assertions.assertEquals("true", new DDConnectionMonitor.McpResponse("OK", null, "{\"value\":true}").valueText());
        Assertions.assertEquals("null", new DDConnectionMonitor.McpResponse("OK", null, "{\"value\":null}").valueText());
    }

    @Test
    void responseEditorProjectionDisplaysResponsesWithoutAValueEnvelope() {
        for (String body : new String[] {
            "{\"code\":\"INVALID_ARGUMENTS\",\"message\":\"failed\"}",
            "[{\"id\":\"listProjects\",\"name\":\"List projects\"}]",
            "[]", "{\"projects\":[]}", "42", "true", "null"
        }) {
            var response = new DDConnectionMonitor.McpResponse("OK", null, body);
            Assertions.assertEquals(JsonParser.parseString(body), JsonParser.parseString(response.valueText()));
            Assertions.assertEquals(body, response.body());
        }
        Assertions.assertEquals("failed", new DDConnectionMonitor.McpResponse("INTERNAL_ERROR", "failed", "\"failed\"").valueText());
        Assertions.assertEquals("{invalid", new DDConnectionMonitor.McpResponse("OK", null, "{invalid").valueText());
    }

    @Test
    void completesTheOriginalRowWithoutChangingItsPositionOrEarlierSnapshots() {
        var clock = new AtomicLong(100);
        var monitor = new DDConnectionMonitor(clock::get);
        var request = monitor.beginMcpRequest("query", Map.of("query", "select 1"));
        monitor.record(DDConnectionMonitor.EventType.CONNECTED, "connected");
        var second = monitor.beginMcpRequest("another query", null);
        var pending = monitor.getEvents();
        Assertions.assertTrue(request.isPending());
        Assertions.assertFalse(request.isError());

        clock.set(2_500_100);
        monitor.recordMcpResponse(request, "OK", null, "result");

        var events = monitor.getEvents();
        Assertions.assertEquals(3, events.size());
        var completed = events.getFirst();
        Assertions.assertEquals(request.requestId(), completed.requestId());
        Assertions.assertEquals(request.time(), completed.time());
        Assertions.assertEquals(request.details(), completed.details());
        Assertions.assertSame(request.request(), completed.request());
        Assertions.assertFalse(completed.isPending());
        Assertions.assertFalse(completed.isError());
        Assertions.assertEquals(Duration.ofNanos(2_500_000), completed.response().processingTime());
        Assertions.assertTrue(completed.fullDetails().contains("select 1"));
        Assertions.assertTrue(completed.fullDetails().endsWith("result"));
        Assertions.assertTrue(pending.getFirst().isPending());
        Assertions.assertSame(second, events.getLast());

        monitor.recordMcpResponse(second, "INTERNAL_ERROR", null, "null");
        Assertions.assertTrue(monitor.getRequestEvent(second.requestId()).isError());
        Assertions.assertFalse(monitor.getRequestEvent(second.requestId()).isPending());
    }

    @Test
    void clearingPendingRequestsDoesNotResurrectThemWhenResponsesArrive() {
        var monitor = new DDConnectionMonitor();
        var first = monitor.beginMcpRequest("first", null);
        monitor.clearEvents();
        monitor.recordMcpResponse(first, "OK", null, "result");
        Assertions.assertTrue(monitor.getEvents().isEmpty());

        var next = monitor.beginMcpRequest("next", null);
        Assertions.assertNotEquals(first.requestId(), next.requestId());
        monitor.recordMcpResponse(first, "ERROR", "late error", "error");
        Assertions.assertSame(next, monitor.getEvents().getFirst());
        Assertions.assertTrue(next.isPending());
    }

    @Test
    void duplicateResponsesDoNotOverwriteTheCompletedRow() {
        var monitor = new DDConnectionMonitor();
        var request = monitor.beginMcpRequest("query", null);
        monitor.recordMcpResponse(request, "OK", null, "first result");
        var completed = monitor.getRequestEvent(request.requestId());
        monitor.recordMcpResponse(request, "ERROR", "late error", "second result");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertSame(completed, monitor.getRequestEvent(request.requestId()));
    }

    @Test
    void evictsOlderResponsesInsteadOfAccumulatingLargePayloads() {
        var monitor = new DDConnectionMonitor();
        var request = monitor.beginMcpRequest("query", null);
        monitor.recordMcpResponse(request, "OK", null, "a".repeat(6_000_000));
        var second = monitor.beginMcpRequest("second query", null);
        String body = "b".repeat(6_000_000);
        monitor.recordMcpResponse(second, "OK", null, body);

        var events = monitor.getEvents();
        Assertions.assertEquals(1, events.size());
        Assertions.assertSame(body, events.getFirst().response().body());
        Assertions.assertEquals("second query", events.getFirst().details());
        Assertions.assertTrue(events.getFirst().fullDetails().endsWith(body));
    }

    @Test
    void keepsOneOversizedResponseCompleteUntilTheNextEvent() {
        var monitor = new DDConnectionMonitor();
        var request = monitor.beginMcpRequest("query", null);
        monitor.beginMcpRequest("another pending query", null);
        String body = "x".repeat(10_000_001);
        monitor.recordMcpResponse(request, "OK", null, body);
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertSame(body, monitor.getEvents().getFirst().response().body());

        monitor.record(DDConnectionMonitor.EventType.CONNECTED, "connected");
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.CONNECTED, monitor.getEvents().getFirst().type());

        monitor.clearEvents();
        var next = monitor.beginMcpRequest("next query", null);
        monitor.recordMcpResponse(next, "OK", null, body);
        Assertions.assertSame(body, monitor.getEvents().getFirst().response().body());
    }

    @Test
    void retainsFinalConnectionStatisticsUntilTheNextSocketOpens() {
        var monitor = new DDConnectionMonitor();
        monitor.opened();
        monitor.sent(321);
        monitor.received(654);
        monitor.failed("Connection lost");
        monitor.disconnected();
        var finalSnapshot = monitor.snapshot();
        Assertions.assertNotNull(finalSnapshot.connectedAt());
        Assertions.assertEquals(321, finalSnapshot.bytesSent());
        Assertions.assertEquals(654, finalSnapshot.bytesReceived());
        Assertions.assertEquals(finalSnapshot.duration(), monitor.snapshot().duration());
        monitor.clearEvents();
        Assertions.assertEquals(finalSnapshot, monitor.snapshot());

        monitor.opened();
        Assertions.assertEquals(0, monitor.snapshot().bytesSent());
        Assertions.assertEquals(0, monitor.snapshot().bytesReceived());
        Assertions.assertEquals("Connection lost", monitor.snapshot().error());
        monitor.stateChanged(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CONNECTED,
            new DDRemoteDamSessionInfo("deployment", "instance", "session")));
        Assertions.assertNull(monitor.snapshot().error());
    }

    @Test
    void recoveryClearsOnlyTheMatchingErrorAndKeepsHistoricalEvents() {
        var monitor = new DDConnectionMonitor();
        monitor.failed("RPC initialization failed");
        monitor.clearError("RPC initialization failed");
        Assertions.assertNull(monitor.snapshot().error());
        Assertions.assertEquals(1, monitor.getEvents().size());

        monitor.failed("WebSocket closed");
        monitor.clearError("RPC initialization failed");
        Assertions.assertEquals("WebSocket closed", monitor.snapshot().error());
        Assertions.assertEquals(2, monitor.getEvents().size());
    }

    @Test
    void boundsEventsAndDetailsAndReturnsImmutableSnapshots() {
        var monitor = new DDConnectionMonitor();
        var request = monitor.beginMcpRequest("oldest", null);
        var original = monitor.getEvents();
        for (int i = 0; i < 10_000; i++) {
            monitor.record(DDConnectionMonitor.EventType.ERROR, Integer.toString(i));
        }
        var events = monitor.getEvents();
        Assertions.assertEquals(10_000, events.size());
        Assertions.assertEquals("0", events.getFirst().details());
        Assertions.assertEquals("oldest", original.getFirst().details());
        monitor.recordMcpResponse(request, "OK", null, "late response");
        Assertions.assertEquals(events, monitor.getEvents());
        Assertions.assertNull(monitor.getRequestEvent(request.requestId()));
        Assertions.assertThrows(UnsupportedOperationException.class, events::clear);
        monitor.record(DDConnectionMonitor.EventType.ERROR, "x".repeat(100_001));
        Assertions.assertEquals(10_000, monitor.getEvents().size());
        Assertions.assertEquals("1", monitor.getEvents().getFirst().details());
        Assertions.assertEquals(100_000, monitor.getEvents().getLast().details().length());
        monitor.clearEvents();
        Assertions.assertTrue(monitor.getEvents().isEmpty());
        Assertions.assertEquals(10_000, events.size());
    }
}
