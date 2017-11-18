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
package org.jkiss.dbeaver.runtime.internal.ide.ui.handlers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.ide.core.WorkspaceResources;
import org.jkiss.dbeaver.runtime.ide.ui.handlers.CreateLinkHandler;

public class LinkFolderHandler extends CreateLinkHandler {

    @Override
    protected Path[] selectTargets(ExecutionEvent event)
    {
        Shell shell = HandlerUtil.getActiveShell(event);
        DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
        String folder = dialog.open();
        if (folder == null) {
            return NO_TARGETS;
        }
        Path folderPath = Paths.get(folder);
        return new Path[] {folderPath};
    }

    @Override
    protected IStatus createLink(IContainer container, IProgressMonitor monitor, Path... targets)
    {
        return WorkspaceResources.createLinkedFolders(container, monitor, targets);
    }

}