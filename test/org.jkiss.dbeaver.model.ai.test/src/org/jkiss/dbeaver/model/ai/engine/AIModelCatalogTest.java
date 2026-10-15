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
package org.jkiss.dbeaver.model.ai.engine;

import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.function.ThrowableFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

class AIModelCatalogTest extends DBeaverUnitTest {
    private static final String TEST_CATALOG_JSON = """
        {
          "update_time":"2026-10-13T12:49:59Z",
          "api":{
            "openai":{
              "models":{
                "gpt-test":{
                  "limit":{"context":400000,"input":272000,"output":128000},
                  "temperature":false,
                  "tool_call":true,
                  "reasoning":true
                }
              }
            },
            "anthropic":{
              "models":{
                "claude-test":{"limit":{"context":200000}}
              }
            }
          }
        }
        """;

    @TempDir
    Path directory;
    private Clock clock;
    private AtomicLong time;
    private ThrowableFunction<DBRProgressMonitor, String, Exception> jsonLoader;
    private Path cacheFile;

    @BeforeEach
    void setUp() throws Exception {
        time = new AtomicLong(Instant.parse("2026-09-11T12:00:00Z").toEpochMilli());
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.millis()).thenAnswer(invocation -> time.get());
        // inject synthetic JSON so cache tests do not depend on network or catalog updates
        jsonLoader = Mockito.mock(ThrowableFunction.class);
        Mockito.when(jsonLoader.apply(Mockito.any())).thenReturn(TEST_CATALOG_JSON);
        cacheFile = directory.resolve("ai-model-catalog-dbeaver.com.json");
    }

    @Test
    void parsesInjectedJsonAndReusesSnapshotForAllProviders() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);

        Map<String, AIModelCatalogEntry> openAiModels = catalog.getModels("openai");
        Assertions.assertEquals(Set.of("gpt-test"), openAiModels.keySet());
        AIModelCatalogEntry openAiModel = openAiModels.get("gpt-test");
        Assertions.assertEquals(400_000, openAiModel.limit().context());
        Assertions.assertEquals(272_000, openAiModel.limit().input());
        Assertions.assertEquals(128_000, openAiModel.limit().output());
        Assertions.assertEquals(Boolean.FALSE, openAiModel.temperature());
        Assertions.assertEquals(Boolean.TRUE, openAiModel.toolCall());
        Assertions.assertEquals(Boolean.TRUE, openAiModel.reasoning());

        Map<String, AIModelCatalogEntry> anthropicModels = catalog.getModels("anthropic");
        Assertions.assertEquals(Set.of("claude-test"), anthropicModels.keySet());
        Assertions.assertEquals(200_000, anthropicModels.get("claude-test").limit().context());
        Assertions.assertTrue(catalog.getModels("missing-provider").isEmpty());
        Assertions.assertTrue(Files.exists(cacheFile));
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void restartsUseDiskSnapshotUntilWeeklyDeadline() throws Exception {
        new AIModelCatalog(cacheFile, clock, jsonLoader).getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis() - 1);
        AIModelCatalog restarted = new AIModelCatalog(cacheFile, clock, jsonLoader);

        Assertions.assertEquals(400_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());

        Mockito.when(jsonLoader.apply(Mockito.any())).thenReturn(TEST_CATALOG_JSON.replace("400000", "500000"));
        time.incrementAndGet();

        Assertions.assertEquals(500_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(500_000,
            new AIModelCatalog(cacheFile, clock, jsonLoader).getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(jsonLoader, Mockito.times(2)).apply(Mockito.any());
    }

    @Test
    void failedRefreshKeepsOldSnapshotAndPersistsRetryDeadline() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis());
        Mockito.when(jsonLoader.apply(Mockito.any())).thenThrow(new IOException("offline"));

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        AIModelCatalog restarted = new AIModelCatalog(cacheFile, clock, jsonLoader);
        Assertions.assertEquals(400_000, restarted.getModels("openai").get("gpt-test").limit().context());
        time.addAndGet(Duration.ofDays(1).toMillis() - 1);
        restarted.getModels("openai");
        Mockito.verify(jsonLoader, Mockito.times(2)).apply(Mockito.any());

        Mockito.doReturn(TEST_CATALOG_JSON.replace("400000", "600000")).when(jsonLoader).apply(Mockito.any());
        time.incrementAndGet();
        Assertions.assertEquals(600_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(jsonLoader, Mockito.times(3)).apply(Mockito.any());
    }

    @Test
    void firstDownloadFailureReturnsEmptyAndDoesNotRetryOnEveryCallOrRestart() throws Exception {
        Mockito.when(jsonLoader.apply(Mockito.any())).thenThrow(new IOException("offline"));
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);

        Assertions.assertTrue(catalog.getModels("openai").isEmpty());
        Assertions.assertTrue(catalog.getModels("openai").isEmpty());
        Assertions.assertTrue(new AIModelCatalog(cacheFile, clock, jsonLoader).getModels("openai").isEmpty());
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void malformedOrEmptyDownloadsDoNotReplaceGoodMetadata() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis());
        for (String response : new String[]{
            "<html>unavailable</html>", "{}", "null", "{\"api\":null}", "{\"api\":{}}", "{\"api\":{\"openai\":{}}}"
        }) {
            Mockito.when(jsonLoader.apply(Mockito.any())).thenReturn(response);
            Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
            Assertions.assertEquals(400_000,
                new AIModelCatalog(cacheFile, clock, jsonLoader).getCachedModels("openai").get("gpt-test").limit().context());
            time.addAndGet(Duration.ofDays(1).toMillis());
        }
        Mockito.verify(jsonLoader, Mockito.times(7)).apply(Mockito.any());
    }

    @Test
    void corruptDiskCacheCanBeReplacedByValidDownload() throws Exception {
        Files.writeString(cacheFile, "{corrupt cache");
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(400_000,
            new AIModelCatalog(cacheFile, clock, jsonLoader).getCachedModels("openai").get("gpt-test").limit().context());
    }

    @Test
    void diskWriteFailureStillUsesMemoryCacheWithoutRepeatedDownloads() throws Exception {
        Path notDirectory = directory.resolve("file");
        Files.writeString(notDirectory, "not a directory");
        AIModelCatalog catalog = new AIModelCatalog(notDirectory.resolve("models.json"), clock, jsonLoader);

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void cachedLookupNeverDownloadsEvenAfterExpiration() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);
        Assertions.assertTrue(catalog.getCachedModels("openai").isEmpty());
        Mockito.verifyNoInteractions(jsonLoader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(30).toMillis());

        Assertions.assertEquals(400_000,
            new AIModelCatalog(cacheFile, clock, jsonLoader).getCachedModels("openai").get("gpt-test").limit().context());
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void concurrentRequestsLoadInjectedJsonOnlyOnce() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var requests = new ArrayList<Callable<Integer>>();
            for (int index = 0; index < 12; index++) {
                requests.add(() -> catalog.getModels("openai").get("gpt-test").limit().context());
            }
            for (var result : executor.invokeAll(requests)) {
                Assertions.assertEquals(400_000, result.get());
            }
        }
        Mockito.verify(jsonLoader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void interruptionUsesFallbackAndPreservesInterruptFlag() throws Exception {
        Mockito.when(jsonLoader.apply(Mockito.any())).thenThrow(new InterruptedException("cancelled"));
        try {
            Assertions.assertTrue(new AIModelCatalog(null, clock, jsonLoader).getModels("openai").isEmpty());
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void canceledMonitorDoesNotStartDownloadOrCreateCache() throws Exception {
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Mockito.verifyNoInteractions(jsonLoader);
        Assertions.assertFalse(Files.exists(cacheFile));
    }

    @Test
    void requestsDBeaverCatalogUrlAndDecodesMockedGzipResponse() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<byte[]> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(compress(TEST_CATALOG_JSON));
        Mockito.when(client.sendAsync(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(CompletableFuture.completedFuture(response));
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, progress -> AIModelCatalog.download(progress, client));

        Assertions.assertEquals(400_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(client).sendAsync(request.capture(), Mockito.<HttpResponse.BodyHandler<byte[]>>any());
        Assertions.assertEquals("GET", request.getValue().method());
        Assertions.assertTrue(List.of(
            "https://dbeaver.com/product/ai_models_api.json.gz",
            "https://stage-website-com.beavers.team/product/ai_models_api.json.gz"
        ).contains(request.getValue().uri().toString()));
    }

    @Test
    void corruptGzipKeepsCachedSnapshot() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<byte[]> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(compress(TEST_CATALOG_JSON), new byte[]{0x1f, (byte) 0x8b, 0x00});
        Mockito.when(client.sendAsync(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenAnswer(invocation -> CompletableFuture.completedFuture(response));
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, progress -> AIModelCatalog.download(progress, client));
        catalog.getModels(monitor, "openai");
        time.addAndGet(Duration.ofDays(7).toMillis());

        Assertions.assertEquals(400_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
        Assertions.assertEquals(400_000,
            new AIModelCatalog(cacheFile, clock, jsonLoader).getCachedModels("openai").get("gpt-test").limit().context());
    }

    @Test
    void mockedHttpFailureDoesNotDecodeResponseBody() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<byte[]> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(503);
        Mockito.when(client.sendAsync(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        IOException exception = Assertions.assertThrows(IOException.class, () -> AIModelCatalog.download(monitor, client));
        Assertions.assertEquals("AI model catalog returned HTTP 503", exception.getMessage());
        Mockito.verify(response, Mockito.never()).body();
    }

    @Test
    void cancellationDuringDownloadCancelsHttpFutureAndAllowsImmediateRetry() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        CompletableFuture<HttpResponse<byte[]>> pending = new CompletableFuture<>();
        HttpResponse<byte[]> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(compress(TEST_CATALOG_JSON));
        Mockito.when(client.sendAsync(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(pending, CompletableFuture.completedFuture(response));
        Mockito.when(monitor.isCanceled()).thenReturn(false, false, false, true);
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, progress -> AIModelCatalog.download(progress, client));

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Assertions.assertTrue(pending.isCancelled());
        Assertions.assertFalse(Files.exists(cacheFile));
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        Assertions.assertEquals(400_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
        Mockito.verify(client, Mockito.times(2)).sendAsync(
            Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<byte[]>>any());
    }

    @Test
    void cancellationAfterDownloadKeepsMemoryAndDiskSnapshot() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, jsonLoader);
        catalog.getModels(monitor, "openai");
        final String snapshot = Files.readString(cacheFile);
        time.addAndGet(Duration.ofDays(7).toMillis());
        Mockito.when(jsonLoader.apply(monitor)).thenAnswer(invocation -> {
            Mockito.when(monitor.isCanceled()).thenReturn(true);
            return TEST_CATALOG_JSON.replace("400000", "500000");
        });

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Assertions.assertEquals(400_000, catalog.getCachedModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(snapshot, Files.readString(cacheFile));
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        Mockito.doReturn(TEST_CATALOG_JSON.replace("400000", "500000")).when(jsonLoader).apply(monitor);
        Assertions.assertEquals(500_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
    }

    @NotNull
    private static byte[] compress(@NotNull String json) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}
