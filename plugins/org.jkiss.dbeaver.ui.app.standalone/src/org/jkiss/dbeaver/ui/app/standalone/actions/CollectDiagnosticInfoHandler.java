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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A handler that collects diagnostic info, packs it into a zip archive and saves on a disk location specified by the user.
 */
public class CollectDiagnosticInfoHandler extends AbstractHandler {
    private static final Log log = Log.getLog(CollectDiagnosticInfoHandler.class);

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        CollectDiagnosticInfoDialog dialog = new CollectDiagnosticInfoDialog(event);
        int returnCode = dialog.open();
        if (returnCode != Window.OK) {
            log.trace("User cancelled %s".formatted(CollectDiagnosticInfoDialog.class.getName()));
            return null;
        }
        File archive = new File(dialog.getOutputFolder(), "dbeaver-diagnostic-info-%d.zip".formatted(System.currentTimeMillis()));
        if (archive.exists()) {
            // Happens once in a blue moon
            log.warn("File %s already exists".formatted(archive));
            showError();
            return null;
        }

        log.trace("Writing diagnostic info archive");
        try (var out = new ZipOutputStream(new FileOutputStream(archive))) {
            for (File file : getLogFiles()) {
                out.putNextEntry(new ZipEntry(file.getName()));
                try (var in = new FileInputStream(file)) {
                    in.transferTo(out);
                }
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("configuration.txt"));
            out.write(ConfigurationInfo.getSystemSummary().getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (IOException e) {
            log.warn("Cannot collect diagnostic data into archive '%s': caught exception".formatted(archive), e);
            showError();
        }
        UIUtils.asyncExec(() -> ShellUtils.showInSystemExplorer(archive));

        return null;
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
    private static Iterable<File> getLogFiles() {
        Collection<File> logs = new ArrayList<>();
        logs.add(Platform.getLogFileLocation().toFile());
        File debugLogFile = getCurrentDebugLogFile();
        if (debugLogFile.exists() && debugLogFile.isFile()) {
            logs.add(debugLogFile);
        }

        // Copy-paste from the LogOutputStream constructor and LogOutputStream.rotateCurrentLogFile
        File logFileLocation = debugLogFile.getParentFile();
        if (logFileLocation == null || !logFileLocation.isDirectory()) {
            return logs;
        }
        String fileName = debugLogFile.getName();
        String logFileName;
        String logFileNameExtension;
        int fnameExtStart = fileName.lastIndexOf('.');
        if (fnameExtStart >= 0) {
            logFileName = fileName.substring(0, fnameExtStart);
            logFileNameExtension = fileName.substring(fnameExtStart);
        } else {
            logFileName = fileName;
            logFileNameExtension = "";
        }
        String logFileNameRegexStr = "^" + Pattern.quote(logFileName) + "\\-[0-9]+" + Pattern.quote(logFileNameExtension) + "$";
        Predicate<String> logFileNamePattern = Pattern.compile(logFileNameRegexStr).asMatchPredicate();
        File[] debugLogFiles = logFileLocation.listFiles((File dir, String name) -> logFileNamePattern.test(name));
        if (debugLogFiles != null) {
            Collections.addAll(logs, debugLogFiles);
        }
        return logs;
    }

    @NotNull
    private static File getCurrentDebugLogFile() {
        // Copy-paste from DBeaverApplication.initDebugWriter
        String logLocation = DBWorkbench.getPlatform().getPreferenceStore().getString(DBeaverPreferences.LOGS_DEBUG_LOCATION);
        if (CommonUtils.isEmpty(logLocation)) {
            logLocation = GeneralUtils.getMetadataFolder().resolve(DBConstants.DEBUG_LOG_FILE_NAME).toAbsolutePath().toString();
        }
        logLocation = GeneralUtils.replaceVariables(logLocation, new SystemVariablesResolver());
        return new File(logLocation);
    }

    // TODO: There is no unified approach to display links to documentation in our app.
    // TODO: We need to create a universal method in UIUtils and replace all occurrences of creating doc links with the method.
    private static void createDocumentationLink(@NotNull Composite parent, @NotNull String text) {
        String linkToDocs = HelpUtils.getHelpExternalReference("Log-files");
        String href = "<a href=\"" + linkToDocs + "\">" + text + "</a>";
        UIUtils.createInfoLink(parent, href, () -> ShellUtils.launchProgram(linkToDocs));
    }

    private static final class CollectDiagnosticInfoDialog extends BaseDialog {
        @Nullable
        private TextWithOpen textWithOpen;
        @Nullable
        private File outputFolder;

        private CollectDiagnosticInfoDialog(ExecutionEvent event) {
            super(HandlerUtil.getActiveShell(event), CoreApplicationMessages.collect_diagnostic_info_pick_path_title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            dialogArea = UIUtils.createComposite(dialogArea, 1);
            dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));

            Composite outputFolderPickerComposite = UIUtils.createComposite(dialogArea, 2);
            outputFolderPickerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createControlLabel(outputFolderPickerComposite, CoreApplicationMessages.collect_diagnostic_info_pick_path_label);
            textWithOpen = new TextWithOpenFolder(
                outputFolderPickerComposite,
                CoreApplicationMessages.collect_diagnostic_info_pick_path_title
            );
            textWithOpen.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String userHome = System.getProperty(StandardConstants.ENV_USER_HOME);
            if (userHome != null) {
                textWithOpen.setText(userHome);
            } else {
                enableOk(false);
            }
            Text textControl = textWithOpen.getTextControl();
            textControl.addModifyListener(event -> {
                File file = new File(textControl.getText());
                enableOk(file.isDirectory());
            });

            // Warning about potentially sensitive data
            UIUtils.createWarningLabel(
                dialogArea,
                CoreApplicationMessages.collect_diagnostic_info_pick_path_warning,
                GridData.FILL_HORIZONTAL,
                1
            );

            return dialogArea;
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
                    outputFolder = new File(outputFolderPathString);
                }
            }
            super.okPressed();
        }

        @Nullable
        private File getOutputFolder() {
            return outputFolder;
        }
    }
}
