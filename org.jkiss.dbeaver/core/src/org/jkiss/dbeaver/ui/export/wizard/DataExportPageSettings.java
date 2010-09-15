/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;

import java.util.TreeMap;

class DataExportPageSettings extends WizardDataTransferPage {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;

    private static final int DEFAULT_SEGMENT_SIZE = 100000;

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

    @Override
    protected boolean allowNewContainerName() {
        return false;
    }

    public void handleEvent(Event event) {

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
                        int selectionIndex = rowsExtractType.getSelectionIndex();
                        if (selectionIndex == EXTRACT_TYPE_SEGMENTS) {
                            segmentSizeLabel.setVisible(true);
                            segmentSizeText.setVisible(true);
                        } else {
                            segmentSizeLabel.setVisible(false);
                            segmentSizeText.setVisible(false);
                        }
                    }
                });

                segmentSizeLabel = UIUtils.createControlLabel(generalSettings, "Segment size");
                segmentSizeText = new Text(generalSettings, SWT.BORDER);
                segmentSizeLabel.setVisible(false);
                segmentSizeText.setVisible(false);
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
                        int selectionIndex = lobExtractType.getSelectionIndex();
                        if (selectionIndex == EXTRACT_LOB_INLINE) {
                            lobEncodingLabel.setVisible(true);
                            lobEncodingCombo.setVisible(true);
                        } else {
                            lobEncodingLabel.setVisible(false);
                            lobEncodingCombo.setVisible(false);
                        }
                    }
                });

                lobEncodingLabel = UIUtils.createControlLabel(generalSettings, "LOB Encoding");
                lobEncodingCombo = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobEncodingCombo.setItems(new String[] {
                    "Base64",
                    "Hex",
                    "Binary" });
                lobEncodingLabel.setVisible(false);
                lobEncodingCombo.setVisible(false);
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
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && !initialized) {
            initialized = true;
            DataExportWizard wizard = (DataExportWizard)getWizard();
            DataExporterDescriptor exporter = wizard.getSelectedExporter();
            propsEditor.loadProperties(exporter.getPropertyGroups(), new TreeMap<String, Object>());

            segmentSizeText.setText(String.valueOf(DEFAULT_SEGMENT_SIZE));
            rowsExtractType.select(EXTRACT_TYPE_SINGLE_QUERY);
            lobExtractType.select(EXTRACT_LOB_SKIP);
            lobEncodingCombo.select(LOB_ENCODING_HEX);

            validateParameters();
        }
    }

    private void validateParameters() {
        setPageComplete(true);
    }

}