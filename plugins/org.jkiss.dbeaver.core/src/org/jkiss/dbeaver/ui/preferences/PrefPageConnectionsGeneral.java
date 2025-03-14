/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.DBConnectionConstants;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionNameResolver;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageGeneral;
import org.jkiss.dbeaver.ui.dialogs.connection.NavigatorSettingsStorage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

public class PrefPageConnectionsGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage, NavigatorSettingsStorage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.connections";

    private static final String VALUE_TRUST_STRORE_TYPE_WINDOWS = "WINDOWS-ROOT"; //$NON-NLS-1$
    private static final String HELP_CONNECTIONS_LINK = "Create-Connection";
    
    private CSmartCombo<DBPConnectionType> connectionTypeCombo;
    private Combo navigatorSettingsCombo;
    private Text connectionDefaultNamePatternText;

    private String connectionNamePattern;
    private DBPConnectionType defaultConnectionType;
    private DBNBrowseSettings defaultNavigatorSettings;
    private Text sampleConnectionName;
    private ConnectionNameResolver fakeConnectionNameResolver;
    private Button useWinTrustStoreCheck;
    private Button closeConnectionOnOsSleepCheck;

    public PrefPageConnectionsGeneral() {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
        connectionNamePattern = DBWorkbench.getPlatform().getPreferenceStore().getString(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN);
        defaultNavigatorSettings = DataSourceNavigatorSettings.PRESET_FULL.getSettings();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group groupDefaults = UIUtils.createControlGroup(composite, CoreMessages.pref_page_connection_label_default_settings, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            Composite groupComposite = UIUtils.createComposite(groupDefaults, 2);
            connectionTypeCombo = ConnectionPageGeneral.createConnectionTypeCombo(groupComposite);
            connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    defaultConnectionType = connectionTypeCombo.getSelectedItem();
                }
            });
            navigatorSettingsCombo = ConnectionPageGeneral.createNavigatorSettingsCombo(groupComposite, this, null);
            connectionDefaultNamePatternText = UIUtils.createLabelText(groupComposite, CoreMessages.pref_page_connection_label_default_connection_name_pattern, CoreMessages.pref_page_connection_label_default_connection_name_pattern_tip);
            ContentAssistUtils.installContentProposal(
                connectionDefaultNamePatternText,
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(Arrays.stream(ConnectionNameResolver.getConnectionVariables()).map(GeneralUtils::variablePattern).toArray(String[]::new))
            );
            connectionDefaultNamePatternText.setText(connectionNamePattern);
            UIUtils.setContentProposalToolTip(connectionDefaultNamePatternText, "Connection name patterns",
                ConnectionNameResolver.getConnectionVariables());

            fakeConnectionNameResolver = generateSampleDatasourceResolver();
            sampleConnectionName = UIUtils.createLabelText(groupComposite, CoreMessages.pref_page_connection_label_default_connection_name_pattern_sample, CoreMessages.pref_page_connection_label_default_connection_name_pattern_sample_tip);
            sampleConnectionName.setEditable(false);
            sampleConnectionName.setText(GeneralUtils.replaceVariables(connectionDefaultNamePatternText.getText(), fakeConnectionNameResolver));
            connectionDefaultNamePatternText.addModifyListener(e -> sampleConnectionName.setText(GeneralUtils.replaceVariables(connectionDefaultNamePatternText.getText(), fakeConnectionNameResolver)));

            new VariablesHintLabel(
                    groupDefaults,
                    CoreMessages.pref_page_connection_label_default_connection_template_variables,
                    CoreMessages.pref_page_connection_label_default_connection_template_variables_tip,
                    ConnectionNameResolver.getConnectionVariablesInfo(),
                    false
            );
        }

        {
            Group groupBehavior = UIUtils.createControlGroup(composite, CoreMessages.pref_page_connection_label_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            closeConnectionOnOsSleepCheck = UIUtils.createCheckbox(
                groupBehavior,
                CoreMessages.pref_page_connection_label_close_connection_on_sleep,
                CoreMessages.pref_page_connection_label_close_connection_on_sleep_tip,
                true,
                1);
        }

        if (DBWorkbench.getPlatform().getApplication().hasProductFeature(DBConnectionConstants.PRODUCT_FEATURE_SIMPLE_TRUSTSTORE)) {
            createWinstoreSettings(composite);
        }

        {
            Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_eclipse_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            Label descLabel = new Label(groupObjects, SWT.WRAP);
            descLabel.setText(CoreMessages.pref_page_eclipse_ui_general_connections_group_label);

            // Link to secure storage config
            addLinkToSettings(groupObjects, PrefPageDrivers.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageErrorHandle.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageMetaData.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageTransactions.PAGE_ID);
        }

        Link urlHelpLabel = UIUtils.createLink(
            composite,
            "<a>" + CoreMessages.pref_page_connections_wiki_link + "</a>",
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(HELP_CONNECTIONS_LINK));
                }
            });
        GridData gridData = new GridData(GridData.FILL, SWT.END, true, true);
        urlHelpLabel.setLayoutData(gridData);

        updateCombosAndSettings();

        return composite;
    }

    private void createWinstoreSettings(Composite composite) {
        if (RuntimeUtils.isWindows()) {
            Group settings = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_connections_group_security,
                2,
                GridData.FILL_HORIZONTAL,
                300
            );
            if (CommonUtils.isNotEmpty(System.getProperty(GeneralUtils.PROP_TRUST_STORE))
                || (CommonUtils.isNotEmpty(System.getProperty(GeneralUtils.PROP_TRUST_STORE_TYPE))
                && !System.getProperty(GeneralUtils.PROP_TRUST_STORE_TYPE).equalsIgnoreCase(VALUE_TRUST_STRORE_TYPE_WINDOWS))
            ) {
                Composite winTrustStoreComposite = UIUtils.createComposite(settings, 1);
                useWinTrustStoreCheck = UIUtils.createCheckbox(
                    winTrustStoreComposite,
                    CoreMessages.pref_page_connections_use_win_cert_label,
                    DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE)
                );
                winTrustStoreComposite.setToolTipText(CoreMessages.pref_page_connections_use_win_cert_disabled_tip);
                useWinTrustStoreCheck.setEnabled(false);
            } else {
                useWinTrustStoreCheck = UIUtils.createCheckbox(
                    settings,
                    CoreMessages.pref_page_connections_use_win_cert_label,
                    DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE)
                );
                useWinTrustStoreCheck.setToolTipText(CoreMessages.pref_page_connections_use_win_cert_tip);
            }
        }
    }

    @Override
    public void init(IWorkbench iWorkbench) {

    }

    private ConnectionNameResolver generateSampleDatasourceResolver() {
        final DataSourceRegistry dataSourceRegistry = new DataSourceRegistry(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        DBPDriver driver = DriverUtils.getRecentDrivers(DriverUtils.getAllDrivers(), 1).get(0);
        DBPConnectionConfiguration conConfig = new DBPConnectionConfiguration();
        conConfig.setHostName("hostname");
        conConfig.setUserPassword("password1");
        conConfig.setDatabaseName("database1");
        conConfig.setHostPort("42");
        conConfig.setServerName("server1");
        conConfig.setUrl("sample//url");
        DataSourceDescriptor fakeDataSource = (DataSourceDescriptor) dataSourceRegistry.createDataSource(
            DataSourceDescriptor.generateNewId(driver),
            driver,
            conConfig
        );
        dataSourceRegistry.dispose();
        return new ConnectionNameResolver(fakeDataSource, conConfig, null);
    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable iAdaptable) {

    }

    private void addLinkToSettings(Composite composite, String pageID) {
        UIUtils.createPreferenceLink(
            composite,
            "<a>''{0}''</a> " + CoreMessages.pref_page_ui_general_label_settings,
            pageID,
            (IWorkbenchPreferenceContainer) getContainer(),
            null
        );
    }

    @Override
    public DBNBrowseSettings getNavigatorSettings() {
        return defaultNavigatorSettings;
    }

    @Override
    public void setNavigatorSettings(DBNBrowseSettings settings) {
        this.defaultNavigatorSettings = settings;
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        connectionDefaultNamePatternText.setText(preferences.getDefaultString(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN));
        sampleConnectionName.setText(GeneralUtils.replaceVariables(connectionDefaultNamePatternText.getText(), fakeConnectionNameResolver));
        connectionNamePattern = preferences.getDefaultString(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN);

        closeConnectionOnOsSleepCheck.setSelection(preferences.getDefaultBoolean(ModelPreferences.CONNECTION_CLOSE_ON_SLEEP));

        if (RuntimeUtils.isWindows() && useWinTrustStoreCheck != null) {
            useWinTrustStoreCheck.setSelection(
                preferences.getDefaultBoolean(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE));
        }
        updateCombosAndSettings();
    }

    private void updateCombosAndSettings() {
        defaultConnectionType = DBPConnectionType.getDefaultConnectionType();
        for (int i = 0; i < connectionTypeCombo.getItemCount(); i++) {
            if (connectionTypeCombo.getItem(i).getId().equals(defaultConnectionType.getId())) {
                connectionTypeCombo.select(i);
                break;
            }
        }
        defaultNavigatorSettings = DataSourceNavigatorSettings.getDefaultSettings();
        ConnectionPageGeneral.updateNavigatorSettingsPreset(navigatorSettingsCombo, defaultNavigatorSettings);
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (defaultConnectionType != DBPConnectionType.getDefaultConnectionType()) {
            DBPConnectionType.setDefaultConnectionType(defaultConnectionType);
        }
        if (!defaultNavigatorSettings.equals(DataSourceNavigatorSettings.getDefaultSettings())) {
            DataSourceNavigatorSettings.setDefaultSettings(defaultNavigatorSettings);
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN, connectionDefaultNamePatternText.getText());
        store.setValue(ModelPreferences.CONNECTION_CLOSE_ON_SLEEP, closeConnectionOnOsSleepCheck.getSelection());
        if (RuntimeUtils.isWindows() && useWinTrustStoreCheck != null) {
            store.setValue(ModelPreferences.PROP_USE_WIN_TRUST_STORE_TYPE, useWinTrustStoreCheck.getSelection());
        }

        return super.performOk();
    }

}
