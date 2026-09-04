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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Keeps JWT providers alive for the lifetime of the application.
 * <p>
 * A data source opens multiple physical connections (metadata reads, SQL editors, etc.) and each of them
 * goes through the auth model, so tokens must be shared instead of being requested over and over again.
 */
public class ClickhouseJWTProviderRegistry {
    private static final Map<String, ClickhouseJWTProvider> providers = new ConcurrentHashMap<>();

    private ClickhouseJWTProviderRegistry() {
    }

    /**
     * Returns the provider cached for the given key, creating it if needed.
     *
     * @param key     unique key of the provider, includes the data source id and everything the tokens depend on
     * @param creator factory of the provider
     */
    @NotNull
    static ClickhouseJWTProvider getOrCreate(
        @NotNull String key,
        @NotNull Function<String, ClickhouseJWTProvider> creator
    ) {
        return providers.computeIfAbsent(key, creator);
    }

    /**
     * Forgets all tokens obtained for the given data source, forcing a new sign in.
     */
    public static void reset(@NotNull String dataSourceId) {
        // The providers are only dropped, never reset in place: this runs on the UI thread and
        // the token lifecycle of a provider currently performing a login is locked. A discarded
        // provider is never consulted again, so an interactive login still in flight is harmless.
        providers.keySet().removeIf(key -> key.startsWith(dataSourceId + ":"));
    }

    @Nullable
    static ClickhouseJWTProvider get(@NotNull String key) {
        return providers.get(key);
    }
}
