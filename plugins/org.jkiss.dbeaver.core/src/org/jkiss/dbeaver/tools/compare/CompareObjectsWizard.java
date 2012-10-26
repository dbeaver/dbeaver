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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private CompareObjectsSettings settings;

    public CompareObjectsWizard(List<DBNDatabaseNode> nodes) {
        this.settings = new CompareObjectsSettings(nodes);
        IDialogSettings section = UIUtils.getDialogSettings(RS_COMPARE_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(section);
    }

    public CompareObjectsSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(new CompareObjectsPageSettings());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("Export data");
        setNeedsProgressMonitor(true);
    }

    @Override
    public boolean performFinish() {
        // Save settings
        getSettings().saveTo(getDialogSettings());

        // Done
        return true;
    }

}