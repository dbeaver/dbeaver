/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream.page;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
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

    private PropertyTreeViewer propsEditor;
    private Combo lobExtractType;
    private Label lobEncodingLabel;
    private Combo lobEncodingCombo;
    private Combo formatProfilesCombo;
    private PropertySourceCustom propertySource;

    public StreamConsumerPageSettings() {
        super(DTMessages.data_transfer_wizard_settings_name);
        setTitle("Format settings");
        setDescription("File format settings");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        DBPDataFormatterRegistry dataFormatterRegistry = DBWorkbench.getPlatform().getDataFormatterRegistry();

        initializeDialogUnits(parent);
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group generalSettings = new Group(composite, SWT.NONE);
            generalSettings.setText(DTMessages.data_transfer_wizard_settings_group_general);
            gl = new GridLayout(4, false);
            generalSettings.setLayout(gl);
            generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                Composite formattingGroup = UIUtils.createPlaceholder(generalSettings, 3);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 4;
                formattingGroup.setLayoutData(gd);

                UIUtils.createControlLabel(formattingGroup, DTMessages.data_transfer_wizard_settings_label_formatting);
                formatProfilesCombo = new Combo(formattingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = 200;
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

                Button profilesManageButton = new Button(formattingGroup, SWT.PUSH);
                profilesManageButton.setText(DTMessages.data_transfer_wizard_settings_button_edit);
                profilesManageButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        //DataFormatProfilesEditDialog dialog = new DataFormatProfilesEditDialog(getShell());
                        //dialog.open();
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
            }
            {
                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_binaries);
                lobExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
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

                lobEncodingLabel = UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_encoding);
                lobEncodingCombo = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobEncodingCombo.setItems(
                    "Base64", //$NON-NLS-1$
                    "Hex", //$NON-NLS-1$
                    "Binary"); //$NON-NLS-1$
                lobEncodingCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (lobEncodingCombo.getSelectionIndex()) {
                            case LOB_ENCODING_BASE64: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BASE64); break;
                            case LOB_ENCODING_HEX: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.HEX); break;
                            case LOB_ENCODING_BINARY: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BINARY); break;
                        }
                    }
                });
            }
        }

        Group exporterSettings = new Group(composite, SWT.NONE);
        exporterSettings.setText(DTMessages.data_transfer_wizard_settings_group_exporter);
        exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
        exporterSettings.setLayout(new GridLayout(1, false));

        propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);

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