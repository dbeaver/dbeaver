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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.commands.DisableSQLSyntaxParserHandler;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorVariablesResolver;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.scripts.ScriptsHandlerImpl;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContextTypeBase;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContextTypeDriver;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContextTypeProvider;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SQLEditor utils
 */
public class SQLEditorUtils {

    private static final Log log = Log.getLog(SQLEditorUtils.class);

    public static final String SCRIPT_FILE_EXTENSION = "sql"; //$NON-NLS-1$

    private static final String DISABLE_SQL_SYNTAX_PARSER_RESOURCE_PROPERTY = "disable-sql-syntax-parser";
    private static final int MIN_SQL_DESCRIPTION_LENGTH = 512;

    /**
     * A {@link IResource}'s session property to distinguish between persisted and newly created resources.
     */
    private static final QualifiedName NEW_SCRIPT_FILE = new QualifiedName(SQLEditorActivator.PLUGIN_ID, "newScriptFile");

    public static boolean isOpenSeparateConnection(DBPDataSourceContainer container) {
        SeparateConnectionBehavior behavior = SeparateConnectionBehavior.parse(
            container.getPreferenceStore().getString(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION)
        );
        switch (behavior) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case DEFAULT:
            default: 
                return !container.isForceUseSingleConnection() && !container.getDriver().isEmbedded();
        }
    }

    public static IFolder getScriptsFolder(DBPProject project, boolean forceCreate) throws CoreException
    {
    	if (project == null) {
    		IStatus status = new Status(IStatus.ERROR, SQLEditorActivator.PLUGIN_ID, "No active project to locate Script Folder");
			throw new CoreException(status);
		}
        return DBPPlatformDesktop.getInstance().getWorkspace().getResourceDefaultRoot(project, ScriptsHandlerImpl.class, forceCreate);
    }

    @Nullable
    public static ResourceInfo findRecentScript(RCPProject project, @Nullable SQLNavigatorContext context) throws CoreException
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
            if (file.resource != null) {
                long lastModified = ResourceUtils.getResourceLastModified(file.resource);
                if (lastModified > recentTimestamp) {
                    recentTimestamp = lastModified;
                    recentFile = file;
                }
            }
        }
        return recentFile;
    }

    private static void findScriptList(@NotNull RCPProject project, IFolder folder, @Nullable DBPDataSourceContainer container, @NotNull List<ResourceInfo> result) {
        if (folder == null || container == null) {
            return;
        }
        try {
            for (String path : project.findResources(Map.of(EditorUtils.PROP_CONTEXT_DEFAULT_DATASOURCE, container.getId()))) {
                final IResource resource = project.getRootResource().findMember(path);
                if (resource instanceof IFile) {
                    result.add(new ResourceInfo((IFile) resource, container));
                }
            }

            // Search in external files
            for (Map.Entry<String, Map<String, Object>> fileEntry : DBPPlatformDesktop.getInstance().getExternalFileManager().getAllFiles().entrySet()) {
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

    public static List<ResourceInfo> findScriptTree(RCPProject project, IFolder folder, @Nullable DBPDataSourceContainer container)
    {
        List<ResourceInfo> result = new ArrayList<>();
        findScriptList(project, folder, container, result);
        return result;
    }

    @NotNull
    public static List<ResourceInfo> getScriptsFromProject(@NotNull RCPProject dbpProject) throws CoreException {
        IFolder resourceDefaultRoot = DBPPlatformDesktop.getInstance().getWorkspace().getResourceDefaultRoot(dbpProject, ScriptsHandlerImpl.class, false);
        if (resourceDefaultRoot != null) {
            return getScriptsFromFolder(resourceDefaultRoot);
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull
    private static List<ResourceInfo> getScriptsFromFolder(@NotNull IFolder folder) throws CoreException {
        List<ResourceInfo> scripts = new ArrayList<>();
        for (IResource member : folder.members()) {
            if (member instanceof IFile iFile) {
                ResourceInfo resourceInfo = new ResourceInfo(iFile, EditorUtils.getFileDataSource(iFile));
                scripts.add(resourceInfo);
            }
            if (member instanceof IFolder iFolder){
                scripts.addAll(getScriptsFromFolder(iFolder));
            }
        }
        return scripts;
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

        final DBPDataSourceContainer dataSourceContainer = navigatorContext.getDataSourceContainer();
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
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        String pattern = store.getString(SQLPreferenceConstants.SCRIPT_FILE_NAME_PATTERN);

        String filename = GeneralUtils.replaceVariables(pattern, new SQLEditorVariablesResolver(
                dataSourceContainer,
                dataSourceContainer == null ? null : dataSourceContainer.getConnectionConfiguration(),
                navigatorContext.getExecutionContext(),
                null,
                null, null));


        // Make new script file
        IFile tempFile = ResourceUtils.getUniqueFile(scriptsFolder,
                CommonUtils.isEmpty(filename) ? "Script" : CommonUtils.escapeFileName(filename),
                SCRIPT_FILE_EXTENSION);
        tempFile.create(new ByteArrayInputStream(getResolvedNewScriptTemplate(dataSourceContainer).getBytes(StandardCharsets.UTF_8)), true, progressMonitor);
        tempFile.setSessionProperty(NEW_SCRIPT_FILE, true);

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
            String description = getScriptDescription((IFile) resource);
            if (CommonUtils.isEmptyTrimmed(description)) {
                description = "<empty>";
            }
            return description;
        }
        return "";
    }

    public static boolean isNewScriptFile(@NotNull IResource resource) {
        try {
            return Boolean.TRUE.equals(resource.getSessionProperty(NEW_SCRIPT_FILE));
        } catch (CoreException ignored) {
            return false;
        }
    }

    @NotNull
    public static String getNewScriptTemplate(@NotNull DBPPreferenceStore store) {
        return store.getString(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE);
    }

    @NotNull
    public static String getResolvedNewScriptTemplate(@Nullable DBPDataSourceContainer container) {
        if (container != null) {
            final DBPPreferenceStore store = container.getPreferenceStore();
            if (store.getBoolean(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE_ENABLED)) {
                return GeneralUtils.replaceVariables(
                    getNewScriptTemplate(store),
                    new SQLNewScriptTemplateVariablesResolver(container, container.getConnectionConfiguration())
                );
            }
        }
        return "";
    }

    public static IContentType getSQLContentType() {
        return Platform.getContentTypeManager().getContentType("org.jkiss.dbeaver.sql");
    }

    @Nullable
    public static String getScriptDescription(@NotNull IFile sqlScript)
    {
        try {
            //log.debug("Read script '" + sqlScript.getName() + "' description");
            StringBuilder sql = new StringBuilder();
            try (BufferedReader is = new BufferedReader(new InputStreamReader(sqlScript.getContents()))) {
                for (;;) {
                    String line = is.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.startsWith(SQLConstants.SL_COMMENT) ||
                        line.startsWith("Rem") ||
                        line.startsWith("rem") ||
                        line.startsWith("REM")
                        )
                    {
                        continue;
                    }
                    sql.append(line).append('\n');
                    if (sql.length() > MIN_SQL_DESCRIPTION_LENGTH) {
                        break;
                    }
                }
            }
            return SQLUtils.getScriptDescripion(sql.toString());
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

    public static class ResourceInfo {
        private final IResource resource;
        @Deprecated
        private final File localFile;
        private final DBPDataSourceContainer dataSource;
        private final List<ResourceInfo> children;
        private String description;

        ResourceInfo(IFile file, DBPDataSourceContainer dataSource) {
            this.resource = file;
            IPath location = file.getLocation();
            this.localFile = location == null ? null : location.toFile();
            this.dataSource = dataSource;
            this.children = null;
        }
        public ResourceInfo(IFolder folder) {
            this.resource = folder;
            IPath location = folder.getLocation();
            this.localFile = location == null ? null : location.toFile();
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
    
    private abstract static class EditorFileInfo {

        public abstract void setPropertyValue(@NotNull String propertyName, @NotNull Object value);

        @Nullable
        public abstract Object getPropertyValue(@NotNull String propertyName);
        
        @Nullable
        public static EditorFileInfo getFromEditor(@Nullable IEditorInput input) {
            if (input != null) {
                IFile projectFile = EditorUtils.getFileFromInput(input);
                if (projectFile != null) {
                    return new EditorProjectFileInfo(projectFile);
                } else {
                    File platformFile = EditorUtils.getLocalFileFromInput(input);
                    if (platformFile != null) {
                        return new EditorPlatformFileInfo(platformFile);
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }   
    }
    
    private static class EditorProjectFileInfo extends EditorFileInfo {
        private final IFile projectFile;
        
        public EditorProjectFileInfo(@NotNull IFile projectFile) {
            this.projectFile = projectFile;
        }

        @Nullable
        @Override
        public Object getPropertyValue(@NotNull String propertyName) {
            RCPProject project = DBPPlatformDesktop.getInstance().getWorkspace().getProject(projectFile.getProject());
            if (project == null) {
                log.debug("Project '" + projectFile.getProject() + "' not recognized (property read)");
                return null;
            }
            return EditorUtils.getResourceProperty(project, projectFile, propertyName);
        }
        
        @Override
        public void setPropertyValue(@NotNull String propertyName, @NotNull Object value) {
            RCPProject project = DBPPlatformDesktop.getInstance().getWorkspace().getProject(projectFile.getProject());
            if (project == null) {
                log.debug("Project '" + projectFile.getProject() + "' not recognized (property write)");
                return;
            }
            EditorUtils.setResourceProperty(project, projectFile, propertyName, value);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            EditorProjectFileInfo other = obj instanceof EditorProjectFileInfo ? (EditorProjectFileInfo) obj : null;
            return other != null && projectFile != null && projectFile.equals(other.projectFile);
        }
    }
    
    private static class EditorPlatformFileInfo extends EditorFileInfo {
        private final File platformFile;
        
        public EditorPlatformFileInfo(@Nullable File platformFile) {
            this.platformFile = platformFile;
        }

        @Nullable
        @Override
        public Object getPropertyValue(@NotNull String propertyName) {
            final DBPExternalFileManager efManager = DBPPlatformDesktop.getInstance().getExternalFileManager();
            return efManager.getFileProperty(platformFile, propertyName);
        }
    
        @Override
        public void setPropertyValue(@NotNull String propertyName, @NotNull Object value) {
            final DBPExternalFileManager efManager = DBPPlatformDesktop.getInstance().getExternalFileManager();
            efManager.setFileProperty(platformFile, propertyName, value);
        }
        
        @Override
        public boolean equals(@Nullable Object obj) {
            EditorPlatformFileInfo other = obj instanceof EditorPlatformFileInfo ? (EditorPlatformFileInfo) obj : null;
            return other != null && platformFile != null && platformFile.equals(other.platformFile);
        }
    }

    /**
     * Checks whether Disable SQL syntax parser should be applied for the given editor
     */
    public static boolean isSQLSyntaxParserApplied(@Nullable IEditorInput input) {
        return isSQLSyntaxParserEnabled(input) && !SQLEditorBase.isBigScript(input);
    }

    /**
     * Checks whether Disable SQL syntax parser property is set
     */
    public static boolean isSQLSyntaxParserEnabled(@Nullable IEditorInput input) {
        EditorFileInfo file = EditorFileInfo.getFromEditor(input);
        if (file != null) {
            try {
                return !CommonUtils.getBoolean(file.getPropertyValue(DISABLE_SQL_SYNTAX_PARSER_RESOURCE_PROPERTY), false);
            } catch (Throwable e) {
                log.debug(e);
            }
        }
        return true;
    }

    /**
     * Sets value to Disable SQL syntax parser property
     */
    public static void setSQLSyntaxParserEnabled(@Nullable IEditorInput input, boolean value) {
        EditorFileInfo file = EditorFileInfo.getFromEditor(input);
        if (file != null) {
            try {
                file.setPropertyValue(DISABLE_SQL_SYNTAX_PARSER_RESOURCE_PROPERTY, Boolean.toString(!value));
                notifyAssociatedServices(file, value);
            } catch (Throwable e) {
                log.debug(e);
            }
        }
    }
    
    private static void notifyAssociatedServices(@NotNull EditorFileInfo file, boolean newServicesEnabled) {
        Set<DBPPreferenceStore> affectedPrefs = new HashSet<>();
        List<SQLEditor> affectedEditors = new LinkedList<>();
        
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorRef : page.getEditorReferences()) {
                    IEditorPart editor = editorRef.getEditor(false);
                    if (editor instanceof SQLEditorBase sqlEditor) {
                        EditorFileInfo editorFile = EditorFileInfo.getFromEditor(editor.getEditorInput());
                        if (editorFile != null && editorFile.equals(file)) {
                            affectedPrefs.add(sqlEditor.getActivePreferenceStore());
                            if (editor instanceof SQLEditor) {
                                affectedEditors.add((SQLEditor) editor);
                            }
                        }
                    }
                }
            }
        }
        
        for (DBPPreferenceStore prefs : affectedPrefs) {
            notifyPrefs(prefs, newServicesEnabled);
        }
        for (SQLEditor sqlEditor : affectedEditors) {
            sqlEditor.refreshEditorIconAndTitle();
            sqlEditor.refreshAdvancedServices();
        }

        PlatformUI.getWorkbench().getService(ICommandService.class).refreshElements(DisableSQLSyntaxParserHandler.COMMAND_ID, null);
    }
    
    private static void notifyPrefs(@NotNull DBPPreferenceStore prefStore, boolean newServicesEnabled) {
        final boolean foldingEnabled = prefStore.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
        final boolean autoActivationEnabled = prefStore.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        final boolean markWordUnderCursorEnabled = prefStore.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR);
        final boolean markWordForSelectionEnabled = prefStore.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION);
        final boolean oldServicesEnabled = !newServicesEnabled;
        
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.FOLDING_ENABLED,
            oldServicesEnabled && foldingEnabled,
            newServicesEnabled && foldingEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION,
            oldServicesEnabled && autoActivationEnabled,
            newServicesEnabled && autoActivationEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR,
            oldServicesEnabled && markWordUnderCursorEnabled,
            newServicesEnabled && markWordUnderCursorEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION,
            oldServicesEnabled && markWordForSelectionEnabled,
            newServicesEnabled && markWordForSelectionEnabled
        );
    }
    
    
    /**
     * Checks whether template's context is suitable for the editor context
     */
    public static boolean isTemplateContextFitsEditorContext(@NotNull String templateContextTypeId, @NotNull SQLEditorBase editor) {
        boolean result = false;
        if (editor instanceof SQLEditor) {
            DBPDataSourceContainer dsContainer = ((SQLEditor) editor).getDataSourceContainer();
            if (dsContainer != null) {
                DBPDriver driver = dsContainer.getDriver();
                String driverContextTypeId = SQLContextTypeDriver.getTypeId(driver);
                String providerContextTypeId = SQLContextTypeProvider.getTypeId(driver.getProviderId());
                result = isTemplateContextFitsEditorContext(templateContextTypeId, driverContextTypeId, providerContextTypeId);
                if (!result) {
                    for (Pair<String, String> replInfo : driver.getDriverReplacementsInfo()) {
                        driverContextTypeId = SQLContextTypeDriver.getTypeId(replInfo.getFirst(), replInfo.getSecond());
                        result = isTemplateContextFitsEditorContext(templateContextTypeId, driverContextTypeId, providerContextTypeId);
                        if (result) {
                            break;
                        }
                    }
                    
                }
            }
        }
        return result;
    }
    
    /**
     * Checks whether template's context is suitable for the editor context
     */
    private static boolean isTemplateContextFitsEditorContext(
        @NotNull String templateContextTypeId,
        @Nullable String driverContextTypeId,
        @Nullable String providerContextTypeId
    ) {
        return templateContextTypeId.equalsIgnoreCase(SQLContextTypeBase.ID_SQL) ||
            templateContextTypeId.equalsIgnoreCase(driverContextTypeId) ||
            templateContextTypeId.equalsIgnoreCase(providerContextTypeId);
    }

}
