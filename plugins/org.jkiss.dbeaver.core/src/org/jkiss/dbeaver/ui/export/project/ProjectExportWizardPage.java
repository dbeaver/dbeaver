/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
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

    protected ProjectExportWizardPage(String pageName)
    {
        super(pageName);
        setTitle("Export project(s)");
        setDescription("Configure project export settings.");
    }

    @Override
    public boolean isPageComplete()
    {
        if (directoryText == null || directoryText.isDisposed() || projectsTable == null || projectsTable.isDisposed()) {
            return false;
        }
        if (CommonUtils.isEmpty(directoryText.getText())) {
            return false;
        }
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                return true;
            }
        }
        return false;
    }

    public void createControl(Composite parent)
    {
        Set<IProject> projectList = new LinkedHashSet<IProject>();
        final ISelection selection = DBeaverCore.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
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

        Composite placeholder = UIUtils.createPlaceholder(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, false));

        // Project list
        projectsTable = new Table(placeholder, SWT.MULTI | SWT.CHECK | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        projectsTable.setLayoutData(gd);
        projectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getContainer().updateButtons();
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

        // Output folder
        Group generalSettings = UIUtils.createControlGroup(placeholder, "Output", 3, GridData.FILL_HORIZONTAL, 0);
        {
            UIUtils.createControlLabel(generalSettings, "Directory");
            directoryText = new Text(generalSettings, SWT.BORDER);
            directoryText.setText(RuntimeUtils.getUserHomeDir().getAbsolutePath());
            directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            directoryText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    getContainer().updateButtons();
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

        setControl(placeholder);
    }

    public List<IProject> getProjectsToExport()
    {
        List<IProject> result = new ArrayList<IProject>();
        for (TableItem item : projectsTable.getItems()) {
            if (item.getChecked()) {
                result.add((IProject) item.getData());
            }
        }
        return result;
    }

    public File getOutputFolder()
    {
        return new File(directoryText.getText());
    }
}
