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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.file.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.utils.IOUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIMessageFiles extends AIMessage {
    private final List<Path> attachment;
    private final Map<FileTypeHandlerDescriptor.Extension, List<Path>> fileHandlersByExtension;
    private boolean createConnectionWasExecuted = false;
    private boolean importDataWasExecuted = false;

    public AIMessageFiles(@NotNull List<Path> attachment) {
        super(AIMessageType.ATTACHMENT, "Attach files: " + attachment.stream().map(Path::getFileName).toList(), null);
        this.attachment = List.copyOf(attachment);
        this.fileHandlersByExtension = EditorUtils.getExtensionFiles(attachment, true);
    }

    @NotNull
    public List<Path> getAttachment() {
        return attachment;
    }

    public boolean canCreateConnection() {
        return fileHandlersByExtension.keySet().stream().map(FileTypeHandlerDescriptor.Extension::getDescriptor)
            .anyMatch(FileTypeHandlerDescriptor::isDatabaseHandler);
    }

    public void createConnection() {
        Map<FileTypeHandlerDescriptor, List<Path>> fileHandlers = new HashMap<>();
        for (var entry : fileHandlersByExtension.entrySet()) {
            FileTypeHandlerDescriptor handler = entry.getKey().getDescriptor();
            if (handler.isDatabaseHandler()) {
                fileHandlers.computeIfAbsent(handler, k -> new ArrayList<>()).addAll(entry.getValue());
            }
        }
        for (Map.Entry<FileTypeHandlerDescriptor, List<Path>> entry : fileHandlers.entrySet()) {
            FileTypeHandlerDescriptor handler = entry.getKey();
            List<Path> pathList = entry.getValue();

            if (pathList.isEmpty()) {
                continue;
            }

            for (Path path : pathList) {
                if (!IOUtils.isLocalPath(path)) {
                    if (handler == null || !handler.supportsRemoteFiles()) {
                        return;
                    }
                }
            }
            if (handler != null) {
                try {
                    FileOpenHandler handlerInstance = handler.createHandler();
                    handlerInstance.openFiles(pathList, null, FileTypeAction.DATABASE);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Open file error", "Can't open file '" + pathList + "'", e);
                }
            }
        }
        createConnectionWasExecuted = true;
    }

    public void importData() {
        for (Map.Entry<FileTypeHandlerDescriptor.Extension, List<Path>> entry : fileHandlersByExtension.entrySet()) {
            FileTypeHandlerDescriptor.Extension extension = entry.getKey();
            FileTypeHandlerDescriptor handler = extension.getDescriptor();
            List<Path> pathList = entry.getValue();

            for (Path path : pathList) {
                if (!IOUtils.isLocalPath(path) && !handler.supportsRemoteFiles()) {
                    return;
                }
            }
            try {
                if (handler.createHandler() instanceof FileImportHandler importHandler && extension.isSupportsImport() && extension.getId() != null) {
                    importHandler.importFiles(pathList, extension.getId());
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Open file error", "Can't open file '" + pathList + "'", e);
            }
        }
        importDataWasExecuted = true;
    }

    public boolean canImportData() {
        boolean canImport = false;
        for (var handler : fileHandlersByExtension.keySet()) {
            if (handler == null) {
                continue;
            }
            if (handler.isSupportsImport()) {
                if (canImport) {
                    return false;
                } else {
                    canImport = true;
                }
            }
        }
        return canImport;
    }

    @NotNull
    public DBPImage getIcon(@NotNull Path file) {
        for (var entry : fileHandlersByExtension.entrySet()) {
            for (Path path : entry.getValue()) {
                if (path.equals(file) && entry.getKey() != null && entry.getKey().getIcon() != null) {
                    return entry.getKey().getIcon();
                }
            }
        }
        return DBIcon.TYPE_UNKNOWN;
    }

    public boolean setExplicitHandlerForFile(@NotNull String extensionId) {
        FileTypeHandlerDescriptor descriptor = null;
        FileTypeHandlerDescriptor.Extension extension = null;
        for (FileTypeHandlerDescriptor handler : FileTypeHandlerRegistry.getInstance().getHandlers()) {
            for (FileTypeHandlerDescriptor.Extension ext : handler.getExtensions()) {
                if (ext.getId() != null && ext.getId().equals(extensionId)) {
                    descriptor = handler;
                    extension = ext;
                    break;
                }
            }
        }
        if (descriptor == null) {
            return false;
        }
        fileHandlersByExtension.computeIfAbsent(extension, k -> new ArrayList<>()).add(attachment.getFirst());
        return true;
    }

    public boolean isCreateConnectionWasExecuted() {
        return createConnectionWasExecuted;
    }

    public boolean isImportDataWasExecuted() {
        return importDataWasExecuted;
    }
}
