/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.impl.app.ApplicationDescriptor;
import org.jkiss.dbeaver.model.impl.app.ApplicationRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VersionUpdateDialog extends Dialog {

    private static final Log log = Log.getLog(VersionUpdateDialog.class);

    private static final int INFO_ID = 1000;
    private static final int UPGRADE_ID = 1001;
    private static final int CHECK_EA_ID = 1002;

    private final Version currentVersion;
    private final VersionDescriptor newVersion;

    private Font boldFont;
    private boolean showConfig;
    private Button dontShowAgainCheck;
    private final String earlyAccessURL;

    public VersionUpdateDialog(Shell parentShell, @NotNull Version currentVersion, @NotNull VersionDescriptor newVersion, boolean showConfig)
    {
        super(parentShell);
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.showConfig = showConfig;

        earlyAccessURL = Platform.getProduct().getProperty("earlyAccessURL");
    }

    @NotNull
    public VersionDescriptor getNewVersion() {
        return newVersion;
    }

    public boolean isShowConfig() {
        return showConfig;
    }

    public Font getBoldFont() {
        return boldFont;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private boolean isNewVersionAvailable() {
        return newVersion.getProgramVersion().compareTo(currentVersion) > 0;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_version_update_title);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));
        Composite propGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_version_update_title, 2, GridData.FILL_BOTH, 0);

        createTopArea(composite);

        boldFont = UIUtils.makeBoldFont(composite.getFont());

        final Label titleLabel = new Label(propGroup, SWT.NONE);
        titleLabel.setText(
            NLS.bind(!isNewVersionAvailable() ? CoreMessages.dialog_version_update_no_new_version : CoreMessages.dialog_version_update_available_new_version, GeneralUtils.getProductName()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        titleLabel.setLayoutData(gd);
        titleLabel.setFont(boldFont);

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_current_version);
        new Label(propGroup, SWT.NONE)
            .setText(currentVersion.toString());

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_new_version);
        new Label(propGroup, SWT.NONE)
            .setText(newVersion.getProgramVersion().toString() + "    (" + newVersion.getUpdateTime() + ")"); //$NON-NLS-2$ //$NON-NLS-3$

        if (isNewVersionAvailable()) {
            final Label notesLabel = UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_notes);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesLabel.setLayoutData(gd);

            final Text notesText = new Text(propGroup, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
            String releaseNotes = CommonUtils.notEmpty(newVersion.getReleaseNotes());
            if (releaseNotes.isEmpty()) {
                releaseNotes = CoreMessages.dialog_version_update_no_notes;
            }
            releaseNotes = formatReleaseNotes(releaseNotes);

            notesText.setText(releaseNotes);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            //gd.heightHint = notesText.getLineHeight() * 20;
            notesText.setLayoutData(gd);

            final Label hintLabel = new Label(propGroup, SWT.NONE);
            hintLabel.setText(NLS.bind(
                CoreMessages.dialog_version_update_press_more_info,
                CoreMessages.dialog_version_update_button_more_info,
                newVersion.getPlainVersion()));
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            hintLabel.setLayoutData(gd);
            hintLabel.setFont(boldFont);
        }

        createBottomArea(composite);

        return parent;
    }

    private static String formatReleaseNotes(String releaseNotes) {
        while (releaseNotes.startsWith("\n")) {
            releaseNotes = releaseNotes.substring(1);
        }
        String[] rnLines = releaseNotes.split("\n");
        int leadSpacesNum = 0;
        for (int i = 0; i < rnLines[0].length(); i++) {
            if (rnLines[0].charAt(i) == ' ') {
                leadSpacesNum++;
            } else {
                break;
            }
        }
        StringBuilder result = new StringBuilder();
        for (String rnLine : rnLines) {
            if (rnLine.length() > leadSpacesNum) {
                if (result.length() > 0) result.append("\n");
                result.append(rnLine.substring(leadSpacesNum));
            }
        }

        return result.toString();
    }

    protected void createTopArea(Composite composite) {

    }

    protected void createBottomArea(Composite composite) {

    }

    @Override
    public boolean close()
    {
        boldFont.dispose();
        return super.close();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (showConfig && isNewVersionAvailable()) {
            ((GridLayout) parent.getLayout()).numColumns++;
            dontShowAgainCheck = UIUtils.createCheckbox(parent, NLS.bind(CoreMessages.dialog_version_update_ignore_version, newVersion.getPlainVersion()), false);
        }

        if (isNewVersionAvailable()) {
            createButton(
                parent,
                UPGRADE_ID,
                CoreMessages.dialog_version_update_button_upgrade,
                true);
        } else {
            if (!CommonUtils.isEmpty(earlyAccessURL)) {
                createButton(
                    parent,
                    CHECK_EA_ID,
                    CoreMessages.dialog_version_update_button_early_access,
                    false);
            }
        }
        createButton(
            parent,
            INFO_ID,
            CoreMessages.dialog_version_update_button_more_info,
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
            CoreApplicationActivator.getDefault().getPreferenceStore().setValue("suppressUpdateCheck." + newVersion.getPlainVersion(), true);
        }
        if (buttonId == INFO_ID) {
            ShellUtils.launchProgram(newVersion.getBaseURL());
        } else if (buttonId == UPGRADE_ID) {
            final PlatformInstaller installer = getPlatformInstaller();
            if (installer != null) {
                final AbstractJob job = new AbstractJob("Downloading installation file") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        final ApplicationDescriptor app = ApplicationRegistry.getInstance().getApplication();
                        final Path folder;
                        final Path file;

                        try {
                            final String executableName = installer.getExecutableName(app);
                            final String executableExtension = installer.getExecutableExtension();

                            folder = Files.createTempDirectory(executableName);
                            file = Files.createFile(folder.resolve(executableName + '.' + executableExtension));

                            log.debug("Downloading installation file to " + file);
                            WebUtils.downloadRemoteFile(monitor, "Obtaining installer", getDownloadURL(app, installer, newVersion), file.toFile(), null);
                        } catch (IOException e) {
                            return GeneralUtils.makeErrorStatus(CoreMessages.dialog_version_update_downloader_error_cannot_download, e);
                        } catch (InterruptedException e) {
                            log.debug("Canceled by user", e);
                            return Status.OK_STATUS;
                        }

                        if (UIUtils.confirmAction(CoreMessages.dialog_version_update_downloader_title, NLS.bind(CoreMessages.dialog_version_update_downloader_confirm_install, app.getName()))) {
                            final IWorkbench workbench = PlatformUI.getWorkbench();
                            final IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();

                            // Arm shutdown listener now because later will be too late
                            final IWorkbenchListener listener = new IWorkbenchListener() {
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
                job.schedule();
            } else {
                ShellUtils.launchProgram(getDownloadPageURL(newVersion));
            }
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

    @Nullable
    private PlatformInstaller getPlatformInstaller() {
        switch (Platform.getOS()) {
            case Platform.OS_WIN32:
                return new WindowsInstaller();
            case Platform.OS_MACOSX:
                return new MacintoshInstaller();
            default:
                return null;
        }
    }

    @NotNull
    private String getDownloadURL(@NotNull ApplicationDescriptor application, @NotNull PlatformInstaller installer, @NotNull VersionDescriptor version) {
        final String name = installer.getExecutableName(application);
        final String extension = installer.getExecutableExtension();
        return CommonUtils.removeTrailingSlash(version.getDownloadURL()) + '/' + name + '.' + extension;
    }

    @NotNull
    private String getDownloadPageURL(@NotNull VersionDescriptor version) {
        String os = Platform.getOS();
        switch (os) {
            case "win32": os = "win"; break;
            case "macosx": os = "mac"; break;
            default: os = "linux"; break;
        }
        String arch = Platform.getOSArch();
        String dist = null;
        if (os.equals("linux")) {
            // Determine package manager
            try {
                RuntimeUtils.executeProcess("/usr/bin/apt-get", "--version");
                dist = "deb";
            } catch (DBException e) {
                dist = "rpm";
            }
        }
        return CommonUtils.removeTrailingSlash(version.getBaseURL()) + "?start" +
            "&os=" + os +
            "&arch=" + arch +
            (dist == null ? "" : "&dist=" + dist);
    }

    private interface PlatformInstaller {
        void run(@NotNull Path executable, @NotNull Log log) throws Exception;

        @NotNull
        String getExecutableName(@NotNull ApplicationDescriptor application);

        @NotNull
        String getExecutableExtension();
    }

    private static final class WindowsInstaller implements PlatformInstaller {
        @Override
        public void run(@NotNull Path executable, @NotNull Log log) throws Exception {
            final String path = executable.toString();
            Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/C",
                "start", "/W", path, "&&", "del", path,
            });
        }

        @NotNull
        @Override
        public String getExecutableName(@NotNull ApplicationDescriptor application) {
            return application.getId() + "-latest-x86_64-setup";
        }

        @NotNull
        @Override
        public String getExecutableExtension() {
            return "exe";
        }
    }

    private static final class MacintoshInstaller implements PlatformInstaller {
        @Override
        public void run(@NotNull Path executable, @NotNull Log log) throws Exception {
            final String path = CommonUtils.escapeBourneShellString(executable.toString());
            Runtime.getRuntime().exec(new String[]{
                "/bin/sh", "-c",
                "open -F -W " + path + " && rm " + path
            });
        }

        @NotNull
        @Override
        public String getExecutableName(@NotNull ApplicationDescriptor application) {
            return application.getId() + "-latest-macos";
        }

        @NotNull
        @Override
        public String getExecutableExtension() {
            return "dmg";
        }
    }
}
