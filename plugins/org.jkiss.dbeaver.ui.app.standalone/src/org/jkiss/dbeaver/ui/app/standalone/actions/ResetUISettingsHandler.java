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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ResetUISettingsHandler extends AbstractHandler {
    private static final Log log = Log.getLog(ResetUISettingsHandler.class);

    private static final String PLUGINS_FOLDER = ".plugins";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final boolean result = UIUtils.confirmAction(
            HandlerUtil.getActiveShell(event),
            "Confirm UI settings reset",
            "You are about to reset UI settings. All UI settings and user preferences will be lost.\n\nThese include:\n\n" +
                " - menus, toolbars, windows, editors\n" +
                " - theme, colors and fonts\n" +
                " - other settings from installed third-party plugins\n\n" +
                "In order to apply changes, DBeaver will be restarted.\n\n" +
                "Are you sure you want to continue?",
            DBIcon.STATUS_WARNING
        );

        if (!result) {
            return null;
        }

        final IWorkbench workbench = PlatformUI.getWorkbench();

        if (workbench.restart()) {
            final Path path = DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(PLUGINS_FOLDER);

            if (Files.notExists(path) || !Files.isDirectory(path)) {
                return null;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
                } catch (IOException e) {
                    log.error("Error walking plugin settings", e);
                }
            }));
        }

        return null;
    }
}
