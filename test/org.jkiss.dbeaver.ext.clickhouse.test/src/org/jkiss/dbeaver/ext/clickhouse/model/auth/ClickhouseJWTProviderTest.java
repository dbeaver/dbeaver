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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.dbeaver.ext.clickhouse.model.auth.ClickhouseJWTProvider.DeviceCodePollResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class ClickhouseJWTProviderTest {

    @Test
    void tokenExpirationIsReadFromTheExpClaim() {
        // Tokens are refreshed based on this value, so a wrong reading means either
        // needless logins or expired tokens sent to the server.
        Assertions.assertEquals(1787694926L, ClickhouseJWTProvider.getTokenExpiration(jwtWithPayload(
            "{\"iss\":\"ClickHouse\",\"exp\":1787694926}")));
    }

    @Test
    void tokenExpirationIsZeroForUnreadableTokens() {
        // Zero means "unknown", which the caller treats as expired instead of failing
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration(null));
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration(""));
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration("not-a-jwt"));
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration("only.two"));
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration(jwtWithPayload("{\"iss\":\"ClickHouse\"}")));
        Assertions.assertEquals(0L, ClickhouseJWTProvider.getTokenExpiration("aaa.!!!not-base64!!!.bbb"));
    }

    @Test
    void tokenIsValidOnlyBeforeTheExpirationBuffer() {
        long now = System.currentTimeMillis() / 1000;
        // Comfortably in the future
        Assertions.assertTrue(ClickhouseJWTProvider.isTokenValid("token", now + 3600));
        // Already expired
        Assertions.assertFalse(ClickhouseJWTProvider.isTokenValid("token", now - 1));
        // Inside the buffer: still valid by the clock, but too close to be used
        Assertions.assertFalse(ClickhouseJWTProvider.isTokenValid("token", now + 5));
        // A missing token is never valid, whatever the expiration says
        Assertions.assertFalse(ClickhouseJWTProvider.isTokenValid(null, now + 3600));
        Assertions.assertFalse(ClickhouseJWTProvider.isTokenValid("", now + 3600));
    }

    @Test
    void customExpirationBufferIsHonoured() {
        long now = System.currentTimeMillis() / 1000;
        // Service scoped tokens use a wider buffer than identity provider ones
        Assertions.assertTrue(ClickhouseJWTProvider.isTokenValid("token", now + 45, 30));
        Assertions.assertFalse(ClickhouseJWTProvider.isTokenValid("token", now + 20, 30));
    }

    @Test
    void pollResponseWithTokenCompletesTheLogin() {
        Assertions.assertEquals(
            DeviceCodePollResult.TOKEN_ISSUED,
            ClickhouseJWTProvider.classifyPollResponse(json("{\"access_token\":\"abc\",\"expires_in\":3600}")));
    }

    @Test
    void pendingAndSlowDownKeepPolling() {
        // These two are the only errors that must not abort the device flow
        Assertions.assertEquals(
            DeviceCodePollResult.AUTHORIZATION_PENDING,
            ClickhouseJWTProvider.classifyPollResponse(json("{\"error\":\"authorization_pending\"}")));
        Assertions.assertEquals(
            DeviceCodePollResult.SLOW_DOWN,
            ClickhouseJWTProvider.classifyPollResponse(json("{\"error\":\"slow_down\"}")));
    }

    @Test
    void otherErrorsStopPolling() {
        Assertions.assertEquals(
            DeviceCodePollResult.FAILED,
            ClickhouseJWTProvider.classifyPollResponse(json("{\"error\":\"expired_token\"}")));
        Assertions.assertEquals(
            DeviceCodePollResult.FAILED,
            ClickhouseJWTProvider.classifyPollResponse(json("{\"error\":\"access_denied\"}")));
        // A response that is neither a token nor a known error must not loop forever
        Assertions.assertEquals(
            DeviceCodePollResult.FAILED,
            ClickhouseJWTProvider.classifyPollResponse(json("{}")));
    }

    @Test
    void pollErrorPrefersTheDescription() {
        Assertions.assertEquals("The user denied the request",
            ClickhouseJWTProvider.getPollError(
                json("{\"error\":\"access_denied\",\"error_description\":\"The user denied the request\"}")));
        Assertions.assertEquals("access_denied",
            ClickhouseJWTProvider.getPollError(json("{\"error\":\"access_denied\"}")));
        Assertions.assertEquals("unknown_error", ClickhouseJWTProvider.getPollError(json("{}")));
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String jwtWithPayload(String payload) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "header." + encoded + ".signature";
    }
}
