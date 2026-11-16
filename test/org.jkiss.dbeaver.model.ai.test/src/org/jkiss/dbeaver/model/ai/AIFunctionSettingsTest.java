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

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

class AIFunctionSettingsTest {
    private static final String FUNCTION_ID = "uiOpenDBeaverSQLEditor";
    private static final String LEGACY_FUNCTION_ID = "openSQLEditor";

    private AIFunctionDescriptor function;
    private AIFunctionSettings.ToolboxSettings settings;

    @BeforeEach
    void setUp() {
        function = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(function.getId()).thenReturn(FUNCTION_ID);
        Mockito.when(function.getLegacyId()).thenReturn(LEGACY_FUNCTION_ID);
        Mockito.when(function.isEnabledByDefault()).thenReturn(true);
        Mockito.when(function.getDefaultAllowMode()).thenReturn(AIFunctionAllowMode.ASK);
        settings = new AIFunctionSettings.ToolboxSettings();
    }

    @Test
    void legacyDisabledIdDisablesCanonicalFunction() {
        settings.getDisabledFunctions().add(LEGACY_FUNCTION_ID);

        Assertions.assertFalse(settings.isFunctionEnabled(function));
    }

    @Test
    void updatingFunctionReplacesLegacyIdWithCanonicalId() {
        settings.getDisabledFunctions().add(LEGACY_FUNCTION_ID);

        settings.setFunctionEnabled(function, true);
        Assertions.assertTrue(settings.getDisabledFunctions().isEmpty());

        settings.setFunctionEnabled(function, false);
        Assertions.assertEquals(Set.of(FUNCTION_ID), settings.getDisabledFunctions());
    }

    @Test
    void askModeWinsOverAlwaysAllowForConflictingLegacySettings() {
        settings.getAlwaysAllowedFunctions().add(FUNCTION_ID);
        settings.getAskFunctions().add(LEGACY_FUNCTION_ID);

        Assertions.assertEquals(AIFunctionAllowMode.ASK, settings.getFunctionAllowMode(function));
    }

    @Test
    void updatingAllowModeReplacesLegacyIdWithCanonicalId() {
        settings.getAskFunctions().add(LEGACY_FUNCTION_ID);

        settings.setFunctionAllowMode(function, AIFunctionAllowMode.ALWAYS_ALLOW);

        Assertions.assertTrue(settings.getAskFunctions().isEmpty());
        Assertions.assertEquals(Set.of(FUNCTION_ID), settings.getAlwaysAllowedFunctions());
    }

    @Test
    void migratesDisabledTableReadOnLoadAndSavesOnlyCanonicalName() {
        AIFunctionSettings loaded = JSONUtils.GSON.fromJson("""
            {"functions":{"db":{"df":["readTableSampleRows"]}}}
            """, AIFunctionSettings.class);
        loaded.finishSettingsLoading();
        AIToolbox toolbox = Mockito.mock(AIToolbox.class);
        Mockito.when(toolbox.getToolboxId()).thenReturn(AIConstants.INTERNAL_TOOLBOX_ID);
        AIFunctionDescriptor tableRead = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(tableRead.getId()).thenReturn("readTableData");
        Mockito.when(tableRead.isEnabledByDefault()).thenReturn(true);
        AIFunctionSettings.ToolboxSettings internal = loaded.getToolboxSettings(toolbox);

        Assertions.assertFalse(internal.isFunctionEnabled(tableRead));
        Assertions.assertEquals(Set.of("readTableData"), internal.getDisabledFunctions());
        String saved = JSONUtils.GSON.toJson(loaded);
        Assertions.assertFalse(saved.contains("readTableSampleRows"));
        Assertions.assertTrue(saved.contains("readTableData"));

        internal.setFunctionEnabled(tableRead, true);
        Assertions.assertTrue(internal.isFunctionEnabled(tableRead));
        Assertions.assertTrue(internal.getDisabledFunctions().isEmpty());
    }

    @Test
    void migratesAllInternalFunctionSettingsWithoutChangingExternalToolboxes() {
        AIFunctionSettings loaded = JSONUtils.GSON.fromJson("""
            {"functions":{
                "db":{
                    "ef":["readTableSampleRows","readTableData","otherFunction"],
                    "df":["readTableSampleRows"],
                    "aaf":["readTableSampleRows"],
                    "qf":["readTableSampleRows"]
                },
                "external":{"df":["readTableSampleRows"]}
            }}
            """, AIFunctionSettings.class);
        loaded.finishSettingsLoading();
        AIToolbox toolbox = Mockito.mock(AIToolbox.class);
        Mockito.when(toolbox.getToolboxId()).thenReturn(AIConstants.INTERNAL_TOOLBOX_ID);
        AIFunctionSettings.ToolboxSettings internal = loaded.getToolboxSettings(toolbox);
        Assertions.assertEquals(Set.of("readTableData", "otherFunction"), internal.getEnabledFunctions());
        Assertions.assertEquals(Set.of("readTableData"), internal.getDisabledFunctions());
        Assertions.assertEquals(Set.of("readTableData"), internal.getAlwaysAllowedFunctions());
        Assertions.assertEquals(Set.of("readTableData"), internal.getAskFunctions());

        Mockito.when(toolbox.getToolboxId()).thenReturn("external");
        Assertions.assertEquals(Set.of("readTableSampleRows"), loaded.getToolboxSettings(toolbox).getDisabledFunctions());
        String saved = JSONUtils.GSON.toJson(loaded);
        loaded.finishSettingsLoading();
        Assertions.assertEquals(saved, JSONUtils.GSON.toJson(loaded));
    }
}
