/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

class DataExportPageSettings extends ActiveWizardPage<DataExportWizard> {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;

    private EditablePropertiesControl propsEditor;
    private Combo lobExtractType;
    private Label segmentSizeLabel;
    private Text segmentSizeText;
    private Combo rowsExtractType;
    private Label lobEncodingLabel;
    private Combo lobEncodingCombo;

    private boolean initialized = false;

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
                UIUtils.createControlLabel(generalSettings, "Extract type");
                rowsExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                rowsExtractType.setItems(new String[] {
                    "Single query",
                    "By segments" });
                rowsExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataExportSettings exportSettings = getWizard().getSettings();
                        switch (rowsExtractType.getSelectionIndex()) {
                            case EXTRACT_TYPE_SEGMENTS: exportSettings.setExtractType(DataExportSettings.ExtractType.SEGMENTS); break;
                            case EXTRACT_TYPE_SINGLE_QUERY: exportSettings.setExtractType(DataExportSettings.ExtractType.SINGLE_QUERY); break;
                        }
                        updatePageCompletion();
                    }
                });

                segmentSizeLabel = UIUtils.createControlLabel(generalSettings, "Segment size");
                segmentSizeText = new Text(generalSettings, SWT.BORDER);
                segmentSizeText.addModifyListener(new ModifyListener() {
                    public void modifyText(ModifyEvent e)
                    {
                        try {
                            getWizard().getSettings().setSegmentSize(Integer.parseInt(segmentSizeText.getText()));
                        } catch (NumberFormatException e1) {
                            // just skip it
                        }
                    }
                });
            }

            {
                UIUtils.createControlLabel(generalSettings, "LOBs");
                lobExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobExtractType.setItems(new String[] {
                    "Skip column",
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

                lobEncodingLabel = UIUtils.createControlLabel(generalSettings, "LOB Encoding");
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

    @Override
    public void activatePart() {
        DataExportSettings exportSettings = getWizard().getSettings();
        DataExporterDescriptor exporter = exportSettings.getDataExporter();
        propsEditor.loadProperties(exporter.getPropertyGroups(), exportSettings.getExtractorProperties());

        segmentSizeText.setText(String.valueOf(exportSettings.getSegmentSize()));
        switch (exportSettings.getExtractType()) {
            case SINGLE_QUERY: rowsExtractType.select(EXTRACT_TYPE_SINGLE_QUERY); break;
            case SEGMENTS: rowsExtractType.select(EXTRACT_TYPE_SEGMENTS); break;
        }
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
        int selectionIndex = rowsExtractType.getSelectionIndex();
        if (selectionIndex == EXTRACT_TYPE_SEGMENTS) {
            segmentSizeLabel.setVisible(true);
            segmentSizeText.setVisible(true);
        } else {
            segmentSizeLabel.setVisible(false);
            segmentSizeText.setVisible(false);
        }

        selectionIndex = lobExtractType.getSelectionIndex();
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