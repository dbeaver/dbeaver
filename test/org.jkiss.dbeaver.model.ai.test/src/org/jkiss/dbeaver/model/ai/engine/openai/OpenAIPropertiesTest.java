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
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.engine.AIAccountProperties;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
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

}
