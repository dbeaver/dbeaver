/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ide.IDE;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.fs.DBFFileStoreProvider;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ProgramInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerDescriptor;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerRegistry;
import org.jkiss.dbeaver.ui.editors.file.IFileTypeHandler;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Default resource handler
 */
public class DefaultResourceHandlerImpl extends AbstractResourceHandler {

    public static final DefaultResourceHandlerImpl INSTANCE = new DefaultResourceHandlerImpl();

    @Override
    public int getFeatures(IResource resource) {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        } else if (resource instanceof IFolder) {
            return FEATURE_DELETE | FEATURE_RENAME | FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
        }
        return super.getFeatures(resource);
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource) {
        final ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null) {
            return program.getProgram().getName();
        }
        return "resource"; //$NON-NLS-1$
    }

    @Nullable
    @Override
    public String getResourceDescription(@NotNull IResource resource) {
        return "";
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException {
        IPath location = resource.getLocation();
        if (location != null) {
            // Try to open using file handler
            Path path = location.toPath();
            if (Files.exists(path)) {
                String fileExtension = IOUtils.getFileExtension(path);
                if (!CommonUtils.isEmpty(fileExtension)) {
                    FileTypeHandlerDescriptor fthd = FileTypeHandlerRegistry.getInstance().findHandler(fileExtension);
                    if (fthd != null) {
                        try {
                            IFileTypeHandler handler = fthd.createHandler();
                            handler.openFiles(Collections.singletonList(path), Map.of(), null);
                            return;
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
        if (resource instanceof DBFFileStoreProvider) {
            IFileStore fileStore = ((DBFFileStoreProvider) resource).getFileStore();
            long length = fileStore.fetchInfo().getLength();
            if (!UIUtils.confirmAction(resource.getFullPath().toString(), "Open remote resource '" + resource.getFullPath() +
                "'?\nSize = " + ByteNumberFormat.getInstance().format(length) + " bytes")) {
                return;
            }

            // open the editor on the file
            IEditorDescriptor editorDesc;
            try {
                editorDesc = IDE.getEditorDescriptor((IFile)resource, true, true);
            } catch (OperationCanceledException ex) {
                return;
            }

            try {
                final Path[] target = new Path[1];

                UIUtils.runInProgressService(monitor -> {
                    try {
                        target[0] = Files.createTempFile(
                            DBWorkbench.getPlatform().getTempFolder(monitor, "external-files"),
                            null,
                            fileStore.getName()
                        );

                        try (InputStream is = fileStore.openInputStream(EFS.NONE, null)) {
                            try (OutputStream os = Files.newOutputStream(target[0])) {
                                final IFileInfo info = fileStore.fetchInfo(EFS.NONE, null);
                                ContentUtils.copyStreams(is, info.getLength(), os, monitor);
                            }
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                });

                if (IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID.equals(editorDesc.getId())) {
                    // Here we could potentially start a new process
                    // and wait for it to finish, this will allow us to:
                    //  1. Delete the temporary file right away
                    //  2. Detect changes made by an external editor
                    // But for now it's okay, I assume.

                    Program.launch(target[0].toString());
                } else {
                    IDE.openEditor(
                        UIUtils.getActiveWorkbenchWindow().getActivePage(),
                        DBFUtils.getUriFromPath(target[0]),
                        editorDesc.getId(),
                        true
                    );
                }
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Error opening resource", "Can't open resource using external editor", e.getTargetException());
            } catch (InterruptedException ignored) {
            }
        } else if (resource instanceof IFile) {
            IDE.openEditor(UIUtils.getActiveWorkbenchWindow().getActivePage(), (IFile) resource);
        } else if (resource instanceof IFolder) {
            DBWorkbench.getPlatformUI().executeShellProgram(location.toOSString());
        }
    }

}
