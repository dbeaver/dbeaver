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

    private Combo encodingCombo;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Text threadsNumText;
    private Button rowCountCheckbox;

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
            Group generalSettings = UIUtils.createControlGroup(composite, "Progress", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createControlLabel(generalSettings, "Maximum threads");
            threadsNumText = new Text(generalSettings, SWT.BORDER);
            threadsNumText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    try {
                        getWizard().getSettings().setMaxJobCount(Integer.parseInt(threadsNumText.getText()));
                    } catch (NumberFormatException e1) {
                        // do nothing
                    }
                }
            });

            rowCountCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Select row count", true);
            rowCountCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setQueryRowCount(rowCountCheckbox.getSelection());
                }
            });
        }

        setControl(composite);

    }

    @Override
    public void activatePart()
    {
        DataExportSettings exportSettings = getWizard().getSettings();
        directoryText.setText(exportSettings.getOutputFolder());
        fileNameText.setText(exportSettings.getOutputFilePattern());
        threadsNumText.setText(String.valueOf(exportSettings.getMaxJobCount()));
        rowCountCheckbox.setSelection(exportSettings.isQueryRowCount());
        compressCheckbox.setSelection(exportSettings.isCompressResults());
        encodingCombo.setText(exportSettings.getOutputEncoding());

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
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