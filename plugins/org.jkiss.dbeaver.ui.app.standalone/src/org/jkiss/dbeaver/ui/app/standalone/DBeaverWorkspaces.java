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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class that helps managing workspaces.
 */
final class DBeaverWorkspaces {
    private static final Log log = Log.getLog(DBeaverWorkspaces.class);

    // See org.eclipse.ui.internal.ide.ChooseWorkspaceData.RECENT_MAX_LENGTH
    private static final int RECENT_MAX_LENGTH = 10;

    private DBeaverWorkspaces() {
    }

    /**
     * Fetches the list of recent workspaces and updates Eclipse preferences, if needed.
     * <p>
     * The first entry in the list is the active workspace.
     *
     * @param application application to retrieve workspaces work
     * @param location    instance location of the application
     * @return a list of recent workspace paths
     */
    @NotNull
    static List<String> fetchRecentWorkspaces(@NotNull DBeaverApplication application, @NotNull Location location) {
        ChooseWorkspaceData data = new ChooseWorkspaceData(location.getDefault());

        List<String> backedUpWorkspaces = getBackedUpWorkspaces(application);
        List<String> eclipseWorkspaces = Stream.of(data.getRecentWorkspaces())
            .filter(Objects::nonNull)
            .toList();

        List<String> workspaces;
        if (!backedUpWorkspaces.isEmpty()) {
            // We have our .workspaces file, prioritize it
            workspaces = backedUpWorkspaces;
        } else {
            // Otherwise, grab workspaces Eclipse knows about
            workspaces = eclipseWorkspaces;
        }

        workspaces = workspaces.stream()
            .distinct()
            .limit(RECENT_MAX_LENGTH)
            .collect(Collectors.toList());

        String workingFolder = application.getDefaultWorkingFolder().toString();
        if (!workspaces.contains(workingFolder)) {
            if (workspaces.size() == RECENT_MAX_LENGTH) {
                workspaces.removeLast();
            }
            workspaces.addFirst(workingFolder);
        }

        if (!eclipseWorkspaces.equals(workspaces)) {
            data.setRecentWorkspaces(workspaces.toArray(new String[RECENT_MAX_LENGTH]));
            data.writePersistedData();
        }

        if (!backedUpWorkspaces.equals(workspaces)) {
            saveWorkspacesToBackup(application, workspaces);
        }

        return workspaces;
    }

    /**
     * Flushes the list of recent workspaces from Eclipse preferences to DBeaver's {@code .workspaces} file.
     *
     * @param application application for which recent workspaces must be flushed
     * @param location    instance location of the application
     */
    static void flushRecentWorkspaces(@NotNull DBeaverApplication application, @NotNull Location location) {
        var data = new ChooseWorkspaceData(location.getDefault());
        var workspaces = Stream.of(data.getRecentWorkspaces())
            .filter(Objects::nonNull)
            .toList();
        saveWorkspacesToBackup(application, workspaces);
    }

    @NotNull
    private static List<String> getBackedUpWorkspaces(@NotNull DBeaverApplication application) {
        Path path = application.getWorkspacesFile();
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            log.debug("Unable to read backed up workspaces", e); //$NON-NLS-1$
            return Collections.emptyList();
        }
    }

    private static void saveWorkspacesToBackup(@NotNull DBeaverApplication application, @NotNull List<String> workspaces) {
        try {
            Path path = application.getWorkspacesFile();
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            } else if (Files.isDirectory(path)) {
                // Bug in 22.0.5
                Files.delete(path);
            }
            Files.write(path, workspaces, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.debug("Unable to save backed up workspaces", e); //$NON-NLS-1$
        }
    }
}
