/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NIOFolder
 */
public final class NIOFolder extends NIOContainer implements IFolder {

    public NIOFolder(NIOFileSystemRoot root, Path backendFolder) {
        super(root, backendFolder);
    }

    @Override
    public int getType() {
        return FOLDER;
    }

    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        create(force ? IResource.FORCE : IResource.NONE, local, monitor);
    }

    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        try {
            Files.createDirectory(getNioPath());
            NIOMonitor.notifyResourceChange(this, NIOListener.Action.CREATE);
        } catch (IOException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        try {
            Files.delete(getNioPath());
            NIOMonitor.notifyResourceChange(this, NIOListener.Action.DELETE);
        } catch (IOException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public IFile getFile(String name) {
        return getFile(new org.eclipse.core.runtime.Path(name));
    }

    public IFolder getFolder(String name) {
        return getFolder(new org.eclipse.core.runtime.Path(name));
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }
}
