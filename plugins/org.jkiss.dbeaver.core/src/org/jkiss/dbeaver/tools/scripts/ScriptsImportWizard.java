/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.SimpleDatabaseEditorContext;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
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

    private static final Log log = Log.getLog(ScriptsImportWizard.class);
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
            UIUtils.run(getContainer(), true, true, importer);
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            DBWorkbench.getPlatformUI().showError(
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

    private int importScripts(DBRProgressMonitor monitor, ScriptsImportData importData) throws IOException, CoreException
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
            if (!targetName.toLowerCase().endsWith("." + SQLEditorUtils.SCRIPT_FILE_EXTENSION)) { //$NON-NLS-1$
                targetName += "." + SQLEditorUtils.SCRIPT_FILE_EXTENSION; //$NON-NLS-1$
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
            try (FileInputStream in = new FileInputStream(file)) {
                targetFile.create(in, true, nullMonitor);
            }
            // Set datasource
            if (importData.getDataSourceContainer() != null) {
                EditorUtils.setFileDataSource(targetFile, new SimpleDatabaseEditorContext(importData.getDataSourceContainer()));
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

        ScriptsImporter(ScriptsImportData importData)
        {
            this.importData = importData;
        }

        int getImportedCount()
        {
            return importedCount;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
            try {
                importedCount = importScripts(monitor, importData);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
