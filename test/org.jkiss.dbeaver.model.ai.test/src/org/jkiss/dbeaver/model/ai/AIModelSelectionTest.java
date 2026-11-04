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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

class AIModelSelectionTest {
    @Test
    void defaultSelectionKeepsOnlyChatModels() {
        AIEngine<?> engine = Mockito.mock(AIEngine.class, Mockito.CALLS_REAL_METHODS);
        AIModel chat = new AIModel("chat", null, Set.of(AIModelFeature.CHAT));
        AIModel embedding = new AIModel("embedding", null, Set.of(AIModelFeature.EMBEDDING));
        AIModel unknown = new AIModel("unknown", null, Set.of());

        List<AIModel> models = List.of(chat, embedding, unknown).stream().filter(engine::isChatModelSelectable).toList();

        Assertions.assertEquals(List.of(chat), models);
    }

    @Test
    void customSelectionSurvivesEngineClosureWithoutChangingFeatures() throws Exception {
        AIEngine<?> engine = Mockito.mock(AIEngine.class, Mockito.CALLS_REAL_METHODS);
        AIModel unknown = new AIModel("unknown", null, Set.of());
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Mockito.when(engine.getModels(monitor)).thenReturn(List.of(unknown));
        Mockito.when(engine.isChatModelSelectable(unknown)).thenReturn(true);
        List<AIModel> models;
        try (engine) {
            models = engine.getModels(monitor).stream().filter(engine::isChatModelSelectable).toList();
        }

        Assertions.assertEquals(List.of(unknown), models);
        Assertions.assertFalse(models.getFirst().features().contains(AIModelFeature.CHAT));
        Mockito.verify(engine).close();
    }
}
