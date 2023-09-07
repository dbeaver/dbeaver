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
import org.eclipse.core.resources.IProject;
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
import org.jkiss.dbeaver.model.fs.nio.NIOFileSystemRoot;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URI format: {@code dbvfs://{project-name}/{fs-type}/{fs-id}/{fs-root-id}?{fs-specific-path}}
 */
public class NIO2FileSystem extends FileSystem {
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
            log.error("Malformed dbvfs URI: " + uri);
            return EFS.getNullFileSystem().getStore(uri);
        }

        final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace == null) {
            // still initializing
            return EFS.getNullFileSystem().getStore(uri);
        }
        final DBPProject project = workspace.getProject(projectName);
        final DBNModel navigator = Objects.requireNonNull(project.getNavigatorModel());
        final DBNProject projectNode = Objects.requireNonNull(navigator.getRoot().getProjectNode(project));
        final DBNFileSystemNIO2List fileSystemsNode = projectNode.getExtraNode(DBNFileSystemNIO2List.class);
        final DBNFileSystemNIO2 fileSystemNode = fileSystemsNode.getFileSystem(fsRootParts[0], fsRootParts[1]);
        final DBNFileSystemNIO2Resource fileSystemRootNode = fileSystemNode.getRoot(fsRootParts[2]);

        final NIOFileSystemRoot root = new NIOFileSystemRoot(
            project.getEclipseProject(),
            fileSystemRootNode.getRoot()
        );

        try {
            final NIO2FileStore fileStore = new NIO2FileStore(root, fileSystemRootNode.getRoot().getRootPath(new VoidProgressMonitor()).resolve(fsPath));
            fileStoreCache.put(uri, new WeakReference<>(fileStore));
            return fileStore;
        } catch (DBException e) {
            // boom
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static IResource toResource(@NotNull IProject project, @NotNull DBFVirtualFileSystemRoot root, @Nullable Path path) {
        final IFolder folder = project.getFolder(getFileSystemPath(project, root));

        if (!folder.exists() || !folder.isLinked()) {
            try {
                folder.createLink(
                    NIO2FileSystem.toURI(project, root, null),
                    IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL | IResource.VIRTUAL,
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
    public static URI toURI(@NotNull IProject project, @NotNull DBFVirtualFileSystemRoot root, @Nullable Path path) {
        try {
            return new URI(
                "dbvfs",
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
    private static String getFileSystemPath(@NotNull IProject project, @NotNull DBFVirtualFileSystemRoot root) {
        return ".dbvfs_" + project.getName() + '-' + root.getFileSystem().getType() + '-' + root.getFileSystem().getId() + '-' + root.getRootId();
    }
}
