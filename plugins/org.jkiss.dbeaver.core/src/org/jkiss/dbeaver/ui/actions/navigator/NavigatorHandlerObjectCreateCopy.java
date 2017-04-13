/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

public class NavigatorHandlerObjectCreateCopy extends NavigatorHandlerObjectCreateBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode curNode = NavigatorUtils.getSelectedNode(selection);
        if (curNode != null) {
            Collection<DBNNode> cbNodes = TreeNodeTransfer.getFromClipboard();
            if (cbNodes == null) {
                UIUtils.showErrorDialog(HandlerUtil.getActiveShell(event), "Paste error", "Clipboard contains data in unsupported format");
                return null;
            }
            for (DBNNode nodeObject : cbNodes) {
                if (nodeObject instanceof DBNDatabaseNode) {
                    createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), curNode, ((DBNDatabaseNode)nodeObject));
                } else if (nodeObject instanceof DBNResource && curNode instanceof DBNResource) {
                    pasteResource((DBNResource)nodeObject, (DBNResource)curNode);
                }
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
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
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
            UIUtils.showErrorDialog(null, "Copy error", "Error copying resource", e.getTargetException());
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

}