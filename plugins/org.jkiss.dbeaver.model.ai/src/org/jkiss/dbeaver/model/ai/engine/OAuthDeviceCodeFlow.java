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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.utils.function.ThrowableConsumer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

public final class OAuthDeviceCodeFlow {
    private static final int POLLING_SAFETY_MARGIN_SECONDS = 3;
    private static final int EXPIRATION_SAFETY_MARGIN_SECONDS = 3;
    private static final int SLOW_DOWN_INCREMENT_SECONDS = 5;
    private static final Duration CANCELLATION_CHECK_INTERVAL = Duration.ofSeconds(1);

    @NotNull
    private final LongSupplier nanoTime;
    @NotNull
    private final ThrowableConsumer<Duration, InterruptedException> sleeper;

    public OAuthDeviceCodeFlow() {
        this(System::nanoTime, Thread::sleep);
    }

    public OAuthDeviceCodeFlow(
        @NotNull LongSupplier nanoTime,
        @NotNull ThrowableConsumer<Duration, InterruptedException> sleeper
    ) {
        this.nanoTime = nanoTime;
        this.sleeper = sleeper;
    }

    @NotNull
    public AIAccountAuthenticator.Tokens complete(
        @NotNull AIAccountAuthenticator authenticator,
        @NotNull AIAccountAuthenticator.DeviceAuthorization authorization,
        @NotNull CompletableFuture<Void> cancellation,
        @NotNull String providerName
    ) throws DBException {
        long deadlineNanos = nanoTime.getAsLong() + Duration.ofSeconds(
            Math.max(0, authorization.expiresInSeconds() - EXPIRATION_SAFETY_MARGIN_SECONDS)
        ).toNanos();
        int intervalSeconds = Math.max(1, authorization.intervalSeconds());
        while (nanoTime.getAsLong() < deadlineNanos) {
            checkCancellation(cancellation, providerName);
            AIAccountAuthenticator.DeviceAuthorizationPollResult result =
                authenticator.pollDeviceAuthorization(authorization, cancellation);
            checkCancellation(cancellation, providerName);
            if (result.status() == AIAccountAuthenticator.DeviceAuthorizationPollStatus.COMPLETE) {
                return Objects.requireNonNull(result.tokens());
            }
            if (result.status() == AIAccountAuthenticator.DeviceAuthorizationPollStatus.SLOW_DOWN) {
                intervalSeconds += SLOW_DOWN_INCREMENT_SECONDS;
            }

            long remainingNanos = deadlineNanos - nanoTime.getAsLong();
            if (remainingNanos <= 0) {
                break;
            }
            long waitNanos = Math.min(
                Duration.ofSeconds(intervalSeconds + POLLING_SAFETY_MARGIN_SECONDS).toNanos(),
                remainingNanos
            );
            sleep(Duration.ofNanos(waitNanos), cancellation, providerName);
        }
        throw new DBException(providerName + " device authorization timed out");
    }

    private static void checkCancellation(
        @NotNull CompletableFuture<Void> cancellation,
        @NotNull String providerName
    ) throws DBException {
        if (cancellation.isCancelled()) {
            throw new DBException(providerName + " device authorization was cancelled");
        }
    }

    private void sleep(
        @NotNull Duration duration,
        @NotNull CompletableFuture<Void> cancellation,
        @NotNull String providerName
    ) throws DBException {
        long remainingNanos = duration.toNanos();
        while (remainingNanos > 0) {
            checkCancellation(cancellation, providerName);
            long sleepNanos = Math.min(remainingNanos, CANCELLATION_CHECK_INTERVAL.toNanos());
            try {
                sleeper.accept(Duration.ofNanos(sleepNanos));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DBException(providerName + " device authorization was interrupted", e);
            }
            remainingNanos -= sleepNanos;
        }
    }
}
