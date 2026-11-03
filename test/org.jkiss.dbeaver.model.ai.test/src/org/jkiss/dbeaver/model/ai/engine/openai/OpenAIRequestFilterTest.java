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

import org.jkiss.dbeaver.DBException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public class OpenAIRequestFilterTest extends DBeaverUnitTest {

    private static final URI ENDPOINT = URI.create("http://localhost:8000/v1/responses");

    @Test
    public void filterShouldKeepRequestTimeout() throws DBException {
        //given
        var timeout = Duration.ofSeconds(42);
        var request = newRequestBuilder().timeout(timeout).build();
        //when
        var result = new OpenAIRequestFilter("token").filter(request, false);
        //then
        Assertions.assertEquals(timeout, result.timeout().orElseThrow());
    }

    @Test
    public void filterShouldKeepRequestVersion() throws DBException {
        //given
        var request = newRequestBuilder().version(HttpClient.Version.HTTP_1_1).build();
        //when
        var result = new OpenAIRequestFilter("token").filter(request, false);
        //then
        Assertions.assertEquals(HttpClient.Version.HTTP_1_1, result.version().orElseThrow());
    }

    @Test
    public void filterShouldKeepUriMethodAndBody() throws DBException {
        //given
        var body = "{\"model\":\"test\"}";
        var request = newRequestBuilder().POST(HttpRequest.BodyPublishers.ofString(body)).build();
        //when
        var result = new OpenAIRequestFilter("token").filter(request, false);
        //then
        Assertions.assertEquals(ENDPOINT, result.uri());
        Assertions.assertEquals("POST", result.method());
        Assertions.assertEquals(body.length(), result.bodyPublisher().orElseThrow().contentLength());
    }

    @Test
    public void filterShouldAddAuthorizationHeader() throws DBException {
        //given
        var request = newRequestBuilder().build();
        //when
        var result = new OpenAIRequestFilter("secret").filter(request, false);
        //then
        Assertions.assertEquals("Bearer secret", result.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    public void filterShouldSetContentTypeOnlyWhenRequested() throws DBException {
        //given
        var request = newRequestBuilder().build();
        var filter = new OpenAIRequestFilter("token");
        //when
        var withContentType = filter.filter(request, true);
        var withoutContentType = filter.filter(request, false);
        //then
        Assertions.assertEquals("application/json", withContentType.headers().firstValue("Content-Type").orElseThrow());
        Assertions.assertTrue(withoutContentType.headers().firstValue("Content-Type").isEmpty());
    }

    @Test
    public void filterShouldOmitAuthorizationWithoutToken() throws DBException {
        for (String token : Arrays.asList(null, "", "  ")) {
            var result = new OpenAIRequestFilter(token).filter(newRequestBuilder().build(), true);
            Assertions.assertTrue(result.headers().firstValue("Authorization").isEmpty());
            Assertions.assertEquals("application/json", result.headers().firstValue("Content-Type").orElseThrow());
        }
    }

    @Test
    public void customHeadersShouldOverrideDefaultsAndPreserveOtherHeaders() throws DBException {
        var request = newRequestBuilder().header("X-Existing", "existing").header("X-Custom", "old").build();
        var filter = new OpenAIRequestFilter("token", Map.of(
            "authorization", "Custom secret",
            "content-type", "application/custom+json",
            "x-custom", "new"
        ));

        var result = filter.filter(request, true);

        Assertions.assertEquals(Arrays.asList("Custom secret"), result.headers().allValues("Authorization"));
        Assertions.assertEquals(Arrays.asList("application/custom+json"), result.headers().allValues("Content-Type"));
        Assertions.assertEquals(Arrays.asList("new"), result.headers().allValues("X-Custom"));
        Assertions.assertEquals("existing", result.headers().firstValue("X-Existing").orElseThrow());
    }

    @Test
    public void customAuthorizationShouldWorkWithoutApiToken() throws DBException {
        var filter = new OpenAIRequestFilter(null, Map.of("Authorization", "Basic secret"));

        var result = filter.filter(newRequestBuilder().build(), false);

        Assertions.assertEquals("Basic secret", result.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    public void filterShouldReportInvalidOrRestrictedCustomHeader() {
        for (String name : Arrays.asList("X Api Key", "Host", "host", "Content-Length", "Connection")) {
            var filter = new OpenAIRequestFilter(null, Map.of(name, "secret"));

            DBException error = Assertions.assertThrows(DBException.class, () -> filter.filter(newRequestBuilder().build(), false));

            Assertions.assertTrue(error.getMessage().contains(name));
            Assertions.assertFalse(error.getMessage().contains("secret"));
            Assertions.assertNull(error.getCause());
        }
    }

    @Test
    public void filterShouldReportInvalidHeaderValueWithoutExposingCredentials() {
        var filter = new OpenAIRequestFilter(null, Map.of("X-Api-Key", "secret\r\nother"));

        DBException error = Assertions.assertThrows(DBException.class, () -> filter.filter(newRequestBuilder().build(), false));

        Assertions.assertTrue(error.getMessage().contains("X-Api-Key"));
        Assertions.assertFalse(error.getMessage().contains("secret"));
        Assertions.assertNull(error.getCause());
    }

    @Test
    public void headerValidationShouldUseHttpClientRules() {
        Assertions.assertNull(OpenAIRequestFilter.findInvalidHeader(Map.of("Authorization", "Basic secret", "X-Empty", "")));
        Assertions.assertEquals("X Api Key", OpenAIRequestFilter.findInvalidHeader(Map.of("X Api Key", "secret")));
        Assertions.assertEquals("Host", OpenAIRequestFilter.findInvalidHeader(Map.of("Host", "localhost")));
        Assertions.assertEquals("X-Api-Key", OpenAIRequestFilter.findInvalidHeader(Map.of("X-Api-Key", "secret\r\nother")));
    }

    private static HttpRequest.Builder newRequestBuilder() {
        return HttpRequest.newBuilder(ENDPOINT).POST(HttpRequest.BodyPublishers.noBody());
    }
}
