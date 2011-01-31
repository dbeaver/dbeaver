/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;


public class ProjectImportWizardPage extends WizardPage {

    private String curFolder;
    private Text fileNameText;
    private Button importDriverCheck;

    protected ProjectImportWizardPage(String pageName)
    {
        super(pageName);
        setTitle("Import project(s)");
        setDescription("Configure project import settings.");
    }

    @Override
    public boolean isPageComplete()
    {
        if (fileNameText != null && !fileNameText.isDisposed()) {
            String fileName = fileNameText.getText();
            if (CommonUtils.isEmpty(fileName)) {
                setMessage("Input file is not specified", IMessageProvider.ERROR);
                return false;
            }
            File file = new File(fileName);
            if (!file.exists()) {
                setMessage("File '" + fileName + "' doesn't exist", IMessageProvider.ERROR);
                return false;
            }
            if (!file.isFile()) {
                setMessage("File '" + fileName + "' is a directory", IMessageProvider.ERROR);
                return false;
            }
            setMessage("Configure project import settings", IMessageProvider.NONE);
            return true;
        }
        return false;
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, "Input", 3, GridData.FILL_HORIZONTAL, 0);

        fileNameText = UIUtils.createLabelText(configGroup, "File", "");
        fileNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                updateState();
            }
        });
        Button openFolder = new Button(configGroup, SWT.PUSH);
        openFolder.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
        openFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
                fd.setText("Open export archive");
                fd.setFilterPath(curFolder);
                String[] filterExt = {"*.dbp", "*.*"};
                fd.setFilterExtensions(filterExt);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    fileNameText.setText(selected);
                }
            }
        });
        importDriverCheck = UIUtils.createCheckbox(configGroup, "Import driver libraries", true);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 3;
        importDriverCheck.setLayoutData(gd);

        setControl(configGroup);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    public String getImportFile()
    {
        return fileNameText.getText();
    }

    public boolean isImportDriverLibraries()
    {
        return importDriverCheck.getSelection();
    }
}
