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

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.ai.DDAIEngine;
import org.jkiss.dbeaver.model.datadam.ai.DDAIEngineProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class DDTemperatureTest {
    @Test
    void savedTemperatureReachesRequestWithoutLosingServerDefaultOrZero() throws Exception {
        Gson gson = new Gson();
        for (Double temperature : Arrays.asList(null, 0.0, 0.7)) {
            DDAIEngineProperties properties = new DDAIEngineProperties();
            properties.setModel("temperature-test-model");
            if (temperature == null) {
                properties.clearTemperature();
            } else {
                properties.setTemperature(temperature);
            }
            DDAIEngineProperties restored = gson.fromJson(gson.toJson(properties), DDAIEngineProperties.class);
            Assertions.assertEquals(temperature, restored.getRequestTemperature());
            try (TestEngine engine = new TestEngine(restored)) {
                Assertions.assertEquals(temperature, engine.requestTemperature());
            }
            restored.setTemperature(0.0);
            Assertions.assertEquals(0.0, gson.fromJson(gson.toJson(restored), DDAIEngineProperties.class).getRequestTemperature());
        }
    }

    private static class TestEngine extends DDAIEngine {
        private TestEngine(@NotNull DDAIEngineProperties properties) {
            super(properties);
        }

        @Nullable
        private Double requestTemperature() throws DBException {
            return getRequestTemperature();
        }
    }
}
