/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.scripts;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.*;
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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class ScriptsImportWizardPage extends WizardPage {

    private Text directoryText;
    private Text extensionsText;
    private CImageCombo scriptsDataSources;
    private Button overwriteCheck;
    private DBNNode importRoot = null;

    protected ScriptsImportWizardPage()
    {
        super(CoreMessages.dialog_scripts_import_wizard_name);

        setTitle(CoreMessages.dialog_scripts_import_wizard_title);
        setDescription(CoreMessages.dialog_scripts_import_wizard_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return
            !CommonUtils.isEmpty(directoryText.getText()) &&
            !CommonUtils.isEmpty(extensionsText.getText()) &&
            importRoot instanceof DBNResource;
    }

    @Override
    public void createControl(Composite parent)
    {
        String externalDir = DBeaverCore.getGlobalPreferenceStore().getString(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR);
        if (CommonUtils.isEmpty(externalDir)) {
            externalDir = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }

        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayout(new GridLayout(1, false));

        // Input settings
        Composite generalSettings = UIUtils.createPlaceholder(placeholder, 3);
        generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_scripts_import_wizard_label_input_directory);
            directoryText = new Text(generalSettings, SWT.BORDER);
            directoryText.setText(externalDir);
            directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            directoryText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    updateState();
                }
            });

            Button openFolder = new Button(generalSettings, SWT.PUSH);
            openFolder.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
            openFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                    dialog.setMessage(CoreMessages.dialog_scripts_import_wizard_dialog_choose_dir_message);
                    dialog.setText(CoreMessages.dialog_scripts_import_wizard_dialog_choose_dir_text);
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

            extensionsText = UIUtils.createLabelText(generalSettings, CoreMessages.dialog_scripts_import_wizard_label_file_mask, "*.sql,*.txt"); //$NON-NLS-2$
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            extensionsText.setLayoutData(gd);

            UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_scripts_import_wizard_label_default_connection);
            scriptsDataSources = new CImageCombo(generalSettings, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
            for (DataSourceDescriptor dataSourceDescriptor : DataSourceDescriptor.getActiveDataSources()) {
                scriptsDataSources.add(DBeaverIcons.getImage(dataSourceDescriptor.getObjectImage()), dataSourceDescriptor.getName(), null, dataSourceDescriptor);
            }

            if (scriptsDataSources.getItemCount() > 0) {
                scriptsDataSources.select(0);
            }

            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            gd.verticalIndent = 2;
            scriptsDataSources.setLayoutData(gd);
        }

        UIUtils.createControlLabel(placeholder, CoreMessages.dialog_scripts_import_wizard_label_root_folder);
        importRoot = ScriptsExportUtils.getScriptsNode();
        final DatabaseNavigatorTree scriptsNavigator = new DatabaseNavigatorTree(placeholder, importRoot, SWT.BORDER | SWT.SINGLE, true);
        scriptsNavigator.setLayoutData(new GridData(GridData.FILL_BOTH));
        scriptsNavigator.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection sel = (IStructuredSelection)event.getSelection();
                if (sel == null || sel.isEmpty()) {
                    importRoot = null;
                } else {
                    importRoot = (DBNNode) sel.getFirstElement();
                }
                updateState();
            }
        });
        scriptsNavigator.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return element instanceof DBNResource && ((DBNResource) element).getResource() instanceof IFolder;
            }
        });
        scriptsNavigator.getViewer().expandToLevel(2);

        overwriteCheck = UIUtils.createCheckbox(placeholder, CoreMessages.dialog_project_export_wizard_page_checkbox_overwrite_files, false);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 3;
        overwriteCheck.setLayoutData(gd);

        setControl(placeholder);

        updateState();
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    public ScriptsImportData getImportData()
    {
        DBPDataSourceContainer dataSourceContainer = null;
        final int dsIndex = scriptsDataSources.getSelectionIndex();
        if (dsIndex >= 0) {
            dataSourceContainer = (DBPDataSourceContainer) scriptsDataSources.getData(dsIndex);
        }
        final String outputDir = directoryText.getText();
        DBeaverCore.getGlobalPreferenceStore().setValue(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR, outputDir);
        return new ScriptsImportData(
            new File(outputDir),
            extensionsText.getText(),
            overwriteCheck.getSelection(),
            (DBNResource) importRoot,
            dataSourceContainer);
    }
}
