/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * NewConnectionDialog
 */
public class ActiveWizardDialog extends WizardDialog
{
    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard)
    {
        this(window, wizard, null);
    }

    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard, IStructuredSelection selection)
    {
        super(window.getShell(), wizard);

        // Initialize wizard
        if (wizard instanceof IWorkbenchWizard) {
            if (selection == null) {
                if (window.getSelectionService().getSelection() instanceof IStructuredSelection) {
                    selection = (IStructuredSelection)window.getSelectionService().getSelection();
                }
            }
            ((IWorkbenchWizard)wizard).init(window.getWorkbench(), selection);
        }
        addPageChangingListener(new IPageChangingListener() {
            @Override
            public void handlePageChanging(PageChangingEvent event)
            {
                if (event.getCurrentPage() instanceof ActiveWizardPage) {
                    ((ActiveWizardPage) event.getCurrentPage()).deactivatePage();
                }
//                if (event.getTargetPage() instanceof ActiveWizardPage) {
//                    ((ActiveWizardPage) event.getTargetPage()).activatePage();
//                }
            }
        });


    }

}
