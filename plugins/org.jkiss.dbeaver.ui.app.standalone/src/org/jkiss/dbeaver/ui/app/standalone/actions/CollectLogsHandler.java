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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPApplicationDesktop;
import org.jkiss.dbeaver.model.app.DBPLogLocations;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;

public class CollectLogsHandler extends AbstractHandler {
    private static final Log log = Log.getLog(CollectLogsHandler.class);

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (!(application instanceof DBPApplicationDesktop)) {
            log.warn("Cannot collect log files: unexpected DBPApplication implementation");
            showError();
            return null;
        }

        DBPLogLocations logLocations = ((DBPApplicationDesktop) application).getLogLocations();
        Collection<File> allLogFiles = logLocations.getDebugLogFiles();
        allLogFiles.add(Platform.getLogFileLocation().toFile());
        for (File targetFolder : getTargetFolders(logLocations)) {
            boolean ok = tryZippingAndShowingLogs(targetFolder, allLogFiles);
            if (ok) {
                return null;
            }
        }

        showError();
        return null;
    }

    private static boolean tryZippingAndShowingLogs(File targetFolder, Iterable<File> files) {
        File archive = new File(targetFolder, "dbeaver-logs-%d.zip".formatted(System.currentTimeMillis()));
        try (var zipfs = FileSystems.newFileSystem(archive.toPath(), Map.of("create", true))) {
            for (File file : files) {
                Path target = zipfs.getPath(file.getName());
                Files.copy(file.toPath(), target);
            }
            Boolean ok = new UITask<Boolean>() {
                @Override
                protected Boolean runTask() {
                    return ShellUtils.showInSystemExplorer(archive);
                }
            }.execute();
            return Boolean.TRUE.equals(ok);
        } catch (IOException e) {
            log.warn("Cannot collect log files into archive '%s': caught exception".formatted(archive), e);
            return false;
        }
    }

    private static Iterable<File> getTargetFolders(DBPLogLocations logLocations) {
        Collection<File> result = new HashSet<>();
        File eclipseLogFolder = Platform.getLogFileLocation().toFile().getParentFile();
        if (eclipseLogFolder != null) {
            result.add(eclipseLogFolder);
        }
        File debugLogFolder = logLocations.getDebugLogFolder();
        if (debugLogFolder != null) {
            result.add(debugLogFolder);
        }
        return result;
    }

    private static void showError() {
        UIUtils.syncExec(() -> MessageBoxBuilder.builder(UIUtils.getActiveWorkbenchShell())
            .setTitle(CoreApplicationMessages.collect_logs_error_message_title)
            .setMessage(CoreApplicationMessages.collect_logs_error_message_text)
            .setReplies(Reply.CLOSE)
            .setDefaultReply(Reply.CLOSE)
            .setPrimaryImage(DBIcon.STATUS_ERROR)
            .setCustomArea(parent -> UIUtils.createDocumentationLink(
                parent,
                CoreApplicationMessages.collect_logs_error_message_hint,
                "Log-files")
            ).showMessageBox()
        );
    }
}
