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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AIPreferencePageMain extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(AIPreferencePageMain.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai";
    private final AISettings settings;

    private AIEngineDescriptor completionEngine;
    private Combo serviceCombo;

    private final Map<String, String> serviceNameMappings = new HashMap<>();
    private final Map<String, EngineConfiguratorPage> engineConfiguratorMapping = new HashMap<>();
    private EngineConfiguratorPage activeEngineConfiguratorPage;
    private Button enableAICheck;
    private Button connectionTestButton;

    public AIPreferencePageMain() {
        this.settings = AISettingsManager.getInstance().getSettings();
        String activeEngine = this.settings.activeEngine();
        completionEngine = AIEngineRegistry.getInstance().getEngineDescriptor(activeEngine);
    }

    @Override
    public IAdaptable getElement() {
        return this.settings;
    }

    @Override
    public void setElement(IAdaptable element) {

    }

    @Nullable
    private AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> createEngineConfigurator() {
        UIPropertyConfiguratorDescriptor engineDescriptor =
            UIPropertyConfiguratorRegistry.getInstance().getDescriptor(completionEngine.getEngineObjectType().getImplName());
        if (engineDescriptor != null) {
            try {
                return engineDescriptor.createConfigurator();
            } catch (DBException e) {
                log.error(e);
            }
        }
        return null;
    }

    @Override
    protected void performDefaults() {
        if (!hasAccessToPage()) {
            return;
        }
        enableAICheck.setSelection(!this.settings.isAiDisabled());
    }

    @Override
    public boolean performOk() {
        if (!hasAccessToPage()) {
            return false;
        }
        this.settings.setAiDisabled(!enableAICheck.getSelection());
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        this.settings.setActiveEngine(serviceNameMappings.get(serviceCombo.getText()));
        if (!serviceCombo.getText().isEmpty()) {
            for (Map.Entry<String, EngineConfiguratorPage> entry : engineConfiguratorMapping.entrySet()) {
                try {
                    AIEngineProperties engineConfiguration = this.settings.getEngineConfiguration(entry.getKey());
                    entry.getValue().saveSettings(engineConfiguration);
                } catch (DBException e) {
                    log.error("Error saving engine settings", e);

                    DBWorkbench.getPlatformUI().showError(
                        "Error saving AI settings",
                        "Error saving engine settings for " + entry.getKey(),
                        e
                    );
                }
            }
        }
        AISettingsManager.getInstance().saveSettings(this.settings);
        try {
            store.save();
        } catch (IOException e) {
            log.debug(e);
        }

        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        enableAICheck = UIUtils.createCheckbox(
            composite,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_label,
            AIUIMessages.gpt_preference_page_checkbox_enable_ai_tip,
            false,
            2);

        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite serviceComposite = UIUtils.createComposite(composite, 2);
        serviceComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        serviceCombo = UIUtils.createLabelCombo(serviceComposite, "Engine", SWT.DROP_DOWN | SWT.READ_ONLY);
        List<AIEngineDescriptor> completionEngines = AIEngineRegistry.getInstance()
            .getCompletionEngines();
        completionEngines.sort(Comparator.comparing(AIEngineDescriptor::getLabel));
        int defaultEngineSelection = -1;
        for (int i = 0; i < completionEngines.size(); i++) {
            serviceCombo.add(completionEngines.get(i).getLabel());
            serviceNameMappings.put(completionEngines.get(i).getLabel(), completionEngines.get(i).getId());
            if (completionEngines.get(i).isDefault()) {
                defaultEngineSelection = i;
            }
            if (completionEngines.get(i).getId().equals(this.settings.activeEngine())) {
                serviceCombo.select(i);
            }
        }
        if (serviceCombo.getSelectionIndex() == -1 && defaultEngineSelection != -1) {
            serviceCombo.select(defaultEngineSelection);
        }

        Composite engineGroup = UIUtils.createTitledComposite(composite, "Engine Settings", 2, SWT.BORDER, 5);
        engineGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (completionEngine != null) {
            drawConfiguratorComposite(this.settings.activeEngine(), engineGroup);
        }
        serviceCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String id = serviceNameMappings.get(serviceCombo.getText());
                completionEngine = AIEngineRegistry.getInstance().getEngineDescriptor(id);
                if (activeEngineConfiguratorPage != null) {
                    activeEngineConfiguratorPage.disposeControl();
                }
                drawConfiguratorComposite(id, engineGroup);
                engineGroup.layout(true, true);
                UIUtils.resizeShell(parent.getShell());
            }
        });
        performDefaults();

        createTestConnectionButton(composite);
        return composite;
    }

    private void drawConfiguratorComposite(@NotNull String id, @NotNull Composite engineGroup) {
        activeEngineConfiguratorPage = engineConfiguratorMapping.get(id);

        if (activeEngineConfiguratorPage == null) {
            AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> engineConfigurator
                = createEngineConfigurator();
            if (engineConfigurator == null) {
                log.error("Engine configurator not found for " + completionEngine.getId());
            }
            activeEngineConfiguratorPage = new EngineConfiguratorPage(engineConfigurator);
            activeEngineConfiguratorPage.createControl(engineGroup, completionEngine);
            engineConfiguratorMapping.put(id, activeEngineConfiguratorPage);
        } else {
            activeEngineConfiguratorPage.createControl(engineGroup, completionEngine);
        }

        try {
            activeEngineConfiguratorPage.loadSettings(this.settings.getEngineConfiguration(id));
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                "Error loading AI settings",
                "Error loading engine settings for " + id,
                e
            );
        }
        if (Objects.nonNull(connectionTestButton)) {
            connectionTestButton.setEnabled(activeEngineConfiguratorPage.getCurrentProperties().isPresent());
        }
    }


    private void createTestConnectionButton(@NotNull Composite parent) {
        connectionTestButton = UIUtils.createPushButton(
            parent,
            AIUIMessages.gpt_preference_page_ai_connection_test_label,
            null,
            SelectionListener.widgetSelectedAdapter(e -> {
                String engineId = serviceCombo.getText();
                try {
                    testConnection();
                    DBWorkbench.getPlatformUI().showMessageBox(
                        AIUIMessages.gpt_preference_page_ai_connection_test_connection_success_title,
                        NLS.bind(
                            AIUIMessages.gpt_preference_page_ai_connection_test_connection_success_message,
                            engineId
                        ),
                        false
                    );
                } catch (DBException | InterruptedException ex) {
                    showConnectionErrorMessage(ex, engineId);
                } catch (InvocationTargetException ex) {
                    showConnectionErrorMessage(ex.getCause(), engineId);
                }
            })
        );

        connectionTestButton.setEnabled(activeEngineConfiguratorPage.getCurrentProperties().isPresent());
    }

    private void testConnection() throws DBException, InterruptedException, InvocationTargetException {
        Optional<AIEngineProperties> currentProperties = activeEngineConfiguratorPage.getCurrentProperties();
        try (
            AIEngine selectedEngine = currentProperties.isPresent()
                ? completionEngine.createEngineInstance(currentProperties.get())
                : completionEngine.createEngineInstance()
        ) {
            UIUtils.getDialogRunnableContext().run(true, true, monitor -> {
                try {
                    selectedEngine.getModels(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
    }


    private void showConnectionErrorMessage(Throwable ex, String engineId) {
        DBWorkbench.getPlatformUI().showError(
            AIUIMessages.gpt_preference_page_ai_connection_test_connection_error_title,
            NLS.bind(AIUIMessages.gpt_preference_page_ai_connection_test_connection_error_message, engineId),
            ex
        );
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    private static class EngineConfiguratorPage {
        private final AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> configurator;
        private Composite composite;

        EngineConfiguratorPage(@Nullable AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> configurator) {
            this.configurator = configurator;
        }

        private void createControl(Composite parent, AIEngineDescriptor engine) {
            composite = UIUtils.createComposite(parent, 1);
            composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (configurator != null) {
                configurator.createControl(composite, engine, () -> {});
            }
        }

        private void disposeControl() {
            composite.dispose();
        }

        private void loadSettings(AIEngineProperties settings) {
            if (configurator != null) {
                configurator.loadSettings(settings);
            }
        }

        private void saveSettings(AIEngineProperties settings) {
            if (configurator != null) {
                configurator.saveSettings(settings);
            }
        }

        private Optional<AIEngineProperties> getCurrentProperties() {
            return Optional
                .ofNullable(configurator)
                .flatMap(AIIObjectPropertyConfigurator::getCurrentProperties);
        }
    }



    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER);
    }
}
