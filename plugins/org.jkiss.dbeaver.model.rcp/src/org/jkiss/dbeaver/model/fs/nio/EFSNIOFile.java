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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * NIOFile
 */
public final class EFSNIOFile extends EFSNIOResource implements IFile {

    public EFSNIOFile(EFSNIOFileSystemRoot root, Path backendFile) {
        super(root, backendFile);
    }

    public int getType() {
        return FILE;
    }

    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        create(source, (force ? IResource.FORCE : IResource.NONE), monitor);
    }

    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        try {
            Files.copy(source, getNioPath(), StandardCopyOption.REPLACE_EXISTING);

            EFSNIOMonitor.notifyResourceChange(this, EFSNIOListener.Action.CREATE);
        } catch (Exception e) {
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
        int updateFlags = force ? IResource.FORCE : IResource.NONE;
        updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
        delete(updateFlags, monitor);
    }

    public String getCharset() throws CoreException {
        return "UTF-8"; //$NON-NLS-1$
    }

    public String getCharset(boolean checkImplicit) throws CoreException {
        return getCharset();
    }

    public String getCharsetFor(Reader reader) throws CoreException {
        return getCharset();
    }

    public IContentDescription getContentDescription() throws CoreException {
        // TODO
        return null;
    }

    public InputStream getContents() throws CoreException {
        try {
            return Files.newInputStream(getNioPath());
        } catch (Exception ex) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(ex)); //$NON-NLS-1$
        }
    }

    public InputStream getContents(boolean force) throws CoreException {
        return getContents();
    }

    @Deprecated
    public int getEncoding() throws CoreException {
        throw new UnsupportedOperationException();
    }

    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        return new IFileState[0];
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    @Deprecated
    public void setCharset(String newCharset) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        // funnel all operations to central method
        int updateFlags = force ? IResource.FORCE : IResource.NONE;
        updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
        setContents(source, updateFlags, monitor);
    }

    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        // funnel all operations to central method
        int updateFlags = force ? IResource.FORCE : IResource.NONE;
        updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
        setContents(source, updateFlags, monitor);
    }

    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        setContents(source.getContents(), updateFlags, monitor);
    }

    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        try {
            Files.copy(source, getNioPath(), StandardCopyOption.REPLACE_EXISTING);

            EFSNIOMonitor.notifyResourceChange(this, EFSNIOListener.Action.CHANGE);
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

}
