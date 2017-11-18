/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

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
    protected List<Path> selectTarget(ExecutionEvent event)
    {
        Shell shell = HandlerUtil.getActiveShell(event);
        DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
        String folder = dialog.open();
        if (folder == null) {
            return Collections.emptyList();
        }
        Path folderPath = Paths.get(folder);
        return Collections.singletonList(folderPath);
    }

    @Override
    protected void createLink(IResource resource, List<Path> paths, IProgressMonitor monitor) throws CoreException
    {
        IFolder container = (IFolder) resource;
        for (Path path : paths) {
            String folderName = path.getFileName().toString();
            final IFolder linkedFolder = container.getFolder(folderName);
            linkedFolder.createLink(path.toUri(), IResource.NONE, monitor);
        }
    }

}