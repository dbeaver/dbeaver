/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScriptsExportWizard extends Wizard implements IExportWizard {

    private static final Log log = Log.getLog(ScriptsExportWizard.class);

    private ScriptsExportWizardPage mainPage;

    public ScriptsExportWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_scripts_export_wizard_window_title); //NON-NLS-1
        setDefaultPageImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SQL_SCRIPT));
        setNeedsProgressMonitor(true);
        mainPage = new ScriptsExportWizardPage(CoreMessages.dialog_scripts_export_wizard_page_name); //NON-NLS-1
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
        final ScriptsExportData exportData = mainPage.getExportData();
        try {
            DBeaverUI.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        exportScripts(monitor, exportData);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Export error",
                "Cannot export scripts",
                ex.getTargetException());
            return false;
        }
        return true;
	}

    public void exportScripts(DBRProgressMonitor monitor, final ScriptsExportData exportData)
        throws IOException, CoreException, InterruptedException
    {
        Collection<IResource> scripts = exportData.getScripts();
        int totalFiles = 0;
        for (IResource res : scripts) {
            if (res instanceof IFolder) {
                totalFiles += countFiles((IFolder) res);
            } else {
                totalFiles++;
            }
        }
        monitor.beginTask("Export scripts", totalFiles);
        for (IResource res : scripts) {
            if (res instanceof IFolder) {
                exportFolder(monitor, (IFolder)res, exportData);
            } else {
                exportScript(monitor, (IFile) res, exportData);
            }
        }
        monitor.done();
    }

    private int countFiles(IFolder folder)
    {
        try {
            int count = 0;
            for (IResource res : folder.members()) {
                if (res instanceof IFile) {
                    count++;
                } else if (res instanceof IFolder) {
                    count += countFiles((IFolder) res);
                }
            }
            return count;
        } catch (CoreException e) {
            return 0;
        }
    }

    private void exportFolder(DBRProgressMonitor monitor, IFolder folder, final ScriptsExportData exportData) throws CoreException, IOException
    {
        if (monitor.isCanceled()) {
            return;
        }
        File fsDir = makeExternalFile(folder, exportData.getOutputFolder());
        if (!fsDir.exists()) {
            if (!fsDir.mkdirs()) {
                throw new IOException("Can't create directory '" + fsDir.getAbsolutePath() + "'");
            }
        }
        for (IResource res : folder.members()) {
            if (monitor.isCanceled()) {
                return;
            }
            if (res instanceof IFile) {
                exportScript(monitor, (IFile)res, exportData);
            } else if (res instanceof IFolder) {
                exportFolder(monitor, (IFolder)res, exportData);
            }
        }
    }

    private File makeExternalFile(IResource folder, File outputFolder)
    {
        List<IResource> path = new ArrayList<>();
        for (IResource f = folder; f.getParent() instanceof IFolder; f = f.getParent()) {
            path.add(0, f);
        }
        File fsDir = outputFolder;
        for (IResource pathItem : path) {
            fsDir = new File(fsDir, pathItem.getName());
        }
        return fsDir;
    }

    private void exportScript(DBRProgressMonitor monitor, IFile file, final ScriptsExportData exportData)
        throws IOException, CoreException
    {
        File fsFile = makeExternalFile(file, exportData.getOutputFolder());
        if (fsFile.exists()) {
            if (fsFile.isDirectory()) {
                throw new IOException("Target file '" + fsFile.getAbsolutePath() + "' is a directory");
            } else if (!exportData.isOverwriteFiles()) {
                log.warn("File '" + fsFile.getAbsolutePath() + "' already exists - skipped"); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            } else {
                log.warn("Overwriting file '" + fsFile.getAbsolutePath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        final File fileDir = fsFile.getParentFile();
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                throw new IOException("Can't create directory '" + fileDir.getAbsolutePath() + "'");
            }
        }
        try (final InputStream scriptContents = file.getContents(true)) {
            try (FileOutputStream out = new FileOutputStream(fsFile)) {
                IOUtils.copyStream(scriptContents, out);
            }
        }
        monitor.worked(1);
    }

}
