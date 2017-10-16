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

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class NavigatorHandlerLinkFolder extends NavigatorHandlerCreateLink {

    @Override
    protected String selectTarget(ExecutionEvent event)
    {
        Shell shell = HandlerUtil.getActiveShell(event);
        DirectoryDialog dialog = new DirectoryDialog(shell, SWT.NONE);
        String folder = dialog.open();
        return folder;
    }

    @Override
    protected void createLink(IResource resource, String path, IProgressMonitor monitor) throws CoreException
    {
        IFolder container = (IFolder)resource;
        final File externalFolder = new File(path);
        final IFolder linkedFolder = container.getFolder(externalFolder.getName());
        linkedFolder.createLink(externalFolder.toURI(), IResource.NONE, monitor);
    }

}