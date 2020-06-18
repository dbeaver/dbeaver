/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

public class StreamConsumerPageSettings extends ActiveWizardPage<DataTransferWizard> {

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;
    private static final int LOB_ENCODING_NATIVE = 3;

    private PropertyTreeViewer propsEditor;
    private Combo lobExtractType;
    private Label lobEncodingLabel;
    private Combo lobEncodingCombo;
    private Combo formatProfilesCombo;
    private PropertySourceCustom propertySource;

    public StreamConsumerPageSettings() {
        super(DTMessages.data_transfer_wizard_settings_name);
        setTitle(DTUIMessages.stream_consumer_page_settings_title);
        setDescription(DTUIMessages.stream_consumer_page_settings_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        DBPDataFormatterRegistry dataFormatterRegistry = DBWorkbench.getPlatform().getDataFormatterRegistry();

        initializeDialogUnits(parent);
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite generalSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_settings_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            ((GridLayout)generalSettings.getLayout()).verticalSpacing = 0;
            ((GridLayout)generalSettings.getLayout()).marginHeight = 0;
            ((GridLayout)generalSettings.getLayout()).marginWidth = 0;
            {
                formatProfilesCombo = UIUtils.createLabelCombo(generalSettings, DTMessages.data_transfer_wizard_settings_label_formatting, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 3;
                formatProfilesCombo.setLayoutData(gd);
                formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (formatProfilesCombo.getSelectionIndex() > 0) {
                            settings.setFormatterProfile(
                                dataFormatterRegistry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
                        } else {
                            settings.setFormatterProfile(null);
                        }
                    }
                });

                UIUtils.createDialogButton(generalSettings, DTMessages.data_transfer_wizard_settings_button_edit, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                            getShell(),
                            dataFormatterRegistry,
                            "org.jkiss.dbeaver.preferences.main.dataformat", // TODO: replace this hardcode with some model invocation
                            null,
                            getSelectedFormatterProfile(),
                            PreferencesUtil.OPTION_NONE);
                        if (propDialog != null) {
                            propDialog.open();
                            reloadFormatProfiles();
                        }
                    }
                });

                reloadFormatProfiles();

                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_binaries);
                Composite binariesPanel = UIUtils.createComposite(generalSettings, 4);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 4;
                binariesPanel.setLayoutData(gd);
                lobExtractType = new Combo(binariesPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobExtractType.setItems(
                    DTMessages.data_transfer_wizard_settings_binaries_item_set_to_null,
                    DTMessages.data_transfer_wizard_settings_binaries_item_save_to_file,
                    DTMessages.data_transfer_wizard_settings_binaries_item_inline);
                lobExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (lobExtractType.getSelectionIndex()) {
                            case EXTRACT_LOB_SKIP: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.SKIP); break;
                            case EXTRACT_LOB_FILES: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.FILES); break;
                            case EXTRACT_LOB_INLINE: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.INLINE); break;
                        }
                        updatePageCompletion();
                    }
                });

                lobEncodingLabel = UIUtils.createControlLabel(binariesPanel, DTMessages.data_transfer_wizard_settings_label_encoding);
                lobEncodingCombo = new Combo(binariesPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobEncodingCombo.setItems(
                    "Base64", //$NON-NLS-1$
                    "Hex", //$NON-NLS-1$
                    "Binary", //$NON-NLS-1$
                    "Native"); //$NON-NLS-1$
                lobEncodingCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (lobEncodingCombo.getSelectionIndex()) {
                            case LOB_ENCODING_BASE64: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BASE64); break;
                            case LOB_ENCODING_HEX: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.HEX); break;
                            case LOB_ENCODING_BINARY: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BINARY); break;
                            case LOB_ENCODING_NATIVE: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.NATIVE); break;
                        }
                    }
                });
            }
        }

        {
            Composite exporterSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_settings_group_exporter, 1, GridData.FILL_BOTH, 0);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
        }

        setControl(composite);
    }

    private Object getSelectedFormatterProfile()
    {
        DBPDataFormatterRegistry registry = DBWorkbench.getPlatform().getDataFormatterRegistry();
        int selectionIndex = formatProfilesCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        } else if (selectionIndex == 0) {
            return registry.getGlobalProfile();
        } else {
            return registry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo));
        }
    }

    private void reloadFormatProfiles()
    {
        DBPDataFormatterRegistry registry = DBWorkbench.getPlatform().getDataFormatterRegistry();
        formatProfilesCombo.removeAll();
        formatProfilesCombo.add(DTMessages.data_transfer_wizard_settings_listbox_formatting_item_default);
        for (DBDDataFormatterProfile profile : registry.getCustomProfiles()) {
            formatProfilesCombo.add(profile.getProfileName());
        }
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        DBDDataFormatterProfile formatterProfile = settings.getFormatterProfile();
        if (formatterProfile != null) {
            if (!UIUtils.setComboSelection(formatProfilesCombo, formatterProfile.getProfileName())) {
                formatProfilesCombo.select(0);
            }
        } else {
            formatProfilesCombo.select(0);
        }
    }

    @Override
    public void activatePage() {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        propertySource = new PropertySourceCustom(
            processor.getProperties(),
            getWizard().getSettings().getProcessorProperties());
        propsEditor.loadProperties(propertySource);

        switch (settings.getLobExtractType()) {
            case SKIP: lobExtractType.select(EXTRACT_LOB_SKIP); break;
            case FILES: lobExtractType.select(EXTRACT_LOB_FILES); break;
            case INLINE: lobExtractType.select(EXTRACT_LOB_INLINE); break;
        }
        switch (settings.getLobEncoding()) {
            case BASE64: lobEncodingCombo.select(LOB_ENCODING_BASE64); break;
            case HEX: lobEncodingCombo.select(LOB_ENCODING_HEX); break;
            case BINARY: lobEncodingCombo.select(LOB_ENCODING_BINARY); break;
            case NATIVE: lobEncodingCombo.select(LOB_ENCODING_NATIVE); break;
        }

        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        getWizard().getSettings().setProcessorProperties(propertySource.getPropertiesWithDefaults());
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        int selectionIndex = lobExtractType.getSelectionIndex();
        if (selectionIndex == EXTRACT_LOB_INLINE) {
            lobEncodingLabel.setVisible(true);
            lobEncodingCombo.setVisible(true);
        } else {
            lobEncodingLabel.setVisible(false);
            lobEncodingCombo.setVisible(false);
        }

        return true;
    }

}