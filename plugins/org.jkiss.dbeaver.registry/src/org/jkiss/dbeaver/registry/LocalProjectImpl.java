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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.localstore.BucketTree;
import org.eclipse.core.internal.properties.PropertyBucket;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class LocalProjectImpl extends BaseProjectImpl {

    private static final Log log = Log.getLog(LocalProjectImpl.class);

    private static final String EMPTY_PROJECT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<projectDescription>\n" +
        "<name>${project-name}</name>\n" +
        "<comment></comment>\n" +
        "<projects>\n" +
        "</projects>\n" +
        "<buildSpec>\n" +
        "</buildSpec>\n" +
        "<natures>\n" +
        "</natures>\n" +
        "</projectDescription>";

    @NotNull
    private final IProject project;

    public LocalProjectImpl(@NotNull BaseWorkspaceImpl workspace, @NotNull IProject project, @Nullable SMSessionContext sessionContext) {
        super(workspace, sessionContext);
        this.project = project;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return project.getName();
    }

    @NotNull
    @Override
    public Path getAbsolutePath() {
        return project.getLocation().toFile().toPath();
    }

    @Nullable
    @Override
    public IProject getEclipseProject() {
        return project;
    }

    @Override
    public boolean isOpen() {
        return project.isOpen();
    }

    @Override
    public void ensureOpen() throws IllegalStateException {
        if (!project.isOpen()) {
            NullProgressMonitor monitor = new NullProgressMonitor();
            try {
                project.open(monitor);
                if (!DBWorkbench.isDistributed()) {
                    project.refreshLocal(IFile.DEPTH_ONE, monitor);
                }
            } catch (CoreException e) {
                if (getWorkspace().getPlatform().getApplication().isStandalone() &&
                    e.getMessage().contains(IProjectDescription.DESCRIPTION_FILE_NAME)) {
                    try {
                        recoverProjectDescription();
                        project.open(monitor);
                        project.refreshLocal(IFile.DEPTH_ONE, monitor);
                    } catch (Exception e2) {
                        log.error("Error opening project", e2);
                        return;
                    }
                }
            }
        }
        if (isInMemory()) {
            setFormat(ProjectFormat.MODERN);
            return;
        }

        Path mdFolder = getMetadataFolder(false);

        Path dsConfig = getAbsolutePath().resolve(DataSourceRegistry.LEGACY_CONFIG_FILE_NAME);
        if (!Files.exists(mdFolder) && Files.exists(dsConfig)) {
            setFormat(ProjectFormat.LEGACY);
        } else {
            setFormat(ProjectFormat.MODERN);
        }

        // Check project structure and migrate
        checkAndUpdateProjectStructure();

        // Now project is in modern format
        setFormat(ProjectFormat.MODERN);
    }

    @Override
    public boolean isUseSecretStorage() {
        return false;
    }

    @Nullable
    @Override
    public DBNModel getNavigatorModel() {
        return getWorkspace().getPlatform().getNavigatorModel();
    }

    /**
     * Validates project files structure.
     * If project was created in older DBeaver version then converts it to newer format
     */
    private void checkAndUpdateProjectStructure() {
        if (getFormat() == ProjectFormat.UNKNOWN || getFormat() == ProjectFormat.MODERN) {
            return;
        }

        Path mdConfig = getMetadataPath().resolve(METADATA_STORAGE_FILE);
        if (!Files.exists(mdConfig)) {
            // Migrate
            Map<String, Map<String, Object>> projectResourceProperties = extractProjectResourceProperties();
            synchronized (metadataSync) {
                setResourceProperties(projectResourceProperties);
            }
            flushMetadata();
        }
    }

    private Map<String, Map<String, Object>> extractProjectResourceProperties() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        DBPWorkspaceEclipse workspaceEclipse;
        if (getWorkspace() instanceof DBPWorkspaceEclipse) {
            workspaceEclipse = (DBPWorkspaceEclipse) getWorkspace();
        } else {
            return result;
        }
        try {
            BucketTree bucketTree = new BucketTree((Workspace) workspaceEclipse.getEclipseWorkspace(), new PropertyBucket());
            try {
                final IPath projectPath = project.getFullPath();
                bucketTree.accept(new Bucket.Visitor() {
                                      @Override
                                      public int visit(Bucket.Entry entry) {
                                          Object value = entry.getValue();
                                          if (value instanceof String[][]) {
                                              String[][] bucketProps = (String[][]) value;
                                              for (String[] resProps : bucketProps) {
                                                  if (resProps.length == 3) {
                                                      if ("org.jkiss.dbeaver".equals(resProps[0])) {
                                                          if ("sql-editor-project-id".equals(resProps[1])) {
                                                              continue;
                                                          }
                                                          Map<String, Object> propsMap = result.computeIfAbsent(
                                                              entry.getPath().makeRelativeTo(projectPath).toString(), s -> new LinkedHashMap<>());
                                                          propsMap.put(resProps[1], resProps[2]);
                                                      }
                                                  }
                                              }
                                          }
                                          return CONTINUE;
                                      }
                                  },
                    projectPath,
                    BucketTree.DEPTH_INFINITE);
            } catch (CoreException e) {
                log.error(e);
            }
        } catch (Throwable e) {
            log.error("Error extracting project metadata", e);
        }

        return result;
    }

    public void recoverProjectDescription() throws IOException {
        // .project file missing. Let's try to create an empty project config
        Path mdFile = getAbsolutePath().resolve(IProjectDescription.DESCRIPTION_FILE_NAME);
        log.debug("Recovering project '" + project.getName() + "' metadata " + mdFile.toAbsolutePath());

        IOUtils.writeFileFromString(
            mdFile.toFile(),
            EMPTY_PROJECT_TEMPLATE.replace("${project-name}", project.getName()));
    }

}
