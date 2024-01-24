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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt2;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.database.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.prefs.PrefPageDataTransfer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

public class MappingRulesDialog extends BaseDialog {

    private static final Log log = Log.getLog(MappingRulesDialog.class);

    private final DBPDataSource dataSource;
    private final List<Object> elementList;
    private DBPPreferenceStore dbpPreferenceStore;
    private DBPPreferenceStore store;

    private Combo nameCaseCombo;
    private Combo replaceCombo;
    private Spinner typeLengthSpinner;
    private Button saveSettings;

    private int originalNameCaseSelection;
    private int originalReplaceSelection;
    private int originalMaxTypeLength;
    private boolean originalSaveSettingsValue;

    MappingRulesDialog(@NotNull Shell parentShell, @NotNull DBPDataSource dataSource, @NotNull List<Object> elementList) {
        super(parentShell, DTUIMessages.mappings_rules_dialog_title, null);
        this.dataSource = dataSource;
        this.elementList = elementList;
        dbpPreferenceStore = dataSource.getContainer().getPreferenceStore();
        store = DTActivator.getDefault().getPreferences();
        // First check datasource settings, then - global
        originalNameCaseSelection = dbpPreferenceStore.contains(DTConstants.PREF_NAME_CASE_MAPPING) ?
            dbpPreferenceStore.getInt(DTConstants.PREF_NAME_CASE_MAPPING) : store.getInt(DTConstants.PREF_NAME_CASE_MAPPING);
        originalReplaceSelection = dbpPreferenceStore.contains(DTConstants.PREF_REPLACE_MAPPING) ?
            dbpPreferenceStore.getInt(DTConstants.PREF_REPLACE_MAPPING) : store.getInt(DTConstants.PREF_REPLACE_MAPPING);
        originalMaxTypeLength = dbpPreferenceStore.contains(DTConstants.PREF_MAX_TYPE_LENGTH) ?
            dbpPreferenceStore.getInt(DTConstants.PREF_MAX_TYPE_LENGTH) : store.contains(DTConstants.PREF_MAX_TYPE_LENGTH) ?
            store.getInt(DTConstants.PREF_MAX_TYPE_LENGTH) : store.getDefaultInt(DTConstants.PREF_MAX_TYPE_LENGTH);
        originalSaveSettingsValue = store.contains(DTConstants.PREF_SAVE_LOCAL_SETTINGS) ?
            store.getBoolean(DTConstants.PREF_SAVE_LOCAL_SETTINGS) : true;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        final Group mappingGroup = UIUtils.createControlGroup(
            composite,
            DTUIMessages.pref_data_transfer_mapping_group,
            2,
            GridData.FILL_HORIZONTAL,
            0);

        final Label label = UIUtils.createLabel(mappingGroup,
            DTUIMessages.pref_data_transfer_info_label_mapping);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        nameCaseCombo = UIUtils.createLabelCombo(
            mappingGroup,
            DTUIMessages.pref_data_transfer_name_case_label,
            SWT.READ_ONLY | SWT.DROP_DOWN);
        nameCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (MappingNameCase value : MappingNameCase.values()) {
            nameCaseCombo.add(value.getName());
        }
        nameCaseCombo.select(originalNameCaseSelection);

        replaceCombo = UIUtils.createLabelCombo(
            mappingGroup,
            DTUIMessages.pref_data_transfer_replacing_combo_label,
            SWT.READ_ONLY | SWT.DROP_DOWN);
        replaceCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (MappingReplaceMechanism value : MappingReplaceMechanism.values()) {
            replaceCombo.add(value.getName());
        }
        replaceCombo.select(originalReplaceSelection);
        replaceCombo.setToolTipText(DTUIMessages.pref_data_transfer_replacing_combo_tip);

        UIUtils.createControlLabel(mappingGroup, DTUIMessages.pref_data_transfer_spanner_max_length);
        typeLengthSpinner = UIUtils.createSpinner(
            mappingGroup,
            DTUIMessages.pref_data_transfer_spanner_max_length_tip,
            originalMaxTypeLength,
            1,
            Integer.MAX_VALUE);

        saveSettings = UIUtils.createCheckbox(
            mappingGroup,
            DTUIMessages.mappings_rules_dialog_save_settings_checkbox,
            originalSaveSettingsValue);
        saveSettings.setToolTipText(DTUIMessages.mappings_rules_dialog_save_settings_checkbox_tip);
        GridData gd2 = new GridData();
        gd2.horizontalSpan = 2;
        saveSettings.setLayoutData(gd2);
        UIUtils.createLink(mappingGroup, DTMessages.data_transfer_wizard_output_label_global_settings, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.showPreferencesFor(getShell(), null, PrefPageDataTransfer.PAGE_ID);
            }
        });

        return composite;
    }

    @Override
    protected void okPressed() {
        if (nameCaseCombo.getSelectionIndex() != originalNameCaseSelection
            || replaceCombo.getSelectionIndex() != originalReplaceSelection
            || typeLengthSpinner.getSelection() != originalMaxTypeLength
        ) {
            // Something changed. Show notification dialog here
            if (UIUtils.confirmAction(
                getShell(),
                DTUIMessages.mappings_rules_dialog_confirmation_title,
                DTUIMessages.mappings_rules_dialog_confirmation_message,
                DBIcon.STATUS_WARNING
            )) {
                updateMappingsNames();
            } else {
                return;
            }
        } else if (originalSaveSettingsValue != saveSettings.getSelection()) {
            store.setValue(DTConstants.PREF_SAVE_LOCAL_SETTINGS, saveSettings.getSelection());
            if (saveSettings.getSelection()) {
                // User want just to save local settings as global
                store.setValue(DTConstants.PREF_NAME_CASE_MAPPING, nameCaseCombo.getSelectionIndex());
                store.setValue(DTConstants.PREF_REPLACE_MAPPING, replaceCombo.getSelectionIndex());
                store.setValue(DTConstants.PREF_MAX_TYPE_LENGTH, typeLengthSpinner.getSelection());
            }
            PrefUtils.savePreferenceStore(store);
        }
        super.okPressed();
    }

    private void updateMappingsNames() {
        boolean changeNameCase = nameCaseCombo != null && nameCaseCombo.getSelectionIndex() != originalNameCaseSelection;
        boolean changeReplaceMechanism = replaceCombo != null && replaceCombo.getSelectionIndex() != originalReplaceSelection;
        boolean changeDataTypeLength = typeLengthSpinner != null && typeLengthSpinner.getSelection() != originalMaxTypeLength;
        for (Object element : elementList) {
            if (element instanceof DatabaseMappingContainer) {
                DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                DatabaseMappingType mappingType = container.getMappingType();
                if (mappingType == DatabaseMappingType.skip || mappingType == DatabaseMappingType.unspecified) {
                    // Skip this cases
                    continue;
                }
                Collection<DatabaseMappingAttribute> attributeMappings = container.getAttributeMappings();
                for (DatabaseMappingAttribute mapping : attributeMappings) {
                    if (mapping.getMappingType() != DatabaseMappingType.create) {
                        continue;
                    }
                    DBSAttributeBase source = mapping.getSource();
                    mapping.setTargetName(getTransformedName(
                        source != null ? source.getName() : mapping.getTargetName(), changeNameCase));
                    if (changeDataTypeLength && source instanceof DBSTypedObjectExt2) {
                        int maxDataTypeLength = typeLengthSpinner.getSelection();
                        if (source.getMaxLength() > maxDataTypeLength) {
                            ((DBSTypedObjectExt2) source).setMaxLength(maxDataTypeLength);
                        }
                    }
                }
                if (mappingType == DatabaseMappingType.create && (changeNameCase || changeReplaceMechanism)) {
                    container.setTargetName(getTransformedName(getOriginalTargetName(container), changeNameCase));
                }
            }
        }
        boolean saveToGlobalSettings = saveSettings.getSelection();

        // Save settings
        if (changeNameCase) {
            if (saveToGlobalSettings) {
                store.setValue(DTConstants.PREF_NAME_CASE_MAPPING, nameCaseCombo.getSelectionIndex());
            } else {
                dbpPreferenceStore.setValue(DTConstants.PREF_NAME_CASE_MAPPING, nameCaseCombo.getSelectionIndex());
            }
        }
        if (changeReplaceMechanism) {
            if (saveToGlobalSettings) {
                store.setValue(DTConstants.PREF_REPLACE_MAPPING, replaceCombo.getSelectionIndex());
            } else {
                dbpPreferenceStore.setValue(DTConstants.PREF_REPLACE_MAPPING, replaceCombo.getSelectionIndex());
            }
        }
        if (changeDataTypeLength) {
            if (saveToGlobalSettings) {
                store.setValue(DTConstants.PREF_MAX_TYPE_LENGTH, typeLengthSpinner.getSelection());
            } else {
                dbpPreferenceStore.setValue(DTConstants.PREF_MAX_TYPE_LENGTH, typeLengthSpinner.getSelection());
            }
        }
        store.setValue(DTConstants.PREF_SAVE_LOCAL_SETTINGS, saveToGlobalSettings);
        PrefUtils.savePreferenceStore(store);
        if (!saveToGlobalSettings) {
            PrefUtils.savePreferenceStore(dbpPreferenceStore);
        }
    }

    @Nullable
    private String getTransformedName(@Nullable String targetName, boolean caseChanged) {
        if (CommonUtils.isEmpty(targetName)) {
            return targetName;
        }
        String finalName = targetName;
        MappingNameCase nameCase = MappingNameCase.getCaseBySelectionId(nameCaseCombo.getSelectionIndex());
        if (nameCase != MappingNameCase.DEFAULT) {
            finalName = nameCase.getIdentifierCase().transform(targetName);
        } else if (caseChanged) {
            // Only if user changed upper/lower case to default manually - then transform to database case
            finalName = dataSource.getSQLDialect().storesUnquotedCase().transform(targetName);
        }
        if (CommonUtils.isNotEmpty(finalName) && finalName.contains(" ")) {
            MappingReplaceMechanism replaceMechanism =
                MappingReplaceMechanism.getCaseBySelectionId(replaceCombo.getSelectionIndex());
            if (MappingReplaceMechanism.UNDERSCORES == replaceMechanism) {
                finalName = finalName.replaceAll(" ", "_");
            } else if (MappingReplaceMechanism.CAMELCASE == replaceMechanism
                && !(nameCase == MappingNameCase.DEFAULT && dataSource.getSQLDialect().storesUnquotedCase() == DBPIdentifierCase.UPPER)
                && nameCase != MappingNameCase.UPPER // No need to transform upper case names
            ) {
                String camelCaseName = CommonUtils.toCamelCase(finalName);
                if (CommonUtils.isNotEmpty(camelCaseName)) {
                    finalName = camelCaseName.replaceAll(" ", "");
                }
            }
        }
        if (CommonUtils.isNotEmpty(finalName)) {
            // Add quotes for the result name if needed
            return DBUtils.getQuotedIdentifier(dataSource, finalName);
        }
        log.debug("Can't transform target attribute name: " + targetName);
        return targetName;
    }

    // Remove extensions from the file name if source is file
    // Return object name if source is a table
    private String getOriginalTargetName(@NotNull DatabaseMappingContainer container) {
        DBSDataContainer source = container.getSource();
        if (source instanceof StreamEntityMapping) {
            String sourceName = source.getName();
            if (sourceName.contains(".")) {
                return sourceName.substring(0, sourceName.lastIndexOf("."));
            }
        } else if (source != null) {
            return source.getName();
        }
        return container.getTargetName();
    }
}
