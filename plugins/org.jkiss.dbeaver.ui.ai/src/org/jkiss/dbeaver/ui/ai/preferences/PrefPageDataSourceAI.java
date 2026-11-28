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
import org.eclipse.swt.layout.GridLayout;
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
import org.jkiss.dbeaver.ui.forms.HintLabel;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class PrefPageDataSourceAI extends AbstractPrefPage implements IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(PrefPageDataSourceAI.class);

    private IAdaptable element;
    private AIContextSettingsDataSource settings;
    private Button enableAiCheck;
    private Combo metadataScopeCombo;
    private IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> aiConfigurator;
    private Composite aiSettingsContent;
    private ControlEnableState aiConfiguratorEnableState;
    private Composite mcpSettingsGroup;

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            settings = new AIContextSettingsDataSource(connection);
            createConnectionSettings(content, connection);
            aiSettingsContent = UIUtils.createComposite(content, 1);
            aiSettingsContent.setLayoutData(new GridData(GridData.FILL_BOTH));
            mcpSettingsGroup = UIUtils.createTitledComposite(
                aiSettingsContent, AIUIMessages.ai_connection_settings_mcp_group, 1, GridData.HORIZONTAL_ALIGN_BEGINNING);
            enableAiCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                event -> updateConfiguratorEnablement(aiSettingsContent)));
            updateMcpSettingsLayout();
            UIPropertyConfiguratorDescriptor descriptor = UIPropertyConfiguratorRegistry.getInstance()
                .getDescriptor(AIContextSettingsDataSource.class.getName());
            if (descriptor != null) {
                try {
                    IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> configurator =
                        descriptor.createConfigurator();
                    Composite aiConfiguratorContent = UIUtils.createComposite(aiSettingsContent, 1);
                    aiConfiguratorContent.setLayoutData(new GridData(GridData.FILL_BOTH));
                    configurator.createControl(aiConfiguratorContent, settings, () -> {});
                    configurator.loadSettings(this);
                    updateConfiguratorEnablement(aiSettingsContent);
                    aiConfigurator = configurator;
                } catch (DBException e) {
                    log.error("Cannot load AI connection settings", e);
                }
            }
            injectConfigurators(aiSettingsContent);
            updateMcpSettingsLayout();
        }
        return content;
    }

    @NotNull
    public Composite getMcpSettingsGroup() {
        return mcpSettingsGroup;
    }

    public void updateMcpSettingsLayout() {
        int columns = 0;
        for (Control child : mcpSettingsGroup.getChildren()) {
            if (child.getVisible()) {
                columns++;
            }
        }
        GridLayout layout = (GridLayout) mcpSettingsGroup.getLayout();
        layout.numColumns = Math.max(1, columns);
        updateConfiguratorEnablement(aiSettingsContent);
        UIUtils.setControlVisible(mcpSettingsGroup.getParent(), columns > 0);
        mcpSettingsGroup.getShell().layout(new Control[] {mcpSettingsGroup.getParent()});
    }

    private void updateConfiguratorEnablement(@NotNull Composite content) {
        boolean enabled = enableAiCheck.getSelection();
        // Restore first so newly contributed controls are included in the disabled state.
        restoreConfiguratorEnablement(content);
        if (!enabled) {
            aiConfiguratorEnableState = ControlEnableState.disable(content);
        }
        CSSUtils.setCSSClass(content, enabled ? null : HintLabel.CSS_CLASS);
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
            parent, AIUIMessages.ai_connection_settings_group, 3, GridData.HORIZONTAL_ALIGN_BEGINNING);
        Control enableAiControl = UIPanelBuilder.build(settingsGroup, panel -> panel
            .margins(0, 0)
            .row(row -> row.withHint(AIUIMessages.ai_connection_settings_enable_tip, checkRow -> checkRow
                .control(checkParent -> enableAiCheck = UIUtils.createCheckbox(
                    checkParent,
                    AIUIMessages.ai_connection_settings_enable_label,
                    settings.isMetaTransferConfirmed()
                )))));
        GridData enableAiData = new GridData(GridData.FILL_HORIZONTAL);
        enableAiData.horizontalSpan = 3;
        enableAiControl.setLayoutData(enableAiData);
        metadataScopeCombo = UIUtils.createLabelCombo(
            settingsGroup,
            AIUIMessages.ai_connection_settings_scope_label,
            AIUIMessages.ai_connection_settings_scope_tip,
            SWT.DROP_DOWN | SWT.READ_ONLY
        );
        metadataScopeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
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
        ToolBar scopeToolBar = new ToolBar(settingsGroup, SWT.FLAT);
        scopeToolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        ToolItem scopeItem = UIUtils.createToolItem(
            scopeToolBar,
            AIUIMessages.ai_connection_settings_custom_scope_label,
            UIIcon.CONFIGURATION,
            SelectionListener.widgetSelectedAdapter(selectionEvent -> configureCustomScope(connection))
        );
        scopeItem.setEnabled(connection.getRegistry().getDataSource(connection.getId()) != null);
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
        metadataScopeCombo.select(settings.getScope().ordinal());
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
        settings.saveSettingsToDataSource();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (settings == null) {
            return;
        }
        settings.loadSettingsFromMap(Map.of());
        enableAiCheck.setSelection(settings.isMetaTransferConfirmed());
        updateScopeSelection();
        if (aiConfigurator != null) {
            restoreConfiguratorEnablement(aiSettingsContent);
            aiConfigurator.resetSettings(this);
            aiConfigurator.loadSettings(this);
            updateConfiguratorEnablement(aiSettingsContent);
        }
        updateMcpSettingsLayout();
    }

    private void persistConnection() {
        DBPDataSourceContainer connection = getConnection();
        if (connection != null && !(getContainer() instanceof IWizardContainer)) {
            connection.persistConfiguration();
        }
    }

    @Nullable
    public AIContextSettingsDataSource getAISettings() {
        return settings;
    }

    @Nullable
    public DBPDataSourceContainer getConnection() {
        return element == null ? null : element.getAdapter(DBPDataSourceContainer.class);
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
