/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ScriptsImportWizard extends Wizard implements IImportWizard {

    static final Log log = LogFactory.getLog(ScriptsImportWizard.class);
    private ScriptsImportWizardPage pageMain;

    public ScriptsImportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Scripts Import Wizard");
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        pageMain = new ScriptsImportWizardPage();
        addPage(pageMain);
        //addPage(new ProjectImportWizardPageFinal(data));
    }

	@Override
	public boolean performFinish() {
        final ScriptsImportData importData = pageMain.getImportData();
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        importProjects(monitor, importData);
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
                "Import error",
                "Cannot import scripts",
                ex.getTargetException());
            return false;
        }
        UIUtils.showMessageBox(getShell(), "Scripts import", "Script(s) successfully imported", SWT.ICON_INFORMATION);
        return true;
	}

    private void importProjects(DBRProgressMonitor monitor, ScriptsImportData importData) throws IOException, DBException
    {
    }


}
