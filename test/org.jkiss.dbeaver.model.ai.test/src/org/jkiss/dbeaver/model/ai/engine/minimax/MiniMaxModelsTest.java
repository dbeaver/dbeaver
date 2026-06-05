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
package org.jkiss.dbeaver.model.ai.engine.minimax;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class MiniMaxModelsTest extends DBeaverUnitTest {

    @Test
    public void knownModelsShouldContainExpectedModels() {
        assertTrue(MiniMaxModels.KNOWN_MODELS.containsKey("MiniMax-M3"));
        assertTrue(MiniMaxModels.KNOWN_MODELS.containsKey("MiniMax-M2.7"));
        assertTrue(MiniMaxModels.KNOWN_MODELS.containsKey("MiniMax-M2.7-highspeed"));
        assertEquals(3, MiniMaxModels.KNOWN_MODELS.size());
    }

    @Test
    public void getModelByNameShouldReturnKnownModel() {
        var model = MiniMaxModels.getModelByName("MiniMax-M3");
        assertTrue(model.isPresent());
        assertEquals("MiniMax-M3", model.get().name());
        assertEquals(Integer.valueOf(512_000), model.get().contextWindowSize());
    }

    @Test
    public void getModelByNameShouldReturnEmptyForUnknown() {
        var model = MiniMaxModels.getModelByName("unknown-model");
        assertFalse(model.isPresent());
    }

    @Test
    public void getModelByNameNullShouldReturnEmpty() {
        var model = MiniMaxModels.getModelByName(null);
        assertFalse(model.isPresent());
    }

    @Test
    public void modelDefaultTemperatureShouldBeOne() {
        var model = MiniMaxModels.getModelByName("MiniMax-M3");
        assertTrue(model.isPresent());
        assertEquals(1.0, model.get().defaultTemperature(), 0.001);
    }
}
