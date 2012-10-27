/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.compare;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    static final Log log = LogFactory.getLog(CompareObjectsWizard.class);

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private CompareObjectsSettings settings;

    public CompareObjectsWizard(List<DBNDatabaseNode> nodes)
    {
        this.settings = new CompareObjectsSettings(nodes);
        IDialogSettings section = UIUtils.getDialogSettings(RS_COMPARE_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(section);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    public CompareObjectsSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages()
    {
        super.addPages();
        addPage(new CompareObjectsPageSettings());
        addPage(new CompareObjectsPageOutput());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection)
    {
        setWindowTitle("Compare objects");
        setNeedsProgressMonitor(true);
    }

    private void showError(String error)
    {
        ((WizardPage)getContainer().getCurrentPage()).setErrorMessage(error);
    }

    @Override
    public boolean performFinish()
    {
        // Save settings
        getSettings().saveTo(getDialogSettings());
        showError(null);

        // Compare
        final CompareObjectsExecutor executor = new CompareObjectsExecutor(settings);
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        monitor.beginTask("Compare objects", 100);
                        executor.compareNodes(monitor, getSettings().getNodes());
                        monitor.done();
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            UIUtils.showMessageBox(getShell(), "Objects compare", "Objects compare finished", SWT.ICON_INFORMATION);
        } catch (InvocationTargetException e) {
            if (executor.getInitializeError() != null) {
                showError(executor.getInitializeError().getMessage());
            } else {
                log.error(e.getTargetException());
                showError(e.getTargetException().getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            showError("Compare interrupted");
            return false;
        } finally {
            executor.dispose();
        }

        // Done
        return true;
    }

}