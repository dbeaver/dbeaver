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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class OpenAIRequestFilterTest extends DBeaverUnitTest {

    private static final URI ENDPOINT = URI.create("http://localhost:8000/v1/responses");

    @Test
    public void filterShouldKeepRequestTimeout() {
        //given
        var timeout = Duration.ofSeconds(42);
        var request = newRequestBuilder().timeout(timeout).build();
        //when
        var result = new OpenAIRequestFilter("token").filter(request, false);
        //then
        Assertions.assertEquals(timeout, result.timeout().orElseThrow());
    }

    @Test
    public void filterShouldKeepRequestVersion() {
        //given
        var request = newRequestBuilder().version(HttpClient.Version.HTTP_1_1).build();
        //when
        var result = new OpenAIRequestFilter("token").filter(request, false);
        //then
        Assertions.assertEquals(HttpClient.Version.HTTP_1_1, result.version().orElseThrow());
    }

    @Test
    public void filterShouldKeepUriMethodAndBody() {
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
    public void filterShouldAddAuthorizationHeader() {
        //given
        var request = newRequestBuilder().build();
        //when
        var result = new OpenAIRequestFilter("secret").filter(request, false);
        //then
        Assertions.assertEquals("Bearer secret", result.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    public void filterShouldSetContentTypeOnlyWhenRequested() {
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

    private static HttpRequest.Builder newRequestBuilder() {
        return HttpRequest.newBuilder(ENDPOINT).POST(HttpRequest.BodyPublishers.noBody());
    }
}
