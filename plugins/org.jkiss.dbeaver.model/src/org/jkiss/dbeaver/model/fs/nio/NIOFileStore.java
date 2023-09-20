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
package org.jkiss.dbeaver.model.fs.nio;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class NIOFileStore extends FileStore {
    private static final Log log = Log.getLog(NIOFileStore.class);

    private final DBPProject project;
    private final DBFVirtualFileSystemRoot root;
    private final Path path;

    private volatile String[] childNames;
    private volatile NIOFileInfo info;
    private boolean allowedToLoadChildNames;

    public NIOFileStore(@NotNull DBPProject project, @NotNull DBFVirtualFileSystemRoot root, @NotNull Path path) {
        this.project = project;
        this.root = root;
        this.path = path;
    }

    public void setAllowedToLoadChildNames(boolean allowedToLoadChildNames) {
        this.allowedToLoadChildNames = allowedToLoadChildNames;
    }

    @Override
    public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
        if (!allowedToLoadChildNames) {
            return new String[0];
        }

        if (childNames == null) {
            synchronized (this) {
                if (childNames == null) {
                    try (Stream<Path> stream = Files.list(path)) {
                        childNames = stream
                            .filter(NIOFileStore::isValidPath)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .map(name -> URLDecoder.decode(name, StandardCharsets.UTF_8))
                            .toArray(String[]::new);
                    } catch (IOException e) {
                        throw new CoreException(Status.error("Unable to get child names", e));
                    }
                }
            }
        }

        return childNames;
    }

    public boolean childrenCached() {
        return childNames != null;
    }

    public void purgeCachedChildren() {
        childNames = null;
    }

    @Override
    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
        checkOptions(options, EFS.NONE);

        if (info == null) {
            synchronized (this) {
                if (info == null) {
                    info = new NIOFileInfo(path);
                }
            }
        }

        return info;
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.NONE);

        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to open input stream", e));
        }
    }

    @Override
    public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.NONE);

        try {
            return Files.newOutputStream(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to open output stream", e));
        }
    }

    @Override
    public void delete(int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.NONE);

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to delete file", e));
        }

        NIOMonitor.notifyResourceChange(this, NIOListener.Action.DELETE);
    }

    @Override
    public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.OVERWRITE | EFS.SHALLOW);

        try {
            Files.copy(path, getPath(destination), getCopyOptions(options));
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to move resource", e));
        }

        if (destination instanceof NIOFileStore) {
            NIOMonitor.notifyResourceChange((NIOFileStore) destination, NIOListener.Action.CREATE);
        }
    }

    @Override
    public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.OVERWRITE);

        copy(destination, options, monitor);
        delete(options, monitor);
    }

    @Override
    public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
        checkOptions(options, EFS.SHALLOW);

        final NIOFileInfo info = (NIOFileInfo) fetchInfo();

        if (info.exists() && info.isDirectory()) {
            return this;
        }

        try {
            if (CommonUtils.isBitSet(options, EFS.SHALLOW)) {
                Files.createDirectory(path);
            } else {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to create directories", e));
        }

        info.setExists(true);
        info.setDirectory(true);

        NIOMonitor.notifyResourceChange(this, NIOListener.Action.CREATE);

        return this;
    }

    @Override
    public IFileStore getChild(String name) {
        final int index = name.indexOf('\0');
        if (index > 0) {
            // Eclipse sometimes adds \0 for some reason
            name = name.substring(0, index);
        }

        return getFileSystem().getStore(NIOFileSystem.toURI(project, root, path.resolve(name)));
    }

    @Override
    public String getName() {
        final int names = path.getNameCount();

        if (names == 0) {
            return "/";
        } else {
            return URLDecoder.decode(path.getName(names - 1).toString(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public IFileStore getParent() {
        final Path parent = path.getParent();

        if (parent != null) {
            return getFileSystem().getStore(NIOFileSystem.toURI(project, root, parent));
        } else {
            return null;
        }
    }

    @Override
    public URI toURI() {
        return NIOFileSystem.toURI(project, root, path);
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    @NotNull
    public DBFVirtualFileSystemRoot getRoot() {
        return root;
    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    private static Path getPath(@NotNull IFileStore fileStore) throws CoreException {
        if (fileStore instanceof NIOFileStore) {
            return ((NIOFileStore) fileStore).path;
        } else if (fileStore instanceof LocalFile) {
            return new File(fileStore.toURI()).toPath();
        } else {
            throw new CoreException(Status.error("Unsupported file store: " + fileStore));
        }
    }

    @NotNull
    private static CopyOption[] getCopyOptions(int options) {
        final Set<CopyOption> result = new HashSet<>();

        if ((options & EFS.OVERWRITE) != 0) {
            result.add(StandardCopyOption.REPLACE_EXISTING);
        }

        return result.toArray(CopyOption[]::new);
    }

    private static void checkOptions(int options, int allowed) {
        final int remaining = options & ~allowed;

        if (remaining != 0) {
            log.debug(String.format("Unsupported copy options: %#08x", remaining));
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean isValidPath(@NotNull Path path) {
        try {
            path.toUri();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
