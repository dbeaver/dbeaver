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

    private static final String PATTERN_TABLE = "{table}";
    private static final String PATTERN_TIMESTAMP = "{timestamp}";

    private static final int DEfAULT_THREADS_NUM = 5;

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
                        validateParameters();
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
            fileNameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 2, 1));
            fileNameText.setText(PATTERN_TABLE + "-" + PATTERN_TIMESTAMP);
            fileNameText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    validateParameters();
                }
            });

            UIUtils.createControlLabel(generalSettings, "Encoding");
            encodingCombo = UIUtils.createEncodingCombo(generalSettings, System.getProperty("file.encoding"));
            encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 2, 1));

            compressCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Compress", false);
            compressCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 2, 1));

            // select count
            // maximum threads
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, "Progress", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createControlLabel(generalSettings, "Maximum threads");
            threadsNumText = new Text(generalSettings, SWT.BORDER);
            threadsNumText.setText(String.valueOf(DEfAULT_THREADS_NUM));
            rowCountCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Select row count", true);
        }

        setControl(composite);

        validateParameters();
    }

    private void validateParameters() {
        boolean valid = true;
        if (directoryText == null || CommonUtils.isEmpty(directoryText.getText())) {
            valid = false;
        }
        if (fileNameText == null || CommonUtils.isEmpty(fileNameText.getText())) {
            valid = false;
        }
        setPageComplete(valid);
    }

}