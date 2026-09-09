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
package org.jkiss.dbeaver.model.ai.registry;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

public class AISettingsManagerTest extends DBeaverUnitTest {

    @Test
    public void skipsUnknownLegacyEngineConfiguration() throws Exception {
        String config = """
            {
              "unsupported": {
                "properties": {
                  "applicableAuthTypes": [
                    {
                      "title": "Unsupported"
                    }
                  ]
                }
              }
            }
            """;

        try (JsonReader reader = new JsonReader(new StringReader(config))) {
            Assertions.assertTrue(new AISettingsManager.EngineConfigAdapter().read(reader).isEmpty());
            Assertions.assertEquals(JsonToken.END_DOCUMENT, reader.peek());
        }
    }
}
