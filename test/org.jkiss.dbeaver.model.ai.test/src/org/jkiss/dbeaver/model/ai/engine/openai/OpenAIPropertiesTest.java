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

import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OpenAIPropertiesTest extends DBeaverUnitTest {

    @Test
    public void contextWindowSizeMustBePositive() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        PropertySourceEditable propertySource = new PropertySourceEditable(properties, properties);
        propertySource.collectProperties();
        ObjectPropertyDescriptor descriptor = (ObjectPropertyDescriptor) propertySource.getProperty(
            AIConstants.AI_CONTEXT_SIZE_PROPERTY);

        Assertions.assertEquals(1, descriptor.getMinValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> descriptor.writeValue(properties, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> descriptor.writeValue(properties, -1));

        descriptor.writeValue(properties, 1);
        Assertions.assertEquals(1, properties.getContextWindowSize());
    }
}
