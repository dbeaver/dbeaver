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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * NIOContainer
 */
public abstract class EFSNIOContainer extends EFSNIOResource implements IContainer {

    protected EFSNIOContainer(EFSNIOFileSystemRoot root, Path path) {
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

            for (Path it : backendResources) {
                if (!Files.isDirectory(it)) {
                    return new EFSNIOFile(getRoot(), it);
                }
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

    public String getDefaultCharset() {
        return "UTF-8"; //$NON-NLS-1$
    }

    public String getDefaultCharset(boolean checkImplicit) {
        return getDefaultCharset();
    }

    public IFile getFile(IPath path) {
        Path childBackendFile = getNioPath().resolve(path.toString());
        return new EFSNIOFile(getRoot(), childBackendFile);
    }

    public IFolder getFolder(IPath path) {
        Path childBackendFolder = getNioPath().resolve(path.toString());
        return new EFSNIOFolder(getRoot(), childBackendFolder);
    }

    public IResource[] members() throws CoreException {
        List<IResource> members = new ArrayList<>();

        try {
            try (Stream<Path> files = Files.list(getNioPath())) {
                files.forEach(member -> {
                    if (Files.isDirectory(member)) {
                        members.add(new EFSNIOFolder(getRoot(), member));
                    } else {
                        members.add(new EFSNIOFile(getRoot(), member));
                    }
                });
            }
        } catch (Exception e) {
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

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) {
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

    public IResourceFilterDescription[] getFilters() {
        return new IResourceFilterDescription[0];
    }

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
            ((EFSNIOResource) member).visit(visitor, depth);
        }

        return true;
    }
}
