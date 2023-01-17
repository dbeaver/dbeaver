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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * NIOContainer
 */
public abstract class NIOContainer extends NIOResource implements IContainer {

    protected NIOContainer(NIOFileSystemRoot root, Path path) {
        super(root, path);
    }

    public int getType() {
        return 0;
    }

    public boolean exists(IPath path) {
        IResource member = findMember(path);
        return member.exists();
    }

    public IResource findMember(IPath path) {
        if (path.isEmpty()) {
            return this;
        }

        Path member = getNioPath().resolve(path.toString());
        {
            LinkedList<Path> backendResources = new LinkedList<>();
            for (Path it = member; it != getNioPath(); it = it.getParent()) {
                backendResources.addFirst(it);
            }

            NIOContainer container = this;
            for (Path it : backendResources) {
                if (!Files.isDirectory(it)) {
                    return new NIOFile(getRoot(), it);
                }

                container = new NIOFolder(getRoot(), it);
            }
        }

        return null;
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        return findMember(path);
    }

    public IResource findMember(String path, boolean includePhantoms) {
        return findMember(path);
    }

    public IResource findMember(String path) {
        return findMember(new org.eclipse.core.runtime.Path(path));
    }

    public String getDefaultCharset() throws CoreException {
        return "UTF-8"; //$NON-NLS-1$
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        return getDefaultCharset();
    }

    public IFile getFile(IPath path) {
        Path childBackendFile = getNioPath().resolve(path.toString());
        return new NIOFile(getRoot(), childBackendFile);
    }

    public IFolder getFolder(IPath path) {
        Path childBackendFolder = getNioPath().resolve(path.toString());
        return new NIOFolder(getRoot(), childBackendFolder);
    }

    public IResource[] members() throws CoreException {
        List<IResource> members = new ArrayList<IResource>();

        try {
            Files.list(getNioPath()).forEach(member -> {
                if (Files.isDirectory(member)) {
                    members.add(new NIOFolder(getRoot(), member));
                } else {
                    members.add(new NIOFile(getRoot(), member));
                }
            });
        } catch (IOException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }

        return members.toArray(new IResource[0]);
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        return members();
    }

    public IResource[] members(int memberFlags) throws CoreException {
        return members();
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        return new IFile[0];
    }

    @Deprecated
    public void setDefaultCharset(String charset) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IResourceFilterDescription createFilter(int type, FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)
        throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IResourceFilterDescription[] getFilters() throws CoreException {
        return new IResourceFilterDescription[0];
    }

    // protected NIOContainer getParent(IPath path)
    // {
    // int segmentCount = path.segmentCount();
    // if (segmentCount > 1)
    // {
    // return new NIOFolder(this, path.segment(0)).getParent(path.removeFirstSegments(1));
    // }
    //
    // if (segmentCount == 1)
    // {
    // return this;
    // }
    //
    // return null;
    // }

    @Override
    protected boolean visit(IResourceVisitor visitor, int depth) throws CoreException {
        if (!super.visit(visitor, depth)) {
            return false;
        }

        if (depth < 1) {
            return false;
        }

        --depth;
        for (IResource member : members()) {
            ((NIOResource) member).visit(visitor, depth);
        }

        return true;
    }
}
