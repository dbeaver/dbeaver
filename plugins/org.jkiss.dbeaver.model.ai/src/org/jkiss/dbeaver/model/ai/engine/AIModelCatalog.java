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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.internal.AIActivator;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.function.ThrowableFunction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class AIModelCatalog {
    private static final Log log = Log.getLog(AIModelCatalog.class);
    private static final URI CATALOG_URI = URI.create("https://models.dev/api.json");
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(7);
    private static final Duration RETRY_INTERVAL = Duration.ofDays(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final Gson GSON = new Gson();
    private static final TypeToken<Map<String, Provider>> PROVIDERS_TYPE = new TypeToken<>() {};
    private static final ThreadLocal<AIModelCatalog> testInstance = new ThreadLocal<>();

    @Nullable
    private final Path cacheFile;
    private final Clock clock;
    private final ThrowableFunction<DBRProgressMonitor, String, Exception> loader;
    private volatile Map<String, Provider> providers = Map.of();
    private long nextRefreshAt;
    private volatile boolean initialized;

    @ForTest
    public AIModelCatalog(
        @Nullable Path cacheFile,
        @NotNull Clock clock,
        @NotNull ThrowableFunction<DBRProgressMonitor, String, Exception> loader
    ) {
        this.cacheFile = cacheFile;
        this.clock = clock;
        this.loader = loader;
    }

    @NotNull
    public static AIModelCatalog getInstance() {
        AIModelCatalog catalog = testInstance.get();
        return catalog == null ? InstanceHolder.INSTANCE : catalog;
    }

    @ForTest
    @NotNull
    public static AutoCloseable useForTests(@NotNull AIModelCatalog catalog) {
        AIModelCatalog previous = testInstance.get();
        testInstance.set(catalog);
        return () -> {
            if (previous == null) {
                testInstance.remove();
            } else {
                testInstance.set(previous);
            }
        };
    }

    @ForTest
    @NotNull
    public static AutoCloseable useForTests(@NotNull Map<String, Map<String, AIModelCatalogEntry>> models) {
        AIModelCatalog catalog = new AIModelCatalog(null, Clock.systemUTC(), monitor -> {
            throw new AssertionError("Unexpected catalog download in fixture");
        });
        Map<String, Provider> providers = new HashMap<>();
        models.forEach((id, entries) -> providers.put(id, new Provider(Map.copyOf(entries))));
        catalog.providers = Map.copyOf(providers);
        catalog.initialized = true;
        catalog.nextRefreshAt = Long.MAX_VALUE;
        return useForTests(catalog);
    }

    @NotNull
    public Map<String, AIModelCatalogEntry> getModels(@NotNull String providerId) {
        return getModels(new VoidProgressMonitor(), providerId);
    }

    @NotNull
    public synchronized Map<String, AIModelCatalogEntry> getModels(@NotNull DBRProgressMonitor monitor, @NotNull String providerId) {
        checkCanceled(monitor);
        loadCache();
        long now = clock.millis();
        if (now >= nextRefreshAt) {
            try {
                Map<String, Provider> downloaded = readProviders(loader.apply(monitor));
                checkCanceled(monitor);
                providers = downloaded;
                nextRefreshAt = clock.millis() + REFRESH_INTERVAL.toMillis();
            } catch (OperationCanceledException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("AI model catalog download interrupted", e);
                return providerModels(providerId);
            } catch (Exception e) {
                checkCanceled(monitor);
                // Cancellation must not postpone a subsequent attempt or overwrite the disk cache.
                nextRefreshAt = now + RETRY_INTERVAL.toMillis();
                log.debug("Unable to refresh AI model catalog; using cached metadata", e);
            }
            checkCanceled(monitor);
            saveCache();
        }
        return providerModels(providerId);
    }

    @NotNull
    public Map<String, AIModelCatalogEntry> getCachedModels(@NotNull String providerId) {
        if (!initialized) {
            synchronized (this) {
                loadCache();
            }
        }
        return providerModels(providerId);
    }

    @NotNull
    private Map<String, AIModelCatalogEntry> providerModels(@NotNull String providerId) {
        Provider provider = providers.get(providerId);
        return provider == null ? Map.of() : provider.models();
    }

    private void loadCache() {
        if (initialized) {
            return;
        }
        try {
            if (cacheFile == null || !Files.exists(cacheFile)) {
                return;
            }
            CacheSnapshot snapshot = GSON.fromJson(Files.readString(cacheFile), CacheSnapshot.class);
            if (snapshot == null || snapshot.providers() == null) {
                throw new IOException("Invalid AI model catalog cache");
            }
            providers = immutableProviders(snapshot.providers());
            nextRefreshAt = snapshot.nextRefreshAt();
        } catch (Exception e) {
            log.debug("Unable to read AI model catalog cache", e);
        } finally {
            initialized = true;
        }
    }

    private void saveCache() {
        if (cacheFile == null) {
            return;
        }
        try {
            Path directory = cacheFile.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            Path temporaryFile = Files.createTempFile(directory, "models-dev-", ".tmp");
            try {
                Files.writeString(temporaryFile, GSON.toJson(new CacheSnapshot(nextRefreshAt, providers)));
                try {
                    Files.move(temporaryFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporaryFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (Exception e) {
            log.debug("Unable to save AI model catalog cache; using in-memory metadata", e);
        }
    }

    @NotNull
    private static Map<String, Provider> readProviders(@NotNull String json) throws IOException {
        Map<String, Provider> parsed = GSON.fromJson(json, PROVIDERS_TYPE);
        Map<String, Provider> result = immutableProviders(parsed);
        if (result.values().stream().allMatch(provider -> provider.models().isEmpty())) {
            throw new IOException("AI model catalog contains no models");
        }
        return result;
    }

    @NotNull
    private static Map<String, Provider> immutableProviders(@Nullable Map<String, Provider> parsed) throws IOException {
        if (parsed == null) {
            throw new IOException("Missing AI model catalog providers");
        }
        Map<String, Provider> result = new HashMap<>();
        for (var entry : parsed.entrySet()) {
            Provider provider = entry.getValue();
            if (provider == null || provider.models() == null) {
                throw new IOException("Missing model list for provider " + entry.getKey());
            }
            result.put(entry.getKey(), new Provider(Map.copyOf(provider.models())));
        }
        return Map.copyOf(result);
    }

    @NotNull
    private static String download(@NotNull DBRProgressMonitor monitor) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        ) {
            return download(monitor, client);
        }
    }

    @NotNull
    static String download(@NotNull DBRProgressMonitor monitor, @NotNull HttpClient client) throws IOException, InterruptedException {
        checkCanceled(monitor);
        HttpRequest request = HttpRequest.newBuilder(CATALOG_URI)
            .header(HttpConstants.HEADER_ACCEPT, HttpConstants.CONTENT_TYPE_JSON)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            while (!future.isDone()) {
                checkCanceled(monitor);
                Thread.sleep(POLL_INTERVAL);
            }
            checkCanceled(monitor);
            HttpResponse<String> response = future.get();
            if (response.statusCode() != 200) {
                throw new IOException("AI model catalog returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (ExecutionException e) {
            throw new IOException("Unable to download AI model catalog", e.getCause());
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private static void checkCanceled(@NotNull DBRProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    @NotNull
    private static AIModelCatalog createInstance() {
        Path cacheFile = null;
        try {
            AIActivator activator = AIActivator.getInstance();
            if (activator != null) {
                cacheFile = RuntimeUtils.getPluginStateLocation(activator).resolve("models-dev.json");
            }
        } catch (Exception e) {
            log.debug("Unable to locate AI model catalog cache directory", e);
        }
        return new AIModelCatalog(cacheFile, Clock.systemUTC(), AIModelCatalog::download);
    }

    private record Provider(@NotNull Map<String, AIModelCatalogEntry> models) {
    }

    private record CacheSnapshot(long nextRefreshAt, @NotNull Map<String, Provider> providers) {
    }

    private static class InstanceHolder {
        private static final AIModelCatalog INSTANCE = createInstance();
    }
}
