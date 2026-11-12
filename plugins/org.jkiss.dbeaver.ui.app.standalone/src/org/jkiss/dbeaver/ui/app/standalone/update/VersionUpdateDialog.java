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
package org.jkiss.dbeaver.ui.app.standalone.update;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor.PatchSeverity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.app.standalone.update.install.MacDmgInstaller;
import org.jkiss.dbeaver.ui.app.standalone.update.install.PlatformInstaller;
import org.jkiss.dbeaver.ui.app.standalone.update.install.WinExeInstaller;
import org.jkiss.dbeaver.ui.app.standalone.update.install.WinZipInstaller;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class VersionUpdateDialog extends Dialog {

    private static final Log log = Log.getLog(VersionUpdateDialog.class);

    private static final String OS_WINDOWS = "win";
    private static final String OS_MACOS = "mac";
    private static final String OS_LINUX = "linux";
    private static final String DISTRIBUTION_DEB = "deb";
    private static final String DISTRIBUTION_RPM = "rpm";
    private static final String UPDATES_FOLDER = "dbeaver-updates";

    private static final int INFO_ID = 1000;
    private static final int UPGRADE_ID = 1001;
    private static final int CHECK_EA_ID = 1002;

    private final Version currentVersion;
    private final int currentPatch;
    private final VersionDescriptor newVersion;

    private final boolean showConfig;
    private Button dontShowAgainCheck;
    private final String earlyAccessURL;

    public VersionUpdateDialog(@Nullable Shell parentShell, @NotNull Version currentVersion, @NotNull VersionDescriptor newVersion, boolean showConfig)
    {
        super(parentShell);
        this.currentVersion = currentVersion;
        this.currentPatch = DBeaverVersionChecker.getProductPatchNumber();
        this.newVersion = newVersion;
        this.showConfig = showConfig;

        earlyAccessURL = GeneralUtils.getProductEarlyAccessURL();
    }

    @NotNull
    public VersionDescriptor getNewVersion() {
        return newVersion;
    }

    public boolean isShowConfig() {
        return showConfig;
    }

    protected boolean isPatchUpdate() {
        return newVersion.isPatchUpdate(currentVersion, currentPatch);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private boolean isNewVersionAvailable() {
        return newVersion.isNewerThan(currentVersion, currentPatch);
    }

    @NotNull
    @Override
    protected Control createDialogArea(@NotNull Composite parent)
    {
        getShell().setText(CoreApplicationMessages.dialog_version_update_title);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));
        Composite propGroup = UIUtils.createTitledComposite(
            composite,
            CoreApplicationMessages.dialog_version_update_title,
            2,
            GridData.FILL_BOTH
        );

        createTopArea(composite);

        final Label titleLabel = new Label(propGroup, SWT.NONE);
        titleLabel.setText(
            NLS.bind(!isNewVersionAvailable() ? CoreApplicationMessages.dialog_version_update_no_new_version : CoreApplicationMessages.dialog_version_update_available_new_version, GeneralUtils.getProductName()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        titleLabel.setLayoutData(gd);
        titleLabel.setFont(BaseThemeSettings.instance.baseFontBold);

        UIUtils.createControlLabel(propGroup, CoreApplicationMessages.dialog_version_update_current_version);
        new Label(propGroup, SWT.NONE)
            .setText(currentVersion + (currentPatch > 0
                ? " (" + NLS.bind(CoreApplicationMessages.dialog_about_label_patch, currentPatch) + ")" : ""));

        UIUtils.createControlLabel(propGroup, CoreApplicationMessages.dialog_version_update_new_version);
        new Label(propGroup, SWT.NONE)
            .setText((newVersion.getPatchNumber() > 0 ? newVersion.getUpdateVersion() : newVersion.getProgramVersion().toString())
                + "    (" + CommonUtils.notEmpty(newVersion.getPatchUpdateTime()) + ")");

        if (isNewVersionAvailable()) {
            PatchSeverity severity = newVersion.getMaxSeverity(currentVersion, currentPatch);
            if (severity != PatchSeverity.NONE) {
                Label severityLabel = new Label(propGroup, SWT.NONE);
                severityLabel.setText(NLS.bind(
                    CoreApplicationMessages.dialog_version_update_severity, VersionUpdatePresentation.getSeverityLabel(severity)));
                severityLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
            }
            final Label notesLabel = UIUtils.createControlLabel(propGroup, CoreApplicationMessages.dialog_version_update_notes);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesLabel.setLayoutData(gd);

            final Text notesText = new Text(propGroup, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
            notesText.setText(VersionUpdatePresentation.getReleaseNotes(newVersion, currentVersion, currentPatch));
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            gd.heightHint = CommonUtils.clamp(notesText.getLineCount(), 5, 20) * notesText.getLineHeight();
            notesText.setLayoutData(gd);

            final Label hintLabel = new Label(propGroup, SWT.NONE);
            hintLabel.setText(NLS.bind(
                CoreApplicationMessages.dialog_version_update_press_more_info,
                CoreApplicationMessages.dialog_version_update_button_more_info,
                newVersion.getUpdateVersion()));
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            hintLabel.setLayoutData(gd);
            hintLabel.setFont(BaseThemeSettings.instance.baseFontBold);
        }

        createBottomArea(composite);

        return parent;
    }

    protected void createTopArea(Composite composite) {

    }

    protected void createBottomArea(Composite composite) {

    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        if (showConfig && isNewVersionAvailable()) {
            ((GridLayout) parent.getLayout()).numColumns++;
            dontShowAgainCheck = UIUtils.createCheckbox(parent,
                NLS.bind(CoreApplicationMessages.dialog_version_update_ignore_version, newVersion.getUpdateVersion()), false);
        }

        if (isNewVersionAvailable()) {
            Button upgradeButton = createButton(
                parent,
                UPGRADE_ID,
                CoreApplicationMessages.dialog_version_update_button_upgrade,
                true);
            PatchSeverity severity = newVersion.getMaxSeverity(currentVersion, currentPatch);
            if (severity != PatchSeverity.NONE) {
                // Native buttons may ignore custom colors; the icon and text also communicate severity.
                upgradeButton.setImage(DBeaverIcons.getImage(VersionUpdatePresentation.getSeverityIcon(severity)));
                upgradeButton.setToolTipText(NLS.bind(
                    CoreApplicationMessages.dialog_version_update_severity, VersionUpdatePresentation.getSeverityLabel(severity)));
                upgradeButton.setForeground(switch (severity) {
                    case CRITICAL -> BaseThemeSettings.instance.colorErrorForeground;
                    case HIGH -> BaseThemeSettings.instance.colorWarningForeground;
                    default -> BaseThemeSettings.instance.colorAccent;
                });
            }
        } else {
            if (!CommonUtils.isEmpty(earlyAccessURL)) {
                createButton(
                    parent,
                    CHECK_EA_ID,
                    CoreApplicationMessages.dialog_version_update_button_early_access,
                    false);
            }
        }
        createButton(
            parent,
            INFO_ID,
            CoreApplicationMessages.dialog_version_update_button_more_info,
            false);

        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
                !isNewVersionAvailable());
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (dontShowAgainCheck != null && dontShowAgainCheck.getSelection()) {
            CoreApplicationActivator.getDefault().getPreferenceStore().setValue(DBeaverVersionChecker.getSuppressionKey(newVersion), true);
        }
        if (buttonId == INFO_ID) {
            ShellUtils.launchProgram(newVersion.getBaseURL());
        } else if (buttonId == UPGRADE_ID) {
            performUpdate(currentVersion, newVersion);
        } else if (buttonId == CHECK_EA_ID) {
            if (!CommonUtils.isEmpty(earlyAccessURL)) {
                ShellUtils.launchProgram(earlyAccessURL);
            }
        } else if (buttonId == IDialogConstants.PROCEED_ID) {
            final IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
            CheckForUpdateAction.activateStandardHandler(window);
            try {
                ActionUtils.runCommand(CheckForUpdateAction.P2_UPDATE_COMMAND, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            } finally {
                CheckForUpdateAction.deactivateStandardHandler(window);
            }
        }
        close();
    }

    public static void performUpdate(@NotNull VersionDescriptor version) {
        performUpdate(GeneralUtils.getProductVersion(), version);
    }

    static void performUpdate(@NotNull Version currentVersion, @NotNull VersionDescriptor newVersion) {
        VersionUpdateHandler.startUpdate(currentVersion, newVersion);
    }

    static boolean performUpdate(@NotNull VersionDescriptor version, @Nullable Consumer<IStatus> completion) {
        var app = (DBeaverApplication) DBWorkbench.getPlatform().getApplication();
        var installer = getPlatformInstaller();
        var downloadUrl = app.getLatestVersionDownloadUrl(version);
        if (installer != null && downloadUrl != null) {
            scheduleDownloadAndInstall(installer, downloadUrl, completion);
            return true;
        } else {
            ShellUtils.launchProgram(getDownloadPageURL(version));
            return false;
        }
    }

    private static void scheduleDownloadAndInstall(
        @NotNull PlatformInstaller installer,
        @NotNull URI downloadUrl,
        @Nullable Consumer<IStatus> completion
    ) {
        final AbstractJob job = new AbstractJob("Downloading installation file") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                Path folder;
                Path file;

                try {
                    Path globalTempFolder = DBWorkbench.getPlatform().getTempFolder(monitor, UPDATES_FOLDER).getParent();
                    if (globalTempFolder == null) {
                        throw new IOException("Could not resolve the parent directory of the DBeaver temporary folder");
                    }

                    var updatesFolder = globalTempFolder.resolveSibling(UPDATES_FOLDER);
                    Files.createDirectories(updatesFolder);
                    if (installer instanceof WinZipInstaller) {
                        // Concurrent downloads must not overwrite the archive that another helper is preparing.
                        folder = Files.createTempDirectory(updatesFolder, "zip-");
                    } else {
                        folder = updatesFolder;
                    }

                    var url = downloadUrl.toString();
                    var filename = url.substring(url.lastIndexOf('/') + 1);
                    file = folder.resolve(filename);

                    log.debug("Downloading installation file to " + file);
                    try {
                        WebUtils.downloadRemoteFile(monitor, "Obtaining installer", url, file, null);
                    } catch (InterruptedException e) {
                        log.debug("Canceled by user", e);
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException deleteError) {
                            log.warn("Could not delete partially downloaded installation file '" + file + "'", deleteError);
                        }
                        return Status.CANCEL_STATUS;
                    }
                } catch (IOException e) {
                    return GeneralUtils.makeErrorStatus(CoreApplicationMessages.dialog_version_update_downloader_error_cannot_download, e);
                }

                if (UIUtils.confirmAction(
                    CoreApplicationMessages.dialog_version_update_downloader_title,
                    NLS.bind(CoreApplicationMessages.dialog_version_update_downloader_confirm_install, GeneralUtils.getProductName())
                )) {
                    try {
                        monitor.subTask(CoreApplicationMessages.version_update_prepare);
                        installer.prepare(file, monitor);
                    } catch (InterruptedException e) {
                        return Status.CANCEL_STATUS;
                    } catch (Exception e) {
                        return GeneralUtils.makeErrorStatus(CoreApplicationMessages.version_update_error_prepare, e);
                    }
                    var workbench = PlatformUI.getWorkbench();
                    var workbenchWindow = UIUtils.getActiveWorkbenchWindow();

                    // Arm shutdown listener now because later will be too late
                    var listener = new IWorkbenchListener() {
                        {
                            workbench.addWorkbenchListener(this);
                        }

                        @Override
                        public boolean preShutdown(IWorkbench workbench, boolean forced) {
                            return true;
                        }

                        @Override
                        public void postShutdown(IWorkbench workbench) {
                            try {
                                installer.run(file, log);
                            } catch (Exception e) {
                                log.error("Failed to run the installer script", e);
                            }
                        }
                    };

                    UIUtils.asyncExec(() -> {
                        ActionUtils.runCommand(IWorkbenchCommandConstants.FILE_EXIT, workbenchWindow);

                        if (!workbench.isClosing()) {
                            workbench.removeWorkbenchListener(listener);
                            ShellUtils.launchProgram(folder.toString());
                        }
                    });
                } else {
                    ShellUtils.showInSystemExplorer(file.toAbsolutePath().toString());
                }

                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        if (completion != null) {
            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(@NotNull IJobChangeEvent event) {
                    completion.accept(job.isCanceled() ? Status.CANCEL_STATUS : event.getResult());
                }
            });
        }
        job.schedule();
    }

    @Nullable
    private static PlatformInstaller getPlatformInstaller() {
        var dist = System.getProperty(DBeaverApplication.PROP_DISTRIBUTION_TYPE);
        return switch (Platform.getOS()) {
            case Platform.OS_WIN32 -> switch (dist) {
                case "zip" -> new WinZipInstaller();
                case null, default -> new WinExeInstaller();
            };
            case Platform.OS_MACOSX -> new MacDmgInstaller();
            default -> null;
        };
    }

    @NotNull
    private static String getDownloadPageURL(@NotNull VersionDescriptor version) {
        String os;
        if (RuntimeUtils.isWindows()) {
            os = OS_WINDOWS;
        } else if (RuntimeUtils.isMacOS()) {
            os = OS_MACOS;
        } else {
            os = OS_LINUX;
        }
        String dist = System.getProperty(DBeaverApplication.PROP_DISTRIBUTION_TYPE);
        if (CommonUtils.isEmpty(dist) && RuntimeUtils.isLinux()) {
            // If distribution type was not set explicitly, then let's attempt a dumb guess.
            try {
                RuntimeUtils.executeProcess("/usr/bin/apt-get", "--version");
                dist = DISTRIBUTION_DEB;
            } catch (DBException e) {
                dist = DISTRIBUTION_RPM;
            }
        }
        return CommonUtils.removeTrailingSlash(version.getBaseURL()) + "?start" +
            "&os=" + os +
            "&arch=" + Platform.getOSArch() +
            (dist == null ? "" : "&dist=" + dist);
    }
}
