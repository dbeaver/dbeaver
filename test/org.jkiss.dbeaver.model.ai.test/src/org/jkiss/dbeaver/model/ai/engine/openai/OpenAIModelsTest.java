/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.junit.Test;

import static org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class OpenAIModelsTest extends DBeaverUnitTest {

    @Test
    public void effectiveModelNameNullShouldReturnDefaultModelName() {
        //when
        var result = getEffectiveModelName(null);
        //then
        assertEquals(DEFAULT_MODEL, result);
    }

    @Test
    public void effectiveModelNameKnownUppercaseShouldReturnKnownModelLowercase() {
        //given
        var expectedModelName = KNOWN_MODELS.keySet().stream().findFirst().orElseThrow();
        var inputModelName = expectedModelName.toUpperCase();
        //when
        var result = getEffectiveModelName(inputModelName);
        //then
        assertEquals(expectedModelName, result);
    }

    @Test
    public void effectiveModelNameUnknownUppercaseShouldReturnKnownModelUppercase() {
        //given
        var inputModelName = "some-UNKNOWN-MODEL";
        assumeFalse(KNOWN_MODELS.containsKey(inputModelName.toLowerCase()));
        //when
        var result = getEffectiveModelName(inputModelName);
        //then
        assertEquals(inputModelName, result);
    }


}
