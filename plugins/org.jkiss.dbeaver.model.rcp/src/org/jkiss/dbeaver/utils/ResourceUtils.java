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

package org.jkiss.dbeaver.utils;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFile;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.AlphanumericComparator;
import org.jkiss.utils.ArrayUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Eclipse resource utilities
 */
public class ResourceUtils {

    private static final Log log = Log.getLog(ResourceUtils.class);

    public static void checkFolderExists(@NotNull IFolder folder) throws DBException {
        checkFolderExists(folder, new VoidProgressMonitor());
    }

    public static void checkFolderExists(
        @NotNull IFolder folder,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException {
        if (!folder.exists()) {
            try {
                folder.create(true, true, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't create folder '" + folder.getFullPath() + "'", e);
            }
        }
    }

    public static long getResourceLastModified(@NotNull IResource resource) {
        try {
            IFileStore fileStore = EFS.getStore(resource.getLocationURI());
            IFileInfo iFileInfo = fileStore.fetchInfo();
            return iFileInfo.getLastModified();
        } catch (CoreException e) {
            log.debug(e);
            return -1;
        }
    }

    public static long getFileLength(@NotNull IResource resource) {
        try {
            IFileStore fileStore = EFS.getStore(resource.getLocationURI());
            IFileInfo iFileInfo = fileStore.fetchInfo();
            return iFileInfo.getLength();
        } catch (CoreException e) {
            log.debug(e);
            return -1;
        }
    }

    public static void syncFile(@NotNull DBRProgressMonitor monitor, @NotNull IResource localFile) {
        // Sync file with contents
        try {
            localFile.refreshLocal(IFile.DEPTH_ZERO, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Can't synchronize file '" + localFile + "' with contents", e);
        }
    }

    @NotNull
    public static IFile getUniqueFile(@NotNull IFolder folder, @NotNull String fileName, @NotNull String fileExt) {
        IFile file = folder.getFile(fileName + "." + fileExt);
        if (!file.exists()) {
            // Fast path
            return file;
        }

        IResource[] members;
        try {
            members = folder.members();
        } catch (CoreException ignored) {
            return getUniqueFileFallback(folder, fileName, fileExt);
        }

        // Look for all matching files and grab the one that has the highest index
        var pattern = Pattern.compile("^%s-(\\d+).%s".formatted(Pattern.quote(fileName), Pattern.quote(fileExt)));
        var files = Stream.of(members)
            .filter(r -> pattern.matcher(r.getName()).matches())
            .sorted(Comparator.comparing(IResource::getName, AlphanumericComparator.getInstance()))
            .toList();

        if (!files.isEmpty()) {
            Matcher matcher = pattern.matcher(files.getLast().getName());
            if (matcher.matches()) {
                try {
                    int index = Integer.parseInt(matcher.group(1));
                    return folder.getFile(fileName + '-' + (index + 1) + '.' + fileExt);
                } catch (NumberFormatException ignored) {
                    // How did we get here?
                }
            }
        }

        return getUniqueFileFallback(folder, fileName, fileExt);
    }

    @NotNull
    private static IFile getUniqueFileFallback(@NotNull IFolder folder, @NotNull String fileName, @NotNull String fileExt) {
        IFile file = folder.getFile(fileName + "." + fileExt);
        int index = 1;
        while (file.exists()) {
            file = folder.getFile(fileName + "-" + index + "." + fileExt);
            index++;
        }
        return file;
    }

    @Nullable
    public static IFile convertPathToWorkspaceFile(@NotNull IPath path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = root.getFileForLocation(path);
        if (file != null) {
            return file;
        }
        // Probably we have a path to some linked resource
        IPath folderPath = path.removeLastSegments(1);
        URI folderURI = folderPath.toFile().toURI();
        IContainer[] containers = root.findContainersForLocationURI(folderURI);
        if (!ArrayUtils.isEmpty(containers)) {
            IContainer container = containers[0];
            file = container.getFile(path.removeFirstSegments(path.segmentCount() - 1));
        }
        return file;
    }

    @Nullable
    public static IPath convertPathToWorkspacePath(@NotNull IPath path) {
        IFile wFile = convertPathToWorkspaceFile(path);
        return wFile == null ? null : wFile.getFullPath();
    }

    @NotNull
    public static IResource createResourceFromPath(
        @NotNull DBFVirtualFileSystemRoot fsRoot,
        @NotNull IProject project,
        @NotNull Path path
    ) {
        EFSNIOFileSystemRoot root = new EFSNIOFileSystemRoot(
            project,
            fsRoot,
            fsRoot.getFileSystem().getType() + "/" + fsRoot.getFileSystem().getId() + "/" + fsRoot.getRootId()
        );
        if (fsRoot.getFileSystem().isDirectory(path)) {
            return new EFSNIOFolder(root, path);
        } else {
            return new EFSNIOFile(root, path);
        }
    }

}
