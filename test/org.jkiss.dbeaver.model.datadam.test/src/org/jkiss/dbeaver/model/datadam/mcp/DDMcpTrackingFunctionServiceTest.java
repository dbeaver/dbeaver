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
package org.jkiss.dbeaver.model.datadam.mcp;

import com.dbeaver.datadam.mcp.api.exception.DDMcpFunctionException;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionCall;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.service.desktop.DDMcpFunctionService;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor.EventType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Timeout(5)
class DDMcpTrackingFunctionServiceTest {
    private final DDMcpFunctionService delegate = Mockito.mock(DDMcpFunctionService.class);
    private final DDConnectionMonitor monitor = new DDConnectionMonitor();
    private final DDMcpTrackingFunctionService service = new DDMcpTrackingFunctionService(delegate, monitor);

    @Test
    void recordsRequestsBeforeExecutionAndDoesNotRetainArgumentsOrResults() throws Exception {
        var call = new DDMcpFunctionCall("query", Map.of("password", "argument-secret"));
        var result = new DDMcpFunctionResult(new JsonPrimitive("result-secret"), false);
        Mockito.when(delegate.callFunction(call)).thenAnswer(invocation -> {
            Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
            return result;
        });

        Assertions.assertSame(result, service.callFunction(call));
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertEquals("callFunction: query", details(0));
        Assertions.assertFalse(monitor.getEvents().toString().contains("argument-secret"));
        Assertions.assertFalse(monitor.getEvents().toString().contains("result-secret"));
        Mockito.verify(delegate).callFunction(call);
    }

    @Test
    void tracksFunctionDiscovery() throws Exception {
        Mockito.when(delegate.getFunctions()).thenReturn(List.of());
        Assertions.assertTrue(service.getFunctions().isEmpty());
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertEquals("getFunctions", details(0));
    }

    @Test
    void recordsOnlyRequestForErrorResults() throws Exception {
        var call = new DDMcpFunctionCall("query", Map.of());
        var result = new DDMcpFunctionResult(new JsonPrimitive("private-error-data"), true);
        Mockito.when(delegate.callFunction(call)).thenReturn(result);
        Assertions.assertSame(result, service.callFunction(call));
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertEquals("callFunction: query", details(0));
        Assertions.assertNull(monitor.snapshot().error());
        Assertions.assertFalse(monitor.getEvents().toString().contains("private-error-data"));
    }

    @Test
    void preservesCheckedAndUncheckedFailuresWithoutRecordingTheirSensitiveDetails() throws Exception {
        var checked = new DDMcpFunctionException(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, "private-error-message", new JsonObject());
        Mockito.when(delegate.getFunctions()).thenThrow(checked);
        Assertions.assertSame(checked, Assertions.assertThrows(DDMcpFunctionException.class, service::getFunctions));
        Assertions.assertEquals("getFunctions", details(0));
        var unchecked = new IllegalStateException("private-error-message");
        Mockito.doThrow(unchecked).when(delegate).getFunctions();
        Assertions.assertSame(unchecked, Assertions.assertThrows(IllegalStateException.class, service::getFunctions));
        Assertions.assertEquals("getFunctions", details(1));
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST, EventType.MCP_REQUEST), types());
        Assertions.assertFalse(monitor.getEvents().toString().contains("private-error-message"));
    }

    @Test
    void recordsConcurrentRequestsOnceEach() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        Mockito.when(delegate.callFunction(Mockito.any())).thenAnswer(invocation -> {
            entered.countDown();
            Assertions.assertTrue(release.await(1, TimeUnit.SECONDS));
            return new DDMcpFunctionResult(new JsonPrimitive("ok"), false);
        });
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> service.callFunction(new DDMcpFunctionCall("first", Map.of())));
            var second = executor.submit(() -> service.callFunction(new DDMcpFunctionCall("second", Map.of())));
            try {
                Assertions.assertTrue(entered.await(1, TimeUnit.SECONDS));
                Assertions.assertEquals(List.of(EventType.MCP_REQUEST, EventType.MCP_REQUEST), types());
            } finally {
                release.countDown();
            }
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        }
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST, EventType.MCP_REQUEST), types());
        Assertions.assertEquals(Set.of("callFunction: first", "callFunction: second"), Set.of(details(0), details(1)));
    }

    @Test
    void monitoringUnavailableDoesNotPreventTheRequest() throws Exception {
        try (var sessions = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            sessions.when(DDRemoteDamDesktopSessionService::getInstance).thenThrow(new DBException("Unavailable"));
            Mockito.when(delegate.getFunctions()).thenReturn(List.of());
            Assertions.assertTrue(new DDMcpTrackingFunctionService(delegate).getFunctions().isEmpty());
            Mockito.verify(delegate).getFunctions();
        }
    }

    @NotNull
    private List<EventType> types() {
        return monitor.getEvents().stream().map(DDConnectionMonitor.Event::type).toList();
    }

    @NotNull
    private String details(int index) {
        return monitor.getEvents().get(index).details();
    }
}
