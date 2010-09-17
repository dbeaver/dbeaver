/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

class DataExportPageOutput extends ActiveWizardPage<DataExportWizard> {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private Combo encodingCombo;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Spinner threadsNumText;
    private Combo rowsExtractType;
    private Label segmentSizeLabel;
    private Text segmentSizeText;
    private Button newConnectionCheckbox;
    private Button rowCountCheckbox;
    private Button showFolderCheckbox;

    DataExportPageOutput() {
        super("Output");
        setTitle("Output");
        setDescription("Configure export output parameters");
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
            Group generalSettings = UIUtils.createControlGroup(composite, "General", 3, GridData.FILL_HORIZONTAL, 0);
            {
                UIUtils.createControlLabel(generalSettings, "Directory");
                directoryText = new Text(generalSettings, SWT.BORDER | SWT.READ_ONLY);
                directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                directoryText.addModifyListener(new ModifyListener() {
                    public void modifyText(ModifyEvent e) {
                        getWizard().getSettings().setOutputFolder(directoryText.getText());
                        updatePageCompletion();
                    }
                });

                Button openFolder = new Button(generalSettings, SWT.PUSH);
                openFolder.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
                openFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                        dialog.setMessage("Choose directory to place exported files");
                        dialog.setText("Export directory");
                        String directory = directoryText.getText();
                        if (!CommonUtils.isEmpty(directory)) {
                            dialog.setFilterPath(directory);
                        }
                        directory = dialog.open();
                        if (directory != null) {
                            directoryText.setText(directory);
                        }
                    }
                });
            }

            UIUtils.createControlLabel(generalSettings, "File name pattern");
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    getWizard().getSettings().setOutputFilePattern(fileNameText.getText());
                    updatePageCompletion();
                }
            });

            UIUtils.createControlLabel(generalSettings, "Encoding");
            encodingCombo = UIUtils.createEncodingCombo(generalSettings, getWizard().getSettings().getOutputEncoding());
            encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 2, 1));
            encodingCombo.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    int index = encodingCombo.getSelectionIndex();
                    if (index >= 0) {
                        getWizard().getSettings().setOutputEncoding(encodingCombo.getItem(index));
                    }
                }
            });

            compressCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Compress", false);
            compressCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 2, 1));
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setCompressResults(compressCheckbox.getSelection());
                }
            });
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, "Progress", 4, GridData.FILL_HORIZONTAL, 0);

            Label threadsNumLabel = UIUtils.createControlLabel(generalSettings, "Maximum threads");
            threadsNumText = new Spinner(generalSettings, SWT.BORDER);
            threadsNumText.setMinimum(1);
            threadsNumText.setMaximum(10);
            threadsNumText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    try {
                        getWizard().getSettings().setMaxJobCount(Integer.parseInt(threadsNumText.getText()));
                    } catch (NumberFormatException e1) {
                        // do nothing
                    }
                }
            });
            if (getWizard().getSettings().getDataProviders().size() < 2) {
                threadsNumLabel.setEnabled(false);
                threadsNumText.setEnabled(false);
            }
            threadsNumText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

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
                segmentSizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
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
                segmentSizeText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
            }

            newConnectionCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Open new connection(s)", true);
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });
            newConnectionCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            rowCountCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Select row count", true);
            rowCountCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setQueryRowCount(rowCountCheckbox.getSelection());
                }
            });
            rowCountCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            showFolderCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Open output folder at end", true);
            showFolderCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setOpenFolderOnFinish(showFolderCheckbox.getSelection());
                }
            });
            showFolderCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        setControl(composite);

    }

    @Override
    public void activatePart()
    {
        DataExportSettings exportSettings = getWizard().getSettings();
        directoryText.setText(exportSettings.getOutputFolder());
        fileNameText.setText(exportSettings.getOutputFilePattern());
        threadsNumText.setSelection(exportSettings.getMaxJobCount());
        newConnectionCheckbox.setSelection(exportSettings.isOpenNewConnections());
        rowCountCheckbox.setSelection(exportSettings.isQueryRowCount());
        compressCheckbox.setSelection(exportSettings.isCompressResults());
        encodingCombo.setText(exportSettings.getOutputEncoding());
        showFolderCheckbox.setSelection(exportSettings.isOpenFolderOnFinish());

        segmentSizeText.setText(String.valueOf(exportSettings.getSegmentSize()));
        switch (exportSettings.getExtractType()) {
            case SINGLE_QUERY: rowsExtractType.select(EXTRACT_TYPE_SINGLE_QUERY); break;
            case SEGMENTS: rowsExtractType.select(EXTRACT_TYPE_SEGMENTS); break;
        }

        updatePageCompletion();
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

        boolean valid = true;
        if (CommonUtils.isEmpty(getWizard().getSettings().getOutputFolder())) {
            valid = false;
        }
        if (CommonUtils.isEmpty(getWizard().getSettings().getOutputFilePattern())) {
            valid = false;
        }
        return valid;
    }

}