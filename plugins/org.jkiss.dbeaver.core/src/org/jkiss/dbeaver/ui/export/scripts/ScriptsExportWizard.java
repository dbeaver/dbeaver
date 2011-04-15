/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ScriptsExportWizard extends Wizard implements IExportWizard {

    static final Log log = LogFactory.getLog(ScriptsExportWizard.class);

    private ScriptsExportWizardPage mainPage;

    public ScriptsExportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Scripts Export Wizard"); //NON-NLS-1
        setDefaultPageImageDescriptor(DBIcon.SQL_SCRIPT.getImageDescriptor());
        setNeedsProgressMonitor(true);
        mainPage = new ScriptsExportWizardPage("Export scripts"); //NON-NLS-1
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
        final ScriptsExportData exportData = mainPage.getExportData();
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        exportProjects(monitor, exportData);
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

    public void exportProjects(DBRProgressMonitor monitor, final ScriptsExportData exportData)
        throws IOException, CoreException, InterruptedException
    {

    }

}
