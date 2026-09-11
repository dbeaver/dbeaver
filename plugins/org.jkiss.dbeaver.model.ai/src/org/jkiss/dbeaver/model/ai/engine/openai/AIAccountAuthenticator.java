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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface AIAccountAuthenticator {
    static boolean isSupported() {
        return !DBWorkbench.isDistributed()
            && !DBWorkbench.getPlatform().getApplication().isMultiuser()
            && !DBWorkbench.getPlatform().getApplication().isHeadlessMode();
    }

    default boolean supportsBrowserAuthorization() {
        return false;
    }

    @NotNull
    default BrowserAuthorization startBrowserAuthorization() throws DBException {
        throw new DBException("Browser authorization is not supported");
    }

    @NotNull
    default Tokens completeBrowserAuthorization() throws DBException {
        throw new DBException("Browser authorization is not supported");
    }

    default void cancelBrowserAuthorization() {
    }

    @NotNull
    DeviceAuthorization startDeviceAuthorization() throws DBException;

    @NotNull
    Tokens completeDeviceAuthorization(
        @NotNull DeviceAuthorization authorization,
        @NotNull CompletableFuture<Void> cancellation
    ) throws DBException;

    @NotNull
    Tokens refresh(@NotNull String refreshToken) throws DBException;

    record BrowserAuthorization(@NotNull URI authorizationUri) {
    }

    record DeviceAuthorization(
        @NotNull String deviceCode,
        @NotNull String userCode,
        @NotNull URI verificationUri,
        int intervalSeconds,
        long expiresInSeconds
    ) {
    }

    record Tokens(
        @NotNull String accessToken,
        @NotNull String refreshToken,
        long expiresInSeconds,
        @Nullable String accountId,
        @Nullable String email
    ) {
    }
}
