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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class AIHttpUtilsTest extends DBeaverUnitTest {
    @Test
    public void appendsPathWhenBaseHasNoTrailingSlash() throws Exception {
        URI uri = AIHttpUtils.resolve("http://localhost:11434/v1", "models");

        Assertions.assertEquals(URI.create("http://localhost:11434/v1/models"), uri);
    }

    @Test
    public void appendsPathWhenBaseAlreadyHasTrailingSlash() throws Exception {
        URI uri = AIHttpUtils.resolve("http://localhost:11434/v1/", "models");

        Assertions.assertEquals(URI.create("http://localhost:11434/v1/models"), uri);
    }

    @Test
    public void appendsChatCompletionsPathForSelfHostedEndpoint() throws Exception {
        URI uri = AIHttpUtils.resolve("http://localhost:11434/v1", "chat/completions");

        Assertions.assertEquals(URI.create("http://localhost:11434/v1/chat/completions"), uri);
    }

    @Test
    public void leavesACompleteUrlWithNoExtraPathsUnchanged() throws Exception {
        URI uri = AIHttpUtils.resolve("https://github.com/login/device/code");

        Assertions.assertEquals(URI.create("https://github.com/login/device/code"), uri);
    }

    @Test
    public void resolvesAgainstBaseWithNoPathSegment() throws Exception {
        URI uri = AIHttpUtils.resolve("http://localhost:11434", "chat/completions");

        Assertions.assertEquals(URI.create("http://localhost:11434/chat/completions"), uri);
    }

    @Test
    public void extractsSubscriptionErrorDetail() {
        String message = "The 'gpt-4' model is not supported when using Codex with a ChatGPT account.";

        Assertions.assertEquals(message, AIHttpUtils.parseOpenAIStyleErrorMessage(400, """
            {"detail":"The 'gpt-4' model is not supported when using Codex with a ChatGPT account."}
            """));
    }

    @Test
    public void preservesApiErrorMessage() {
        Assertions.assertEquals("Invalid model", AIHttpUtils.parseOpenAIStyleErrorMessage(400, """
            {"error":{"code":"invalid_request","message":"Invalid model"},"detail":"Other detail"}
            """));
    }

    @Test
    public void preservesRootErrorMessage() {
        Assertions.assertEquals("Invalid model", AIHttpUtils.parseOpenAIStyleErrorMessage(400, """
            {"message":"Invalid model","detail":"Other detail"}
            """));
    }

    @Test
    public void ignoresNonStringErrorDetails() {
        for (String detail : new String[] {"null", "42", "true", "{}", "[]"}) {
            String body = "{\"detail\":" + detail + "}";

            Assertions.assertEquals("HTTP 400 (" + body + ")", AIHttpUtils.parseOpenAIStyleErrorMessage(400, body));
        }
    }

    @Test
    public void preservesNonJsonErrorDescription() {
        Assertions.assertEquals("HTTP 502 (Bad Gateway)", AIHttpUtils.parseOpenAIStyleErrorMessage(502, """
            <html><head><title>Bad Gateway</title></head><body>Proxy error</body></html>
            """));
    }
}
