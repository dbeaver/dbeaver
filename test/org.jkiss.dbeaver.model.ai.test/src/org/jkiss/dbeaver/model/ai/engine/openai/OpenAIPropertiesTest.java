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

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.engine.AIAccountProperties;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenAIPropertiesTest extends DBeaverUnitTest {

    @Test
    public void contextWindowSizeMustBePositive() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        PropertySourceEditable propertySource = new PropertySourceEditable(properties, properties);
        propertySource.collectProperties();
        ObjectPropertyDescriptor descriptor = (ObjectPropertyDescriptor) propertySource.getProperty(
            AIConstants.AI_CONTEXT_SIZE_PROPERTY);

        Assertions.assertNotNull(descriptor.getConstraints());
        Assertions.assertEquals(1F, descriptor.getConstraints().min());
        Assertions.assertThrows(IllegalArgumentException.class, () -> descriptor.writeValue(properties, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> descriptor.writeValue(properties, -1));

        descriptor.writeValue(properties, 1);
        Assertions.assertEquals(1, properties.getContextWindowSize());
    }

    @Test
    public void tokenIsOptionalCredentialProperty() {
        OpenAIProperties properties = new OpenAIProperties();
        PropertySourceEditable propertySource = new PropertySourceEditable(properties, properties);
        propertySource.collectProperties();

        List<ObjectPropertyDescriptor> credentials = Arrays.stream(propertySource.getProperties())
            .filter(ObjectPropertyDescriptor.class::isInstance)
            .map(ObjectPropertyDescriptor.class::cast)
            .filter(ObjectPropertyDescriptor::isPassword)
            .toList();

        Assertions.assertEquals(1, credentials.size());
        ObjectPropertyDescriptor apiToken = credentials.getFirst();
        Assertions.assertFalse(apiToken.isRequired());
        Assertions.assertEquals(
            AIConstants.AI_NON_GLOBAL_CREDENTIALS_HIDE_EXPRESSION,
            apiToken.getHideExpression()
        );
        List<ObjectPropertyDescriptor> hiddenCredentials = ObjectAttributeDescriptor.extractAnnotations(
                null,
                OpenAIProperties.class,
                null,
                null
            ).stream()
            .filter(ObjectPropertyDescriptor::isPassword)
            .filter(ObjectPropertyDescriptor::isHidden)
            .toList();
        Assertions.assertEquals(2, hiddenCredentials.size());
        Assertions.assertNotNull(propertySource.getProperty(AIConstants.AI_GLOBAL_PROPERTY));
        Assertions.assertNotNull(propertySource.getProperty("authentication"));
    }

    @Test
    public void exposesAccountAuthenticationCapability() throws DBException {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);

        Assertions.assertInstanceOf(AIAccountProperties.class, properties);
        Assertions.assertTrue(properties.supportsDeviceAuthorization());
        Assertions.assertInstanceOf(OpenAIAccountAuthenticator.class, properties.createAccountAuthenticator());
        Assertions.assertEquals(
            AIAccountProperties.ACCOUNT_CREDENTIAL_PROPERTY_IDS,
            Set.of(
                OpenAIProperties.ACCOUNT_ACCESS_TOKEN_PROPERTY,
                OpenAIProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY,
                OpenAIProperties.ACCOUNT_ID_PROPERTY,
                OpenAIProperties.ACCOUNT_EMAIL_PROPERTY,
                OpenAIProperties.ACCOUNT_EXPIRES_AT_PROPERTY
            )
        );
    }

    @Test
    public void accountAuthenticatorRequiresProviderAccountAuthentication() {
        OpenAIProperties properties = new OpenAIProperties();

        Assertions.assertThrows(DBException.class, properties::createAccountAuthenticator);
    }

    @Test
    public void defaultEndpointShouldRequireToken() {
        OpenAIProperties properties = new OpenAIProperties();
        for (String baseUrl : Arrays.asList(null, "", "https://api.openai.com/v1", OpenAIClientResponses.OPENAI_ENDPOINT)) {
            properties.setBaseUrl(baseUrl);
            for (String token : Arrays.asList(null, "", "  ")) {
                properties.setToken(token);
                Assertions.assertFalse(properties.isValidConfiguration());
            }
            properties.setToken("secret");
            Assertions.assertTrue(properties.isValidConfiguration());
        }
    }

    @Test
    public void customEndpointShouldAllowMissingToken() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("http://localhost:8000/v1/");
        for (String token : Arrays.asList(null, "", "  ")) {
            properties.setToken(token);
            Assertions.assertTrue(properties.isValidConfiguration());
        }
        properties.setBaseUrl(null);
        Assertions.assertFalse(properties.isValidConfiguration());
    }

    @Test
    public void customEndpointShouldNotBypassAccountAuthentication() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("http://localhost:8000/v1/");
        properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);

        Assertions.assertFalse(properties.isValidConfiguration());
    }

    @Test
    public void invalidCustomHeadersShouldInvalidateConfiguration() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("http://localhost:8000/v1/");
        properties.setCustomHeaders(Map.of("X Api Key", "secret"));
        Assertions.assertFalse(properties.isValidConfiguration());

        properties.setCustomHeaders(Map.of("Host", "localhost"));
        Assertions.assertFalse(properties.isValidConfiguration());

        properties.setCustomHeaders(Map.of("X-Api-Key", "secret\r\nother"));
        Assertions.assertFalse(properties.isValidConfiguration());

        properties.setCustomHeaders(Map.of("X-Api-Key", "secret"));
        Assertions.assertTrue(properties.isValidConfiguration());
    }

    @Test
    public void customHeadersShouldSurviveSerialization() {
        OpenAIProperties properties = new OpenAIProperties();
        Map<String, String> headers = Map.of("X-Api-Key", "secret", "X-Empty", "");
        properties.setCustomHeaders(headers);
        Gson gson = new Gson();

        OpenAIProperties restored = gson.fromJson(gson.toJson(properties), OpenAIProperties.class);

        Assertions.assertEquals(headers, restored.getCustomHeaders());
        Assertions.assertTrue(gson.fromJson("{}", OpenAIProperties.class).getCustomHeaders().isEmpty());
        restored.setCustomHeaders(Map.of());
        Assertions.assertTrue(restored.getCustomHeaders().isEmpty());
    }

    @Test
    public void customHeadersShouldBeExcludedFromNonSecureConfiguration() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setCustomHeaders(Map.of("Authorization", "secret", "X-Api-Key", "another-secret"));
        properties.setBaseUrl("http://localhost:8000/v1/");

        String json = PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder().create().toJson(properties);

        Assertions.assertFalse(JsonParser.parseString(json).getAsJsonObject().has("openai.headers"));
        Assertions.assertFalse(json.contains("secret"));
        Assertions.assertEquals("http://localhost:8000/v1/", JsonParser.parseString(json).getAsJsonObject().get("gpt.base_url").getAsString());
    }

    @Test
    public void customHeaderSecretsShouldBeRestoredClearedAndDeletedPerProfile() throws DBException {
        Map<String, String> secrets = new HashMap<>();
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> secrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            secrets.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).setPrivateSecretValue(ArgumentMatchers.anyString(), ArgumentMatchers.nullable(String.class));

        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(controller);
            AIConfigurationProfile work = new AIConfigurationProfile();
            work.setProfileId("work");
            AIConfigurationProfile personal = new AIConfigurationProfile();
            personal.setProfileId("personal");
            OpenAIProperties properties = new OpenAIProperties();
            Map<String, String> headers = Map.of("Authorization", "secret", "X-Empty", "");
            properties.setCustomHeaders(headers);
            properties.saveSecrets(work);
            properties.setCustomHeaders(Map.of("X-Api-Key", "personal-secret"));
            properties.saveSecrets(personal);

            OpenAIProperties restored = new OpenAIProperties();
            restored.resolveSecrets(work);
            Assertions.assertEquals(headers, restored.getCustomHeaders());
            OpenAIProperties restoredPersonal = new OpenAIProperties();
            restoredPersonal.resolveSecrets(personal);
            Assertions.assertEquals(Map.of("X-Api-Key", "personal-secret"), restoredPersonal.getCustomHeaders());

            restored.setCustomHeaders(Map.of());
            restored.saveSecrets(work);
            OpenAIProperties cleared = new OpenAIProperties();
            cleared.resolveSecrets(work);
            Assertions.assertTrue(cleared.getCustomHeaders().isEmpty());

            restoredPersonal.deleteSecrets(personal);
            OpenAIProperties deleted = new OpenAIProperties();
            deleted.resolveSecrets(personal);
            Assertions.assertTrue(deleted.getCustomHeaders().isEmpty());
        }
    }

    @Test
    public void engineShouldPassHeadersToResponsesAndLegacyClientsWithoutToken() throws DBException {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("http://localhost:8000/v1/");
        properties.setCustomHeaders(Map.of("X-Api-Key", "secret"));
        OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties);
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl())).GET().build();

        try (OpenAIClientResponses client = engine.createClient(); OpenAIClientChat backupClient = client.createBackupClient()) {
            for (OpenAiClientBase currentClient : List.of(client, backupClient)) {
                HttpRequest filtered = currentClient.applyFilters(request);
                Assertions.assertTrue(filtered.headers().firstValue("Authorization").isEmpty());
                Assertions.assertEquals("secret", filtered.headers().firstValue("X-Api-Key").orElseThrow());
            }
        }
    }

    @Test
    public void engineShouldRejectDefaultEndpointWithoutToken() {
        OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(new OpenAIProperties());

        Assertions.assertThrows(DBException.class, engine::createClient);
    }

}
