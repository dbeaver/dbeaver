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

import com.google.gson.JsonParser;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessageContent;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAccountAuthenticatorTest {
    @Test
    void extractsChatGptAccountIdFromIdToken() {
        String token = tokenWithClaims("{\"chatgpt_account_id\":\"account-1\"}");

        assertEquals("account-1", OpenAIAccountAuthenticator.extractAccountId(token));
    }

    @Test
    void extractsNestedChatGptAccountIdFromIdToken() {
        String token = tokenWithClaims("{\"https://api.openai.com/auth\":{\"chatgpt_account_id\":\"account-2\"}}");

        assertEquals("account-2", OpenAIAccountAuthenticator.extractAccountId(token));
    }

    @Test
    void extractsOrganizationIdFromIdToken() {
        String token = tokenWithClaims("{\"organizations\":[{\"id\":\"org-1\"}]}");

        assertEquals("org-1", OpenAIAccountAuthenticator.extractAccountId(token));
    }

    @Test
    void fallsBackToAccessTokenForAccountId() {
        String token = tokenWithClaims("{\"chatgpt_account_id\":\"account-3\"}");

        assertEquals("account-3", OpenAIAccountAuthenticator.extractAccountId(null, token));
    }

    @Test
    void extractsEmailFromIdToken() {
        String token = tokenWithClaims("{\"email\":\"user@example.com\"}");

        assertEquals("user@example.com", OpenAIAccountAuthenticator.extractEmail(token));
    }

    @Test
    void extractsExpirationFromAccessToken() {
        String token = tokenWithClaims("{\"exp\":1234567890}");

        assertEquals(1_234_567_890_000L, OpenAIAccountAuthenticator.extractExpiresAt(token));
    }

    @Test
    void ignoresMalformedIdToken() {
        assertNull(OpenAIAccountAuthenticator.extractAccountId("not-a-token"));
    }

    @Test
    void listsOnlyPickerVisibleModelsInPriorityOrder() {
        var catalog = JsonParser.parseString("""
            {"models":[
              {"slug":"second","visibility":"list","priority":2},
              {"slug":"hidden","visibility":"hidden","priority":0},
              {"slug":"first","visibility":"list","priority":1}
            ]}
            """).getAsJsonObject();

        assertEquals(List.of("first", "second"), OpenAIAccountAuthenticator.parseModels(catalog));
    }

    @Test
    void defaultsMissingAuthenticationToApiToken() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setAuthentication(null);

        assertEquals(OpenAIProperties.AUTHENTICATION_API_TOKEN, properties.getAuthentication());
    }

    @Test
    void temporaryPropertiesUseUpdatedSourceCredentials() {
        OpenAIProperties source = new OpenAIProperties();
        source.setAccountTokens(
            new OpenAIAccountAuthenticator.Tokens(
                "access-1", "refresh-1", 3600, "account-1", "first@example.com"
            )
        );
        OpenAIProperties copy = new OpenAIProperties();
        copy.useAccountCredentialsFrom(source);

        source.setAccountTokens(
            new OpenAIAccountAuthenticator.Tokens(
                "access-2", "refresh-2", 3600, "account-2", "second@example.com"
            )
        );

        assertEquals("account-2", copy.getAccountId());
        assertEquals("second@example.com", copy.getAccountEmail());

        source.clearAccountTokens();

        assertFalse(copy.isChatGptAccountConnected());
        assertNull(copy.getAccountEmail());
    }

    @Test
    void movesSystemMessagesToAccountInstructions() {
        OAIMessage firstSystemMessage = message("system", "First instruction");
        OAIMessage userMessage = message("user", "Question");
        OAIMessage secondSystemMessage = message("system", "Second instruction");
        OAIResponsesRequest request = new OAIResponsesRequest();
        request.input = List.of(firstSystemMessage, userMessage, secondSystemMessage);
        request.temperature = 1.0;

        OpenAiUtils.prepareChatGptAccountRequest(request);

        assertEquals("First instruction\nSecond instruction", request.instructions);
        assertEquals(List.of(userMessage), request.input);
        assertEquals(1.0, request.temperature);
    }

    @Test
    void detectsTemperatureErrorsWithAndWithoutQuotes() {
        assertTrue(OpenAiUtils.isTemperatureNotSupported("Unsupported parameter: 'temperature'"));
        assertTrue(OpenAiUtils.isTemperatureNotSupported("Unsupported parameter: temperature"));
    }

    private static String tokenWithClaims(String claims) {
        return "header."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8))
            + ".signature";
    }

    private static OAIMessage message(String role, String text) {
        OAIMessage message = new OAIMessage();
        message.role = role;
        message.content = List.of(new OAIMessageContent(true, text));
        return message;
    }
}
