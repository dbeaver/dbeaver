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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AIPreferencePageEngines extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(AIPreferencePageEngines.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.engines";
    private final AISettings settings;

    private AIConfigurationProfile selectedProfile;
    private TableViewer profilesViewer;

    private final Map<String, EngineConfiguratorPage> profileConfiguratorMapping = new HashMap<>();
    private EngineConfiguratorPage activeEngineConfiguratorPage;
    private Button connectionTestButton;
    private Text profileIdText;
    private Text profileNameText;
    private Composite engineGroup;

    public AIPreferencePageEngines() {
        this.settings = AISettingsManager.getInstance().getSettings();
        this.settings.resolveSecrets();
        this.selectedProfile = settings.getDefaultConfigurationOrNull();
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
        if (selectedProfile == null) {
            return null;
        }
        try {
            UIPropertyConfiguratorDescriptor engineDescriptor =
                UIPropertyConfiguratorRegistry.getInstance().getDescriptor(
                    selectedProfile.getEngineDescriptor().getEngineObjectType().getImplName());
            if (engineDescriptor != null) {
                return engineDescriptor.createConfigurator();
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    @Override
    public boolean performOk() {
        if (!hasAccessToPage()) {
            return false;
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        this.settings.setDefaultConfiguration(selectedProfile);
        if (selectedProfile != null) {
            selectedProfile.setProfileName(profileNameText.getText());
            try {
                activeEngineConfiguratorPage.saveSettings(selectedProfile.getConfiguration());
            } catch (DBException e) {
                log.error("Error saving engine settings", e);

                DBWorkbench.getPlatformUI().showError(
                    "Error saving AI settings",
                    "Error saving engine settings for " + selectedProfile.getEngineId(),
                    e
                );
            }
        }
        reloadEngines();
        AISettingsManager.getInstance().saveSettings();
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
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        CustomSashForm partDivider = UIUtils.createPartDivider(null, composite, SWT.VERTICAL);
        partDivider.setLayoutData(new GridData(GridData.FILL_BOTH));

        Button deleteProfileBtn;
        {
            Composite profilesComposite = UIUtils.createComposite(partDivider, 2);
            profilesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            profilesViewer = new TableViewer(profilesComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
            profilesViewer.setContentProvider(ArrayContentProvider.getInstance());
            Table table = profilesViewer.getTable();
            GridData gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = UIUtils.getFontHeight(table) * 6;
            table.setLayoutData(gridData);
/*
            table.addListener(SWT.MeasureItem, new Listener() {
                public void handleEvent(Event event) {
                    // Set the height for all rows in pixels
                    event.height = DBeaverIcons.getImage(AIIcons.AI).getBounds().height + 5;
                }
            });
*/
            createProfilesColumns();
            reloadEngines();

            Composite buttonsPanel = UIUtils.createComposite(profilesComposite, 1);
            buttonsPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createPushButton(
                buttonsPanel,
                null,
                "Create new profile",
                UIIcon.ADD,
                SelectionListener.widgetSelectedAdapter(e -> addNewProfile())
            );
            deleteProfileBtn = UIUtils.createPushButton(
                buttonsPanel,
                null,
                "Delete profile",
                UIIcon.DELETE,
                SelectionListener.widgetSelectedAdapter(e -> deleteProfile())
            );
        }

        Runnable refresher = () -> {
            engineGroup.getShell().layout(true, true);
            UIUtils.resizeShell(parent.getShell());
        };

        {
            Composite settingsPanel = UIUtils.createComposite(partDivider, 1);
            settingsPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            Composite profileGroup = UIUtils.createTitledComposite(
                settingsPanel,
                "Profile",
                4,
                GridData.FILL_HORIZONTAL
            );

            profileIdText = UIUtils.createLabelText(profileGroup, "ID", "", SWT.BORDER | SWT.READ_ONLY);
            profileNameText = UIUtils.createLabelText(profileGroup, "Name", "", SWT.BORDER);

            engineGroup = UIUtils.createTitledComposite(
                settingsPanel,
                "Settings",
                2,
                GridData.FILL_HORIZONTAL
            );

            profilesViewer.addSelectionChangedListener(event -> {
                Object selItem = profilesViewer.getStructuredSelection().getFirstElement();
                if (selItem instanceof AIConfigurationProfile profile) {
                    selectedProfile = profile;
                    showProfileSettings();
                    refresher.run();
                }
                deleteProfileBtn.setEnabled(selItem instanceof AIConfigurationProfile);
            });

            if (selectedProfile != null) {
                profilesViewer.setSelection(new StructuredSelection(selectedProfile));
                profilesViewer.reveal(selectedProfile);
            }

            performDefaults();

            createTestConnectionButton(settingsPanel);
        }

        partDivider.setWeights(200, 800);
        refresher.run();
        UIUtils.packColumns(profilesViewer.getTable(), true);

        UIUtils.asyncExec(() -> {
            // Fixes phantom table header on Linux
            table.setHeaderVisible(true);
            table.setHeaderVisible(false);
        });

        return composite;
    }

    private void reloadEngines() {
        profilesViewer.setInput(settings.getConfigurations());
    }

    private void addNewProfile() {
        AIProfileCreateDialog dialog = new AIProfileCreateDialog(getShell());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        try {
            AIConfigurationProfile newProfile = settings.createConfiguration(
                Objects.requireNonNull(dialog.getProfileId()),
                dialog.getSelectedEngine()
            );
            newProfile.setProfileName(Objects.requireNonNull(dialog.getProfileName()));
            reloadEngines();
            profilesViewer.setSelection(new StructuredSelection(newProfile));

            AISettingsManager.getInstance().saveSettings();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("New configuration", "Configuration create failed", e);
        }
    }

    private void deleteProfile() {
        if (!UIUtils.confirmAction(
            getShell(),
            "Delete configuration",
            "Are you sure you want to delete AI configuration '" + selectedProfile.getProfileName() + "'?"
        )) {
            return;
        }
        settings.removeConfiguration(selectedProfile);
        int selectionIndex = profilesViewer.getTable().getSelectionIndex();
        selectedProfile = null;
        showProfileSettings();

        reloadEngines();
        if (selectionIndex >= profilesViewer.getTable().getItemCount()) {
            selectionIndex--;
        }
        if (selectionIndex >= 0) {
            selectedProfile = settings.getConfigurations()[selectionIndex];
            profilesViewer.setSelection(new StructuredSelection(selectedProfile));
        } else {
            showProfileSettings();
        }

        AISettingsManager.getInstance().saveSettings();
    }

    private void createProfilesColumns() {
        TableViewerColumn nameColumn = new TableViewerColumn(profilesViewer, SWT.LEFT);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                if (!(element instanceof AIConfigurationProfile profile)) {
                    return null;
                }
                try {
                    return DBeaverIcons.getImage(profile.getEngineDescriptor().getIcon());
                } catch (DBException e) {
                    log.debug("Error reading engine icon", e);
                    return null;
                }
            }

            @Override
            public String getText(Object element) {
                if (!(element instanceof AIConfigurationProfile profile)) {
                    return null;
                }
                return profile.getProfileName();
            }

            @Override
            public Font getFont(Object element) {
                if (!(element instanceof AIConfigurationProfile profile)) {
                    return null;
                }
                if (profile == settings.getDefaultConfigurationOrNull()) {
                    return BaseThemeSettings.instance.baseFontBold;
                }
                return super.getFont(element);
            }
        });

        TableViewerColumn engineColumn = new TableViewerColumn(profilesViewer, SWT.LEFT);
        engineColumn.getColumn().setText("Model");
        engineColumn.getColumn().setWidth(200);
        engineColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (!(element instanceof AIConfigurationProfile profile)) {
                    return null;
                }
                try {
                    String engineName = profile.getEngineDescriptor().getLabel();
                    String model = profile.getConfiguration().getModel();
                    return CommonUtils.isEmpty(model) ? engineName :
                        engineName + " - " + model;
                } catch (DBException e) {
                    return e.getMessage();
                }
            }
        });
    }

    private void showProfileSettings() {
        Composite settingsComposite = engineGroup.getParent();
        if (activeEngineConfiguratorPage != null) {
            activeEngineConfiguratorPage.disposeControl();
        }
        if (selectedProfile == null) {
            profileIdText.setText("");
            profileNameText.setText("");
            profileNameText.setEnabled(false);
            settingsComposite.setVisible(false);
            return;
        }
        profileNameText.setEnabled(true);
        settingsComposite.setVisible(true);
        profileIdText.setText(selectedProfile.getProfileId());
        profileNameText.setText(selectedProfile.getProfileName());

        activeEngineConfiguratorPage = profileConfiguratorMapping.get(selectedProfile.getEngineId());

        AIEngineDescriptor engineDescriptor;
        try {
            engineDescriptor = selectedProfile.getEngineDescriptor();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI()
                .showError(
                    "Configurator create error",
                    "Engine configurator for " + selectedProfile.getEngineId() + " was failed",
                    e
                );
            return;
        }
        if (activeEngineConfiguratorPage == null) {
            AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> engineConfigurator
                = createEngineConfigurator();
            if (engineConfigurator == null) {
                DBWorkbench.getPlatformUI()
                    .showError("Configurator not found", "Engine configurator not found for " + selectedProfile.getEngineId());
                return;
            }
            activeEngineConfiguratorPage = new EngineConfiguratorPage(engineConfigurator);
            activeEngineConfiguratorPage.createControl(engineGroup, engineDescriptor);
            profileConfiguratorMapping.put(selectedProfile.getEngineId(), activeEngineConfiguratorPage);
        } else {
            activeEngineConfiguratorPage.createControl(engineGroup, engineDescriptor);
        }

        try {
            activeEngineConfiguratorPage.loadSettings(selectedProfile.getConfiguration());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                "Error loading AI settings",
                "Error loading engine settings for " + selectedProfile.getEngineId(),
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
                if (selectedProfile == null) {
                    return;
                }
                String engineId = selectedProfile.getEngineId();
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

        connectionTestButton.setEnabled(
            activeEngineConfiguratorPage != null && activeEngineConfiguratorPage.getCurrentProperties().isPresent());
    }

    private void testConnection() throws DBException, InterruptedException, InvocationTargetException {
        Optional<AIEngineProperties> currentProperties = activeEngineConfiguratorPage.getCurrentProperties();
        try (
            AIEngine<?> selectedEngine = currentProperties.isPresent()
                ? selectedProfile.getEngineDescriptor().createEngineInstance(currentProperties.get())
                : selectedProfile.getEngineDescriptor().createEngineInstance(selectedProfile)
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
