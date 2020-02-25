/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class ScriptsExportWizardPage extends WizardPage {

    private static final Log log = Log.getLog(ScriptsExportWizardPage.class);

    static final String PREF_SCRIPTS_EXPORT_OUT_DIR = "export.scripts.out.dir"; //$NON-NLS-1$

    private Button overwriteCheck;
    private Text directoryText;
    private DatabaseNavigatorTree scriptsNavigator;
    private final List<DBNResource> selectedResources = new ArrayList<>();

    protected ScriptsExportWizardPage(String pageName)
    {
        super(pageName);
        setTitle(CoreMessages.dialog_project_export_wizard_page_title);
    }

    @Override
    public boolean isPageComplete()
    {
        if (directoryText == null || directoryText.isDisposed() || scriptsNavigator == null || scriptsNavigator.isDisposed()) {
            return false;
        }
        if (CommonUtils.isEmpty(directoryText.getText())) {
            setMessage(CoreMessages.dialog_project_export_wizard_page_message_no_output_dir, IMessageProvider.ERROR);
            return false;
        }
        selectedResources.clear();
        CheckboxTreeViewer viewer = scriptsNavigator.getCheckboxViewer();
        for (Object obj : viewer.getCheckedElements()) {
            if (obj instanceof DBNResource) {
                selectedResources.add((DBNResource) obj);
            }
        }
        if (selectedResources.isEmpty()) {
            setMessage(CoreMessages.dialog_project_export_wizard_page_message_check_script, IMessageProvider.ERROR);
            return false;
        } else {
            setMessage(CoreMessages.dialog_project_export_wizard_page_message_configure_settings, IMessageProvider.NONE);
            return true;
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        String outDir = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SCRIPTS_EXPORT_OUT_DIR);
        if (CommonUtils.isEmpty(outDir)) {
            outDir = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }

        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayout(new GridLayout(1, false));

        // Project list
        scriptsNavigator = new DatabaseNavigatorTree(placeholder, DBWorkbench.getPlatform().getNavigatorModel().getRoot(), SWT.BORDER | SWT.CHECK);
        GridData gd = new GridData(GridData.FILL_BOTH);
        scriptsNavigator.setLayoutData(gd);
        CheckboxTreeViewer viewer = (CheckboxTreeViewer) scriptsNavigator.getViewer();
        viewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event)
            {
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

        // Output folder
        Composite generalSettings = UIUtils.createPlaceholder(placeholder, 3);
        generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            overwriteCheck = UIUtils.createCheckbox(generalSettings, CoreMessages.dialog_project_export_wizard_page_checkbox_overwrite_files, false);
            gd = new GridData(GridData.BEGINNING);
            gd.horizontalSpan = 3;
            overwriteCheck.setLayoutData(gd);
            UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_project_export_wizard_page_label_directory);
            directoryText = new Text(generalSettings, SWT.BORDER);
            directoryText.setText(outDir);
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
                    dialog.setMessage(CoreMessages.dialog_project_export_wizard_page_dialog_choose_export_dir_message);
                    dialog.setText(CoreMessages.dialog_project_export_wizard_page_dialog_choose_export_dir_text);
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

        setControl(placeholder);

        updateState();
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    public ScriptsExportData getExportData()
    {
        Set<IResource> result = new LinkedHashSet<>();
        // Add folders
        for (DBNResource resourceNode : selectedResources) {
            final IResource resource = resourceNode.getResource();
            if (resource instanceof IFolder) {
                addResourceToSet(result, resource);
            }
        }
        // Add files
        for (DBNResource resourceNode : selectedResources) {
            final IResource resource = resourceNode.getResource();
            addResourceToSet(result, resource);
        }

        final String outputDir = directoryText.getText();
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_SCRIPTS_EXPORT_OUT_DIR, outputDir);
        return new ScriptsExportData(result, overwriteCheck.getSelection(), new File(outputDir));
    }

    private void addResourceToSet(Set<IResource> result, IResource resource)
    {
        boolean skip = false;
        for (IResource parent = resource.getParent(); parent != null; parent = parent.getParent()) {
            if (result.contains(parent)) {
                skip = true;
                break;
            }
        }
        if (!skip) {
            result.add(resource);
        }
    }

}
