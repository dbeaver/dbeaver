/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NIOFileStore
 */
public class EFSNIOFileStore extends FileStore {

    private final URI dbvfsURI;
    private final Path path;

    public EFSNIOFileStore(URI dbvfsURI, Path path) {
        this.dbvfsURI = dbvfsURI;
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
        return new String[0];
    }

    @Override
    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
        return new EFSNIOFileInfo(path);
    }

    @Override
    public IFileStore getChild(String name) {
        return null;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public IFileStore getParent() {
        return null;
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        try {
            return Files.newInputStream(path);
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    @Override
    public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        try {
            return Files.newOutputStream(path);
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    @Override
    public URI toURI() {
        return dbvfsURI;
    }

}
