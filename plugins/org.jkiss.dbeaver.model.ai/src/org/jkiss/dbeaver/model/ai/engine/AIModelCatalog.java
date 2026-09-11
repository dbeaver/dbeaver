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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.internal.AIActivator;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.HttpConstants;

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

public final class AIModelCatalog {
    private static final Log log = Log.getLog(AIModelCatalog.class);
    private static final URI CATALOG_URI = URI.create("https://models.dev/api.json");
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(7);
    private static final Duration RETRY_INTERVAL = Duration.ofDays(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Gson GSON = new Gson();
    private static final TypeToken<Map<String, Provider>> PROVIDERS_TYPE = new TypeToken<>() {};

    @Nullable
    private final Path cacheFile;
    private final Clock clock;
    private final CatalogLoader loader;
    private volatile Map<String, Provider> providers = Map.of();
    private long nextRefreshAt;
    private volatile boolean initialized;

    AIModelCatalog(@Nullable Path cacheFile, @NotNull Clock clock, @NotNull CatalogLoader loader) {
        this.cacheFile = cacheFile;
        this.clock = clock;
        this.loader = loader;
    }

    @NotNull
    public static AIModelCatalog getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @NotNull
    public synchronized Map<String, AIModelCatalogEntry> getModels(@NotNull String providerId) {
        loadCache();
        long now = clock.millis();
        if (now >= nextRefreshAt) {
            // retain the retry deadline even when downloading or saving the cache fails
            nextRefreshAt = now + RETRY_INTERVAL.toMillis();
            try {
                Map<String, Provider> downloaded = readProviders(loader.load());
                providers = downloaded;
                nextRefreshAt = clock.millis() + REFRESH_INTERVAL.toMillis();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("AI model catalog download interrupted", e);
            } catch (Exception e) {
                log.debug("Unable to refresh AI model catalog; using cached metadata", e);
            }
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
    private static String download() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        ) {
            HttpRequest request = HttpRequest.newBuilder(CATALOG_URI)
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.CONTENT_TYPE_JSON)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("AI model catalog returned HTTP " + response.statusCode());
            }
            return response.body();
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

    @FunctionalInterface
    interface CatalogLoader {
        @NotNull
        String load() throws IOException, InterruptedException;
    }

    private record Provider(@NotNull Map<String, AIModelCatalogEntry> models) {
    }

    private record CacheSnapshot(long nextRefreshAt, @NotNull Map<String, Provider> providers) {
    }

    private static class InstanceHolder {
        private static final AIModelCatalog INSTANCE = createInstance();
    }
}
