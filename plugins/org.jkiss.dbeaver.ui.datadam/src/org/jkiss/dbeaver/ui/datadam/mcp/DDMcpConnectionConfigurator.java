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
package org.jkiss.dbeaver.ui.datadam.mcp;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionSettings;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.controls.CInfoLabel;
import org.jkiss.dbeaver.ui.datadam.DBDamService;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.management.DDManagementActions;
import org.jkiss.dbeaver.ui.forms.HintLabel;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class DDMcpConnectionConfigurator implements IObjectPropertyConfigurator<PrefPageDataSourceAI, PrefPageDataSourceAI> {
    private static final Log log = Log.getLog(DDMcpConnectionConfigurator.class);
    private Button enableCheck;
    private Composite settingsControls;
    private boolean resetPending;

    @Override
    public void createControl(
        @NotNull Composite parent,
        @NotNull PrefPageDataSourceAI page,
        @NotNull Runnable propertyChangeListener
    ) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection == null || !DDProductConfig.isIntegrationEnabled()) {
            return;
        }
        DDAccountClient.DamSettingsState status = readStatus(page);
        Composite section = UIUtils.createComposite(page.getMcpSettingsGroup(), 1);
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        section.moveAbove(null);
        Control enableControl = UIPanelBuilder.build(section, panel -> panel
            .margins(0, 0)
            .row(row -> row.withHint(DDMcpMessages.mcp_connection_settings_enabled_tip, checkRow -> checkRow
                .control(checkParent -> enableCheck = UIUtils.createCheckbox(
                    checkParent,
                    DDMcpMessages.mcp_connection_settings_group,
                    false
                )))));
        enableControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        settingsControls = UIUtils.createComposite(section, 1);
        GridData settingsData = new GridData(GridData.FILL_HORIZONTAL);
        settingsData.horizontalIndent = UIUtils.getFontHeight(enableCheck);
        settingsControls.setLayoutData(settingsData);
        new CInfoLabel(
            settingsControls,
            DDMcpMessages.mcp_connection_settings_global_tip,
            DBIcon.SMALL_INFO,
            UIUtils.getFontHeight(enableCheck) * HintLabel.PREFERRED_WIDTH_IN_LINE_HEIGHTS
        );
        Link preferencesLink = UIUtils.createLink(
            settingsControls,
            "<a>" + DDTrackingUIMessages.configuration + "</a>",
            SelectionListener.widgetSelectedAdapter(event -> {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                    settingsControls.getShell(),
                    DDManagementActions.PREFERENCE_PAGE,
                    new String[] {DDManagementActions.PREFERENCE_PAGE},
                    null,
                    PreferencesUtil.OPTION_NONE
                );
                if (dialog != null && dialog.open() == Window.OK) {
                    showStatus(section, page, readStatus(page));
                }
            })
        );
        preferencesLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        enableCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updatePreferencesVisibility()));
        updatePreferencesVisibility();
        showStatus(section, page, status);
    }

    @Nullable
    private DDAccountClient.DamSettingsState readStatus(@NotNull PrefPageDataSourceAI page) {
        DBRRunnableContext context = page.getContainer() instanceof IWizardContainer wizardContainer
            ? new RunnableContextDelegate(wizardContainer)
            : UIUtils.getDefaultRunnableContext();
        AtomicReference<DDAccountClient.DamSettingsState> result = new AtomicReference<>();
        try {
            context.run(true, true, monitor -> {
                monitor.beginTask(DDMcpMessages.mcp_connection_status_loading, 1);
                try {
                    if (!monitor.isCanceled()) {
                        result.set(DBDamService.getInstance().getDamSettings());
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            log.debug("Cannot read DataDam MCP status", e.getTargetException());
        } catch (InterruptedException e) {
            return null;
        }
        return result.get();
    }

    private void showStatus(
        @NotNull Composite parent,
        @NotNull PrefPageDataSourceAI page,
        @Nullable DDAccountClient.DamSettingsState status
    ) {
        if (parent.isDisposed()) {
            return;
        }
        boolean enabled = status != null && status.settings().isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false);
        enableCheck.setEnabled(enabled);
        // With local MCP present, keep the DataDam option visible even when globally disabled.
        UIUtils.setControlVisible(parent,
            DDProductConfig.isIntegrationEnabled() && (enabled || page.getMcpSettingsGroup().getChildren().length > 1));
        page.updateMcpSettingsLayout();
    }

    private void updatePreferencesVisibility() {
        UIUtils.setControlVisible(settingsControls, enableCheck.getSelection());
        settingsControls.getShell().layout(new Control[] {settingsControls});
    }

    @Override
    public void loadSettings(@NotNull PrefPageDataSourceAI page) {
        resetPending = false;
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && enableCheck != null) {
            enableCheck.setSelection(DDMcpConnectionSettings.isEnabled(connection));
            updatePreferencesVisibility();
        }
    }

    @Override
    public void saveSettings(@NotNull PrefPageDataSourceAI page) {
        DBPDataSourceContainer connection = page.getConnection();
        if (connection != null && (enableCheck != null || resetPending)) {
            DDMcpConnectionSettings.setEnabled(connection, enableCheck != null && enableCheck.getSelection());
            resetPending = false;
        }
    }

    @Override
    public void resetSettings(@NotNull PrefPageDataSourceAI page) {
        resetPending = true;
        if (enableCheck != null) {
            enableCheck.setSelection(false);
            updatePreferencesVisibility();
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
