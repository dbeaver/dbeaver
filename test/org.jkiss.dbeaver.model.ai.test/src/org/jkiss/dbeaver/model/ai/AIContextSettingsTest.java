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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AIContextSettingsTest extends DBeaverUnitTest {
    @Test
    public void loadsSettingsFlattenedByLegacyDataSourceSerializer() {
        AIContextSettings settings = new TestContextSettings();

        settings.loadSettingsFromString("{confirmed=true, mcpEnabled=false}");

        Assertions.assertTrue(settings.isMetaTransferConfirmed());
        Assertions.assertFalse(settings.isMcpEnabled());
    }

    @Test
    public void datasourceDefaultsToCurrentDatasourceWithoutConnecting() {
        AIContextSettingsDataSource settings = createDataSourceSettings(Map.of());

        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, settings.getScope());
        Assertions.assertFalse(settings.isMetaTransferConfirmed());
        Assertions.assertFalse(settings.isMcpEnabled());
        Mockito.verify(settings.getDataSourceContainer(), Mockito.never()).getDataSource();
        Assertions.assertNull(settings.saveSettingsToMap().get("scope"));
    }

    @Test
    public void datasourceEditsDoNotModifyCachedSettings() {
        AIContextSettings.PersistentSettings cached = new AIContextSettings.PersistentSettings();
        cached.confirmed = true;
        cached.mcpEnabled = true;
        cached.datadamMcpEnabled = true;
        cached.scope = AIDatabaseScope.CUSTOM;
        cached.objects = new String[] {"schema/table", ""};
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getExtensionModel(
            AIContextSettingsDataSource.AI_DS_EXTENSION, AIContextSettings.PersistentSettings.class)).thenReturn(cached);
        AIContextSettingsDataSource settings = new AIContextSettingsDataSource(container);
        Assertions.assertArrayEquals(new String[] {"schema/table"}, settings.getCustomObjectIds());

        settings.setMetaTransferConfirmed(false);
        settings.setMcpEnabled(false);
        settings.setDatadamMcpEnabled(false);
        settings.setScope(AIDatabaseScope.CURRENT_SCHEMA);
        settings.getCustomObjectIds()[0] = "other/table";

        AIContextSettingsDataSource reloaded = new AIContextSettingsDataSource(container);
        Assertions.assertTrue(reloaded.isMetaTransferConfirmed());
        Assertions.assertTrue(reloaded.isMcpEnabled());
        Assertions.assertTrue(reloaded.isDatadamMcpEnabled());
        Assertions.assertEquals(AIDatabaseScope.CUSTOM, reloaded.getScope());
        Assertions.assertArrayEquals(new String[] {"schema/table"}, reloaded.getCustomObjectIds());
        Mockito.verify(container, Mockito.never()).setExtensionModel(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void datasourceSavesTypedSettingsWithoutPersistingUntilRequested() {
        AIContextSettingsDataSource settings = createDataSourceSettings(Map.of("confirmed", true));
        DBPDataSourceContainer container = settings.getDataSourceContainer();

        settings.saveSettingsToDataSource();

        Mockito.verify(container).setExtensionModel(AIContextSettingsDataSource.AI_DS_EXTENSION, settings.settings);
        Mockito.verify(container, Mockito.never()).persistConfiguration();
        settings.saveSettings();
        Mockito.verify(container).persistConfiguration();
    }

    @Test
    public void datasourceSavesToCurrentWizardTargetInsteadOfWorkingCopy() {
        AIContextSettingsDataSource settings = createDataSourceSettings(Map.of());
        DBPDataSourceContainer workingCopy = settings.getDataSourceContainer();
        DBPDataSourceContainer comparisonCopy = Mockito.mock(DBPDataSourceContainer.class);
        DBPDataSourceContainer original = Mockito.mock(DBPDataSourceContainer.class);
        settings.setMetaTransferConfirmed(true);
        settings.setMcpEnabled(true);
        settings.setScope(AIDatabaseScope.CUSTOM);
        settings.setCustomObjectIds(new String[] {"schema/table"});

        for (DBPDataSourceContainer target : List.of(comparisonCopy, original)) {
            settings.saveSettingsToDataSource(target);
            Mockito.verify(target).setExtensionModel(AIContextSettingsDataSource.AI_DS_EXTENSION, settings.settings);
            Mockito.verify(target, Mockito.never()).persistConfiguration();
        }

        Mockito.verify(workingCopy, Mockito.never()).setExtensionModel(Mockito.anyString(), Mockito.any());
        Mockito.verify(workingCopy, Mockito.never()).persistConfiguration();
    }

    @Test
    public void datasourceDefaultsAfterResetAndExplicitNullScope() {
        AIContextSettingsDataSource settings = createDataSourceSettings(Map.of("scope", "CURRENT_SCHEMA"));

        settings.loadSettingsFromMap(Map.of());
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, settings.getScope());

        settings.loadSettingsFromMap(Collections.singletonMap("scope", null));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, settings.getScope());
    }

    @Test
    public void datasourcePreservesExplicitScopes() {
        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            AIContextSettingsDataSource settings = createDataSourceSettings(Map.of("scope", scope.name()));

            Assertions.assertEquals(scope, settings.getScope());
        }
    }

    @Test
    public void initializesUnsetScopeWithoutInspectingConnectionOrExecutionContext() {
        AIContextSettings settings = new TestContextSettings();
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        Assertions.assertNull(settings.getScope());

        AIUtils.updateScopeSettingsIfNeeded(settings, container, executionContext);

        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, settings.getScope());
        Mockito.verifyNoInteractions(container, executionContext);
    }

    @Test
    public void defaultInitializationPreservesExplicitScopesAndCustomObjects() {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            AIContextSettings settings = new TestContextSettings();
            settings.setScope(scope);
            settings.setCustomObjectIds(new String[] {"schema/table"});

            AIUtils.updateScopeSettingsIfNeeded(settings, container, null);

            Assertions.assertEquals(scope, settings.getScope());
            Assertions.assertArrayEquals(new String[] {"schema/table"}, settings.getCustomObjectIds());
        }
    }

    @Test
    public void conversationInheritsCustomDatasourceScope() {
        AIContextSettingsDataSource dataSourceSettings = createDataSourceSettings(Map.of(
            "scope", "CUSTOM",
            "objects", List.of("schema/table")
        ));
        AIContextSettingsChatConversation settings = createConversationSettings(dataSourceSettings);
        Assertions.assertNull(settings.getScope());

        settings.loadDataSourceDefaults();

        Assertions.assertEquals(AIDatabaseScope.CUSTOM, settings.getScope());
        Assertions.assertArrayEquals(new String[] {"schema/table"}, settings.getCustomObjectIds());
    }

    @Test
    public void conversationInheritsGlobalDefaultButPreservesExplicitScope() {
        AIContextSettingsDataSource dataSourceSettings = createDataSourceSettings(Map.of());
        AIContextSettingsChatConversation settings = createConversationSettings(dataSourceSettings);

        settings.loadDataSourceDefaults();
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, settings.getScope());

        settings.setScope(AIDatabaseScope.CURRENT_SCHEMA);
        settings.loadDataSourceDefaults();
        Assertions.assertEquals(AIDatabaseScope.CURRENT_SCHEMA, settings.getScope());
    }

    @NotNull
    private AIContextSettingsDataSource createDataSourceSettings(@NotNull Map<String, Object> config) {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getExtension(AIContextSettingsDataSource.AI_DS_EXTENSION)).thenReturn(config);
        Mockito.when(container.getExtensionModel(
            AIContextSettingsDataSource.AI_DS_EXTENSION, AIContextSettings.PersistentSettings.class))
            .thenReturn(JSONUtils.convertMapToObject(config, AIContextSettings.PersistentSettings.class));
        return new AIContextSettingsDataSource(container);
    }

    @NotNull
    private AIContextSettingsChatConversation createConversationSettings(@NotNull AIContextSettingsDataSource dataSourceSettings) {
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.getDataSource()).thenReturn(dataSourceSettings.getDataSourceContainer());
        return new AIContextSettingsChatConversation(Mockito.mock(AIChatSession.class), conversation);
    }

    private static class TestContextSettings extends AIContextSettings {
        @Nullable
        @Override
        public DBPDataSourceContainer getDataSourceContainer() {
            return null;
        }

        @Override
        public void saveSettings() {
        }
    }
}
