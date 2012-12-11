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
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

public class CreateDiagramHandler extends DataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        IFolder diagramFolder = null;
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNResource && ((DBNResource)element).getResource() instanceof IFolder) {
                diagramFolder = (IFolder) ((DBNResource)element).getResource();
            }
        }
        if (diagramFolder == null && DBeaverCore.getInstance().getProjectRegistry().getActiveProject() == null) {
            UIUtils.showErrorDialog(HandlerUtil.getActiveShell(event), "ERD Error", "Can't create diagram without active project");
            return null;
        }
        ActiveWizardDialog dialog = new ActiveWizardDialog(
            HandlerUtil.getActiveWorkbenchWindow(event),
            new DiagramCreateWizard(diagramFolder));
        dialog.open();

        return null;
    }

}