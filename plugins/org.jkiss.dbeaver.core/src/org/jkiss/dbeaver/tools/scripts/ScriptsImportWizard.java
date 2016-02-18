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

import org.eclipse.core.resources.IResource;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class ScriptsImportWizard extends Wizard implements IImportWizard {

    static final Log log = Log.getLog(ScriptsImportWizard.class);
    private ScriptsImportWizardPage pageMain;

    public ScriptsImportWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_scripts_import_wizard_window_title);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        pageMain = new ScriptsImportWizardPage();
        addPage(pageMain);
        //addPage(new ProjectImportWizardPageFinal(data));
    }

	@Override
	public boolean performFinish() {
        final ScriptsImportData importData = pageMain.getImportData();
        final ScriptsImporter importer = new ScriptsImporter(importData);
        try {
            DBeaverUI.run(getContainer(), true, true, importer);
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                CoreMessages.dialog_scripts_import_wizard_dialog_error_title,
                CoreMessages.dialog_scripts_import_wizard_dialog_error_text,
                ex.getTargetException());
            return false;
        }
        if (importer.getImportedCount() <= 0) {
            UIUtils.showMessageBox(getShell(), CoreMessages.dialog_scripts_import_wizard_dialog_message_title, CoreMessages.dialog_scripts_import_wizard_dialog_message_no_scripts, SWT.ICON_WARNING);
            return false;
        } else {
            UIUtils.showMessageBox(getShell(), CoreMessages.dialog_scripts_import_wizard_dialog_message_title, importer.getImportedCount() + CoreMessages.dialog_scripts_import_wizard_dialog_message_success_imported, SWT.ICON_INFORMATION);
            return true;
        }
	}

    private int importScripts(DBRProgressMonitor monitor, ScriptsImportData importData) throws IOException, DBException, CoreException
    {
        List<Pattern> masks = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(importData.getFileMasks(), ",; "); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            String mask = st.nextToken().trim();
            if (!CommonUtils.isEmpty(mask)) {
                mask = mask.replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
                masks.add(Pattern.compile(mask));
            }
        }
        List<File> filesToImport = new ArrayList<>();
        collectFiles(importData.getInputDir(), masks, filesToImport);
        if (filesToImport.isEmpty()) {
            return 0;
        }
        // Use null monitor for resource actions to not break our main monitor
        final IProgressMonitor nullMonitor = new NullProgressMonitor();
        // Import scripts
        int imported = filesToImport.size();
		monitor.beginTask(CoreMessages.dialog_scripts_import_wizard_monitor_import_scripts, imported);
        for (File file : filesToImport) {
            // Create dirs
            monitor.subTask(file.getName());
            List<File> path = new ArrayList<>();
            for (File parent = file.getParentFile(); !parent.equals(importData.getInputDir()); parent = parent.getParentFile()) {
                path.add(0, parent);
            }
            // Get target dir
            final IResource srcResource = importData.getImportDir().getResource();
            if (!(srcResource instanceof IFolder)) {
                log.warn("Resource '" + srcResource + "' is not a folder"); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
            IFolder targetDir = (IFolder) srcResource;
            for (File folder : path) {
                targetDir = targetDir.getFolder(folder.getName());
                if (!targetDir.exists()) {
                    targetDir.create(true, true, nullMonitor);
                }
            }
            String targetName = file.getName();
            if (!targetName.toLowerCase().endsWith("." + ResourceUtils.SCRIPT_FILE_EXTENSION)) { //$NON-NLS-1$
                targetName += "." + ResourceUtils.SCRIPT_FILE_EXTENSION; //$NON-NLS-1$
            }

            final IFile targetFile = targetDir.getFile(targetName);
            
            if (targetFile.exists()) {
				if (importData.isOverwriteFiles()) {
	                log.warn("Overwriting file '" + targetFile.getFullPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					targetFile.delete(true, true, RuntimeUtils.getNestedMonitor(monitor));
				} else {
	                log.warn("File '" + targetFile.getFullPath() + "' already exists - skipped"); //$NON-NLS-1$ //$NON-NLS-2$
	                imported--;
					continue;
				}
			}
            // Copy file
            FileInputStream in = new FileInputStream(file);
            try {
                targetFile.create(in, true, nullMonitor);
            } finally {
                ContentUtils.close(in);
            }
            // Set datasource
            if (importData.getDataSourceContainer() != null) {
                SQLEditorInput.setScriptDataSource(targetFile, importData.getDataSourceContainer());
            }
            // Done
            monitor.worked(1);
        }
        monitor.done();

        return imported;
    }

    private void collectFiles(File inputDir, List<Pattern> masks, List<File> filesToImport)
    {
        File[] listFiles = inputDir.listFiles();
        if (listFiles == null) {
        	//!inputDir.exists()
			return;
		}
		for (File file : listFiles) {
            if (file.isDirectory()) {
                collectFiles(file, masks, filesToImport);
            } else {
                boolean matched = false;
                for (Pattern mask : masks) {
                    if (mask.matcher(file.getName()).matches()) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    filesToImport.add(file);
                }
            }
        }
    }


    private class ScriptsImporter implements DBRRunnableWithProgress {
        private final ScriptsImportData importData;
        private int importedCount;

        public ScriptsImporter(ScriptsImportData importData)
        {
            this.importData = importData;
        }

        public int getImportedCount()
        {
            return importedCount;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                importedCount = importScripts(monitor, importData);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
