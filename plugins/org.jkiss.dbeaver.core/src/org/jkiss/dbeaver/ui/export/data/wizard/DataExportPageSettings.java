/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.wizard;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.registry.DataFormatterRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;

class DataExportPageSettings extends ActiveWizardPage<DataExportWizard> {

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;

    private EditablePropertiesControl propsEditor;
    private Combo lobExtractType;
    private Label lobEncodingLabel;
    private Combo lobEncodingCombo;
    private Combo formatProfilesCombo;

    DataExportPageSettings() {
        super("Settings");
        setTitle("Settings");
        setDescription("Set export settings");
        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group generalSettings = new Group(composite, SWT.NONE);
            generalSettings.setText("General");
            gl = new GridLayout(4, false);
            generalSettings.setLayout(gl);
            generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                Composite formattingGroup = UIUtils.createPlaceholder(generalSettings, 3);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 4;
                formattingGroup.setLayoutData(gd);
                
                UIUtils.createControlLabel(formattingGroup, "Formatting");
                formatProfilesCombo = new Combo(formattingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = 200;
                formatProfilesCombo.setLayoutData(gd);
                formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (formatProfilesCombo.getSelectionIndex() > 0) {
                            getWizard().getSettings().setFormatterProfile(
                                DBeaverCore.getInstance().getDataFormatterRegistry().getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
                        } else {
                            getWizard().getSettings().setFormatterProfile(null);
                        }
                    }
                });

                Button profilesManageButton = new Button(formattingGroup, SWT.PUSH);
                profilesManageButton.setText("Edit ... ");
                profilesManageButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        //DataFormatProfilesEditDialog dialog = new DataFormatProfilesEditDialog(getShell());
                        //dialog.open();
                        PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                            getShell(),
                            DBeaverCore.getInstance().getDataFormatterRegistry(),
                            PrefPageDataFormat.PAGE_ID,
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
                UIUtils.createControlLabel(generalSettings, "Binaries");
                lobExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobExtractType.setItems(new String[] {
                    "Set to NULL",
                    "Save to files",
                    "Inline" });
                lobExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataExportSettings exportSettings = getWizard().getSettings();
                        switch (lobExtractType.getSelectionIndex()) {
                            case EXTRACT_LOB_SKIP: exportSettings.setLobExtractType(DataExportSettings.LobExtractType.SKIP); break;
                            case EXTRACT_LOB_FILES: exportSettings.setLobExtractType(DataExportSettings.LobExtractType.FILES); break;
                            case EXTRACT_LOB_INLINE: exportSettings.setLobExtractType(DataExportSettings.LobExtractType.INLINE); break;
                        }
                        updatePageCompletion();
                    }
                });

                lobEncodingLabel = UIUtils.createControlLabel(generalSettings, "Encoding");
                lobEncodingCombo = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobEncodingCombo.setItems(new String[] {
                    "Base64",
                    "Hex",
                    "Binary" });
                lobEncodingCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataExportSettings exportSettings = getWizard().getSettings();
                        switch (lobEncodingCombo.getSelectionIndex()) {
                            case LOB_ENCODING_BASE64: exportSettings.setLobEncoding(DataExportSettings.LobEncoding.BASE64); break;
                            case LOB_ENCODING_HEX: exportSettings.setLobEncoding(DataExportSettings.LobEncoding.HEX); break;
                            case LOB_ENCODING_BINARY: exportSettings.setLobEncoding(DataExportSettings.LobEncoding.BINARY); break;
                        }
                    }
                });
            }
        }

        Group exporterSettings = new Group(composite, SWT.NONE);
        exporterSettings.setText("Exporter settings");
        exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
        exporterSettings.setLayout(new GridLayout(1, false));
        
        propsEditor = new EditablePropertiesControl(exporterSettings, SWT.NONE);
        propsEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

        setControl(composite);
    }

    private Object getSelectedFormatterProfile()
    {
        DataFormatterRegistry registry = DBeaverCore.getInstance().getDataFormatterRegistry();
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
        DataFormatterRegistry registry = DBeaverCore.getInstance().getDataFormatterRegistry();
        formatProfilesCombo.removeAll();
        formatProfilesCombo.add("<Connection's default>");
        for (DBDDataFormatterProfile profile : registry.getCustomProfiles()) {
            formatProfilesCombo.add(profile.getProfileName());
        }
        DBDDataFormatterProfile formatterProfile = getWizard().getSettings().getFormatterProfile();
        if (formatterProfile != null) {
            if (!UIUtils.setComboSelection(formatProfilesCombo, formatterProfile.getProfileName())) {
                formatProfilesCombo.select(0);
            }
        } else {
            formatProfilesCombo.select(0);
        }
    }

    @Override
    public void activatePart() {
        DataExportSettings exportSettings = getWizard().getSettings();
        DataExporterDescriptor exporter = exportSettings.getDataExporter();
        propsEditor.loadProperties(exporter.getPropertyGroups(), exportSettings.getExtractorProperties());

        switch (exportSettings.getLobExtractType()) {
            case SKIP: lobExtractType.select(EXTRACT_LOB_SKIP); break;
            case FILES: lobExtractType.select(EXTRACT_LOB_FILES); break;
            case INLINE: lobExtractType.select(EXTRACT_LOB_INLINE); break;
        }
        switch (exportSettings.getLobEncoding()) {
            case BASE64: lobEncodingCombo.select(LOB_ENCODING_BASE64); break;
            case HEX: lobEncodingCombo.select(LOB_ENCODING_HEX); break;
            case BINARY: lobEncodingCombo.select(LOB_ENCODING_BINARY); break;
        }

        updatePageCompletion();
    }

    @Override
    public void deactivatePart()
    {
        getWizard().getSettings().setExtractorProperties(propsEditor.getPropertiesWithDefaults());
        super.deactivatePart();
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