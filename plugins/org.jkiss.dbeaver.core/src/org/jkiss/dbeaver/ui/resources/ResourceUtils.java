/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ResourceUtils
 */
public class ResourceUtils {

    private static final Log log = Log.getLog(ResourceUtils.class);

    public static final String SCRIPT_FILE_EXTENSION = "sql"; //$NON-NLS-1$

    public static class ResourceInfo {
        private final IResource resource;
        private final File localFile;
        private final DBPDataSourceContainer dataSource;
        private final List<ResourceInfo> children;
        private String description;

        public ResourceInfo(IFile file, DBPDataSourceContainer dataSource) {
            this.resource = file;
            this.localFile = file.getLocation().toFile();
            this.dataSource = dataSource;
            this.children = null;
        }
        public ResourceInfo(IFolder folder) {
            this.resource = folder;
            this.localFile = folder.getLocation().toFile();
            this.dataSource = null;
            this.children = new ArrayList<>();
        }
        public ResourceInfo(File localFile, DBPDataSourceContainer dataSource) {
            this.resource = null;
            this.localFile = localFile;
            this.dataSource = dataSource;
            this.children = null;
        }

        public IResource getResource() {
            return resource;
        }

        public File getLocalFile() {
            return localFile;
        }

        public String getName() {
            return resource != null ? resource.getName() : localFile.getName();
        }

        public DBPDataSourceContainer getDataSource() {
            return dataSource;
        }

        public boolean isDirectory() {
            return resource instanceof IFolder;
        }
        public List<ResourceInfo> getChildren() {
            return children;
        }

        public String getDescription() {
            if (description == null) {
                description = getResourceDescription(resource);
            }
            return description;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static String getResourceDescription(IResource resource) {
        if (resource instanceof IFolder) {
            return "";
        } else if (resource instanceof IFile && SCRIPT_FILE_EXTENSION.equals(resource.getFileExtension())) {
            String description = SQLUtils.getScriptDescription((IFile) resource);
            if (CommonUtils.isEmptyTrimmed(description)) {
                description = "<empty>";
            }
            return description;
        } else {
            return "";
        }
    }

    public static IFolder getScriptsFolder(IProject project, boolean forceCreate) throws CoreException
    {
    	if (project == null) {
    		IStatus status = new Status(IStatus.ERROR, DBeaverCore.getCorePluginID(), "No active project to locate Script Folder");
			throw new CoreException(status);
		}
        return DBeaverCore.getInstance().getProjectRegistry().getResourceDefaultRoot(project, ScriptsHandlerImpl.class, forceCreate);
    }

    @Nullable
    public static ResourceInfo findRecentScript(IProject project, @Nullable DBPDataSourceContainer container) throws CoreException
    {
        List<ResourceInfo> scripts = new ArrayList<>();
        findScriptList(getScriptsFolder(project, false), container, scripts);
        long recentTimestamp = 0l;
        ResourceInfo recentFile = null;
        for (ResourceInfo file : scripts) {
            if (file.localFile.lastModified() > recentTimestamp) {
                recentTimestamp = file.localFile.lastModified();
                recentFile = file;
            }
        }
        return recentFile;
    }

    private static void findScriptList(IFolder folder, @Nullable DBPDataSourceContainer container, List<ResourceInfo> result)
    {
        if (folder == null) {
            return;
        }
        try {
            // Search in project scripts
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile && SCRIPT_FILE_EXTENSION.equals(resource.getFileExtension())) {
                    final DBPDataSourceContainer scriptDataSource = EditorUtils.getFileDataSource((IFile) resource);
                    if (container == null || scriptDataSource == container) {
                        result.add(new ResourceInfo((IFile) resource, scriptDataSource));
                    }
                } else if (resource instanceof IFolder) {
                    findScriptList((IFolder) resource, container, result);
                }
            }
            if (container != null) {
                // Search in external files
                for (Map.Entry<String, Map<String, Object>> fileEntry : DBeaverCore.getInstance().getExternalFileManager().getAllFiles().entrySet()) {
                    if (container.getId().equals(fileEntry.getValue().get(EditorUtils.PROP_SQL_DATA_SOURCE_ID))) {
                        File extFile = new File(fileEntry.getKey());
                        if (extFile.exists()) {
                            result.add(new ResourceInfo(extFile, container));
                        }
                    }
                }
            }
        } catch (CoreException e) {
            log.debug(e.getMessage());
        }
    }

    public static List<ResourceInfo> findScriptTree(IFolder folder, @Nullable DBPDataSourceContainer[] containers)
    {
        List<ResourceInfo> result = new ArrayList<>();
        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile && SCRIPT_FILE_EXTENSION.equals(resource.getFileExtension())) {
                    final DBPDataSourceContainer scriptDataSource = EditorUtils.getFileDataSource((IFile) resource);
                    if (containers == null || ArrayUtils.containsRef(containers, scriptDataSource)) {
                        result.add(new ResourceInfo((IFile) resource, scriptDataSource));
                    }
                } else if (resource instanceof IFolder) {
                    final ResourceInfo folderInfo = new ResourceInfo((IFolder) resource);
                    if (findChildScripts(folderInfo, containers)) {
                        result.add(folderInfo);
                    }
                }
            }
            if (!ArrayUtils.isEmpty(containers)) {
                // Search in external files
                for (Map.Entry<String, Map<String, Object>> fileEntry : DBeaverCore.getInstance().getExternalFileManager().getAllFiles().entrySet()) {
                    final Object fileContainerId = fileEntry.getValue().get(EditorUtils.PROP_SQL_DATA_SOURCE_ID);
                    if (fileContainerId != null) {
                        File extFile = new File(fileEntry.getKey());
                        if (extFile.exists()) {
                            for (DBPDataSourceContainer container : containers) {
                                if (container.getId().equals(fileContainerId)) {
                                    result.add(new ResourceInfo(extFile, container));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            log.debug(e);
        }
        return result;
    }

    private static boolean findChildScripts(ResourceInfo folder, @Nullable DBPDataSourceContainer[] containers) throws CoreException {
        boolean hasScripts = false;
        for (IResource resource : ((IFolder)folder.resource).members()) {
            if (resource instanceof IFile && SCRIPT_FILE_EXTENSION.equals(resource.getFileExtension())) {
                final DBPDataSourceContainer scriptDataSource = EditorUtils.getFileDataSource((IFile) resource);
                if (containers == null || ArrayUtils.containsRef(containers, scriptDataSource)) {
                    folder.children.add(new ResourceInfo((IFile) resource, scriptDataSource));
                    hasScripts = true;
                }
            } else if (resource instanceof IFolder) {
                final ResourceInfo folderInfo = new ResourceInfo((IFolder) resource);
                if (findChildScripts(folderInfo, containers)) {
                    folder.children.add(folderInfo);
                    hasScripts = true;
                }
            }

        }
        return hasScripts;
    }

    public static IFile createNewScript(IProject project, @Nullable IFolder folder, @Nullable DBPDataSourceContainer dataSourceContainer) throws CoreException
    {
        final IProgressMonitor progressMonitor = new NullProgressMonitor();

        // Get folder
        final IFolder scriptsRootFolder = ResourceUtils.getScriptsFolder(project, true);
        IFolder scriptsFolder = folder;
        if (scriptsFolder == null) {
            scriptsFolder = scriptsRootFolder;
        }
        if (!scriptsFolder.exists()) {
            scriptsFolder.create(true, true, new NullProgressMonitor());
        }

        if (CommonUtils.equalObjects(scriptsRootFolder, scriptsFolder)) {
            // We are in the root folder
            if (dataSourceContainer != null) {
                if (dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS)) {
                    // Create script folders according to connection folders
                    DBPDataSourceFolder conFolder = dataSourceContainer.getFolder();
                    if (conFolder != null) {
                        List<DBPDataSourceFolder> conFolders = new ArrayList<>();
                        for (DBPDataSourceFolder f = conFolder; f != null; f = f.getParent()) {
                            conFolders.add(0, f);
                        }
                        for (DBPDataSourceFolder f : conFolders) {
                            IFolder dbFolder = scriptsFolder.getFolder(CommonUtils.escapeFileName(f.getName()));
                            if (dbFolder != null) {
                                if (!dbFolder.exists()) {
                                    dbFolder.create(true, true, progressMonitor);
                                }
                                scriptsFolder = dbFolder;
                            }
                        }
                    }
                }
                if (dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS)) {
                    // Create special folder for connection
                    IFolder dbFolder = scriptsFolder.getFolder(CommonUtils.escapeFileName(dataSourceContainer.getName()));
                    if (dbFolder != null) {
                        if (!dbFolder.exists()) {
                            dbFolder.create(true, true, progressMonitor);
                        }
                        scriptsFolder = dbFolder;
                    }
                }
            }
        }

        // Make new script file
        IFile tempFile = ContentUtils.getUniqueFile(scriptsFolder, "Script", SCRIPT_FILE_EXTENSION);
        tempFile.create(new ByteArrayInputStream(new byte[]{}), true, progressMonitor);
        //tempFile.setCharset(GeneralUtils.getDefaultFileEncoding(), progressMonitor);

        // Save ds container reference
        if (dataSourceContainer != null) {
            EditorUtils.setFileDataSource(tempFile, dataSourceContainer);
        }

        return tempFile;
    }

    public static void checkFolderExists(IFolder folder)
            throws DBException
    {
        checkFolderExists(folder, new VoidProgressMonitor());
    }

    public static void checkFolderExists(IFolder folder, DBRProgressMonitor monitor)
            throws DBException
    {
        if (!folder.exists()) {
            try {
                folder.create(true, true, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't create folder '" + folder.getFullPath() + "'", e);
            }
        }
    }

}
