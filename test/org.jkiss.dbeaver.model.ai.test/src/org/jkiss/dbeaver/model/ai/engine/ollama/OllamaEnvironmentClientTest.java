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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class OllamaEnvironmentClientTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingHostsProduceConfigurationErrors(@Nullable String host) throws DBException {
        OllamaProperties properties = withEnvironment(Map.of());
        properties.setHostname(host);
        for (boolean useEnvironment : new boolean[] {false, true}) {
            properties.setUseEnvVariables(useEnvironment);
            try (OllamaCompletionEngine engine = new OllamaCompletionEngine(properties)) {
                DBException error = Assertions.assertThrows(
                    DBException.class, () -> engine.getModels(Mockito.mock(DBRProgressMonitor.class)));
                Assertions.assertEquals("Ollama host is not configured", error.getMessage());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void modelDiscoveryUsesConfiguredHostWithoutSelectedModel(boolean useEnvironment) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/tags", exchange -> {
            byte[] response = "{\"models\":[{\"name\":\"llama3\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String hostname = "http://127.0.0.1:" + server.getAddress().getPort();
            Map<String, String> environment = Map.of(OllamaProperties.ENV_HOST, hostname);
            OllamaProperties properties = withEnvironment(environment);
            if (!useEnvironment) {
                properties.setHostname(hostname);
            }
            properties.setUseEnvVariables(useEnvironment);
            Assertions.assertNull(properties.getModel());
            try (OllamaCompletionEngine engine = new OllamaCompletionEngine(properties)) {
                var models = engine.getModels(Mockito.mock(DBRProgressMonitor.class));
                Assertions.assertEquals(1, models.size());
                Assertions.assertEquals("llama3", models.getFirst().name());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void cloudRequestsIncludeBearerToken() {
        try (OllamaClient client = new OllamaClient("https://ollama.com", "cloud-key")) {
            var request = client.newRequestBuilder().uri(URI.create("https://ollama.com/api/tags")).build();
            Assertions.assertEquals("Bearer cloud-key", request.headers().firstValue("Authorization").orElseThrow());
        }
    }

    @Test
    void localRequestsDoNotAddAuthorization() {
        try (OllamaClient client = new OllamaClient("http://localhost:11434")) {
            var request = client.newRequestBuilder().uri(URI.create("http://localhost:11434/api/tags")).build();
            Assertions.assertTrue(request.headers().firstValue("Authorization").isEmpty());
        }
    }

    @NotNull
    private static OllamaProperties withEnvironment(@NotNull Map<String, String> environment) {
        return Mockito.mock(OllamaProperties.class,
            Mockito.withSettings().spiedInstance(new OllamaProperties()).defaultAnswer(invocation ->
                "getEnvironmentVariable".equals(invocation.getMethod().getName())
                    ? environment.get(invocation.getArgument(0)) : invocation.callRealMethod()));
    }
}
