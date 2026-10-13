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
package org.jkiss.dbeaver.model.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.Map;

class DataDamAIEngineTest {
    @Test
    void dataDamShouldRequireToken() {
        DDAIEngineProperties properties = new DDAIEngineProperties();
        for (String baseUrl : Arrays.asList(null, DDAIEngineProperties.DEFAULT_ENDPOINT, "http://localhost:8000/v1/")) {
            properties.setBaseUrl(baseUrl);
            Assertions.assertTrue(properties.isTokenRequired());
            for (String token : Arrays.asList(null, "", "  ")) {
                properties.setToken(token);
                Assertions.assertFalse(properties.isValidConfiguration());
                Assertions.assertThrows(DBException.class, () -> new TestDataDamEngine(properties).createClient());
            }
            properties.setToken("secret");
            Assertions.assertTrue(properties.isValidConfiguration());
        }
    }

    @Test
    void dataDamClientsShouldApplyCustomHeaders() throws DBException {
        DDAIEngineProperties properties = new DDAIEngineProperties();
        properties.setToken("secret");
        properties.setCustomHeaders(Map.of("X-Api-Key", "custom-secret"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl())).GET().build();

        try (OpenAIClientResponses client = new TestDataDamEngine(properties).createClient()) {
            HttpRequest filtered = client.applyFilters(request);
            Assertions.assertEquals("Bearer secret", filtered.headers().firstValue("Authorization").orElseThrow());
            Assertions.assertEquals("custom-secret", filtered.headers().firstValue("X-Api-Key").orElseThrow());
        }
    }

    @Test
    void dataDamShouldKeepItsTokenSecretAndStoreHeaderSecrets() throws DBException {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(controller);
            AIConfigurationProfile profile = new AIConfigurationProfile();
            profile.setProfileId("datadam-test");
            Mockito.when(controller.getPrivateSecretValue("datadam.token_datadam-test")).thenReturn("secret");
            Mockito.when(controller.getPrivateSecretValue("openai.headers_datadam-test"))
                .thenReturn("{\"X-Api-Key\":\"custom-secret\"}");
            DDAIEngineProperties properties = new DDAIEngineProperties();

            properties.resolveSecrets(profile);
            Assertions.assertEquals("secret", properties.getToken());
            Assertions.assertEquals(Map.of("X-Api-Key", "custom-secret"), properties.getCustomHeaders());
            properties.saveSecrets(profile);
            Mockito.verify(controller).setPrivateSecretValue("datadam.token_datadam-test", "secret");
            Mockito.verify(controller).setPrivateSecretValue("openai.headers_datadam-test", "{\"X-Api-Key\":\"custom-secret\"}");
            properties.deleteSecrets(profile);
            Mockito.verify(controller).setPrivateSecretValue("datadam.token_datadam-test", null);
            Mockito.verify(controller).setPrivateSecretValue("openai.headers_datadam-test", null);
        }
    }

    private static class TestDataDamEngine extends DDAIEngine {
        private TestDataDamEngine(@NotNull DDAIEngineProperties properties) {
            super(properties);
        }

        @NotNull
        @Override
        public OpenAIClientResponses createClient() throws DBException {
            return super.createClient();
        }
    }
}
