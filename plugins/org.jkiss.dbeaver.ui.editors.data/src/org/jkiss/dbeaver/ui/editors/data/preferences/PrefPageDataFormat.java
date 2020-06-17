/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.formatter.DataFormatterDescriptor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.LocaleSelectorControl;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * PrefPageDataFormat
 */
public class PrefPageDataFormat extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageDataFormat.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.dataformat"; //$NON-NLS-1$

    private DBDDataFormatterProfile formatterProfile;

    private Font boldFont;
    private Combo typeCombo;
    private PropertyTreeViewer propertiesControl;
    private Text sampleText;

    private List<DataFormatterDescriptor> formatterDescriptors;
    private LocaleSelectorControl localeSelector;

    private String profileName;
    private Locale profileLocale;
    private Map<String, Map<Object, Object>> profileProperties = new HashMap<>();
    private Combo profilesCombo;
    private PropertySourceCustom propertySource;
    private Button datetimeNativeFormatCheck;
    private Button numericNativeFormatCheck;
    private Button numericScientificFormatCheck;

    public PrefPageDataFormat()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT) ||
            store.contains(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT) ||
            store.contains(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT) ||
            dataSourceDescriptor.getDataFormatterProfile().isOverridesParent();
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected void createPreferenceHeader(Composite composite)
    {
        if (!isDataSourcePreferencePage()) {
            Composite profileGroup = UIUtils.createComposite(composite, 3);
            profileGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createControlLabel(profileGroup, ResultSetMessages.pref_page_data_format_label_profile);
            profilesCombo = new Combo(profileGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            profilesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            profilesCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    changeProfile();
                }
            });
            UIUtils.createDialogButton(
                profileGroup,
                ResultSetMessages.pref_page_data_format_button_manage_profiles, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    manageProfiles();
                }
            });
        }
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite composite = UIUtils.createComposite(parent, 2);

        // Locale
        {
            localeSelector = new LocaleSelectorControl(composite, null);
            localeSelector.addListener(SWT.Selection, event -> {
                if (event.data instanceof Locale) {
                    onLocaleChange((Locale) event.data);
                }
            });
        }

        // Settings
        {
            Group settingsGroup = new Group(composite, SWT.NONE);
            settingsGroup.setText(ResultSetMessages.pref_page_data_format_group_settings);
            settingsGroup.setLayout(new GridLayout(2, false));
            settingsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));

            datetimeNativeFormatCheck = UIUtils.createCheckbox(settingsGroup, ResultSetMessages.pref_page_data_format_datetime_use_native_formatting, ResultSetMessages.pref_page_data_format_datetime_use_native_formatting_tip, false, 2);
            numericNativeFormatCheck = UIUtils.createCheckbox(settingsGroup, ResultSetMessages.pref_page_data_format_numeric_use_native_formatting, ResultSetMessages.pref_page_data_format_numeric_use_native_formatting_tip, false, 2);
            numericScientificFormatCheck = UIUtils.createCheckbox(settingsGroup, ResultSetMessages.pref_page_data_format_numeric_use_scientific_notation, ResultSetMessages.pref_page_data_format_numeric_use_scientific_notation_tip, false, 2);
            numericNativeFormatCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    numericScientificFormatCheck.setEnabled(numericNativeFormatCheck.getSelection());
                }
            });
        }

        // formats
        {
            Group formatGroup = new Group(composite, SWT.NONE);
            formatGroup.setText(ResultSetMessages.pref_page_data_format_group_format);
            formatGroup.setLayout(new GridLayout(2, false));
            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            formatGroup.setLayoutData(gd);

            UIUtils.createControlLabel(formatGroup, ResultSetMessages.pref_page_data_format_label_type);
            typeCombo = new Combo(formatGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    reloadFormatter();
                }
            });

            Label propsLabel = UIUtils.createControlLabel(formatGroup, ResultSetMessages.pref_page_data_format_label_settingt);
            propsLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            propertiesControl = new PropertyTreeViewer(formatGroup, SWT.BORDER);
            propertiesControl.getControl().addListener(SWT.Modify, event -> saveFormatterProperties());

            UIUtils.createControlLabel(formatGroup, ResultSetMessages.pref_page_data_format_label_sample);
            sampleText = new Text(formatGroup, SWT.BORDER | SWT.READ_ONLY);
            sampleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return composite;
    }

    private void manageProfiles()
    {
        DataFormatProfilesEditDialog dialog = new DataFormatProfilesEditDialog(getShell());
        dialog.open();
        refreshProfileList();
    }

    private DBDDataFormatterProfile getDefaultProfile()
    {
        if (isDataSourcePreferencePage()) {
            return getDataSourceContainer().getDataFormatterProfile();
        } else {
            return DataFormatterRegistry.getInstance().getGlobalProfile();
        }
    }

    private void changeProfile()
    {
        int selectionIndex = profilesCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            return;
        }
        DBDDataFormatterProfile newProfile;
        if (selectionIndex == 0) {
            newProfile = getDefaultProfile();
        } else {
            String newProfileName = profilesCombo.getItem(selectionIndex);
            newProfile = DataFormatterRegistry.getInstance().getCustomProfile(newProfileName);
        }
        if (newProfile != formatterProfile) {
            setCurrentProfile(newProfile);
        }
    }

    private void setCurrentProfile(DBDDataFormatterProfile profile)
    {
        if (formatterProfile == profile) {
            return;
        }
        formatterProfile = profile;
        formatterDescriptors = new ArrayList<>(DataFormatterRegistry.getInstance().getDataFormatters());

        profileName = formatterProfile.getProfileName();
        profileLocale = formatterProfile.getLocale();
        profileProperties.clear();
        for (DataFormatterDescriptor dfd : formatterDescriptors) {
            Map<Object, Object> formatterProps = formatterProfile.getFormatterProperties(dfd.getId());
            if (formatterProps != null) {
                profileProperties.put(dfd.getId(), formatterProps);
            }
        }

        try {
            // Set locale
            localeSelector.setLocale(profileLocale);
            // Load types
            typeCombo.removeAll();
            for (DataFormatterDescriptor formatter : formatterDescriptors) {
                typeCombo.add(formatter.getName());
            }
            if (typeCombo.getItemCount() > 0) {
                typeCombo.select(0);
            }
            reloadFormatter();
        } catch (Exception e) {
            log.warn(e);
        }
    }

    private void refreshProfileList()
    {
        if (isDataSourcePreferencePage()) {
            return;
        }
        int selectionIndex = profilesCombo.getSelectionIndex();
        String oldProfile = null;
        if (selectionIndex > 0) {
            oldProfile = profilesCombo.getItem(selectionIndex);
        }
        profilesCombo.removeAll();
        profilesCombo.add("<" + DataFormatterRegistry.getInstance().getGlobalProfile().getProfileName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
        for (DBDDataFormatterProfile profile : DataFormatterRegistry.getInstance().getCustomProfiles()) {
            profilesCombo.add(profile.getProfileName());
        }
        if (oldProfile != null) {
            profilesCombo.setText(oldProfile);
        }
        if (profilesCombo.getSelectionIndex() < 0) {
            profilesCombo.select(0);
        }
        profilesCombo.setEnabled(profilesCombo.getItemCount() >= 2);
        
        changeProfile();
    }


    private DataFormatterDescriptor getCurrentFormatter()
    {
        int selectionIndex = typeCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        }
        return formatterDescriptors.get(selectionIndex);
    }

    private void reloadFormatter()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }

        Map<Object,Object> formatterProps = profileProperties.get(formatterDescriptor.getId());
        Map<Object, Object> defaultProps = formatterDescriptor.getSample().getDefaultProperties(localeSelector.getSelectedLocale());
        propertySource = new PropertySourceCustom(
            formatterDescriptor.getProperties(),
            formatterProps);
        propertySource.setDefaultValues(defaultProps);
        propertiesControl.loadProperties(propertySource);
        reloadSample();
    }

    private void reloadSample()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }
        try {
            DBDDataFormatter formatter = formatterDescriptor.createFormatter();

            Map<Object, Object> defProps = formatterDescriptor.getSample().getDefaultProperties(profileLocale);
            Map<Object, Object> props = profileProperties.get(formatterDescriptor.getId());
            Map<Object, Object> formatterProps = new HashMap<>();
            if (defProps != null && !defProps.isEmpty()) {
                formatterProps.putAll(defProps);
            }
            if (props != null && !props.isEmpty()) {
                formatterProps.putAll(props);
            }
            formatter.init(null, profileLocale, formatterProps);

            String sampleValue = formatter.formatValue(formatterDescriptor.getSample().getSampleValue());
            sampleText.setText(CommonUtils.notEmpty(sampleValue));
        } catch (Exception e) {
            log.warn("Can't render sample value", e); //$NON-NLS-1$
        }
    }

    private void saveFormatterProperties()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }
        Map<Object, Object> props = propertySource.getProperties();
        profileProperties.put(formatterDescriptor.getId(), props);
        reloadSample();
    }

    private void onLocaleChange(Locale locale)
    {
        if (!locale.equals(profileLocale)) {
            profileLocale = locale;
            DataFormatterDescriptor formatter = getCurrentFormatter();
            if (formatter != null) {
                propertySource.setDefaultValues(formatter.getSample().getDefaultProperties(locale));
                propertiesControl.refresh();
            }
            reloadSample();
        }
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore targetPreferenceStore = getTargetPreferenceStore();
        clearPreferences(targetPreferenceStore);
        formatterProfile = null;
        loadPreferences(targetPreferenceStore);
        reloadSample();
        super.performDefaults();
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        refreshProfileList();

        setCurrentProfile(getDefaultProfile());

        datetimeNativeFormatCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT));
        numericNativeFormatCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT));
        numericScientificFormatCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT));
        numericScientificFormatCheck.setEnabled(numericNativeFormatCheck.getSelection());
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            formatterProfile.setProfileName(profileName);
            formatterProfile.setLocale(profileLocale);
            for (String typeId : profileProperties.keySet()) {
                formatterProfile.setFormatterProperties(typeId, profileProperties.get(typeId));
            }
            formatterProfile.saveProfile();

            store.setValue(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT, datetimeNativeFormatCheck.getSelection());
            store.setValue(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT, numericNativeFormatCheck.getSelection());
            store.setValue(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT, numericScientificFormatCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        if (formatterProfile != null) {
            formatterProfile.reset();
        }
        store.setToDefault(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT);
        store.setToDefault(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT);
        store.setToDefault(ModelPreferences.RESULT_SCIENTIFIC_NUMERIC_FORMAT);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
        if (data instanceof DBDDataFormatterProfile) {
            UIUtils.setComboSelection(profilesCombo, ((DBDDataFormatterProfile)data).getProfileName());
            changeProfile();
        }
    }

    @Override
    public void dispose()
    {
        boldFont.dispose();
        super.dispose();
    }

    /**
    * DataFormatProfilesEditDialog
    */
    public static class DataFormatProfilesEditDialog extends org.eclipse.jface.dialogs.Dialog {
        private static final int NEW_ID = IDialogConstants.CLIENT_ID + 1;
        private static final int DELETE_ID = IDialogConstants.CLIENT_ID + 2;
        private org.eclipse.swt.widgets.List profileList;

        DataFormatProfilesEditDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(ResultSetMessages.dialog_data_format_profiles_title);

            Composite group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout(1, false));
            group.setLayoutData(new GridData(GridData.FILL_BOTH));

            profileList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.heightHint = 200;
            profileList.setLayoutData(gd);

            profileList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getButton(DELETE_ID).setEnabled(profileList.getSelectionIndex() >= 0);
                }
            });

            loadProfiles();
            return parent;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, NEW_ID, ResultSetMessages.dialog_data_format_profiles_button_new_profile, false);
            createButton(parent, DELETE_ID, ResultSetMessages.dialog_data_format_profiles_button_delete_profile, false);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

            getButton(DELETE_ID).setEnabled(false);
        }

        @Override
        protected void buttonPressed(int buttonId)
        {
            DataFormatterRegistry registry = DataFormatterRegistry.getInstance();
            if (buttonId == NEW_ID) {
                String profileName = EnterNameDialog.chooseName(getShell(), ResultSetMessages.dialog_data_format_profiles_dialog_name_chooser_title);
                if (registry.getCustomProfile(profileName) != null) {
                    UIUtils.showMessageBox(
                            getShell(),
                            ResultSetMessages.dialog_data_format_profiles_error_title,
                            NLS.bind(ResultSetMessages.dialog_data_format_profiles_error_message, profileName), SWT.ICON_ERROR);
                } else {
                    registry.createCustomProfile(profileName);
                    loadProfiles();
                }
            } else if (buttonId == DELETE_ID) {
                int selectionIndex = profileList.getSelectionIndex();
                if (selectionIndex >= 0) {
                    DBDDataFormatterProfile profile = registry.getCustomProfile(profileList.getItem(selectionIndex));
                    if (profile != null) {
                        if (UIUtils.confirmAction(
                                getShell(),
                                ResultSetMessages.dialog_data_format_profiles_confirm_delete_title,
                                ResultSetMessages.dialog_data_format_profiles_confirm_delete_message)) {
                            registry.deleteCustomProfile(profile);
                            loadProfiles();
                        }
                    }
                }
            } else {
                super.buttonPressed(buttonId);
            }
        }

        private void loadProfiles()
        {
            profileList.removeAll();
            List<DBDDataFormatterProfile> profiles = DataFormatterRegistry.getInstance().getCustomProfiles();
            for (DBDDataFormatterProfile profile : profiles) {
                profileList.add(profile.getProfileName());
            }
            Button deleteButton = getButton(DELETE_ID);
            if (deleteButton != null) {
                deleteButton.setEnabled(false);
            }
        }
    }
}
