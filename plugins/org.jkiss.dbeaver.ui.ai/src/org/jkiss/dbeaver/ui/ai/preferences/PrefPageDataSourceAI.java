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
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class PrefPageDataSourceAI extends AbstractPrefPage implements IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(PrefPageDataSourceAI.class);
    private static final String CSS_CLASS_FORM_HINT = "dbeaverFormHint";

    private IAdaptable element;
    private AIContextSettingsDataSource settings;
    private Button enableAiCheck;
    private Combo metadataScopeCombo;
    private IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> aiConfigurator;
    private Composite aiConfiguratorContent;
    private ControlEnableState aiConfiguratorEnableState;

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            settings = new AIContextSettingsDataSource(connection);
            createConnectionSettings(content, connection);
            injectConfigurators(content);
            UIPropertyConfiguratorDescriptor descriptor = UIPropertyConfiguratorRegistry.getInstance()
                .getDescriptor(AIContextSettingsDataSource.class.getName());
            if (descriptor != null) {
                try {
                    IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> configurator =
                        descriptor.createConfigurator();
                    aiConfiguratorContent = UIUtils.createComposite(content, 1);
                    aiConfiguratorContent.setLayoutData(new GridData(GridData.FILL_BOTH));
                    configurator.createControl(aiConfiguratorContent, settings, () -> {});
                    configurator.loadSettings(this);
                    enableAiCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                        event -> updateConfiguratorEnablement(aiConfiguratorContent)));
                    updateConfiguratorEnablement(aiConfiguratorContent);
                    aiConfigurator = configurator;
                } catch (DBException e) {
                    log.error("Cannot load AI connection settings", e);
                }
            }
        }
        return content;
    }

    private void updateConfiguratorEnablement(@NotNull Composite content) {
        boolean enabled = enableAiCheck.getSelection();
        if (enabled) {
            restoreConfiguratorEnablement(content);
        } else if (aiConfiguratorEnableState == null) {
            aiConfiguratorEnableState = ControlEnableState.disable(content);
        }
        CSSUtils.setCSSClass(content, enabled ? null : CSS_CLASS_FORM_HINT);
        CSSUtils.applyStyles(content);
    }

    private void restoreConfiguratorEnablement(@NotNull Composite content) {
        if (aiConfiguratorEnableState != null) {
            aiConfiguratorEnableState.restore();
            aiConfiguratorEnableState = null;
            resetConfiguratorTextColors(content);
        }
    }

    private void resetConfiguratorTextColors(@NotNull Composite content) {
        for (Control child : content.getChildren()) {
            if (child instanceof Label || child instanceof CLabel || child instanceof StyledText) {
                child.setForeground(null);
            }
            if (child instanceof Composite composite) {
                resetConfiguratorTextColors(composite);
            }
        }
    }

    private void createConnectionSettings(@NotNull Composite parent, @NotNull DBPDataSourceContainer connection) {
        Composite settingsGroup = UIUtils.createTitledComposite(
            parent, AIUIMessages.ai_connection_settings_group, 2, GridData.HORIZONTAL_ALIGN_BEGINNING);
        enableAiCheck = UIUtils.createCheckbox(
            settingsGroup,
            AIUIMessages.ai_connection_settings_enable_label,
            AIUIMessages.ai_connection_settings_enable_tip,
            settings.isMetaTransferConfirmed(),
            2
        );
        metadataScopeCombo = UIUtils.createLabelCombo(
            settingsGroup,
            AIUIMessages.ai_connection_settings_scope_label,
            AIUIMessages.ai_connection_settings_scope_tip,
            SWT.DROP_DOWN | SWT.READ_ONLY
        );
        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            metadataScopeCombo.add(scope.getTitle());
        }
        metadataScopeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            int scopeIndex = metadataScopeCombo.getSelectionIndex();
            if (scopeIndex >= 0) {
                AIDatabaseScope scope = CommonUtils.fromOrdinal(AIDatabaseScope.class, scopeIndex);
                if (scope == AIDatabaseScope.CUSTOM) {
                    updateScopeSelection();
                    configureCustomScope(connection);
                } else {
                    settings.setScope(scope);
                }
            }
        }));
        Button scopeButton = UIUtils.createDialogButton(
            settingsGroup,
            AIUIMessages.ai_connection_settings_custom_scope_label,
            AIUIMessages.ai_connection_settings_custom_scope_tip,
            UIIcon.CONFIGURATION,
            SelectionListener.widgetSelectedAdapter(selectionEvent -> configureCustomScope(connection))
        );
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 2;
        scopeButton.setLayoutData(gridData);
        scopeButton.setEnabled(connection.getRegistry().getDataSource(connection.getId()) != null);
        if (settings.getScope() == null) {
            AIUtils.updateScopeSettingsIfNeeded(settings, connection, DBUtils.getDefaultContext(connection, true));
        }
        updateScopeSelection();
    }

    private void configureCustomScope(@NotNull DBPDataSourceContainer connection) {
        DBPDataSourceContainer metadataConnection = connection.getRegistry().getDataSource(connection.getId());
        if (metadataConnection == null) {
            return;
        }
        DBRRunnableContext runnableContext = getContainer() instanceof IWizardContainer wizardContainer
            && wizardContainer.getCurrentPage().getWizard() instanceof BaseWizard wizard
            ? wizard.getRunnableContext() : null;
        AIChatUtils.chooseCustomScope(
            getShell(), settings, metadataConnection, dataSource -> DBUtils.getDefaultContext(dataSource, false), null, runnableContext);
        updateScopeSelection();
    }

    private void updateScopeSelection() {
        AIDatabaseScope scope = settings.getScope();
        if (scope == null) {
            metadataScopeCombo.deselectAll();
        } else {
            metadataScopeCombo.select(scope.ordinal());
        }
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        if (result) {
            saveAISettings();
            persistConnection();
        }
        return result;
    }

    @Override
    protected void performApply() {
        super.performApply();
        saveAISettings();
        persistConnection();
    }

    private void saveAISettings() {
        if (settings == null) {
            return;
        }
        settings.setMetaTransferConfirmed(enableAiCheck.getSelection());
        if (aiConfigurator != null) {
            aiConfigurator.saveSettings(this);
        }
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            connection.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, settings.saveSettingsToMap());
        }
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (settings == null) {
            return;
        }
        settings.loadSettingsFromMap(Map.of());
        enableAiCheck.setSelection(settings.isMetaTransferConfirmed());
        enableAiCheck.notifyListeners(SWT.Selection, new Event());
        updateScopeSelection();
        if (aiConfigurator != null) {
            restoreConfiguratorEnablement(aiConfiguratorContent);
            aiConfigurator.resetSettings(this);
            aiConfigurator.loadSettings(this);
            updateConfiguratorEnablement(aiConfiguratorContent);
        }
    }

    private void persistConnection() {
        DBPDataSourceContainer connection = getConnection();
        if (connection != null && !(getContainer() instanceof IWizardContainer)) {
            connection.persistConfiguration();
        }
    }

    @Nullable
    public DBPDataSourceContainer getConnection() {
        return element == null ? null : element.getAdapter(DBPDataSourceContainer.class);
    }

    @NotNull
    public Button getAIEnabledCheck() {
        return enableAiCheck;
    }

    @Override
    @Nullable
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(@Nullable IAdaptable element) {
        this.element = element;
    }
}
