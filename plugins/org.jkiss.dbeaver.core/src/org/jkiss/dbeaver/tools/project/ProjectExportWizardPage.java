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
package org.jkiss.dbeaver.tools.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;
import java.util.List;


class ProjectExportWizardPage extends WizardPage {

    private static final String PREF_PROJECTS_EXPORT_OUT_DIR = "export.projects.out.dir"; //NON-NLS-1 //$NON-NLS-1$

    private Text directoryText;
    private Table projectsTable;
    private Button exportDriverCheck;
    private Text fileNameText;
    private boolean fileNameEdited = false;

    protected ProjectExportWizardPage(String pageName)
    {
        super(pageName);
        setTitle(CoreMessages.dialog_project_export_wizard_start_title);
    }

    @Override
    public boolean isPageComplete()
    {
        if (directoryText == null || directoryText.isDisposed() || projectsTable == null || projectsTable.isDisposed()) {
            return false;
        }
        if (CommonUtils.isEmpty(directoryText.getText())) {
            setMessage(CoreMessages.dialog_project_export_wizard_start_message_empty_output_directory, IMessageProvider.ERROR);
            return false;
        }
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                setMessage(CoreMessages.dialog_project_export_wizard_start_message_configure_settings, IMessageProvider.NONE);
                return true;
            }
        }
        setMessage(CoreMessages.dialog_project_export_wizard_start_message_choose_project, IMessageProvider.ERROR);
        return false;
    }

    @Override
    public void createControl(Composite parent)
    {
        String outDir = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_PROJECTS_EXPORT_OUT_DIR);
        if (CommonUtils.isEmpty(outDir)) {
            outDir = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }

        Set<IProject> projectList = new LinkedHashSet<>();
        final ISelection selection = UIUtils.getActiveWorkbenchWindow().getActivePage().getSelection();
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                IResource resource = RuntimeUtils.getObjectAdapter(element, IResource.class);
                if (resource != null) {
                    projectList.add(resource.getProject());
                }
            }
        }
        if (projectList.isEmpty()) {
            IProject activeProject = DBWorkbench.getPlatform().getProjectManager().getActiveProject();
            if (activeProject != null) {
    			projectList.add(activeProject);
			}
        }

        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayout(new GridLayout(1, false));

        // Project list
        projectsTable = new Table(placeholder, SWT.MULTI | SWT.CHECK | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        projectsTable.setLayoutData(gd);
        projectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateState();
            }
        });

        for (IProject project : DBWorkbench.getPlatform().getLiveProjects()) {
            final TableItem item = new TableItem(projectsTable, SWT.NONE);
            item.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
            item.setText(project.getName());
            item.setData(project);
            if (projectList.contains(project)) {
                item.setChecked(true);
            }
        }

        final Composite fileNameGroup = UIUtils.createPlaceholder(placeholder, 2);
        fileNameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileNameText = UIUtils.createLabelText(fileNameGroup, CoreMessages.dialog_project_export_wizard_start_label_output_file, null); //$NON-NLS-2$
        fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (!CommonUtils.equalObjects(fileNameText.getText(), getArchiveFileName(getProjectsToExport()))) {
                    fileNameEdited = true;
                }
            }
        });

        // Output folder
        Composite generalSettings = UIUtils.createPlaceholder(placeholder, 3);
        generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_project_export_wizard_start_label_directory);
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
                    dialog.setMessage(CoreMessages.dialog_project_export_wizard_start_dialog_directory_message);
                    dialog.setText(CoreMessages.dialog_project_export_wizard_start_dialog_directory_text);
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
        exportDriverCheck = UIUtils.createCheckbox(placeholder, CoreMessages.dialog_project_export_wizard_start_checkbox_libraries, false);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 3;
        exportDriverCheck.setLayoutData(gd);

        setControl(placeholder);

        updateState();
    }

    private void updateState()
    {
        if (!fileNameEdited) {
            final String archiveFileName = getArchiveFileName(getProjectsToExport());
            fileNameText.setText(archiveFileName);
        }
        getContainer().updateButtons();
    }

    ProjectExportData getExportData()
    {
        final String outputDir = directoryText.getText();
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_PROJECTS_EXPORT_OUT_DIR, outputDir);
        return new ProjectExportData(
            getProjectsToExport(),
            new File(outputDir),
            exportDriverCheck.getSelection(),
            fileNameText.getText());
    }

    private List<IProject> getProjectsToExport()
    {
        List<IProject> result = new ArrayList<>();
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                result.add((IProject) item.getData());
            }
        }
        return result;
    }

    static String getArchiveFileName(List<IProject> projects)
    {
        String archiveName = CoreMessages.dialog_project_export_wizard_start_archive_name_prefix;
        if (projects.size() == 1) {
            archiveName = projects.get(0).getName();
        }
        archiveName += "-" + RuntimeUtils.getCurrentDate(); //$NON-NLS-1$
        //archiveName += ExportConstants.ARCHIVE_FILE_EXT;
        return archiveName;
    }

}
