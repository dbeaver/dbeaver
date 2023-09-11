/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.nio2;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.fs2.DBNFileSystemNIO2;
import org.jkiss.dbeaver.model.navigator.fs2.DBNFileSystemNIO2List;
import org.jkiss.dbeaver.model.navigator.fs2.DBNFileSystemNIO2Resource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URI format: {@code dbvfs://{project-name}/{fs-type}/{fs-id}/{fs-root-id}?{fs-specific-path}}
 */
public class NIO2FileSystem extends FileSystem {
    private static final String SCHEME = "dbvfs";
    private static final Log log = Log.getLog(NIO2FileSystem.class);

    private final Map<URI, WeakReference<NIO2FileStore>> fileStoreCache = new ConcurrentHashMap<>();

    @Override
    public IFileStore getStore(URI uri) {
        final WeakReference<NIO2FileStore> cachedFileStore = fileStoreCache.get(uri);
        if (cachedFileStore != null) {
            final NIO2FileStore fileStore = cachedFileStore.get();
            if (fileStore != null) {
                return fileStore;
            }
        }

        final String projectName = uri.getHost();
        final String fsPath = CommonUtils.removeLeadingSlash(CommonUtils.notEmpty(uri.getQuery()));
        final String fsRoot = CommonUtils.removeLeadingSlash(CommonUtils.notEmpty(uri.getPath()));
        final String[] fsRootParts = fsRoot.split("/");

        if (CommonUtils.isEmpty(projectName) || fsRootParts.length != 3) {
            log.error("Malformed " + SCHEME + " URI: " + uri);
            return EFS.getNullFileSystem().getStore(uri);
        }

        final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace == null) {
            // Despite "getWorkspace" marked as non-null, it can return "null" during platform initialization
            return EFS.getNullFileSystem().getStore(uri);
        }

        final DBPProject project = workspace.getProject(projectName);
        final DBNModel navigator = project.getNavigatorModel();
        final DBNProject projectNode = navigator != null ? navigator.getRoot().getProjectNode(project) : null;
        final DBNFileSystemNIO2List fileSystemsNode = projectNode != null ? projectNode.getExtraNode(DBNFileSystemNIO2List.class) : null;
        final DBNFileSystemNIO2 fileSystemNode = fileSystemsNode != null ? fileSystemsNode.getFileSystem(fsRootParts[0], fsRootParts[1]) : null;
        final DBNFileSystemNIO2Resource fileSystemRootNode = fileSystemsNode != null ? fileSystemNode.getRoot(fsRootParts[2]) : null;

        if (fileSystemRootNode == null) {
            log.error("The " + SCHEME + " URI contains unrecognized project/filesystem: " + uri);
            return EFS.getNullFileSystem().getStore(uri);
        }

        try {
            final NIO2FileStore fileStore = new NIO2FileStore(
                project,
                fileSystemRootNode.getRoot(),
                fileSystemRootNode.getRoot().getRootPath(new VoidProgressMonitor()).resolve(fsPath)
            );
            fileStoreCache.put(uri, new WeakReference<>(fileStore));
            return fileStore;
        } catch (DBException e) {
            log.error("The " + SCHEME + " URI cannot be resolved: " + uri, e);
            return EFS.getNullFileSystem().getStore(uri);
        }
    }

    @NotNull
    public static IResource toResource(@NotNull DBPProject project, @NotNull DBFVirtualFileSystemRoot root, @Nullable Path path) {
        final IFolder folder = project.getEclipseProject().getFolder(getFileSystemPath(project, root));

        if (!folder.exists() || !folder.isLinked()) {
            try {
                folder.createLink(
                    NIO2FileSystem.toURI(project, root, null),
                    IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL,
                    new NullProgressMonitor()
                );
            } catch (CoreException e) {
                throw new IllegalStateException("Can't create link: " + e.getMessage(), e);
            }
        }

        if (path == null) {
            return folder;
        } else if (Files.isDirectory(path)) {
            return folder.getFolder(path.toUri().getPath());
        } else {
            return folder.getFile(path.toUri().getPath());
        }
    }

    @NotNull
    public static URI toURI(@NotNull DBPProject project, @NotNull DBFVirtualFileSystemRoot root, @Nullable Path path) {
        try {
            return new URI(
                SCHEME,
                project.getName(),
                '/' + root.getFileSystem().getType() + '/' + root.getFileSystem().getId() + '/' + root.getRootId(),
                path != null ? path.toUri().getPath() : null,
                null
            );
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @NotNull
    private static String getFileSystemPath(@NotNull DBPProject project, @NotNull DBFVirtualFileSystemRoot root) {
        return String.format(
            "." + SCHEME + "-%s-%s-%s-%s",
            project.getName(),
            root.getFileSystem().getType(),
            root.getFileSystem().getId(),
            root.getRootId()
        );
    }
}
