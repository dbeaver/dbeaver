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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ai.engine.openai.OpenAiConfigurator;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// native controls require a display and the macOS main thread
@EnabledIfEnvironmentVariable(named = "DBEAVER_SWT_TESTS", matches = "true")
class AIEnvironmentFieldsTest {
    private Display display;
    private Shell shell;
    private TestConfigurator configurator;

    @BeforeEach
    void createControls() {
        display = new Display();
        shell = new Shell(display);
        configurator = new TestConfigurator();
    }

    @AfterEach
    void disposeControls() {
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        if (display != null && !display.isDisposed()) {
            display.dispose();
        }
    }

    @Test
    void restoresUnsavedTextAfterEnvironmentOverride() {
        Text text = new Text(shell, SWT.PASSWORD);
        String[] configured = {"saved-key"};
        text.addModifyListener(event -> {
            if (!configurator.isUpdatingEnvironmentControls()) {
                configured[0] = text.getText();
            }
        });
        text.setText("edited-key");
        configurator.updateEnvironmentText(text, configured[0], "environment-key");
        Assertions.assertEquals("environment-key", text.getText());
        Assertions.assertFalse(text.getEnabled());
        Assertions.assertEquals("edited-key", configured[0]);
        configurator.updateEnvironmentText(text, configured[0], null);
        Assertions.assertEquals("edited-key", text.getText());
        Assertions.assertTrue(text.getEnabled());
        Assertions.assertFalse(configurator.isUpdatingEnvironmentControls());
    }

    @Test
    void locksEqualEnvironmentAndConfiguredValues() {
        Text text = new Text(shell, SWT.NONE);
        configurator.updateEnvironmentText(text, "same-key", "same-key");
        Assertions.assertEquals("same-key", text.getText());
        Assertions.assertFalse(text.getEnabled());
        configurator.updateEnvironmentText(text, "same-key", null);
        Assertions.assertTrue(text.getEnabled());
    }

    @Test
    void preservesCaretWhenUpdatingTextFromModifyListener() {
        Text text = new Text(shell, SWT.PASSWORD);
        text.setText("saved-token");
        text.addModifyListener(event -> {
            if (!configurator.isUpdatingEnvironmentControls()) {
                configurator.updateEnvironmentText(text, text.getText(), null);
            }
        });
        text.setSelection(5);
        text.insert("X");
        Assertions.assertEquals(new Point(6, 6), text.getSelection());
        text.insert("Y");
        Assertions.assertEquals("savedXY-token", text.getText());
        Assertions.assertEquals(new Point(7, 7), text.getSelection());
    }

    @Test
    void enablesTemperatureForEditableEnvironmentModel() {
        assertTemperatureFollowsEnvironmentModel("gpt-5", "gpt-4o", true);
    }

    @Test
    void disablesTemperatureForFixedEnvironmentModel() {
        assertTemperatureFollowsEnvironmentModel("gpt-4o", "gpt-5", false);
    }

    private void assertTemperatureFollowsEnvironmentModel(
        @NotNull String configuredModel,
        @NotNull String environmentModel,
        boolean editable
    ) {
        TestOpenAiConfigurator openAi = new TestOpenAiConfigurator(environmentModel);
        openAi.createControl(shell, Mockito.mock(AIEngineDescriptor.class), () -> { });
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel(configuredModel);
        properties.setToken("saved-token");
        properties.setUseEnvVariables(true);
        properties.setTemperature(0.25);
        properties.setContextWindowSize(12345);
        openAi.loadSettings(properties);

        Assertions.assertEquals(editable, openAi.temperatureEditable());
        openAi.setEnvironmentEnabled(false);
        Assertions.assertEquals(!editable, openAi.temperatureEditable());
        openAi.setEnvironmentEnabled(true);
        Assertions.assertEquals(editable, openAi.temperatureEditable());

        OpenAIProperties saved = new OpenAIProperties();
        openAi.saveSettings(saved);
        Assertions.assertEquals(configuredModel, saved.getModel());
        Assertions.assertEquals(0.25, saved.getTemperature());
        Assertions.assertEquals(12345, saved.getContextWindowSize());
    }

    @Test
    void restoresEmptyAndCustomReadOnlyComboValues() {
        Combo combo = new Combo(shell, SWT.READ_ONLY);
        combo.add("known-region");
        configurator.updateEnvironmentCombo(combo, "", "environment-region");
        Assertions.assertEquals("environment-region", combo.getText());
        Assertions.assertFalse(combo.getEnabled());
        configurator.updateEnvironmentCombo(combo, "", null);
        Assertions.assertEquals("", combo.getText());
        Assertions.assertTrue(combo.getEnabled());
        configurator.updateEnvironmentCombo(combo, "custom-region", null);
        Assertions.assertEquals("custom-region", combo.getText());
    }

    @Test
    void preservesModelAndSkipsMetadataChangesDuringOverride() {
        AtomicInteger modified = new AtomicInteger();
        ModelSelectorField field = ModelSelectorField.builder()
            .withParent(shell)
            .withGridData(new GridData())
            .withModelListSupplier((monitor, refresh) -> List.of())
            .withModifyListener(modified::incrementAndGet)
            .build();
        field.setSelectedModel("saved-model");
        Assertions.assertEquals(1, modified.get());
        field.setEnvironmentModel("environment-model");
        Assertions.assertEquals("saved-model", field.getSelectedModelName());
        Assertions.assertEquals(1, modified.get());
        field.setEnvironmentModel(null);
        Assertions.assertEquals("saved-model", field.getSelectedModelName());
        Assertions.assertEquals(1, modified.get());
    }

    @Test
    void loadsAnotherProfileAndClearsModelWhileOverrideIsActive() {
        ModelSelectorField field = ModelSelectorField.builder()
            .withParent(shell)
            .withGridData(new GridData())
            .withModelListSupplier((monitor, refresh) -> List.of())
            .build();
        field.setSelectedModel("first-model");
        field.setEnvironmentModel("environment-model");
        field.setSelectedModel("second-model");
        field.setEnvironmentModel(null);
        Assertions.assertEquals("second-model", field.getSelectedModelName());
        field.setEnvironmentModel("environment-model");
        field.setSelectedModel(null);
        field.setEnvironmentModel(null);
        Assertions.assertNull(field.getSelectedModelName());
    }

    private static class TestOpenAiConfigurator extends OpenAiConfigurator<AIEngineDescriptor, OpenAIProperties> {
        private final String environmentModel;

        private TestOpenAiConfigurator(@NotNull String environmentModel) {
            this.environmentModel = environmentModel;
        }

        @NotNull
        @Override
        protected OpenAIProperties createConnectionProperties() {
            return new OpenAIProperties() {
                @Nullable
                @Override
                protected String getEnvironmentModel() {
                    return TestOpenAiConfigurator.this.useEnvVariables ? environmentModel : null;
                }
            };
        }

        @NotNull
        @Override
        protected List<AIModel> fetchOpenAiModels(@NotNull DBRProgressMonitor monitor) {
            return List.of();
        }

        private boolean temperatureEditable() {
            return temperatureText.getEnabled();
        }

        private void setEnvironmentEnabled(boolean enabled) {
            useEnvVariablesCheck.setSelection(enabled);
            useEnvVariablesCheck.notifyListeners(SWT.Selection, new Event());
        }
    }

    private static class TestConfigurator extends AbstractAIEngineConfigurator<AIEngineDescriptor, AIEngineProperties> {
        @Override
        public void createControl(
            @NotNull Composite parent,
            @NotNull AIEngineDescriptor object,
            @NotNull Runnable propertyChangeListener
        ) {
        }

        @Override
        public void loadSettings(@NotNull AIEngineProperties properties) {
        }

        @Override
        public void saveSettings(@NotNull AIEngineProperties properties) {
        }

        @Override
        public void resetSettings(@NotNull AIEngineProperties properties) {
        }

        @Override
        public boolean isComplete() {
            return true;
        }
    }
}
