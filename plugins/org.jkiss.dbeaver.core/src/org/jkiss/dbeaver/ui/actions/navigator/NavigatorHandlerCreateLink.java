/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerCreateLink extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNResource)) {
                return null;
            }
            final IResource resource = ((DBNResource) element).getResource();
            if (resource instanceof IFolder) {
                final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
                DirectoryDialog dialog = new DirectoryDialog(workbenchWindow.getShell(), SWT.NONE);
                String folder = dialog.open();
                if (folder != null) {
                    createLink(workbenchWindow, (IFolder)resource, folder);
                }
            }
        }
        return null;
    }

    private void createLink(IWorkbenchWindow workbenchWindow, IFolder folder, String fsFolder)
    {
        final File externalFolder = new File(fsFolder);
        try {
            final IFolder linkedFolder = folder.getFolder(externalFolder.getName());
            workbenchWindow.run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        linkedFolder.createLink(externalFolder.toURI(), IResource.NONE, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Create link", "Can't create link", e);
        } catch (InterruptedException e) {
            // skip
        }
    }

}