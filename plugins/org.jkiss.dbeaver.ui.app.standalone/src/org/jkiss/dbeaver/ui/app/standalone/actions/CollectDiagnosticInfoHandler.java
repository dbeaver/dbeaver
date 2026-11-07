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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ConfigurationInfo;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.LogOutputStream;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.LocalSecretController;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A handler that collects diagnostic info, packs it into a zip archive and saves on a disk location specified by the user.
 */
public class CollectDiagnosticInfoHandler extends AbstractHandler {
    private static final Log log = Log.getLog(CollectDiagnosticInfoHandler.class);
    private static final long MIN_BYTES_PER_WORK = 64 * 1024;

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        CollectDiagnosticInfoDialog dialog = new CollectDiagnosticInfoDialog(event);
        int returnCode = dialog.open();
        if (returnCode != Window.OK) {
            log.trace("User cancelled %s".formatted(CollectDiagnosticInfoDialog.class.getName()));
            return null;
        }
        boolean sanitizeLogs = dialog.sanitizeLogs.get();
        Path archive = dialog.getOutputFolder().resolve(
            "dbeaver-diagnostic-info-%s%d.zip".formatted(sanitizeLogs ? "sanitized-" : "", System.currentTimeMillis())
        );
        if (Files.exists(archive)) {
            // Happens once in a blue moon
            log.warn("File %s already exists".formatted(archive));
            showError();
            return null;
        }

        log.trace("Writing diagnostic info archive");
        AtomicBoolean completed = new AtomicBoolean();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    collectDiagnosticInfo(monitor, archive, sanitizeLogs);
                    completed.set(true);
                } catch (IOException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof OperationCanceledException)) {
                log.warn("Cannot collect diagnostic data into archive '%s': caught exception".formatted(archive), e.getTargetException());
                showError();
            }
            return null;
        } catch (OperationCanceledException e) {
            return null;
        }
        if (completed.get()) {
            UIUtils.asyncExec(() -> ShellUtils.showInSystemExplorer(archive.toString()));
        }
        return null;
    }

    private static void collectDiagnosticInfo(@NotNull DBRProgressMonitor monitor, @NotNull Path archive, boolean sanitizeLogs)
        throws IOException {
        monitor.subTask(CoreApplicationMessages.collect_diagnostic_info_progress_prepare);
        List<DiagnosticsEntry> entries = List.copyOf(getDiagnosticEntries());
        checkCanceled(monitor);
        ProgressPlan plan = planProgress(entries);
        monitor.beginTask(CoreApplicationMessages.collect_diagnostic_info_progress_title, plan.totalWork());
        DiagnosticInfoSanitizer sanitizer = sanitizeLogs ? new DiagnosticInfoSanitizer() : null;
        boolean completed = false;
        boolean archiveCreated = false;
        try {
            try (var out = new ZipOutputStream(Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                archiveCreated = true;
                checkCanceled(monitor);
                monitor.subTask("configuration.txt");
                try {
                    out.putNextEntry(new ZipEntry("configuration.txt"));
                    try {
                        String summary = LocalSecretController.getDiagnosticSecretPreferencePattern()
                            .matcher(ConfigurationInfo.getSystemSummary()).replaceAll("");
                        out.write((sanitizer == null ? summary : sanitizer.sanitize(summary)).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.warn("Cannot write configuration info into archive '%s': caught exception".formatted(archive), e);
                    } finally {
                        out.closeEntry();
                    }
                } catch (IOException e) {
                    log.warn("issues with configuration.txt entry in archive '%s': caught exception".formatted(archive), e);
                }
                monitor.worked(1);
                for (PlannedEntry planned : plan.entries()) {
                    checkCanceled(monitor);
                    DiagnosticsEntry file = planned.entry();
                    monitor.subTask(file.zipEntryName);
                    try {
                        out.putNextEntry(new ZipEntry(file.zipEntryName));
                        ProgressInputStream progressInput = null;
                        try (var in = new ProgressInputStream(Files.newInputStream(file.path), monitor, plan.bytesPerWork(), planned.work())) {
                            progressInput = in;
                            if (sanitizer == null) {
                                in.transferTo(out);
                            } else {
                                // Process one line at a time to avoid retaining large logs in memory.
                                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    checkCanceled(monitor);
                                    out.write(sanitizer.sanitize(line).getBytes(StandardCharsets.UTF_8));
                                    out.write('\n');
                                }
                            }
                        } catch (IOException e) {
                            log.warn(
                                "error transferring log entry '%s' into archive '%s': caught exception".formatted(file.zipEntryName, archive),
                                e
                            );
                        } finally {
                            if (progressInput == null) {
                                monitor.worked(planned.work());
                            } else {
                                progressInput.complete();
                            }
                            out.closeEntry();
                        }
                    } catch (IOException e) {
                        log.warn("error with log entry '%s' in archive '%s': caught exception".formatted(file.zipEntryName, archive), e);
                    }
                }
                checkCanceled(monitor);
            }
            completed = true;
        } finally {
            monitor.done();
            if (archiveCreated && !completed) {
                Files.deleteIfExists(archive);
            }
        }
    }

    @NotNull
    static ProgressPlan planProgress(@NotNull Collection<DiagnosticsEntry> entries) {
        List<EntrySize> sizes = entries.stream().map(entry -> {
            try {
                return new EntrySize(entry, Files.size(entry.path()));
            } catch (IOException e) {
                // The log may have rotated since it was discovered. Its read error is handled during collection.
                return new EntrySize(entry, 0);
            }
        }).toList();
        long totalBytes = 0;
        for (EntrySize size : sizes) {
            totalBytes = Long.MAX_VALUE - totalBytes < size.bytes() ? Long.MAX_VALUE : totalBytes + size.bytes();
        }
        long maxByteUnits = Math.max(1, (long) Integer.MAX_VALUE - sizes.size() - 1);
        long bytesPerWork = Math.max(MIN_BYTES_PER_WORK, 1 + totalBytes / maxByteUnits);
        int totalWork = 1; // Configuration entry
        List<PlannedEntry> planned = new ArrayList<>(sizes.size());
        for (EntrySize size : sizes) {
            int work = (int) Math.max(1, size.bytes() == 0 ? 0 : 1 + (size.bytes() - 1) / bytesPerWork);
            planned.add(new PlannedEntry(size.entry(), work));
            totalWork += work;
        }
        return new ProgressPlan(planned, bytesPerWork, totalWork);
    }

    private static void checkCanceled(@NotNull DBRProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    record ProgressPlan(@NotNull List<PlannedEntry> entries, long bytesPerWork, int totalWork) {}

    record PlannedEntry(@NotNull DiagnosticsEntry entry, int work) {}

    private record EntrySize(@NotNull DiagnosticsEntry entry, long bytes) {}

    static final class ProgressInputStream extends FilterInputStream {
        @NotNull
        private final DBRProgressMonitor monitor;
        private final long bytesPerWork;
        private final int totalWork;
        private long bytesRead;
        private int reportedWork;

        ProgressInputStream(@NotNull InputStream input, @NotNull DBRProgressMonitor monitor, long bytesPerWork, int totalWork) {
            super(input);
            this.monitor = monitor;
            this.bytesPerWork = bytesPerWork;
            this.totalWork = totalWork;
        }

        @Override
        public int read() throws IOException {
            checkCanceled(monitor);
            int value = super.read();
            if (value >= 0) {
                updateProgress(1);
            }
            return value;
        }

        @Override
        public int read(@NotNull byte[] buffer, int offset, int length) throws IOException {
            checkCanceled(monitor);
            int count = super.read(buffer, offset, length);
            if (count > 0) {
                updateProgress(count);
            }
            return count;
        }

        @Override
        public int read(@NotNull byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        private void updateProgress(int count) {
            bytesRead += count;
            int work = (int) Math.min(totalWork, bytesRead / bytesPerWork);
            if (work > reportedWork) {
                monitor.worked(work - reportedWork);
                reportedWork = work;
            }
        }

        void complete() {
            monitor.worked(totalWork - reportedWork);
            reportedWork = totalWork;
        }
    }

    private static void showError() {
        UIUtils.syncExec(() -> MessageBoxBuilder.builder(UIUtils.getActiveWorkbenchShell())
            .setTitle(CoreApplicationMessages.collect_diagnostic_info_error_message_title)
            .setMessage(CoreApplicationMessages.collect_diagnostic_info_error_message_text)
            .setReplies(Reply.CLOSE)
            .setDefaultReply(Reply.CLOSE)
            .setPrimaryImage(DBIcon.STATUS_ERROR)
            .setCustomArea(parent -> createDocumentationLink(parent, CoreApplicationMessages.collect_diagnostic_info_error_message_hint))
            .showMessageBox()
        );
    }

    @NotNull
    private static Collection<DiagnosticsEntry> getDiagnosticEntries() {
        Collection<DiagnosticsEntry> diagnosticsEntries = new ArrayList<>();
        addEclipseLog(diagnosticsEntries);
        addTaskLogs(diagnosticsEntries);
        // Ensure the current debug log comes last as to capture any errors during packaging the info archive
        addDBeaverDebugLogs(diagnosticsEntries);
        return diagnosticsEntries;
    }

    private static void addEclipseLog(@NotNull Collection<DiagnosticsEntry> diagnosticsEntries) {
        Path path = Platform.getLogFileLocation().toPath();
        diagnosticsEntries.add(new DiagnosticsEntry(path, path.getFileName().toString()));
    }

    private static void addDBeaverDebugLogs(@NotNull Collection<DiagnosticsEntry> diagnosticsEntries) {
        String logLocation = DBWorkbench.getPlatform().getPreferenceStore().getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(logLocation)) {
            logLocation = GeneralUtils.getMetadataFolder().resolve(DBConstants.DEBUG_LOG_FILE_NAME).toAbsolutePath().toString();
        }
        logLocation = GeneralUtils.replaceVariables(logLocation, new SystemVariablesResolver());
        Path debugLog = Path.of(logLocation);
        Path logFileLocation = debugLog.getParent();
        if (logFileLocation != null && Files.isDirectory(logFileLocation)) {
            String fileName = debugLog.getFileName().toString();
            var logFileNamePredicate = LogOutputStream.getArchivedLogFileNamePredicate(fileName);
            try (var stream = Files.list(logFileLocation)) {
                Collection<DiagnosticsEntry> tmp = stream
                    .filter(path -> logFileNamePredicate.test(path.getFileName().toString()))
                    .map(path -> new DiagnosticsEntry(path, path.getFileName().toString()))
                    .toList();
                diagnosticsEntries.addAll(tmp);
            } catch (IOException e) {
                log.debug("Cannot list debug log diagnosticsEntries in '%s': caught exception".formatted(logFileLocation), e);
            }
        }
        // Ensure the current debug log comes last as to capture any errors during packaging the info archive
        if (Files.isRegularFile(debugLog)) {
            diagnosticsEntries.add(new DiagnosticsEntry(debugLog, debugLog.getFileName().toString()));
        }
    }

    private static void addTaskLogs(@NotNull Collection<DiagnosticsEntry> diagnosticsEntries) {
        addTaskLogs(diagnosticsEntries, DBWorkbench.getPlatform().getWorkspace().getProjects());
    }

    static void addTaskLogs(@NotNull Collection<DiagnosticsEntry> diagnosticsEntries, @NotNull Iterable<? extends DBPProject> projects) {
        for (DBPProject project: projects) {
            // Diagnostic collection must not open or recover projects just to discover task logs.
            if (!project.isOpen()) {
                continue;
            }
            DBTTaskManager taskManager = project.getTaskManager(false);
            if (taskManager == null) {
                continue;
            }
            String zipEntryProjectNamePrefix = "tasks/" + project.getName() + "/";
            for (DBTTask task: taskManager.getAllTasks()) {
                String zipEntryNamePrefix = zipEntryProjectNamePrefix + task.getName() + "/";
                for (DBTTaskRun taskRun: task.getAllRuns()) {
                    Path runLog = task.getRunLog(taskRun);
                    if (runLog != null) {
                        diagnosticsEntries.add(new DiagnosticsEntry(runLog, zipEntryNamePrefix + runLog.getFileName().toString()));
                    }
                }
            }
        }
    }

    // TODO: There is no unified approach to display links to documentation in our app.
    // TODO: We need to create a universal method in UIUtils and replace all occurrences of creating doc links with the method.
    private static void createDocumentationLink(@NotNull Composite parent, @NotNull String text) {
        String linkToDocs = HelpUtils.getHelpExternalReference("Log-files");
        String href = "<a href=\"" + linkToDocs + "\">" + text + "</a>";
        UIUtils.createInfoLink(parent, href, () -> ShellUtils.launchProgram(linkToDocs));
    }

    record DiagnosticsEntry(@NotNull Path path, @NotNull String zipEntryName) {}

    private static final class CollectDiagnosticInfoDialog extends BaseDialog {
        @NotNull
        private final UIObservable<Boolean> sanitizeLogs = UIObservable.of(
            DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DBeaverPreferences.DIAGNOSTIC_INFO_SANITIZE_LOGS));
        @Nullable
        private TextWithOpen textWithOpen;
        @Nullable
        private Path outputFolder;

        private CollectDiagnosticInfoDialog(ExecutionEvent event) {
            super(HandlerUtil.getActiveShell(event), CoreApplicationMessages.collect_diagnostic_info_pick_path_title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            dialogArea = UIUtils.createComposite(dialogArea, 1);
            dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIPanelBuilder.build(dialogArea, pb -> pb
                .row(CoreApplicationMessages.collect_diagnostic_info_pick_path_label, rb -> rb
                    .control(this::createOutputFolderPicker, cb -> cb.align(UIAlignX.FILL)))
                .row(rb -> rb.checkBox(
                    CoreApplicationMessages.collect_diagnostic_info_sanitize_logs,
                    CoreApplicationMessages.collect_diagnostic_info_sanitize_logs_hint,
                    bb -> bb.selected(sanitizeLogs)))
                .row(rb -> rb.control(container -> UIUtils.createWarningLabel(
                    container, CoreApplicationMessages.collect_diagnostic_info_pick_path_warning, GridData.FILL_HORIZONTAL, 1))));

            return dialogArea;
        }

        @NotNull
        private Composite createOutputFolderPicker(@NotNull Composite parent) {
            textWithOpen = new TextWithOpenFolder(parent, CoreApplicationMessages.collect_diagnostic_info_pick_path_title);
            String initialOutputFolderPath = getInitialOutputFolderPath();
            if (initialOutputFolderPath != null) {
                textWithOpen.setText(initialOutputFolderPath);
            } else {
                enableOk(false);
            }
            Text textControl = textWithOpen.getTextControl();
            textControl.addModifyListener(event -> enableOk(isOutputFolderPathValid(textControl.getText())));
            return textWithOpen.getPanel();
        }

        private void enableOk(boolean enable) {
            Button okButton = getButton(OK);
            if (okButton != null) {
                okButton.setEnabled(enable);
            }
        }

        @Override
        protected void okPressed() {
            if (textWithOpen != null) {
                String outputFolderPathString = textWithOpen.getText();
                if (!CommonUtils.isEmpty(outputFolderPathString)) {
                    outputFolder = Path.of(outputFolderPathString);
                    storeOutputFolderPath(outputFolderPathString);
                }
            }
            DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
            store.setValue(DBeaverPreferences.DIAGNOSTIC_INFO_SANITIZE_LOGS, sanitizeLogs.get());
            PrefUtils.savePreferenceStore(store);
            super.okPressed();
        }

        private void storeOutputFolderPath(@NotNull String outputFolderPathString) {
            DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
            store.setValue(DBeaverPreferences.DIAGNOSTIC_INFO_OUTPUT_FOLDER, outputFolderPathString);
            PrefUtils.savePreferenceStore(store);
        }

        @Nullable
        private String getInitialOutputFolderPath() {
            DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
            String outputFolderPath = store.getString(DBeaverPreferences.DIAGNOSTIC_INFO_OUTPUT_FOLDER);
            if (!isOutputFolderPathValid(outputFolderPath)) {
                return System.getProperty(StandardConstants.ENV_USER_HOME);
            }
            return outputFolderPath;
        }

        @Nullable
        private Path getOutputFolder() {
            return outputFolder;
        }

        private static boolean isOutputFolderPathValid(@Nullable String outputFolderPathString) {
            if (CommonUtils.isEmpty(outputFolderPathString)) {
                return false;
            }
            try {
                return Files.isDirectory(Path.of(outputFolderPathString));
            } catch (InvalidPathException e) {
                log.debug("Invalid path '%s'".formatted(outputFolderPathString), e);
                return false;
            }
        }
    }
}
