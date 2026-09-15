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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.function.ThrowableFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

class AIModelCatalogTest extends DBeaverUnitTest {
    private static final String CATALOG = """
        {
          "openai":{"models":{"gpt-test":{"limit":{"context":400000,"input":272000,"output":128000},
            "temperature":false,"tool_call":true,"reasoning":true}}},
          "anthropic":{"models":{"claude-test":{"limit":{"context":200000}}}}
        }
        """;

    @TempDir
    Path directory;
    private Clock clock;
    private AtomicLong time;
    private ThrowableFunction<DBRProgressMonitor, String, Exception> loader;
    private Path cacheFile;

    @BeforeEach
    void setUp() throws Exception {
        time = new AtomicLong(Instant.parse("2026-09-11T12:00:00Z").toEpochMilli());
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.millis()).thenAnswer(invocation -> time.get());
        loader = Mockito.mock(ThrowableFunction.class);
        Mockito.when(loader.apply(Mockito.any())).thenReturn(CATALOG);
        cacheFile = directory.resolve("models-dev.json");
    }

    @Test
    void downloadsOnceAndSharesSnapshotBetweenProviders() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(200_000, catalog.getModels("anthropic").get("claude-test").limit().context());
        Assertions.assertTrue(catalog.getModels("missing-provider").isEmpty());
        Assertions.assertTrue(Files.exists(cacheFile));
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void restartsUseDiskSnapshotUntilWeeklyDeadline() throws Exception {
        new AIModelCatalog(cacheFile, clock, loader).getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis() - 1);
        AIModelCatalog restarted = new AIModelCatalog(cacheFile, clock, loader);

        Assertions.assertEquals(400_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());

        Mockito.when(loader.apply(Mockito.any())).thenReturn(CATALOG.replace("400000", "500000"));
        time.incrementAndGet();

        Assertions.assertEquals(500_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(500_000,
            new AIModelCatalog(cacheFile, clock, loader).getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(loader, Mockito.times(2)).apply(Mockito.any());
    }

    @Test
    void failedRefreshKeepsOldSnapshotAndPersistsRetryDeadline() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis());
        Mockito.when(loader.apply(Mockito.any())).thenThrow(new IOException("offline"));

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        AIModelCatalog restarted = new AIModelCatalog(cacheFile, clock, loader);
        Assertions.assertEquals(400_000, restarted.getModels("openai").get("gpt-test").limit().context());
        time.addAndGet(Duration.ofDays(1).toMillis() - 1);
        restarted.getModels("openai");
        Mockito.verify(loader, Mockito.times(2)).apply(Mockito.any());

        Mockito.doReturn(CATALOG.replace("400000", "600000")).when(loader).apply(Mockito.any());
        time.incrementAndGet();
        Assertions.assertEquals(600_000, restarted.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(loader, Mockito.times(3)).apply(Mockito.any());
    }

    @Test
    void firstDownloadFailureReturnsEmptyAndDoesNotRetryOnEveryCallOrRestart() throws Exception {
        Mockito.when(loader.apply(Mockito.any())).thenThrow(new IOException("offline"));
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);

        Assertions.assertTrue(catalog.getModels("openai").isEmpty());
        Assertions.assertTrue(catalog.getModels("openai").isEmpty());
        Assertions.assertTrue(new AIModelCatalog(cacheFile, clock, loader).getModels("openai").isEmpty());
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void malformedOrEmptyDownloadsDoNotReplaceGoodMetadata() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(7).toMillis());
        for (String response : new String[]{"<html>unavailable</html>", "{}", "null", "{\"openai\":{}}"}) {
            Mockito.when(loader.apply(Mockito.any())).thenReturn(response);
            Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
            Assertions.assertEquals(400_000,
                new AIModelCatalog(cacheFile, clock, loader).getCachedModels("openai").get("gpt-test").limit().context());
            time.addAndGet(Duration.ofDays(1).toMillis());
        }
        Mockito.verify(loader, Mockito.times(5)).apply(Mockito.any());
    }

    @Test
    void corruptDiskCacheCanBeReplacedByValidDownload() throws Exception {
        Files.writeString(cacheFile, "{corrupt cache");
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(400_000,
            new AIModelCatalog(cacheFile, clock, loader).getCachedModels("openai").get("gpt-test").limit().context());
    }

    @Test
    void diskWriteFailureStillUsesMemoryCacheWithoutRepeatedDownloads() throws Exception {
        Path notDirectory = directory.resolve("file");
        Files.writeString(notDirectory, "not a directory");
        AIModelCatalog catalog = new AIModelCatalog(notDirectory.resolve("models.json"), clock, loader);

        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(400_000, catalog.getModels("openai").get("gpt-test").limit().context());
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void cachedLookupNeverDownloadsEvenAfterExpiration() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);
        Assertions.assertTrue(catalog.getCachedModels("openai").isEmpty());
        Mockito.verifyNoInteractions(loader);
        catalog.getModels("openai");
        time.addAndGet(Duration.ofDays(30).toMillis());

        Assertions.assertEquals(400_000,
            new AIModelCatalog(cacheFile, clock, loader).getCachedModels("openai").get("gpt-test").limit().context());
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void concurrentRequestsDownloadOnlyOneSnapshot() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var requests = new ArrayList<Callable<Integer>>();
            for (int index = 0; index < 12; index++) {
                requests.add(() -> catalog.getModels("openai").get("gpt-test").limit().context());
            }
            for (var result : executor.invokeAll(requests)) {
                Assertions.assertEquals(400_000, result.get());
            }
        }
        Mockito.verify(loader, Mockito.times(1)).apply(Mockito.any());
    }

    @Test
    void interruptionUsesFallbackAndPreservesInterruptFlag() throws Exception {
        Mockito.when(loader.apply(Mockito.any())).thenThrow(new InterruptedException("cancelled"));
        try {
            Assertions.assertTrue(new AIModelCatalog(null, clock, loader).getModels("openai").isEmpty());
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void canceledMonitorDoesNotStartDownloadOrCreateCache() throws Exception {
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Mockito.verifyNoInteractions(loader);
        Assertions.assertFalse(Files.exists(cacheFile));
    }

    @Test
    void cancellationDuringDownloadCancelsHttpFutureAndAllowsImmediateRetry() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(CATALOG);
        Mockito.when(client.sendAsync(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(pending, CompletableFuture.completedFuture(response));
        Mockito.when(monitor.isCanceled()).thenReturn(false, false, false, true);
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, progress -> AIModelCatalog.download(progress, client));

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Assertions.assertTrue(pending.isCancelled());
        Assertions.assertFalse(Files.exists(cacheFile));
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        Assertions.assertEquals(400_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
        Mockito.verify(client, Mockito.times(2)).sendAsync(
            Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void cancellationAfterDownloadKeepsMemoryAndDiskSnapshot() throws Exception {
        AIModelCatalog catalog = new AIModelCatalog(cacheFile, clock, loader);
        catalog.getModels(monitor, "openai");
        final String snapshot = Files.readString(cacheFile);
        time.addAndGet(Duration.ofDays(7).toMillis());
        Mockito.when(loader.apply(monitor)).thenAnswer(invocation -> {
            Mockito.when(monitor.isCanceled()).thenReturn(true);
            return CATALOG.replace("400000", "500000");
        });

        Assertions.assertThrows(OperationCanceledException.class, () -> catalog.getModels(monitor, "openai"));
        Assertions.assertEquals(400_000, catalog.getCachedModels("openai").get("gpt-test").limit().context());
        Assertions.assertEquals(snapshot, Files.readString(cacheFile));
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        Mockito.doReturn(CATALOG.replace("400000", "500000")).when(loader).apply(monitor);
        Assertions.assertEquals(500_000, catalog.getModels(monitor, "openai").get("gpt-test").limit().context());
    }
}
