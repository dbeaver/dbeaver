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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public class OAuthDeviceCodeFlowTest {
    private static final AIAccountAuthenticator.DeviceAuthorization AUTHORIZATION =
        new AIAccountAuthenticator.DeviceAuthorization(
            "device-code",
            "user-code",
            URI.create("https://example.com/device"),
            2,
            30
        );

    @Test
    void handlesPendingAndSlowDownResponses() throws DBException {
        AtomicLong nanoTime = new AtomicLong();
        List<Duration> waits = new ArrayList<>();
        OAuthDeviceCodeFlow flow = new OAuthDeviceCodeFlow(nanoTime::get, duration -> {
            waits.add(duration);
            nanoTime.addAndGet(duration.toNanos());
        });
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        List<Duration> pollTimes = new ArrayList<>();
        AIAccountAuthenticator.Tokens tokens = new AIAccountAuthenticator.Tokens(
            "access", "refresh", 3600, null, null
        );
        List<AIAccountAuthenticator.DeviceAuthorizationPollResult> responses = List.of(
            AIAccountAuthenticator.DeviceAuthorizationPollResult.pending(),
            AIAccountAuthenticator.DeviceAuthorizationPollResult.slowDown(),
            AIAccountAuthenticator.DeviceAuthorizationPollResult.complete(tokens)
        );
        AtomicInteger responseIndex = new AtomicInteger();
        Mockito.when(authenticator.pollDeviceAuthorization(Mockito.eq(AUTHORIZATION), Mockito.any())).thenAnswer(invocation -> {
            pollTimes.add(Duration.ofNanos(nanoTime.get()));
            return responses.get(responseIndex.getAndIncrement());
        });

        AIAccountAuthenticator.Tokens result = flow.complete(
            authenticator,
            AUTHORIZATION,
            cancellationFuture(() -> false),
            "Test provider"
        );

        Assertions.assertSame(tokens, result);
        Assertions.assertEquals(
            List.of(Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(15)),
            pollTimes
        );
        Assertions.assertEquals(Duration.ofSeconds(15), waits.stream().reduce(Duration.ZERO, Duration::plus));
        Assertions.assertTrue(waits.stream().allMatch(wait -> wait.compareTo(Duration.ofSeconds(1)) <= 0));
        Mockito.verify(authenticator, Mockito.times(3))
            .pollDeviceAuthorization(Mockito.eq(AUTHORIZATION), Mockito.any());
    }

    @Test
    void stopsBeforePollingWhenCancelled() {
        AtomicLong nanoTime = new AtomicLong();
        OAuthDeviceCodeFlow flow = new OAuthDeviceCodeFlow(
            nanoTime::get,
            duration -> nanoTime.addAndGet(duration.toNanos())
        );
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);

        DBException exception = Assertions.assertThrows(
            DBException.class,
            () -> flow.complete(
                authenticator,
                AUTHORIZATION,
                cancellationFuture(() -> true),
                "Test provider"
            )
        );

        Assertions.assertEquals("Test provider device authorization was cancelled", exception.getMessage());
        Mockito.verifyNoInteractions(authenticator);
    }

    @Test
    void stopsWaitingWhenCancelled() throws DBException {
        AtomicLong nanoTime = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();
        OAuthDeviceCodeFlow flow = new OAuthDeviceCodeFlow(nanoTime::get, duration -> {
            nanoTime.addAndGet(duration.toNanos());
            cancelled.set(true);
        });
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        Mockito.when(authenticator.pollDeviceAuthorization(Mockito.eq(AUTHORIZATION), Mockito.any())).thenReturn(
            AIAccountAuthenticator.DeviceAuthorizationPollResult.pending()
        );

        DBException exception = Assertions.assertThrows(
            DBException.class,
            () -> flow.complete(
                authenticator,
                AUTHORIZATION,
                cancellationFuture(cancelled::get),
                "Test provider"
            )
        );

        Assertions.assertEquals("Test provider device authorization was cancelled", exception.getMessage());
        Assertions.assertEquals(Duration.ofSeconds(1), Duration.ofNanos(nanoTime.get()));
        Mockito.verify(authenticator).pollDeviceAuthorization(Mockito.eq(AUTHORIZATION), Mockito.any());
    }

    @Test
    void stopsBeforeAuthorizationExpires() throws DBException {
        AtomicLong nanoTime = new AtomicLong();
        OAuthDeviceCodeFlow flow = new OAuthDeviceCodeFlow(
            nanoTime::get,
            duration -> nanoTime.addAndGet(duration.toNanos())
        );
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        AIAccountAuthenticator.DeviceAuthorization expiringAuthorization =
            new AIAccountAuthenticator.DeviceAuthorization(
                "device-code",
                "user-code",
                URI.create("https://example.com/device"),
                5,
                9
            );
        Mockito.when(authenticator.pollDeviceAuthorization(Mockito.eq(expiringAuthorization), Mockito.any())).thenReturn(
            AIAccountAuthenticator.DeviceAuthorizationPollResult.pending()
        );

        DBException exception = Assertions.assertThrows(
            DBException.class,
            () -> flow.complete(
                authenticator,
                expiringAuthorization,
                cancellationFuture(() -> false),
                "Test provider"
            )
        );

        Assertions.assertEquals("Test provider device authorization timed out", exception.getMessage());
        Mockito.verify(authenticator).pollDeviceAuthorization(Mockito.eq(expiringAuthorization), Mockito.any());
        Assertions.assertEquals(Duration.ofSeconds(6), Duration.ofNanos(nanoTime.get()));
    }

    private static CompletableFuture<Void> cancellationFuture(BooleanSupplier cancellation) {
        return new CompletableFuture<>() {
            @Override
            public boolean isCancelled() {
                return cancellation.getAsBoolean() || super.isCancelled();
            }
        };
    }
}
