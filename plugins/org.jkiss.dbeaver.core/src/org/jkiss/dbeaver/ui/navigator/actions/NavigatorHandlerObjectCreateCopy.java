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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

public class NavigatorHandlerObjectCreateCopy extends NavigatorHandlerObjectCreateBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode curNode = NavigatorUtils.getSelectedNode(selection);
        if (curNode != null) {
            Clipboard clipboard = new Clipboard(Display.getDefault());
            try {
                @SuppressWarnings("unchecked")
                Collection<DBNNode> cbNodes = (Collection<DBNNode>) clipboard.getContents(TreeNodeTransfer.getInstance());
                if (cbNodes != null) {
                    for (DBNNode nodeObject : cbNodes) {
                        if (nodeObject instanceof DBNDatabaseNode) {
                            createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), curNode, ((DBNDatabaseNode) nodeObject));
                        } else if (nodeObject instanceof DBNResource && curNode instanceof DBNResource) {
                            pasteResource((DBNResource) nodeObject, (DBNResource) curNode);
                        }
                    }
                } else if (curNode instanceof DBNResource) {
                    String[] files = (String[]) clipboard.getContents(FileTransfer.getInstance());
                    if (files != null) {
                        for (String fileName : files) {
                            final File file = new File(fileName);
                            if (file.exists()) {
                                pasteResource(file, (DBNResource) curNode);
                            }
                        }
                    } else {
                        DBWorkbench.getPlatformUI().showError("Paste error", "Unsupported clipboard format. File or folder were expected.");
                    }
                } else {
                    DBWorkbench.getPlatformUI().showError("Paste error", "Clipboard contains data in unsupported format");
                }
            } finally {
                clipboard.dispose();
            }

        }
        return null;
    }

    private void pasteResource(DBNResource resourceNode, DBNResource toFolder) {
        final IResource resource = resourceNode.getResource();
        final IResource targetResource = toFolder.getResource();
        assert resource != null;
        assert targetResource != null;
        final IContainer targetFolder = targetResource instanceof IContainer ? (IContainer) targetResource : targetResource.getParent();
        try {
            UIUtils.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        copyResource(monitor, resource, targetFolder);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Copy error", "Error copying resource", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void copyResource(@NotNull DBRProgressMonitor monitor, @NotNull IResource resource, @NotNull IContainer targetFolder) throws CoreException, IOException {
        final IProgressMonitor nestedMonitor = RuntimeUtils.getNestedMonitor(monitor);
        final String extension = resource.getFileExtension();
        String targetName = resource.getName();

        if (resource.getParent().equals(targetFolder)) {
            String plainName = extension != null && !extension.isEmpty() && targetName.endsWith(extension) ?
                targetName.substring(0, targetName.length() - extension.length() - 1) : targetName;
            for (int i = 1; ; i++) {
                String testName =  plainName + "-" + i;
                if (!CommonUtils.isEmpty(extension)) {
                    testName += "." + extension;
                }
                if (targetFolder.findMember(testName) == null) {
                    targetName = testName;
                    break;
                }
            }
        } else if (targetFolder.findMember(targetName) != null) {
            throw new IOException("Target resource '" + targetName + "' already exists");
        }
        if (resource instanceof IFile) {
            // Copy single file
            final IFile targetFile = targetFolder.getFile(new Path(targetName));
            if (!targetFile.exists()) {
                targetFile.create(new ByteArrayInputStream(new byte[0]), true, nestedMonitor);
            }
            final Map<QualifiedName, String> props = resource.getPersistentProperties();
            if (props != null && !props.isEmpty()) {
                for (Map.Entry<QualifiedName, String> prop : props.entrySet()) {
                    targetFile.setPersistentProperty(prop.getKey(), prop.getValue());
                }
            }
            try (InputStream is = ((IFile) resource).getContents()) {
                targetFile.setContents(is, true, true, nestedMonitor);
            }
        } else if (resource instanceof IFolder) {
            // Copy folder with all files and subfolders
        }
    }

    private void pasteResource(final File file, DBNResource toFolder) {
        final IResource targetResource = toFolder.getResource();
        assert targetResource != null;
        final IContainer targetFolder = targetResource instanceof IContainer ? (IContainer) targetResource : targetResource.getParent();
        try {
            UIUtils.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        final IFile targetFile = targetFolder.getFile(new Path(file.getName()));
                        if (targetFile.exists()) {
                            throw new IOException("Target file '" + targetFile.getFullPath() + "' already exists");
                        }
                        try (InputStream is = new FileInputStream(file)) {
                            targetFile.create(is, true, monitor.getNestedMonitor());
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Copy error", "Error copying resource", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

}