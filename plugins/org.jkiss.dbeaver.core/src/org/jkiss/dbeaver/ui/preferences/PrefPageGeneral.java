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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.language.PlatformLanguageDescriptor;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * General product preferences.
 */
public class PrefPageGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.general"; //$NON-NLS-1$

    private static final String PRODUCT_CONFIGURATION_COMMAND_ID =
        "org.jkiss.dbeaver.ui.app.config.showWizard"; //$NON-NLS-1$

    @Nullable
    private Button automaticUpdateCheck;
    private Combo workspaceLanguage;
    private Combo clientTimezone;

    public PrefPageGeneral() {
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        createGeneralSettings(composite);
        createRegionalSettings(composite);
        createProductConfigurationButton(composite);
        setSettings();
        injectConfigurators(composite);
        return composite;
    }

    private void createGeneralSettings(@NotNull Composite parent) {
        if (ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
            return;
        }
        Composite group = UIUtils.createTitledComposite(
            parent,
            UIConnectionMessages.pref_page_ui_general_group_general,
            2,
            GridData.VERTICAL_ALIGN_BEGINNING
        );
        automaticUpdateCheck = UIUtils.createCheckbox(
            group,
            CoreMessages.pref_page_ui_general_checkbox_automatic_updates,
            null,
            false,
            2
        );
    }

    private void createRegionalSettings(@NotNull Composite parent) {
        Composite group = UIUtils.createTitledComposite(
            parent,
            CoreMessages.pref_page_ui_general_group_regional,
            2,
            GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
        );
        workspaceLanguage = UIUtils.createLabelCombo(
            group,
            CoreMessages.pref_page_ui_general_combo_language,
            CoreMessages.pref_page_ui_general_combo_language_tip,
            SWT.READ_ONLY | SWT.DROP_DOWN
        );
        workspaceLanguage.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        List<PlatformLanguageDescriptor> languages = PlatformLanguageRegistry.getInstance().getLanguages();
        DBPPlatformLanguage platformLanguage = DBPPlatformDesktop.getInstance().getPlatformLanguage();
        for (int i = 0; i < languages.size(); i++) {
            PlatformLanguageDescriptor language = languages.get(i);
            workspaceLanguage.add(language.getLabel());
            if (CommonUtils.equalObjects(platformLanguage, language)) {
                workspaceLanguage.select(i);
            }
        }
        if (workspaceLanguage.getSelectionIndex() < 0) {
            workspaceLanguage.select(0);
        }

        clientTimezone = UIUtils.createLabelCombo(
            group,
            CoreMessages.pref_page_ui_general_combo_timezone,
            CoreMessages.pref_page_ui_general_combo_timezone_tip,
            SWT.DROP_DOWN
        );
        clientTimezone.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        clientTimezone.add(DBConstants.DEFAULT_TIMEZONE);
        for (String timezoneName : TimezoneRegistry.getTimezoneNames()) {
            clientTimezone.add(timezoneName);
        }
        clientTimezone.addModifyListener(e -> {
            updateApplyButton();
            getContainer().updateButtons();
        });
        IContentProposalProvider proposalProvider = (contents, position) -> {
            List<IContentProposal> proposals = new ArrayList<>();
            for (String item : clientTimezone.getItems()) {
                if (StringUtils.containsIgnoreCase(item, contents.toLowerCase())) {
                    proposals.add(new ContentProposal(item));
                }
            }
            return proposals.toArray(IContentProposal[]::new);
        };
        ContentAssistUtils.installContentProposal(clientTimezone, new ComboContentAdapter(), proposalProvider);

        Control restartLabel = UIUtils.createInfoLabel(
            group,
            CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart
        );
        restartLabel.setLayoutData(new GridData(
            GridData.HORIZONTAL_ALIGN_BEGINNING,
            GridData.VERTICAL_ALIGN_BEGINNING,
            false,
            false,
            2,
            1
        ));
    }

    private void createProductConfigurationButton(@NotNull Composite parent) {
        var command = ActionUtils.findCommand(PRODUCT_CONFIGURATION_COMMAND_ID);
        if (command == null) {
            return;
        }
        Composite group = UIUtils.createTitledComposite(
            parent,
            CoreMessages.pref_page_general_group_product_configuration,
            1,
            GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
        );
        Button button = UIUtils.createPushButton(
            group,
            CoreMessages.pref_page_general_button_open_product_configuration,
            null,
            SelectionListener.widgetSelectedAdapter(e ->
                ActionUtils.runCommand(PRODUCT_CONFIGURATION_COMMAND_ID, UIUtils.getActiveWorkbenchWindow()))
        );
        button.setEnabled(command.isEnabled());
    }

    private void setSettings() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (automaticUpdateCheck != null) {
            automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
        }
        String timezone = store.getString(ModelPreferences.CLIENT_TIMEZONE);
        clientTimezone.setText(DBConstants.DEFAULT_TIMEZONE.equals(timezone) ? DBConstants.DEFAULT_TIMEZONE : timezone);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (automaticUpdateCheck != null) {
            automaticUpdateCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
        }
        UIUtils.setComboSelection(clientTimezone, store.getDefaultString(ModelPreferences.CLIENT_TIMEZONE));
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(
            DBeaverPreferences.UI_AUTO_UPDATE_CHECK,
            automaticUpdateCheck == null ? Boolean.FALSE : automaticUpdateCheck.getSelection()
        );
        PrefUtils.savePreferenceStore(store);

        if (DBConstants.DEFAULT_TIMEZONE.equals(clientTimezone.getText())) {
            TimezoneRegistry.setDefaultZone(null, true);
        } else {
            TimezoneRegistry.setDefaultZone(
                ZoneId.of(TimezoneRegistry.extractTimezoneId(clientTimezone.getText())), true);
        }

        if (workspaceLanguage.getSelectionIndex() >= 0) {
            PlatformLanguageDescriptor language = PlatformLanguageRegistry.getInstance().getLanguages()
                .get(workspaceLanguage.getSelectionIndex());
            DBPPlatformLanguage currentLanguage = DBPPlatformDesktop.getInstance().getPlatformLanguage();
            if (currentLanguage != language) {
                if (DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager languageManager) {
                    languageManager.setPlatformLanguage(language);
                }
                if (UIUtils.confirmAction(
                    getShell(),
                    "Restart " + GeneralUtils.getProductName(),
                    "You need to restart " + GeneralUtils.getProductName()
                        + " to perform actual language change.\nDo you want to restart?"
                )) {
                    restartWorkbenchOnPrefChange();
                }
            }
        }
        return super.performOk();
    }
}
