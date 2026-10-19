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

public class OpenAiUtilsTest extends DBeaverUnitTest {
    @Test
    public void recognizesUnsupportedAndDeprecatedTemperature() {
        Assertions.assertTrue(OpenAiUtils.isTemperatureNotSupported("`temperature` is deprecated for this model."));
        Assertions.assertTrue(OpenAiUtils.isTemperatureNotSupported("Unsupported parameter: 'temperature'"));
        Assertions.assertFalse(OpenAiUtils.isTemperatureNotSupported(null));
        Assertions.assertFalse(OpenAiUtils.isTemperatureNotSupported("temperature must be between 0 and 1"));
        Assertions.assertFalse(OpenAiUtils.isTemperatureNotSupported("`top_k` is deprecated for this model."));
        Assertions.assertFalse(OpenAiUtils.isTemperatureNotSupported("Unsupported parameter: 'top_k'"));
    }
}
