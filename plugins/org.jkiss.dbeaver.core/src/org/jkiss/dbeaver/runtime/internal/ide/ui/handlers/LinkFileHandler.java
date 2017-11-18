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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.ide.core.WorkspaceResources;
import org.jkiss.dbeaver.runtime.ide.ui.handlers.CreateLinkHandler;

public class LinkFileHandler extends CreateLinkHandler {

    private static final String COMMAND_PARAMETER_LINK_FILE_CONTENTTYPE = "org.jkiss.dbeaver.core.resource.link.file.contenttype"; //$NON-NLS-1$

    @Override
    protected Path[] selectTargets(ExecutionEvent event)
    {
        Shell shell = HandlerUtil.getActiveShell(event);
        FileDialog dialog = new FileDialog(shell, SWT.MULTI);
        String contentTypeId = event.getParameter(COMMAND_PARAMETER_LINK_FILE_CONTENTTYPE);
        if (contentTypeId != null) {
            IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeId);
            if (contentType != null) {
                StringBuilder sb = new StringBuilder();
                String[] fileSpecs = contentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
                for (String extension : fileSpecs) {
                    if (sb.length() > 0) {
                        sb.append(';');
                    }
                    sb.append('*').append('.').append(extension);
                }
                if (sb.length() > 0) {
                    String[] names = new String[] { contentType.getName() };
                    String[] extensions = new String[] { sb.toString() };
                    dialog.setFilterNames(names);
                    dialog.setFilterExtensions(extensions);
                }

            }
        }
        String file = dialog.open();
        if (file == null) {
            return NO_TARGETS;
        }
        List<Path> paths = new ArrayList<>();
        String filterPath = dialog.getFilterPath();
        String[] fileNames = dialog.getFileNames();
        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            Path filePath = Paths.get(filterPath, fileName);
            paths.add(filePath);
        }
        return (Path[]) paths.toArray(new Path[paths.size()]);
    }

    @Override
    protected IStatus createLink(IContainer container, IProgressMonitor monitor, Path... targets) 
    {
        return WorkspaceResources.createLinkedFiles(container, monitor, targets);
    }

}