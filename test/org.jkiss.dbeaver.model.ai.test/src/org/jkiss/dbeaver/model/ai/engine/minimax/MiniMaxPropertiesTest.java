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

public class MiniMaxPropertiesTest extends DBeaverUnitTest {

    @Test
    public void defaultBaseUrlShouldBeMiniMaxEndpoint() {
        MiniMaxProperties props = new MiniMaxProperties();
        assertEquals(MiniMaxConstants.MINIMAX_ENDPOINT, props.getBaseUrl());
    }

    @Test
    public void customBaseUrlShouldBeUsed() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setBaseUrl("https://custom.example.com/v1/");
        assertEquals("https://custom.example.com/v1/", props.getBaseUrl());
    }

    @Test
    public void emptyBaseUrlShouldFallbackToDefault() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setBaseUrl("");
        assertEquals(MiniMaxConstants.MINIMAX_ENDPOINT, props.getBaseUrl());
    }

    @Test
    public void defaultModelShouldBeMiniMaxM3() {
        MiniMaxProperties props = new MiniMaxProperties();
        assertEquals(MiniMaxConstants.DEFAULT_MODEL, props.getModel());
        assertEquals("MiniMax-M3", props.getModel());
    }

    @Test
    public void customModelShouldBeUsed() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setModel("MiniMax-M2.7-highspeed");
        assertEquals("MiniMax-M2.7-highspeed", props.getModel());
    }

    @Test
    public void defaultTemperatureShouldBeOne() {
        MiniMaxProperties props = new MiniMaxProperties();
        assertEquals(1.0, props.getTemperature(), 0.001);
    }

    @Test
    public void isLegacyApiShouldAlwaysBeTrue() {
        MiniMaxProperties props = new MiniMaxProperties();
        assertTrue(props.isLegacyApi());
    }

    @Test
    public void isValidConfigurationShouldBeFalseWithoutToken() {
        MiniMaxProperties props = new MiniMaxProperties();
        assertFalse(props.isValidConfiguration());
    }

    @Test
    public void isValidConfigurationShouldBeTrueWithToken() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setToken("test-token");
        assertTrue(props.isValidConfiguration());
    }

    @Test
    public void clampTemperatureZeroShouldReturnDefault() {
        assertEquals(
            MiniMaxConstants.DEFAULT_TEMPERATURE,
            MiniMaxProperties.clampTemperature(0.0),
            0.001
        );
    }

    @Test
    public void clampTemperatureNegativeShouldReturnDefault() {
        assertEquals(
            MiniMaxConstants.DEFAULT_TEMPERATURE,
            MiniMaxProperties.clampTemperature(-0.5),
            0.001
        );
    }

    @Test
    public void clampTemperatureAboveOneShouldClampToOne() {
        assertEquals(1.0, MiniMaxProperties.clampTemperature(1.5), 0.001);
    }

    @Test
    public void clampTemperatureValidShouldPassThrough() {
        assertEquals(0.7, MiniMaxProperties.clampTemperature(0.7), 0.001);
    }

    @Test
    public void contextWindowSizeShouldDefaultFromModel() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setModel("MiniMax-M3");
        assertEquals(Integer.valueOf(512_000), props.getContextWindowSize());
    }

    @Test
    public void contextWindowSizeCustomShouldOverrideModel() {
        MiniMaxProperties props = new MiniMaxProperties();
        props.setModel("MiniMax-M3");
        props.setContextWindowSize(100_000);
        assertEquals(Integer.valueOf(100_000), props.getContextWindowSize());
    }
}
