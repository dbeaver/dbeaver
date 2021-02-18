/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.scripts.ScriptsHandlerImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLEditor utils
 */
public class SQLEditorUtils {

    private static final Log log = Log.getLog(SQLEditorUtils.class);

    public static final String SCRIPT_FILE_EXTENSION = "sql"; //$NON-NLS-1$

    public static boolean isOpenSeparateConnection(DBPDataSourceContainer container) {
        return container.getPreferenceStore().getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION) &&
            !container.getDriver().isEmbedded();
    }

    public static IFolder getScriptsFolder(DBPProject project, boolean forceCreate) throws CoreException
    {
    	if (project == null) {
    		IStatus status = new Status(IStatus.ERROR, SQLEditorActivator.PLUGIN_ID, "No active project to locate Script Folder");
			throw new CoreException(status);
		}
        return DBWorkbench.getPlatform().getWorkspace().getResourceDefaultRoot(project, ScriptsHandlerImpl.class, forceCreate);
    }

    @Nullable
    public static ResourceInfo findRecentScript(DBPProject project, @Nullable SQLNavigatorContext context) throws CoreException
    {
        List<ResourceInfo> scripts = new ArrayList<>();
        findScriptList(
            project,
            getScriptsFolder(project, false),
            context == null ? null : context.getDataSourceContainer(),
            scripts);

        long recentTimestamp = 0L;
        ResourceInfo recentFile = null;
        for (ResourceInfo file : scripts) {
            if (file.localFile.lastModified() > recentTimestamp) {
                recentTimestamp = file.localFile.lastModified();
                recentFile = file;
            }
        }
        return recentFile;
    }

    private static void findScriptList(@NotNull DBPProject project, IFolder folder, @Nullable DBPDataSourceContainer container, List<ResourceInfo> result)
    {
        if (folder == null || container == null) {
            return;
        }
        try {
            for (Map.Entry<String, Map<String, Object>> rp : project.getResourceProperties().entrySet()) {
                String resName = rp.getKey();
                Map<String, Object> props = rp.getValue();
                Object dsId = props.get(EditorUtils.PROP_SQL_DATA_SOURCE_ID);
                if (CommonUtils.equalObjects(container.getId(), dsId)) {
                    IResource resource = project.getEclipseProject().findMember(resName);
                    if (resource instanceof IFile) {
                        result.add(new ResourceInfo((IFile) resource, container));
                    }
                }
            }

            // Search in external files
            for (Map.Entry<String, Map<String, Object>> fileEntry : DBWorkbench.getPlatform().getExternalFileManager().getAllFiles().entrySet()) {
                if (container.getId().equals(fileEntry.getValue().get(EditorUtils.PROP_SQL_DATA_SOURCE_ID))) {
                    File extFile = new File(fileEntry.getKey());
                    if (extFile.exists()) {
                        result.add(new ResourceInfo(extFile, container));
                    }
                }
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
        }
    }

    public static List<ResourceInfo> findScriptTree(DBPProject project, IFolder folder, @Nullable DBPDataSourceContainer container)
    {
        List<ResourceInfo> result = new ArrayList<>();
        findScriptList(project, folder, container, result);
        return result;
    }

    public static IFile createNewScript(DBPProject project, @Nullable IFolder folder, @NotNull SQLNavigatorContext navigatorContext) throws CoreException
    {
        final IProgressMonitor progressMonitor = new NullProgressMonitor();

        // Get folder
        final IFolder scriptsRootFolder = getScriptsFolder(project, true);
        IFolder scriptsFolder = folder;
        if (scriptsFolder == null) {
            scriptsFolder = scriptsRootFolder;
        }
        if (!scriptsFolder.exists()) {
            scriptsFolder.create(true, true, new NullProgressMonitor());
        }

        if (CommonUtils.equalObjects(scriptsRootFolder, scriptsFolder)) {
            // We are in the root folder
            DBPDataSourceContainer dataSourceContainer = navigatorContext.getDataSourceContainer();
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

        // Save ds container reference
        if (navigatorContext.getDataSourceContainer() != null) {
            EditorUtils.setFileDataSource(tempFile, navigatorContext);
        }

        return tempFile;
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
        }
        return "";
    }

    public static class ResourceInfo {
        private final IResource resource;
        private final File localFile;
        private final DBPDataSourceContainer dataSource;
        private final List<ResourceInfo> children;
        private String description;

        ResourceInfo(IFile file, DBPDataSourceContainer dataSource) {
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
        ResourceInfo(File localFile, DBPDataSourceContainer dataSource) {
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
}
