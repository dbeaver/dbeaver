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
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.jkiss.dbeaver.model.fs.DBFFileStoreProvider;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

/**
 * NIOResource
 */
public abstract class EFSNIOResource extends PlatformObject implements DBFFileStoreProvider, IResource, IResourceProxy {
    public static final QualifiedName NIO_RESOURCE_PROPERTY_NAME = new QualifiedName("org.jkiss.dbeaver.resources", "nioPath"); //$NON-NLS-1$ //$NON-NLS-2$

    private final EFSNIOFileSystemRoot root;
    private final Path nioPath;

    protected EFSNIOResource(EFSNIOFileSystemRoot root, Path nioPath) {
        this.root = root;
        this.nioPath = nioPath;
    }

    public EFSNIOFileSystemRoot getRoot() {
        return root;
    }

    public Path getNioPath() {
        return nioPath;
    }

    public boolean contains(ISchedulingRule rule) {
        return false;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        return false;
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        accept(visitor, DEPTH_INFINITE, 0);
    }

    public void accept(final IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
        accept(new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                return visitor.visit((EFSNIOResource) resource);
            }
        }, depth, 0);
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        accept(visitor, DEPTH_INFINITE, 0);
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        accept(visitor, depth, 0);
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        visit(visitor, depth);
    }

    protected boolean visit(IResourceVisitor visitor, int depth) throws CoreException {
        return visitor.visit(this);
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        try {
            File targetFile = destination.toFile();
            if (targetFile != null) {
                try (InputStream is = Files.newInputStream(nioPath)) {
                    Files.copy(
                        is,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                throw new IOException("Can't find file for location " + destination);
            }
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        try {
            if (destination instanceof IFile) {
                ((IFile) destination).setContents(
                    Files.newInputStream(nioPath),
                    updateFlags,
                    monitor);
            } else {
                throw new IOException("Can't copy to " + destination);
            }
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IMarker createMarker(String type) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IMarker createMarker(String type, Map<String, ? extends Object> attributes) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IResourceProxy createProxy() {
        return this;
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        delete(force ? IResource.FORCE : IResource.NONE, monitor);
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        try {
            Files.delete(getNioPath());

            EFSNIOMonitor.notifyResourceChange(this, EFSNIOListener.Action.DELETE);
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    @Override
    public int hashCode() {
        // Same as o.e.c.i.Resource.hashCode()
        return nioPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // Similar to o.e.c.i.Resource.equals(Object)
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EFSNIOResource)) {
            return false;
        }
        EFSNIOResource other = (EFSNIOResource) obj;
        return nioPath.equals(other.nioPath);
    }

    public boolean exists() {
        return Files.exists(nioPath);
    }

    public IMarker findMarker(long id) throws CoreException {
        return null;
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return new IMarker[0];
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        return IMarker.SEVERITY_INFO;
    }

    public String getFileExtension() {
        Path fileName = getNioPath().getFileName();
        if (fileName == null) {
            return null;
        }
        String fileNameStr = fileName.toString();
        int divPos = fileNameStr.lastIndexOf('.');
        return divPos == -1 ? null : fileNameStr.substring(divPos + 1);
    }

    public IPath getFullPath() {
        return new org.eclipse.core.runtime.Path(nioPath.toUri().getSchemeSpecificPart());
    }

    public long getLocalTimeStamp() {
        try {
            return Files.getLastModifiedTime(nioPath).toMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    public IPath getLocation() {
        return new org.eclipse.core.runtime.Path(getLocationURI().toString());
    }

    public URI getLocationURI() {
        return URI.create(
            DBNNode.NodePathType.dbvfs.getPrefix() + root.getProject().getName() + "/" + root.getPrefix() +
                "/?" + URLEncoder.encode(nioPath.toUri().getPath(), StandardCharsets.UTF_8));
    }

    public IMarker getMarker(long id) {
        return null;
    }

    public long getModificationStamp() {
        return getLocalTimeStamp();
    }

    public String getName() {
        Path fileName = nioPath.getFileName();
        return fileName == null ? nioPath.toUri().getPath() : fileName.toString();
    }

    public IPathVariableManager getPathVariableManager() {
        return getProject().getPathVariableManager();
    }

    public IContainer getParent() {
        Path parentPath = nioPath.getParent();
        if (parentPath == null) {
            return getProject();
        }
        if (CommonUtils.equalObjects(nioPath.toUri(), parentPath.toUri())) {
            //
            return getProject();
        }
        return new EFSNIOFolder(root, parentPath);
    }

    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        return Collections.emptyMap();
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        return null;
    }

    public IProject getProject() {
        return root.getProject();
    }

    public IPath getProjectRelativePath() {
        return getLocation().makeRelativeTo(getProject().getLocation());
    }

    public IPath getRawLocation() {
        return getLocation();
    }

    public URI getRawLocationURI() {
        return getLocationURI();
    }

    public ResourceAttributes getResourceAttributes() {
        return null;
    }

    public Map<QualifiedName, Object> getSessionProperties() {
        return Collections.singletonMap(NIO_RESOURCE_PROPERTY_NAME, nioPath);
    }

    public Object getSessionProperty(QualifiedName key) {
        if (NIO_RESOURCE_PROPERTY_NAME.equals(key)) {
            return nioPath;
        }

        return null;
    }

    public IWorkspace getWorkspace() {
        return getProject().getWorkspace();
    }

    public boolean isAccessible() {
        return true;
    }

    public boolean isDerived() {
        return false;
    }

    public boolean isDerived(int options) {
        return false;
    }

    public boolean isHidden() {
        return false;
    }

    public boolean isHidden(int options) {
        return false;
    }

    public boolean isLinked() {
        return false;
    }

    public boolean isVirtual() {
        return false;
    }

    public boolean isLinked(int options) {
        return false;
    }

    @Deprecated
    public boolean isLocal(int depth) {
        return true;
    }

    public boolean isPhantom() {
        return false;
    }

    @Deprecated
    public boolean isReadOnly() {
        return true;
    }

    public boolean isSynchronized(int depth) {
        return true;
    }

    public boolean isTeamPrivateMember() {
        return false;
    }

    public boolean isTeamPrivateMember(int options) {
        return false;
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        move(destination, IResource.FORCE, monitor);
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        try {
            Path targetPath = destination.toPath();
            Files.move(nioPath, targetPath);

            EFSNIOMonitor.notifyResourceChange(new EFSNIOFile(root, targetPath), EFSNIOListener.Action.CREATE);
            EFSNIOMonitor.notifyResourceChange(this, EFSNIOListener.Action.DELETE);
        } catch (Exception e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        // Do nothing
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    @Deprecated
    public void setDerived(boolean isDerived) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setHidden(boolean isHidden) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    @Deprecated
    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    @Deprecated
    public void setReadOnly(boolean readOnly) {
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IPath requestFullPath() {
        return getFullPath();
    }

    public IResource requestResource() {
        return this;
    }

    @Override
    public String toString() {
        return getLocationURI().toString();
    }

    public static class FeatureNotSupportedException extends CoreException {
        private static final long serialVersionUID = 1L;

        public FeatureNotSupportedException() {
            super(Status.info("Feature not supported"));
        }
    }

    public static String getPathFileNameOrHost(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            // Use host name (the first part)
            String virtName = null;
            URI uri = path.toUri();
            if (uri != null) {
                String uriPath = uri.getHost();
                if (!CommonUtils.isEmpty(uriPath)) {
                    virtName = uriPath;
                }
            }
            if (virtName == null) {
                virtName = path.toString();
            }
            return CommonUtils.removeTrailingSlash(
                CommonUtils.removeLeadingSlash(virtName));
        }
        return fileName.toString();
    }

    @Override
    public EFSNIOFileStore getFileStore() {
        return new EFSNIOFileStore(getLocationURI(), getNioPath());
    }
}
