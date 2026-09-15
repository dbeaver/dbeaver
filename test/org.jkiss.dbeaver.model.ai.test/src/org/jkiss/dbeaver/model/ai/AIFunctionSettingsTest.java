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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

public class AIFunctionSettingsTest extends DBeaverUnitTest {

    private static final String LEGACY_ID = "runtimeFunction";
    private static final String SETTINGS_ID = "sharedFunction";

    @Test
    public void preservesBehaviorWithoutSeparateSettingsId() {
        AIFunctionDescriptor function = createFunction(LEGACY_ID, null, true, AIFunctionAllowMode.ASK, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();

        Assertions.assertEquals(LEGACY_ID, function.getSettingsId());
        Assertions.assertTrue(settings.isFunctionEnabled(function));

        settings.setFunctionEnabled(function, false);
        Assertions.assertEquals(Set.of(LEGACY_ID), settings.getDisabledFunctions());
        Assertions.assertFalse(settings.isFunctionEnabled(function));

        settings.setFunctionAllowMode(function, AIFunctionAllowMode.ALWAYS_ALLOW);
        Assertions.assertEquals(Set.of(LEGACY_ID), settings.getAlwaysAllowedFunctions());
        Assertions.assertEquals(AIFunctionAllowMode.ALWAYS_ALLOW, settings.getFunctionAllowMode(function));
    }

    @Test
    public void readsCanonicalSettingsForDefaultEnabledAndDisabledFunctions() {
        AIFunctionDescriptor defaultEnabled = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, false);
        AIFunctionDescriptor defaultDisabled = createFunction(
            LEGACY_ID, SETTINGS_ID, false, AIFunctionAllowMode.ASK, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();

        settings.setDisabledFunctions(Set.of(SETTINGS_ID));
        Assertions.assertFalse(settings.isFunctionEnabled(defaultEnabled));

        settings.setEnabledFunctions(Set.of(SETTINGS_ID));
        Assertions.assertTrue(settings.isFunctionEnabled(defaultDisabled));
    }

    @Test
    public void fallsBackToLegacyRuntimeId() {
        AIFunctionDescriptor defaultEnabled = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, false);
        AIFunctionDescriptor defaultDisabled = createFunction(
            LEGACY_ID, SETTINGS_ID, false, AIFunctionAllowMode.ALWAYS_ALLOW, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();

        settings.setDisabledFunctions(Set.of(LEGACY_ID));
        Assertions.assertFalse(settings.isFunctionEnabled(defaultEnabled));

        settings.setEnabledFunctions(Set.of(LEGACY_ID));
        Assertions.assertTrue(settings.isFunctionEnabled(defaultDisabled));

        settings.setAlwaysAllowedFunctions(Set.of(LEGACY_ID));
        Assertions.assertEquals(AIFunctionAllowMode.ALWAYS_ALLOW, settings.getFunctionAllowMode(defaultEnabled));

        settings.setAlwaysAllowedFunctions(Set.of());
        settings.setAskFunctions(Set.of(LEGACY_ID));
        Assertions.assertEquals(AIFunctionAllowMode.ASK, settings.getFunctionAllowMode(defaultDisabled));
    }

    @Test
    public void writesOnlyCanonicalIdAndRemovesLegacyId() {
        AIFunctionDescriptor function = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();
        settings.setEnabledFunctions(Set.of(LEGACY_ID, SETTINGS_ID));
        settings.setDisabledFunctions(Set.of(LEGACY_ID, SETTINGS_ID));

        settings.setFunctionEnabled(function, false);

        Assertions.assertTrue(settings.getEnabledFunctions().isEmpty());
        Assertions.assertEquals(Set.of(SETTINGS_ID), settings.getDisabledFunctions());
    }

    @Test
    public void clearsOverridesWhenReturningToDefaultState() {
        AIFunctionDescriptor defaultEnabled = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, false);
        AIFunctionDescriptor defaultDisabled = createFunction(
            LEGACY_ID, SETTINGS_ID, false, AIFunctionAllowMode.ASK, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();

        settings.setDisabledFunctions(Set.of(LEGACY_ID, SETTINGS_ID));
        settings.setFunctionEnabled(defaultEnabled, true);
        Assertions.assertTrue(settings.getDisabledFunctions().isEmpty());

        settings.setEnabledFunctions(Set.of(LEGACY_ID, SETTINGS_ID));
        settings.setFunctionEnabled(defaultDisabled, false);
        Assertions.assertTrue(settings.getEnabledFunctions().isEmpty());

        settings.setAlwaysAllowedFunctions(Set.of(LEGACY_ID, SETTINGS_ID));
        settings.setAskFunctions(Set.of(LEGACY_ID, SETTINGS_ID));
        settings.setFunctionAllowMode(defaultEnabled, AIFunctionAllowMode.ASK);
        Assertions.assertTrue(settings.getAlwaysAllowedFunctions().isEmpty());
        Assertions.assertTrue(settings.getAskFunctions().isEmpty());
    }

    @Test
    public void readsAndWritesCanonicalAllowModeSettings() {
        AIFunctionDescriptor askByDefault = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, false);
        AIFunctionDescriptor allowByDefault = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ALWAYS_ALLOW, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();

        settings.setFunctionAllowMode(askByDefault, AIFunctionAllowMode.ALWAYS_ALLOW);
        Assertions.assertEquals(Set.of(SETTINGS_ID), settings.getAlwaysAllowedFunctions());
        Assertions.assertEquals(AIFunctionAllowMode.ALWAYS_ALLOW, settings.getFunctionAllowMode(askByDefault));

        settings.setFunctionAllowMode(allowByDefault, AIFunctionAllowMode.ASK);
        Assertions.assertTrue(settings.getAlwaysAllowedFunctions().isEmpty());
        Assertions.assertEquals(Set.of(SETTINGS_ID), settings.getAskFunctions());
        Assertions.assertEquals(AIFunctionAllowMode.ASK, settings.getFunctionAllowMode(allowByDefault));
    }

    @Test
    public void canonicalAllowModeTakesPriorityOverLegacyMode() {
        AIFunctionDescriptor function = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ALWAYS_ALLOW, false);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();
        settings.setAlwaysAllowedFunctions(Set.of(LEGACY_ID));
        settings.setAskFunctions(Set.of(SETTINGS_ID));

        Assertions.assertEquals(AIFunctionAllowMode.ASK, settings.getFunctionAllowMode(function));
    }

    @Test
    public void omitConfirmationPreservesExistingSemantics() {
        AIFunctionDescriptor function = createFunction(
            LEGACY_ID, SETTINGS_ID, true, AIFunctionAllowMode.ASK, true);
        AIFunctionSettings.ToolboxSettings settings = new AIFunctionSettings.ToolboxSettings();
        settings.setAskFunctions(Set.of(LEGACY_ID));

        Assertions.assertEquals(AIFunctionAllowMode.ALWAYS_ALLOW, settings.getFunctionAllowMode(function));
        settings.setFunctionAllowMode(function, AIFunctionAllowMode.ASK);
        Assertions.assertEquals(Set.of(LEGACY_ID), settings.getAskFunctions());
    }

    @NotNull
    private static AIFunctionDescriptor createFunction(
        @NotNull String id,
        @Nullable String settingsId,
        boolean enabledByDefault,
        @NotNull AIFunctionAllowMode defaultAllowMode,
        boolean omitConfirmation
    ) {
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(function.getId()).thenReturn(id);
        if (settingsId != null) {
            Mockito.when(function.getSettingsId()).thenReturn(settingsId);
        }
        Mockito.when(function.isEnabledByDefault()).thenReturn(enabledByDefault);
        Mockito.when(function.getDefaultAllowMode()).thenReturn(defaultAllowMode);
        Mockito.when(function.isOmitConfirmation()).thenReturn(omitConfirmation);
        return function;
    }
}
