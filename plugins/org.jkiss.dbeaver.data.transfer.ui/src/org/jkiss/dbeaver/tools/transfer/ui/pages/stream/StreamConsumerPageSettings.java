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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

public class StreamConsumerPageSettings extends DataTransferPageNodeSettings {

    private static final Log log = Log.getLog(StreamConsumerPageSettings.class);

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
    private ValueFormatSelector valueFormatSelector;

    public StreamConsumerPageSettings() {
        super(DTMessages.data_transfer_wizard_settings_name);
        setTitle(DTUIMessages.stream_consumer_page_settings_title);
        setDescription(DTUIMessages.stream_consumer_page_settings_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        DBPDataFormatterRegistry dataFormatterRegistry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();

        initializeDialogUnits(parent);
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite exporterSettings = UIUtils.createComposite(composite, 1);
            exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
            //UIUtils.createControlLabel(exporterSettings, DTMessages.data_transfer_wizard_settings_group_exporter);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
            propsEditor.getControl().setLayoutData(GridDataFactory.create(GridData.FILL_BOTH).hint(200, 150).create());
        }
        {
            final ExpandableComposite generalExpander = new ExpandableComposite(
                composite,
                ExpandableComposite.CLIENT_INDENT | SWT.SEPARATOR,
                ExpandableComposite.TWISTIE
            );
            generalExpander.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
            generalExpander.setText(UIConnectionMessages.dialog_connection_advanced_settings);
            generalExpander.setFont(BaseThemeSettings.instance.baseFontBold);
            generalExpander.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    UIUtils.resizeShell(parent.getShell());
                }
            });

            Composite generalSettings = UIUtils.createComposite(generalExpander, 5);
            //UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_group_general, 5);

            generalExpander.setClient(generalSettings);

            {
                formatProfilesCombo = UIUtils.createLabelCombo(generalSettings, DTMessages.data_transfer_wizard_settings_label_formatting, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.horizontalSpan = 3;
                formatProfilesCombo.setLayoutData(gd);
                formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (formatProfilesCombo.getSelectionIndex() > 0) {
                            settings.setFormatterProfile(
                                dataFormatterRegistry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
                        } else {
                            settings.setFormatterProfile(null);
                        }
                    }
                });

                Button editProfileButton = UIUtils.createDialogButton(
                    generalSettings,
                    DTMessages.data_transfer_wizard_settings_button_edit,
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
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
                    }
                );
                editProfileButton.setEnabled(true);

                reloadFormatProfiles();
            }

            {
                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_binaries);
                Composite binariesPanel = UIUtils.createComposite(generalSettings, 4);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
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

                valueFormatSelector = new ValueFormatSelector(generalSettings);
                valueFormatSelector.select(settings.getValueFormat());
                valueFormatSelector.getCombo().addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setValueFormat(valueFormatSelector.getSelection());
                    }
                });
                valueFormatSelector.getCombo().setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 4, 1));
            }
        }

        setControl(composite);
    }

    private Object getSelectedFormatterProfile()
    {
        DBPDataFormatterRegistry registry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();
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
        DBPDataFormatterRegistry registry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();
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
        getWizard().loadNodeSettings();

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
        propsEditor.saveEditorValues();
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

    @Override
    public boolean isPageApplicable() {
        return isConsumerOfType(StreamTransferConsumer.class);
    }

}