/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TaskClearHistoryHandler implements DBRRunnableWithProgress {
    private static final Log log = Log.getLog(TaskClearHistoryHandler.class);

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        final DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        final DBTTaskManager manager = project.getTaskManager();

        try {
            monitor.beginTask("Clear task run history", 2);

            monitor.subTask("Cancel running tasks");
            manager.cancelRunningTasks();
            monitor.worked(1);

            monitor.subTask("Delete task run records");
            Files.walkFileTree(manager.getStatisticsFolder(), new SimpleFileVisitor<>() {
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
            monitor.worked(1);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }
}
