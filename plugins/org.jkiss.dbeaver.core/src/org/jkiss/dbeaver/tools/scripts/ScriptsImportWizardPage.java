/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.scripts;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class ScriptsImportWizardPage extends WizardPage {

    private Text directoryText;
    private Text extensionsText;
    private CSmartCombo<DBPDataSourceContainer> scriptsDataSources;
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
            importRoot instanceof DBNResource && ((DBNResource) importRoot).getResource() instanceof IFolder;
    }

    @Override
    public void createControl(Composite parent)
    {
        String externalDir = DBWorkbench.getPlatform().getPreferenceStore().getString(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR);
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
            scriptsDataSources = new CSmartCombo<>(generalSettings, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new ConnectionLabelProvider());
            for (DBPDataSourceContainer dataSourceDescriptor : DataSourceRegistry.getAllDataSources()) {
                scriptsDataSources.addItem(dataSourceDescriptor);
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
        importRoot = DBWorkbench.getPlatform().getNavigatorModel().getRoot();
        final DatabaseNavigatorTree scriptsNavigator = new DatabaseNavigatorTree(placeholder, importRoot, SWT.BORDER | SWT.SINGLE, false);
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
                return element instanceof DBNResource && ((DBNResource) element).getResource() instanceof IContainer;
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
            dataSourceContainer = (DBPDataSourceContainer) scriptsDataSources.getItem(dsIndex);
        }
        final String outputDir = directoryText.getText();
        DBWorkbench.getPlatform().getPreferenceStore().setValue(ScriptsExportWizardPage.PREF_SCRIPTS_EXPORT_OUT_DIR, outputDir);
        return new ScriptsImportData(
            new File(outputDir),
            extensionsText.getText(),
            overwriteCheck.getSelection(),
            (DBNResource) importRoot,
            dataSourceContainer);
    }

    private static class ConnectionLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public Image getImage(Object element) {
            final DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode((DataSourceDescriptor) element);
            return node == null ? null : DBeaverIcons.getImage(node.getNodeIcon());
        }

        @Override
        public String getText(Object element) {
            return ((DataSourceDescriptor) element).getName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            return element == null ? null : UIUtils.getConnectionColor(((DataSourceDescriptor) element).getConnectionConfiguration());
        }
    }

}
