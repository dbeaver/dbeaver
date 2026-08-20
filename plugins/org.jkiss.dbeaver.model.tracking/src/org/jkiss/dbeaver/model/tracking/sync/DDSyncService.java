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
package org.jkiss.dbeaver.model.tracking.sync;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.sync.DBPSyncRegistry;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncSettings;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.model.tracking.DDAccessKey;
import org.jkiss.dbeaver.model.tracking.sync.core.DDContainer;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncEntry;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DDSyncService {

    private static final Log log = Log.getLog(DDSyncService.class);

    public static final String BINDING_FILE = ".synchronize";
    public static final String PROP_PROJECT_ID = "datadam.project-id";

    private static final String KEY_SEPARATOR = "~";

    private final DDSyncStore store;
    private final DBPWorkspace workspace;

    public DDSyncService(
        @NotNull String url,
        @NotNull DDAccessKey accessKey,
        @NotNull DBPWorkspace workspace
    ) {
        this.store = new DDSyncStore(url, accessKey);
        this.workspace = workspace;
    }

    @NotNull
    public List<DDContainer> listContainers() throws DBException {
        return store.listContainers();
    }

    @Nullable
    public DDSyncBinding getBinding() {
        return readBinding(workspace.getAbsolutePath());
    }

    @Nullable
    public static DDSyncBinding readBinding(@NotNull Path workspacePath) {
        Path bindingFile = workspacePath.resolve(BINDING_FILE);
        if (!Files.exists(bindingFile)) {
            return null;
        }
        try {
            return JSONUtils.GSON.fromJson(Files.readString(bindingFile), DDSyncBinding.class);
        } catch (IOException | RuntimeException e) {
            log.debug("Error reading synchronization binding", e);
            return null;
        }
    }

    public void bind(@NotNull String containerId, @Nullable String label) throws DBException {
        try {
            Files.writeString(
                workspace.getAbsolutePath().resolve(BINDING_FILE),
                JSONUtils.GSON.toJson(new DDSyncBinding(containerId, label)));
        } catch (IOException e) {
            throw new DBException("Error writing synchronization binding", e);
        }
    }

    @NotNull
    public String createContainer(@NotNull String label) throws DBException {
        DDContainer created = store.createContainer(label);
        bind(created.id(), created.label());
        return created.id();
    }

    @NotNull
    public List<String> upload(@NotNull String containerId) throws DBException {
        List<String> uploaded = new ArrayList<>();
        for (DBPSyncUnit unit : DBPSyncRegistry.getInstance().getUnits()) {
            if (!DBPSyncSettings.isEnabled(unit)) {
                continue;
            }
            if (unit.getScope() == DBPSyncScope.WORKSPACE) {
                if (uploadUnit(containerId, unit, unit.getId(), new DBPSyncTarget(workspace, null))) {
                    uploaded.add(unit.getId());
                }
            } else {
                for (DBPProject project : workspace.getProjects()) {
                    String key = getProjectId(project) + KEY_SEPARATOR + unit.getId();
                    if (uploadUnit(containerId, unit, key, new DBPSyncTarget(workspace, project))) {
                        uploaded.add(project.getName() + "/" + unit.getId());
                    }
                }
            }
        }
        return uploaded;
    }

    @NotNull
    public List<String> download(@NotNull String containerId) throws DBException {
        List<String> restored = new ArrayList<>();
        for (DDSyncEntry entry : store.load(containerId)) {
            DDUnitRef ref = parseKey(entry.key());
            if (ref == null) {
                log.debug("Skip unknown synchronization key: " + entry.key());
                continue;
            }
            DBPSyncUnit unit = ref.unit();
            if (!DBPSyncSettings.isEnabled(unit)) {
                continue;
            }
            DBPProject project = unit.getScope() == DBPSyncScope.WORKSPACE
                ? null
                : resolveProject(ref.projectId(), entry.label());
            unit.write(new DBPSyncTarget(workspace, project), entry.resources());
            restored.add(project == null
                ? unit.getId()
                : project.getName() + "/" + unit.getId());
        }
        return restored;
    }

    @Nullable
    private static DDUnitRef parseKey(@NotNull String key) {
        String[] parts = key.split(KEY_SEPARATOR, 2);
        String projectId = parts.length > 1 && isUuid(parts[0]) ? parts[0] : null;
        DBPSyncUnit unit = DBPSyncRegistry.getInstance().findById(projectId == null ? parts[0] : parts[1]);
        if (unit == null) {
            return null;
        }
        return new DDUnitRef(unit, projectId);
    }

    private static boolean isUuid(@NotNull String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private record DDUnitRef(
        @NotNull DBPSyncUnit unit,
        @Nullable String projectId
    ) {
    }

    private boolean uploadUnit(
        @NotNull String containerId,
        @NotNull DBPSyncUnit unit,
        @NotNull String key,
        @NotNull DBPSyncTarget target
    ) throws DBException {
        Map<String, byte[]> resources = unit.read(target);
        if (resources.isEmpty()) {
            return false;
        }
        DBPProject project = target.project();
        store.save(containerId, new DDSyncEntry(
            key,
            project == null ? null : project.getName(),
            null,
            resources));
        return true;
    }

    @NotNull
    private DBPProject resolveProject(@Nullable String projectId, @Nullable String projectName) throws DBException {

        if (projectId == null) {
            throw new DBException("Project id is missing in synchronization data");
        }

        for (DBPProject project : workspace.getProjects()) {
            if (projectId.equals(CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null))) {
                return project;
            }
        }
        String name = CommonUtils.isEmpty(projectName) ? projectId : projectName;
        if (workspace.getProject(name) != null) {
            name = name + " (" + projectId + ")";
        }
        DBPProject created = workspace.createProject(name, null);
        created.setProjectProperty(PROP_PROJECT_ID, projectId);
        return created;
    }

    @NotNull
    private static String getProjectId(@NotNull DBPProject project) {
        String projectId = CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null);
        if (CommonUtils.isEmpty(projectId)) {
            projectId = UUID.randomUUID().toString();
            project.setProjectProperty(PROP_PROJECT_ID, projectId);
        }
        return projectId;
    }
}
