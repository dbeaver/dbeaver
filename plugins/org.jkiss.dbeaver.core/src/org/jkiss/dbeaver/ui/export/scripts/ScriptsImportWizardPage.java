/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;

import java.io.File;


class ScriptsImportWizardPage extends WizardPage {

    private Text directoryText;
    private Text extensionsText;
    private DatabaseNavigatorTree scriptsNavigator;
    private DBNNode importRoot = null;

    protected ScriptsImportWizardPage()
    {
        super("Import script(s)");

        setTitle("Import script(s)");
        setDescription("Configure scripts import settings.");
    }

    @Override
    public boolean isPageComplete()
    {
        return
            !CommonUtils.isEmpty(directoryText.getText()) &&
            !CommonUtils.isEmpty(extensionsText.getText()) &&
            importRoot instanceof DBNResource;
    }

    public void createControl(Composite parent)
    {
        String externalDir = DBeaverCore.getInstance().getGlobalPreferenceStore().getString(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR);
        if (CommonUtils.isEmpty(externalDir)) {
            externalDir = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }

        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayout(new GridLayout(1, false));

        // Input settings
        Composite generalSettings = UIUtils.createPlaceholder(placeholder, 3);
        generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            UIUtils.createControlLabel(generalSettings, "Input directory");
            directoryText = new Text(generalSettings, SWT.BORDER);
            directoryText.setText(externalDir);
            directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            directoryText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    updateState();
                }
            });

            Button openFolder = new Button(generalSettings, SWT.PUSH);
            openFolder.setImage(DBIcon.TREE_FOLDER.getImage());
            openFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                    dialog.setMessage("Choose scripts source directory");
                    dialog.setText("Source directory");
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

            extensionsText = UIUtils.createLabelText(generalSettings, "File extension(s)", "*.sql,*.txt");
        }

        importRoot = ScriptsExportUtils.getScriptsNode();
        scriptsNavigator = new DatabaseNavigatorTree(placeholder, importRoot, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        scriptsNavigator.setLayoutData(gd);
        scriptsNavigator.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event)
            {
                updateState();
            }
        });

        setControl(placeholder);

        updateState();
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    public ScriptsImportData getImportData()
    {
        final String outputDir = directoryText.getText();
        DBeaverCore.getInstance().getGlobalPreferenceStore().setValue(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR, outputDir);
        return new ScriptsImportData(new File(outputDir), extensionsText.getText(), (DBNResource) importRoot);
    }
}
