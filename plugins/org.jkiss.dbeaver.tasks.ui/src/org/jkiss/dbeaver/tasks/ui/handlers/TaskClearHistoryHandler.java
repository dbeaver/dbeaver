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
package org.jkiss.dbeaver.tasks.ui.handlers;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskEvent;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskRunStorage;
import org.jkiss.dbeaver.registry.task.TaskConstants;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

public class TaskClearHistoryHandler implements DBRRunnableWithProgress {
    private static final Log log = Log.getLog(TaskClearHistoryHandler.class);
    private static final int CANCEL_POLL_INTERVAL = 100;

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        // Uninitialized project managers have no running tasks or cached statistics to clear.
        final List<DBTTaskManager> managers = workspace.getProjects().stream()
            .map(project -> project.getTaskManager(false))
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        try {
            monitor.beginTask("Clear task run history", 2);

            monitor.subTask("Cancel running tasks");
            for (DBTTaskManager manager : managers) {
                manager.cancelRunningTasks();
            }
            // Cancellation is asynchronous. Wait for final history writes, including directly executed tasks
            // that cannot be joined through the Eclipse job manager, before deleting their records and logs.
            while (managers.stream().anyMatch(DBTTaskManager::hasRunningTasks)) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
                Thread.sleep(CANCEL_POLL_INTERVAL);
            }
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            monitor.worked(1);

            monitor.subTask("Delete task run records");
            DBTTaskRunStorage storage = DBTTaskRunStorage.getInstance();
            if (storage != null) {
                storage.deleteRuns(null, null, null);
            }
            Path statisticsFolder = workspace.getMetadataFolder().resolve(TaskConstants.TASK_STATS_FOLDER);
            if (Files.exists(statisticsFolder)) {
                Files.walkFileTree(statisticsFolder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        log.trace("Deleting " + file);
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        log.trace("Deleting " + dir);
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            for (DBTTaskManager manager : managers) {
                for (DBTTask task : manager.getAllTasks()) {
                    task.refreshRunStatistics();
                    TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(task, DBTTaskEvent.Action.TASK_UPDATE));
                }
            }
            monitor.worked(1);
        } catch (IOException | DBException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }
}
