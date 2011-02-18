/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.util.*;
import java.util.List;


public class ProjectExportWizardPage extends WizardPage {

    private Text directoryText;
    private Table projectsTable;
    private Button exportDriverCheck;
    private Text fileNameText;
    private boolean fileNameEdited = false;

    protected ProjectExportWizardPage(String pageName)
    {
        super(pageName);
        setTitle("Export project(s)");
    }

    @Override
    public boolean isPageComplete()
    {
        if (directoryText == null || directoryText.isDisposed() || projectsTable == null || projectsTable.isDisposed()) {
            return false;
        }
        if (CommonUtils.isEmpty(directoryText.getText())) {
            setMessage("Output directory is not specified.", IMessageProvider.ERROR);
            return false;
        }
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                setMessage("Configure project export settings.", IMessageProvider.NONE);
                return true;
            }
        }
        setMessage("Choose a project(s) to export.", IMessageProvider.ERROR);
        return false;
    }

    public void createControl(Composite parent)
    {
        Set<IProject> projectList = new LinkedHashSet<IProject>();
        final ISelection selection = DBeaverCore.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                IResource resource = (IResource) Platform.getAdapterManager().getAdapter(element, IResource.class);
                if (resource != null) {
                    projectList.add(resource.getProject());
                }
            }
        }
        if (projectList.isEmpty()) {
            projectList.add(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
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

        for (IProject project : DBeaverCore.getInstance().getLiveProjects()) {
            final TableItem item = new TableItem(projectsTable, SWT.NONE);
            item.setImage(DBIcon.PROJECT.getImage());
            item.setText(project.getName());
            item.setData(project);
            if (projectList.contains(project)) {
                item.setChecked(true);
            }
        }

        final Composite fileNameGroup = UIUtils.createPlaceholder(placeholder, 2);
        fileNameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileNameText = UIUtils.createLabelText(fileNameGroup, "Output file", "");
        fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileNameText.addModifyListener(new ModifyListener() {
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
            UIUtils.createControlLabel(generalSettings, "Directory");
            directoryText = new Text(generalSettings, SWT.BORDER);
            directoryText.setText(RuntimeUtils.getUserHomeDir().getAbsolutePath());
            directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            directoryText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    updateState();
                }
            });

            Button openFolder = new Button(generalSettings, SWT.PUSH);
            openFolder.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
            openFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
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
        exportDriverCheck = UIUtils.createCheckbox(placeholder, "Export driver libraries", false);
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
        return new ProjectExportData(
            getProjectsToExport(),
            new File(directoryText.getText()),
            exportDriverCheck.getSelection(),
            fileNameText.getText());
    }

    private List<IProject> getProjectsToExport()
    {
        List<IProject> result = new ArrayList<IProject>();
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                result.add((IProject) item.getData());
            }
        }
        return result;
    }

    static String getArchiveFileName(List<IProject> projects)
    {
        String archiveName = "All";
        if (projects.size() == 1) {
            archiveName = projects.get(0).getName();
        }
        archiveName += "-" + RuntimeUtils.getCurrentDate();
        //archiveName += ExportConstants.ARCHIVE_FILE_EXT;
        return archiveName;
    }

}
