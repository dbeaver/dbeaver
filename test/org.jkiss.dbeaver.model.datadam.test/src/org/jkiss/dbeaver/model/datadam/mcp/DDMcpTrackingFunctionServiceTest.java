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
import com.google.gson.JsonParser;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    void recordsParametersBeforeExecutionWithoutRetainingResults() throws Exception {
        var arguments = new HashMap<String, Object>();
        arguments.put("query", "select 'example'\nfrom sample");
        arguments.put("options", Map.of("limit", 10, "columns", List.of("id", "name")));
        var call = new DDMcpFunctionCall("query", arguments);
        var result = new DDMcpFunctionResult(new JsonPrimitive("result-secret"), false);
        Mockito.when(delegate.callFunction(call)).thenAnswer(invocation -> {
            Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
            Assertions.assertEquals("select 'example'\nfrom sample", JsonParser.parseString(
                details(0).substring(details(0).indexOf('\n'))).getAsJsonObject().get("query").getAsString());
            arguments.clear();
            return result;
        });

        Assertions.assertSame(result, service.callFunction(call));
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertTrue(details(0).startsWith("callFunction: query\n\n{\n"));
        var parameters = JsonParser.parseString(details(0).substring(details(0).indexOf('\n'))).getAsJsonObject();
        Assertions.assertEquals(10, parameters.getAsJsonObject("options").get("limit").getAsInt());
        Assertions.assertEquals(2, parameters.getAsJsonObject("options").getAsJsonArray("columns").size());
        var request = monitor.getEvents().getFirst().request();
        Assertions.assertNotNull(request);
        Assertions.assertEquals("select 'example'\nfrom sample", request.parameters().get("query"));
        Assertions.assertEquals(parameters.get("options"), JsonParser.parseString(request.parameters().get("options")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> request.parameters().clear());
        Assertions.assertFalse(monitor.getEvents().toString().contains("result-secret"));
        Mockito.verify(delegate).callFunction(call);
    }

    @Test
    void boundsParameterDetailsWithoutChangingTheCall() throws Exception {
        var call = new DDMcpFunctionCall("query", Map.of("query", "x".repeat(100_001)));
        var result = new DDMcpFunctionResult(new JsonPrimitive("ok"), false);
        Mockito.when(delegate.callFunction(call)).thenReturn(result);

        Assertions.assertSame(result, service.callFunction(call));
        Assertions.assertEquals(100_000, details(0).length());
        Assertions.assertTrue(details(0).startsWith("callFunction: query\n\n{\n"));
        Mockito.verify(delegate).callFunction(call);
    }

    @Test
    void preservesConnectionIdentifiersAndParameterRowsWhenDetailsAreTruncated() throws Exception {
        var arguments = new LinkedHashMap<String, Object>();
        arguments.put("query", "x".repeat(200_000));
        arguments.put("project_id", "project-one");
        arguments.put("datasource_id", "connection-one");
        arguments.put("optional", null);
        var call = new DDMcpFunctionCall("query", arguments);
        Mockito.when(delegate.callFunction(call)).thenReturn(new DDMcpFunctionResult(new JsonPrimitive("ok"), false));

        service.callFunction(call);
        var event = monitor.getEvents().getFirst();
        var request = event.request();
        Assertions.assertNotNull(request);
        Assertions.assertEquals("callFunction: query", request.operation());
        Assertions.assertEquals("project-one", request.projectId());
        Assertions.assertEquals("connection-one", request.datasourceId());
        Assertions.assertEquals(arguments.keySet(), request.parameters().keySet());
        Assertions.assertEquals("project-one", request.parameters().get("project_id"));
        Assertions.assertEquals("connection-one", request.parameters().get("datasource_id"));
        Assertions.assertEquals("null", request.parameters().get("optional"));
        Assertions.assertTrue(request.parameters().entrySet().stream()
            .mapToInt(parameter -> parameter.getKey().length() + parameter.getValue().length()).sum() <= 100_000);
        Assertions.assertEquals(100_000, event.details().length());
    }

    @Test
    void tracksFunctionDiscovery() throws Exception {
        Mockito.when(delegate.getFunctions()).thenReturn(List.of());
        Assertions.assertTrue(service.getFunctions().isEmpty());
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertEquals("getFunctions", details(0));
        Assertions.assertNull(monitor.getEvents().getFirst().request());
    }

    @Test
    void recordsOnlyRequestForErrorResults() throws Exception {
        var call = new DDMcpFunctionCall("query", Map.of());
        var result = new DDMcpFunctionResult(new JsonPrimitive("private-error-data"), true);
        Mockito.when(delegate.callFunction(call)).thenReturn(result);
        Assertions.assertSame(result, service.callFunction(call));
        Assertions.assertEquals(List.of(EventType.MCP_REQUEST), types());
        Assertions.assertEquals("callFunction: query\n\n{}", details(0));
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
        Assertions.assertEquals(Set.of("callFunction: first\n\n{}", "callFunction: second\n\n{}"), Set.of(details(0), details(1)));
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
