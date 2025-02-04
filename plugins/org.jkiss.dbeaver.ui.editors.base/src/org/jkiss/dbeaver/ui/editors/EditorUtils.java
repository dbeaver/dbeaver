/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.LocalFileStorage;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IDataSourceContainerUpdate;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerDescriptor;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerRegistry;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * EditorUtils
 */
public class EditorUtils {

    public static final String PROP_SQL_DATA_SOURCE_ID = "sql-editor-data-source-id"; //$NON-NLS-1$
    public static final String PROP_SQL_PROJECT_ID = "sql-editor-project-id"; //$NON-NLS-1$

    public static final String PROP_CONTEXT_DEFAULT_DATASOURCE = "default-datasource"; //$NON-NLS-1$
    public static final String PROP_CONTEXT_DEFAULT_CATALOG = "default-catalog"; //$NON-NLS-1$
    private static final String PROP_CONTEXT_DEFAULT_SCHEMA = "default-schema"; //$NON-NLS-1$

    private static final String PROP_SQL_DATA_SOURCE_CONTAINER = "sql-editor-data-source-container"; //$NON-NLS-1$
    private static final String PROP_EDITOR_CONTEXT = "database-editor-context"; //$NON-NLS-1$
    private static final String PROP_EXECUTION_CONTEXT = "sql-editor-execution-context"; //$NON-NLS-1$
    private static final String PROP_INPUT_FILE = "sql-editor-input-file"; //$NON-NLS-1$

    public static final String COLORS_AND_FONTS_PAGE_ID = "org.eclipse.ui.preferencePages.ColorsAndFonts"; //$NON-NLS-1$

    private static final Log log = Log.getLog(EditorUtils.class);

    /**
     * Get project by the specified editor input
     */
    @Nullable
    public static DBPProject getFileProject(@Nullable IEditorInput editorInput) {
        if (editorInput != null) {
            if (editorInput instanceof ILazyEditorInput lei) {
                return lei.getProject();
            }
            if (editorInput instanceof IDatabaseEditorInput dei) {
                DBSObject dbo = dei.getDatabaseObject();
                if (dbo != null) {
                    DBPDataSource dataSource = dbo.getDataSource();
                    if (dataSource != null) {
                        return dataSource.getContainer().getProject();
                    }
                }
            }
            IFile curFile = EditorUtils.getFileFromInput(editorInput);
            if (curFile != null) {
                return DBPPlatformDesktop.getInstance().getWorkspace().getProject(curFile.getProject());
            }
        }
        return null;
    }

    @Nullable
    public static IFile getFileFromInput(IEditorInput editorInput) {
        if (editorInput == null) {
            return null;
        } else if (editorInput instanceof IFileEditorInput fei) {
            return fei.getFile();
        } else if (editorInput instanceof IPathEditorInput pei) {
            final IPath path = pei.getPath();
            return path == null ? null : ResourceUtils.convertPathToWorkspaceFile(path);
        } else if (editorInput instanceof IInMemoryEditorInput mei) {
            IFile file = (IFile) mei.getProperty(PROP_INPUT_FILE);
            if (file != null) {
                return file;
            }
        } else if (editorInput instanceof IURIEditorInput) {
            // Most likely it is an external file
            return null;
        }
        // Try to get path input adapter (works for external files)
        final IPathEditorInput pathInput = editorInput.getAdapter(IPathEditorInput.class);
        if (pathInput != null) {
            final IPath path = pathInput.getPath();
            return path == null ? null : ResourceUtils.convertPathToWorkspaceFile(path);
        }

        try {
            Method getFileMethod = editorInput.getClass().getMethod("getFile");
            if (IFile.class.isAssignableFrom(getFileMethod.getReturnType())) {
                return (IFile) getFileMethod.invoke(editorInput);
            }
        } catch (Exception e) {
            //log.debug("Error getting file from editor input with reflection: " + e.getMessage());
            // Just ignore
        }
        return null;
    }

    public static IStorage getStorageFromInput(Object element) {
        if (element instanceof IStorageEditorInput sei) {
            try {
                return sei.getStorage();
            } catch (CoreException e) {
                log.error(e);
            }
        }
        if (element instanceof IAdaptable adaptable) {
            IStorage storage = adaptable.getAdapter(IStorage.class);
            if (storage != null) {
                return storage;
            }
        }
        if (element instanceof IEditorInput ei) {
            IFile file = getFileFromInput(ei);
            if (file != null) {
                return file;
            }
            if (element instanceof IURIEditorInput uriei) {
                URI uri = uriei.getURI();
                File localFile = new File(uri);
                if (localFile.exists()) {
                    return new LocalFileStorage(localFile, StandardCharsets.UTF_8.name());
                }
            }
        }
        return null;
    }

    public static File getLocalFileFromInput(Object element) {
        if (element instanceof IEditorInput ei) {
            IFile file = getFileFromInput(ei);
            if (file != null) {
                IPath location = file.getLocation();
                return location == null ? null : location.toFile();
            }
            if (element instanceof IURIEditorInput uriei) {
                try {
                    final File localFile = new File(uriei.getURI());
                    if (localFile.exists()) {
                        return localFile;
                    }
                } catch (Exception e) {
                    // Something is wrong with URI
                    return null;
                }
            }
        }
        return null;
    }

    public static Path getPathFromInput(Object element) {
        if (element instanceof IEditorInput ei) {
            IFile file = getFileFromInput(ei);
            if (file != null) {
                IPath location = file.getLocation();
                return location == null ? null : location.toPath();
            }
        }
        File localFile = getLocalFileFromInput(element);
        return localFile == null ? null : localFile.toPath();
    }

    //////////////////////////////////////////////////////////
    // Datasource <-> resource manipulations

    @Nullable
    public static Object getResourceProperty(@NotNull RCPProject project, @NotNull IResource resource, @NotNull String propName) {
        return project.getResourceProperty(project.getResourcePath(resource), propName);
    }

    public static void setResourceProperty(@NotNull RCPProject project, @NotNull IResource resource, @NotNull String propName, @Nullable Object value) {
        project.setResourceProperty(project.getResourcePath(resource), propName, value);
    }

    public static DatabaseEditorContext getEditorContext(IEditorInput editorInput) {
        if (editorInput instanceof IInMemoryEditorInput) {
            return (DatabaseEditorContext) ((IInMemoryEditorInput) editorInput).getProperty(PROP_EDITOR_CONTEXT);
        }
        return null;
    }

    public static DBCExecutionContext getInputExecutionContext(IEditorInput editorInput) {
        if (editorInput instanceof IInMemoryEditorInput) {
            return (DBCExecutionContext) ((IInMemoryEditorInput) editorInput).getProperty(PROP_EXECUTION_CONTEXT);
        }
        return null;
    }

    public static DBPDataSourceContainer getInputDataSource(IEditorInput editorInput) {
        if (editorInput instanceof IDatabaseEditorInput dei) {
            final DBSObject object = dei.getDatabaseObject();
            if (object != null && object.getDataSource() != null) {
                return object.getDataSource().getContainer();
            }
            if (editorInput instanceof DBPDataSourceContainerProvider containerProvider) {
                return containerProvider.getDataSourceContainer();
            }
            return null;
        } else if (editorInput instanceof IInMemoryEditorInput mei) {
            return (DBPDataSourceContainer) mei.getProperty(PROP_SQL_DATA_SOURCE_CONTAINER);
        } else {
            IFile file = getFileFromInput(editorInput);
            if (file != null) {
                return getFileDataSource(file);
            } else {
                File localFile = getLocalFileFromInput(editorInput);
                if (localFile != null) {
                    final DBPExternalFileManager efManager = DBPPlatformDesktop.getInstance().getExternalFileManager();
                    String dataSourceId = (String) efManager.getFileProperty(localFile, PROP_SQL_DATA_SOURCE_ID);
                    String projectName = (String) efManager.getFileProperty(localFile, PROP_SQL_PROJECT_ID);
                    if (CommonUtils.isEmpty(dataSourceId) || CommonUtils.isEmpty(projectName)) {
                        return null;
                    }
                    final IProject project = DBPPlatformDesktop.getInstance().getWorkspace().getEclipseWorkspace().getRoot().getProject(projectName);
                    if (project == null || !project.exists()) {
                        log.error("Can't locate project '" + projectName + "' in workspace");
                        return null;
                    }
                    DBPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(project);
                    return projectMeta == null ? null : projectMeta.getDataSourceRegistry().getDataSource(dataSourceId);

                } else {
                    return null;
                }
            }
        }
    }

    /**
     * String[2] = { defaultCatalogName, defaultSchema }
     */
    public static String[] getInputContextDefaults(DBPDataSourceContainer dataSource,  IEditorInput editorInput) {
        String defaultDatasource = null;
        String defaultCatalogName = null;
        String defaultSchema = null;
        if (editorInput instanceof IInMemoryEditorInput mei) {
            defaultDatasource = (String) mei.getProperty(PROP_CONTEXT_DEFAULT_DATASOURCE);
            defaultCatalogName = (String) mei.getProperty(PROP_CONTEXT_DEFAULT_CATALOG);
            defaultSchema= (String) mei.getProperty(PROP_CONTEXT_DEFAULT_SCHEMA);
        } else {
            IFile file = getFileFromInput(editorInput);
            if (file != null) {
                RCPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(file.getProject());
                if (projectMeta != null) {
                    defaultDatasource = (String) EditorUtils.getResourceProperty(projectMeta, file, PROP_CONTEXT_DEFAULT_DATASOURCE);
                    defaultCatalogName = (String) EditorUtils.getResourceProperty(projectMeta, file, PROP_CONTEXT_DEFAULT_CATALOG);
                    defaultSchema = (String) EditorUtils.getResourceProperty(projectMeta, file, PROP_CONTEXT_DEFAULT_SCHEMA);
                }
            } else {
                File localFile = getLocalFileFromInput(editorInput);
                if (localFile != null) {
                    final DBPExternalFileManager efManager = DBPPlatformDesktop.getInstance().getExternalFileManager();
                    defaultDatasource = (String) efManager.getFileProperty(localFile, PROP_CONTEXT_DEFAULT_DATASOURCE);
                    defaultCatalogName = (String) efManager.getFileProperty(localFile, PROP_CONTEXT_DEFAULT_CATALOG);
                    defaultSchema = (String) efManager.getFileProperty(localFile, PROP_CONTEXT_DEFAULT_SCHEMA);
                }
            }
        }
        if (!CommonUtils.isEmpty(defaultDatasource) && !defaultDatasource.equals(dataSource.getId())) {
            // Wrong datasource
            return new String[] { null, null };
        }
        return new String[] { defaultCatalogName, defaultSchema };
    }

    @Nullable
    public static DBPDataSourceContainer getFileDataSource(IFile file) {
        if (!file.exists()) {
            return null;
        }
        RCPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(file.getProject());
        if (projectMeta != null) {
            Object dataSourceId = EditorUtils.getResourceProperty(projectMeta, file, PROP_CONTEXT_DEFAULT_DATASOURCE);
            if (dataSourceId != null) {
                DBPDataSourceContainer dataSource = projectMeta.getDataSourceRegistry().getDataSource(dataSourceId.toString());
                if (dataSource == null) {
                    log.debug("Datasource " + dataSourceId + " not found in project " + projectMeta.getName() + " (" + file.getFullPath().toString() + ")");
                }
                return dataSource;
            } else {
                // Try to extract from embedded comment
                return null;
            }
        }
        return null;
    }

    public static void setInputDataSource(
        @NotNull IEditorInput editorInput,
        @NotNull DatabaseEditorContext context)
    {
        if (editorInput instanceof IInMemoryEditorInput) {
            ((IInMemoryEditorInput) editorInput).setProperty(PROP_EDITOR_CONTEXT, context);
            DBCExecutionContext executionContext = context.getExecutionContext();
            if (executionContext != null) {
                ((IInMemoryEditorInput) editorInput).setProperty(PROP_EXECUTION_CONTEXT, executionContext);
            }
            DBPDataSourceContainer dataSourceContainer = context.getDataSourceContainer();
            if (dataSourceContainer != null) {
                ((IInMemoryEditorInput) editorInput).setProperty(PROP_SQL_DATA_SOURCE_CONTAINER, dataSourceContainer);
            }
            if (!isDefaultContextSettings(context)) {
                if (dataSourceContainer != null) {
                    ((IInMemoryEditorInput) editorInput).setProperty(PROP_CONTEXT_DEFAULT_DATASOURCE, dataSourceContainer.getId());
                }
                String catalogName = getDefaultCatalogName(context);
                if (catalogName != null) ((IInMemoryEditorInput) editorInput).setProperty(PROP_CONTEXT_DEFAULT_CATALOG, getDefaultCatalogName(context));
                String schemaName = getDefaultSchemaName(context);
                if (schemaName != null) {
                    ((IInMemoryEditorInput) editorInput).setProperty(PROP_CONTEXT_DEFAULT_SCHEMA, schemaName);
                }
            }
            return;
        }
        IFile file = getFileFromInput(editorInput);
        if (file != null) {
            setFileDataSource(file, context);
        } else {
            File localFile = getLocalFileFromInput(editorInput);
            if (localFile != null) {
                setFileDataSource(localFile, context);
            } else {
                log.error("Can't set datasource for input " + editorInput);
            }
        }
    }

    /**
     * Associated inout file with editor input.
     * Can be used with IInMemoryEditorInput only.
     */
    public static void setInputInputFile(
        @NotNull IEditorInput editorInput,
        @NotNull IFile file
    ) {
        if (editorInput instanceof IInMemoryEditorInput) {
            ((IInMemoryEditorInput) editorInput).setProperty(PROP_INPUT_FILE, file);
        }
    }

    public static void setFileDataSource(@NotNull File localFile, @NotNull DatabaseEditorContext context) {
        final DBPExternalFileManager efManager = DBPPlatformDesktop.getInstance().getExternalFileManager();
        DBPDataSourceContainer dataSourceContainer = context.getDataSourceContainer();
        efManager.setFileProperty(
            localFile,
            PROP_SQL_PROJECT_ID,
            dataSourceContainer == null ? null : dataSourceContainer.getRegistry().getProject().getName());
        String dataSourceId = dataSourceContainer == null ? null : dataSourceContainer.getId();
        efManager.setFileProperty(
            localFile,
            PROP_SQL_DATA_SOURCE_ID,
            dataSourceId);
        if (!isDefaultContextSettings(context)) {
            efManager.setFileProperty(localFile, PROP_CONTEXT_DEFAULT_DATASOURCE, dataSourceId);
            String catalogName = getDefaultCatalogName(context);
            if (catalogName != null) efManager.setFileProperty(localFile, PROP_CONTEXT_DEFAULT_CATALOG, getDefaultCatalogName(context));
            String schemaName = getDefaultSchemaName(context);
            if (catalogName != null || schemaName != null) efManager.setFileProperty(localFile, PROP_CONTEXT_DEFAULT_SCHEMA, getDefaultCatalogName(context));
        }
    }

    public static void setFileDataSource(@NotNull IFile file, @NotNull DatabaseEditorContext context) {
        RCPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(file.getProject());
        if (projectMeta == null) {
            return;
        }
        DBPDataSourceContainer dataSourceContainer = context.getDataSourceContainer();
        String dataSourceId = dataSourceContainer == null ? null : dataSourceContainer.getId();

        String resourcePath = projectMeta.getResourcePath(file);
        projectMeta.setResourceProperty(resourcePath, PROP_CONTEXT_DEFAULT_DATASOURCE, dataSourceId);
        projectMeta.setResourceProperty(resourcePath, PROP_CONTEXT_DEFAULT_CATALOG, null);
        projectMeta.setResourceProperty(resourcePath, PROP_CONTEXT_DEFAULT_SCHEMA, null);
        if (!isDefaultContextSettings(context)) {
            String defaultCatalogName = getDefaultCatalogName(context);
            if (!CommonUtils.isEmpty(defaultCatalogName)) {
                projectMeta.setResourceProperty(resourcePath, PROP_CONTEXT_DEFAULT_CATALOG, defaultCatalogName);
            }
            String defaultSchemaName = getDefaultSchemaName(context);
            if (!CommonUtils.isEmpty(defaultSchemaName)) {
                projectMeta.setResourceProperty(resourcePath, PROP_CONTEXT_DEFAULT_SCHEMA, defaultSchemaName);
            }
        }
    }

    private static boolean isDefaultContextSettings(DatabaseEditorContext context) {
        return context.getExecutionContext() == null && context.getSelectedObject() == null;
    }

    private static String getDefaultCatalogName(DatabaseEditorContext context) {
        DBCExecutionContext executionContext = context.getExecutionContext();
        if (executionContext != null) {
            DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
            if (contextDefaults != null) {
                DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                if (defaultCatalog != null) {
                    return defaultCatalog.getName();
                }
            }
        } else {
            DBSCatalog catalog;
            if (context.getSelectedObject() instanceof DBSCatalog) {
                catalog = (DBSCatalog) context.getSelectedObject();
            } else {
                catalog = DBUtils.getParentOfType(DBSCatalog.class, context.getSelectedObject());
            }
            return catalog == null ? null : catalog.getName();
        }
        return null;
    }

    private static String getDefaultSchemaName(DatabaseEditorContext context) {
        DBCExecutionContext executionContext = context.getExecutionContext();
        if (executionContext != null) {
            DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
            if (contextDefaults != null) {
                DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                if (defaultSchema != null) {
                    return defaultSchema.getName();
                }
            }
        } else {
            DBSSchema schema;
            if (context.getSelectedObject() instanceof DBSSchema) {
                schema = (DBSSchema) context.getSelectedObject();
            } else {
                schema = DBUtils.getParentOfType(DBSSchema.class, context.getSelectedObject());
            }
            return schema == null ? null : schema.getName();
        }
        return null;
    }

    public static IEditorPart openExternalFileEditor(File file, IWorkbenchWindow window) {
        try {
            IEditorDescriptor desc = getFileEditorDescriptor(file, window);
            IFileStore fileStore = EFS.getStore(file.toURI());
            IEditorInput input = new FileStoreEditorInput(fileStore);
            return IDE.openEditor(window.getActivePage(), input, desc.getId());
        } catch (CoreException e) {
            log.error("Can't open editor from file '" + file.getAbsolutePath(), e);
            return null;
        }
    }

    @NotNull
    public static IEditorDescriptor getFileEditorDescriptor(@NotNull File file, @NotNull IWorkbenchWindow window) {
        IEditorDescriptor desc = window.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
        if (desc == null) {
            desc = window.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName() + ".txt");
        }
        if (desc == null) {
            desc = window.getWorkbench().getEditorRegistry().findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
        }
        return desc;
    }

    public static boolean isInAutoSaveJob() {
        Job currentJob = Job.getJobManager().currentJob();
        if (currentJob == null) {
            return false;
        }
        return "Auto save all editors".equals(currentJob.getName());
    }

    public static void trackControlContext(IWorkbenchSite site, Control control, String contextId) {
        final IContextService contextService = site.getService(IContextService.class);
        if (contextService != null) {
            final IContextActivation[] activation = new IContextActivation[1];
            FocusListener focusListener = new FocusListener() {

                @Override
                public void focusGained(FocusEvent e) {
                    // No need to deactivate the same context
                    if (activation[0] != null) {
                        contextService.deactivateContext(activation[0]);
                        activation[0] = null;
                    }
                    activation[0] = contextService.activateContext(contextId);
                    //new Exception().printStackTrace();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (activation[0] != null) {
                        contextService.deactivateContext(activation[0]);
                        activation[0] = null;
                    }
                }
            };
            control.addFocusListener(focusListener);
            control.addDisposeListener(e -> {
                if (activation[0] != null) {
                    contextService.deactivateContext(activation[0]);
                    activation[0] = null;
                }
            });
        }
    }

    public static void revertEditorChanges(IEditorPart editorPart) {
        if (editorPart instanceof IRevertableEditor) {
            ((IRevertableEditor) editorPart).doRevertToSaved();
        } else if (editorPart instanceof ITextEditor) {
            ((ITextEditor) editorPart).doRevertToSaved();
        }

        // Revert editor's transaction
        if (editorPart instanceof DBPContextProvider && editorPart instanceof IDataSourceContainerUpdate) {
            DBCExecutionContext executionContext = ((DBPContextProvider) editorPart).getExecutionContext();
            if (executionContext != null) {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
                try {
                    if (txnManager != null && !txnManager.isAutoCommit()) {
                        RuntimeUtils.runTask(monitor -> {
                            try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback editor transaction")) {
                                txnManager.rollback(session, null);
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            }
                        }, "End editor transaction", 5000);
                    }
                } catch (DBCException e) {
                    log.error(e);
                }
            }
        }
    }

    public static void appendProjectToolTip(@NotNull StringBuilder tip, @Nullable DBPProject project) {
        if (project == null || project.getWorkspace().getProjects().size() < 2) {
            return;
        }
        if (!tip.isEmpty() && tip.charAt(tip.length() - 1) != '\n') {
            tip.append('\n');
        }
        tip.append(EditorsMessages.database_editor_project).append(": ").append(project.getName());
    }

    public static List<Path> openExternalFiles(@NotNull String[] fileNames, @Nullable DBPDataSourceContainer currentContainer) {
        log.debug("Open external file(s) [" + Arrays.toString(fileNames) + "]");
        List<Path> openedFiles = new ArrayList<>();
        Path[] filePaths = Arrays.stream(fileNames).map(Path::of).toArray(Path[]::new);
        openFileEditors(filePaths, currentContainer, openedFiles);

        return openedFiles;
    }

    public static boolean openExternalFiles(@NotNull Path[] filePaths, @Nullable DBPDataSourceContainer currentContainer) {
        log.debug("Open external file(s) [" + Arrays.toString(filePaths) + "]");
        List<Path> openedFiles = new ArrayList<>();
        return openFileEditors(filePaths, currentContainer, openedFiles);
    }

    public static boolean openFileEditors(
        @NotNull Path[] fileNames,
        @Nullable DBPDataSourceContainer currentContainer,
        @NotNull List<Path> openedFiles
    ) {
        Map<FileTypeHandlerDescriptor, List<Path>> filesByHandler = new LinkedHashMap<>();
        for (Path path : fileNames) {
            if (Files.exists(path)) {
                String fileExtension = IOUtils.getFileExtension(path);
                FileTypeHandlerDescriptor handler = CommonUtils.isEmpty(fileExtension) ?
                    null : FileTypeHandlerRegistry.getInstance().findHandler(fileExtension);
                filesByHandler.computeIfAbsent(handler, d -> new ArrayList<>()).add(path);
                openedFiles.add(path);
            } else {
                DBWorkbench.getPlatformUI().showError("Open file", "Can't open '" + path + "': file doesn't exist");
            }
        }

        for (Map.Entry<FileTypeHandlerDescriptor, List<Path>> entry : filesByHandler.entrySet()) {
            FileTypeHandlerDescriptor handler = entry.getKey();
            List<Path> pathList = entry.getValue();
            if (handler == null) {
                for (Path path : pathList) {
                    if (!IOUtils.isLocalPath(path)) {
                        return false;
                    }
                    final IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
                    EditorUtils.openExternalFileEditor(path.toFile(), window);
                }
            } else {
                try {
                    handler.createHandler().openFiles(pathList, Map.of(), currentContainer);
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError("Open file error", "Can't open file '" + pathList + "'", e);
                }
            }
        }
        return true;
    }

    public static void activatePartContexts(IWorkbenchPart part) {
        IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
        if (contextService == null) {
            return;
        }
        try {
            contextService.deferUpdates(true);
//            if (part instanceof INavigatorModelView) {
            // We check for instanceof (do not use adapter) because otherwise it become active
            // for all entity editor and clashes with SQL editor and other complex stuff.
//                if (activationNavigator != null) {
//                    //log.debug("Double activation of navigator context");
//                    contextService.deactivateContext(activationNavigator);
//                }
//                activationNavigator = contextService.activateContext(INavigatorModelView.NAVIGATOR_CONTEXT_ID);
//            }

            // What the point of setting SQL editor context here? It is set by editor itself
//            if (part instanceof SQLEditorBase || part.getAdapter(SQLEditorBase.class) != null) {
//                if (activationSQL != null) {
//                    //log.debug("Double activation of SQL context");
//                    contextService.deactivateContext(activationSQL);
//                }
//                activationSQL = contextService.activateContext(SQLEditorContributions.SQL_EDITOR_CONTEXT);
//            }
            // Refresh auto-commit element state (#3315)
            ActionUtils.fireCommandRefresh(ConnectionCommands.CMD_TOGGLE_AUTOCOMMIT);
        } finally {
            contextService.deferUpdates(false);
        }
    }

    public static void deactivatePartContexts(IWorkbenchPart part) {
    }

    public static void refreshPartContexts(IWorkbenchPart part) {
        deactivatePartContexts(part);
        activatePartContexts(part);
    }
}
