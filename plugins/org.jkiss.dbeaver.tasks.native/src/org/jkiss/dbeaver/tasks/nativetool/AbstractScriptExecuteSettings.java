/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMControllerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AbstractScriptExecuteSettings<BASE_OBJECT extends DBSObject> extends AbstractNativeToolSettings<BASE_OBJECT> {
    private String inputFile;
    private String inputResourceProjectId;
    private String inputResourceProjectName;
    private String inputResourcePath;
    private Path executionInputFile;

    public AbstractScriptExecuteSettings() {
    }

    public AbstractScriptExecuteSettings(@NotNull DBPProject project) {
        super(project);
    }

    public String getInputFile() {
        return executionInputFile == null ? inputFile : executionInputFile.toString();
    }

    public void setInputFile(String inputFile) {
        if (!Objects.equals(this.inputFile, inputFile)) {
            inputResourceProjectId = null;
            inputResourceProjectName = null;
            inputResourcePath = null;
        }
        this.inputFile = inputFile;
    }

    public void setInputResource(@NotNull String projectId, @NotNull String resourcePath) throws DBException {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProjectById(projectId);
        if (project == null) {
            throw new DBException("Project '" + projectId + "' not found");
        }
        inputResourceProjectId = projectId;
        inputResourceProjectName = project.getName();
        inputResourcePath = resourcePath;
        inputFile = resourcePath;
    }

    public void prepareInputFile(@NotNull DBRProgressMonitor monitor) throws DBException {
        if ((CommonUtils.isEmpty(inputResourceProjectId) && CommonUtils.isEmpty(inputResourceProjectName))
            || CommonUtils.isEmpty(inputResourcePath)) {
            return;
        }
        DBPProject project = CommonUtils.isEmpty(inputResourceProjectId)
            ? null
            : DBWorkbench.getPlatform().getWorkspace().getProjectById(inputResourceProjectId);
        if (project == null && !CommonUtils.isEmpty(inputResourceProjectName)) {
            project = DBWorkbench.getPlatform().getWorkspace().getProject(inputResourceProjectName);
        }
        RMControllerProvider provider = project == null ? null : DBUtils.getAdapter(RMControllerProvider.class, project);
        if (provider == null) {
            throw new DBException("Project '" + (CommonUtils.isEmpty(inputResourceProjectName)
                ? inputResourceProjectId : inputResourceProjectName)
                + "' does not provide remote resources");
        }
        byte[] contents = provider.getResourceController().getResourceContents(project.getId(), inputResourcePath);
        Path file = null;
        try {
            file = Files.createTempFile(DBWorkbench.getPlatform().getTempFolder(monitor, "native-scripts"), "script-", ".sql");
            Files.write(file, contents);
            executionInputFile = file;
        } catch (IOException e) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw new DBException("Error preparing native script '" + inputResourcePath + "'", e);
        }
    }

    @Nullable
    public Path clearExecutionInputFile() {
        Path file = executionInputFile;
        executionInputFile = null;
        return file;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) throws DBException {
        if (CommonUtils.isEmpty(inputFile)) {
            inputFile = preferenceStore.getString("inputFile");
            inputResourceProjectId = preferenceStore.getString("inputResourceProjectId");
            inputResourceProjectName = preferenceStore.getString("inputResourceProjectName");
            inputResourcePath = preferenceStore.getString("inputResourcePath");
        }
        super.loadSettings(runnableContext, preferenceStore);
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);

        preferenceStore.setValue("inputFile", inputFile);
        preferenceStore.setValue("inputResourceProjectId", CommonUtils.toString(inputResourceProjectId));
        preferenceStore.setValue("inputResourceProjectName", CommonUtils.toString(inputResourceProjectName));
        preferenceStore.setValue("inputResourcePath", CommonUtils.toString(inputResourcePath));
    }
}
