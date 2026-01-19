/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.file;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.file.FileTypeAction;
import org.jkiss.dbeaver.model.file.IFileOpenHandler;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AbstractFileHandler implements IFileOpenHandler {
    @Override
    public void openFiles(
        @NotNull List<Path> fileList,
        @Nullable DBPDataSourceContainer dataSource,
        @NotNull FileTypeAction action
    ) throws DBException {
        switch (action) {
            case EXTERNAL_EDITOR -> {
                for (Path path : fileList) {
                    if (!IOUtils.isLocalPath(path)) {
                        path = copyRemoteFileToTempDir(path);
                    }
                    ShellUtils.openExternalFile(path);
                }
            }
            case INTERNAL_EDITOR -> {
                for (Path path : fileList) {
                    EditorUtils.openExternalFileEditor(path, UIUtils.getActiveWorkbenchWindow());
                }
            }
            case DATABASE -> {
                throw new DBException("Unsupported file action: " + action);
            }
        }
    }

    @NotNull
    @Override
    public FileTypeAction[] supportedActions() {
        return new FileTypeAction[]{FileTypeAction.EXTERNAL_EDITOR, FileTypeAction.INTERNAL_EDITOR};
    }

    @NotNull
    private Path copyRemoteFileToTempDir(@NotNull Path remotePath) throws DBException {
        return UIUtils.runWithMonitor(monitor -> {
            try {
                Path tempFile = ContentUtils.makeTempFile(
                    ContentUtils.getLobFolder(monitor, DBWorkbench.getPlatform()),
                    remotePath.getFileName().toString(),
                    IOUtils.getFileExtension(remotePath)
                );
                try (InputStream is = Files.newInputStream(remotePath)) {
                    try (OutputStream os = Files.newOutputStream(tempFile)) {
                        ContentUtils.copyStreams(is, Files.size(remotePath), os, monitor);
                    }
                }
                return tempFile;
            } catch (IOException e) {
                throw new DBException("Error copying file", e);
            }
        });
    }
}
