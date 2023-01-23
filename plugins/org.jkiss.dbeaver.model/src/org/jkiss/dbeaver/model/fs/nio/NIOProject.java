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
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NIOProject
 */
public final class NIOProject extends NIOContainer implements IProject {
    private final IProjectDescription description;

    public NIOProject(NIOFileSystemRoot root, Path path) {
        super(root, path);
        this.description = new Description();
    }

    @Override
    public IPath getFullPath() {
        return new org.eclipse.core.runtime.Path(
            getNioPath().toAbsolutePath().toString()).makeAbsolute();
    }

    @Override
    public IProject getProject() {
        return this;
    }

    @Override
    public int getType() {
        return PROJECT;
    }

    @Override
    public String getName() {
        return description.getName();
    }

    public void build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void close(IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void create(IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
        return getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
    }

    public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
        IBuildConfiguration[] buildConfigs = description.getBuildConfigReferences(configName);
        return buildConfigs.length == 0 ? null : buildConfigs[0];
    }

    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
        return description.getBuildConfigReferences(IBuildConfiguration.DEFAULT_CONFIG_NAME);
    }

    public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing) throws CoreException {
        return getBuildConfigs();
    }

    public boolean hasBuildConfig(String configName) throws CoreException {
        return IBuildConfiguration.DEFAULT_CONFIG_NAME.equals(configName);
    }

    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public IProjectDescription getDescription() throws CoreException {
        return description;
    }

    public IFile getFile(String name) {
        return null;
    }

    public IFolder getFolder(String name) {
        return null;
    }

    public IProjectNature getNature(String natureId) throws CoreException {
        if (description.hasNature(natureId)) {
            return new IProjectNature() {
                public IProject getProject() {
                    return NIOProject.this;
                }

                public void setProject(IProject project) {
                    throw new UnsupportedOperationException();
                }

                public void configure() throws CoreException {
                    throw new UnsupportedOperationException();
                }

                public void deconfigure() throws CoreException {
                    throw new UnsupportedOperationException();
                }
            };
        }

        return null;
    }

    public IPath getWorkingLocation(String id) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public IProject[] getReferencedProjects() throws CoreException {
        return new IProject[0];
    }

    public IProject[] getReferencingProjects() {
        return new IProject[0];
    }

    public void clearCachedDynamicReferences() {
    }

    public boolean hasNature(String natureId) throws CoreException {
        return description.hasNature(natureId);
    }

    public boolean isNatureEnabled(String natureId) throws CoreException {
        return hasNature(natureId);
    }

    public boolean isOpen() {
        return true;
    }

    public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void open(IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setDescription(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    public void setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new FeatureNotSupportedException();
    }

    /**
     * Eclipse 2022-09 additions
     */
    public String getDefaultLineSeparator() {
        return GeneralUtils.getDefaultLineSeparator();
    }

    public final class Description implements IProjectDescription {
        private String name;

        private String comment;

        private IBuildConfiguration[] buildConfigurations;

        private final List<ICommand> commands = new ArrayList<ICommand>();

        private final List<String> natureIDs = new ArrayList<String>();

        public Description() {

        }

        public String getName() {
            return name;
        }

        public String getComment() {
            return comment;
        }

        public ICommand[] getBuildSpec() {
            return commands.toArray(new ICommand[commands.size()]);
        }

        public String[] getNatureIds() {
            return natureIDs.toArray(new String[natureIDs.size()]);
        }

        public boolean hasNature(String natureId) {
            return natureIDs.contains(natureId);
        }

        public IBuildConfiguration[] getBuildConfigReferences(String configName) {
            if (IBuildConfiguration.DEFAULT_CONFIG_NAME.equals(configName)) {
                return buildConfigurations;
            }

            return new IBuildConfiguration[0];
        }

        public IProject[] getDynamicReferences() {
            return new IProject[0];
        }

        @Deprecated
        public IPath getLocation() {
            return NIOProject.this.getLocation();
        }

        public URI getLocationURI() {
            return NIOProject.this.getLocationURI();
        }

        public IProject[] getReferencedProjects() {
            return new IProject[0];
        }

        public ICommand newCommand() {
            return null;
        }

        public void setActiveBuildConfig(String configName) {
        }

        public void setBuildConfigs(String[] configNames) {
        }

        public void setBuildConfigReferences(String configName, IBuildConfiguration[] references) {
        }

        public void setBuildSpec(ICommand[] buildSpec) {
        }

        public void setComment(String comment) {
        }

        @Deprecated
        public void setDynamicReferences(IProject[] projects) {
        }

        public void setLocation(IPath location) {
        }

        public void setLocationURI(URI location) {
        }

        public void setName(String projectName) {
        }

        public void setNatureIds(String[] natures) {
        }

        public void setReferencedProjects(IProject[] projects) {
        }

    }
}
