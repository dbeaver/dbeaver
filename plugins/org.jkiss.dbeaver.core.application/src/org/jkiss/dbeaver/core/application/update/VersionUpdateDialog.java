/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application.update;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.application.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

public class VersionUpdateDialog extends Dialog {

    private static final int INFO_ID = 1000;
    private static final int UPGRADE_ID = 1001;
    private static final int CHECK_EA_ID = 1002;

    private VersionDescriptor currentVersion;
    @Nullable
    private VersionDescriptor newVersion;

    private Font boldFont;
    private boolean showConfig;
    private Button dontShowAgainCheck;
    private final String earlyAccessURL;

    public VersionUpdateDialog(Shell parentShell, VersionDescriptor currentVersion, @Nullable VersionDescriptor newVersion, boolean showConfig)
    {
        super(parentShell);
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.showConfig = showConfig;

        earlyAccessURL = Platform.getProduct().getProperty("earlyAccessURL");
    }

    public Version getCurrentVersion() {
        return GeneralUtils.getProductVersion();
    }

    @Nullable
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
        return newVersion != null && newVersion.getProgramVersion().compareTo(getCurrentVersion()) > 0;
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

        final String versionStr = getCurrentVersion().toString();

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_current_version);
        new Label(propGroup, SWT.NONE)
            .setText(versionStr);

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_new_version);
        new Label(propGroup, SWT.NONE)
            .setText(newVersion == null ? versionStr : newVersion.getProgramVersion().toString() + "    (" + newVersion.getUpdateTime() + ")"); //$NON-NLS-2$ //$NON-NLS-3$

        if (isNewVersionAvailable()) {
            final Label notesLabel = UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_notes);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesLabel.setLayoutData(gd);

            final Text notesText = new Text(propGroup, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
            String releaseNotes = CommonUtils.notEmpty(newVersion.getReleaseNotes());
            if (releaseNotes.isEmpty()) {
                releaseNotes = "No release notes";
            }
            releaseNotes = formatReleaseNotes(releaseNotes);

            notesText.setText(releaseNotes);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            //gd.heightHint = notesText.getLineHeight() * 20;
            notesText.setLayoutData(gd);

            final Label hintLabel = new Label(propGroup, SWT.NONE);
            hintLabel.setText(NLS.bind(
                CoreMessages.dialog_version_update_press_more_info_,
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
            dontShowAgainCheck = UIUtils.createCheckbox(parent, "Don't show for the version " + newVersion.getPlainVersion(), false);
        }

        if (isNewVersionAvailable()) {
            createButton(
                parent,
                UPGRADE_ID,
                "Upgrade ...",
                true);
        } else {
            if (!CommonUtils.isEmpty(earlyAccessURL)) {
                createButton(
                    parent,
                    CHECK_EA_ID,
                    "Early Access",
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
        if (dontShowAgainCheck != null && dontShowAgainCheck.getSelection() && newVersion != null) {
            CoreApplicationActivator.getDefault().getPreferenceStore().setValue("suppressUpdateCheck." + newVersion.getPlainVersion(), true);
        }
        if (buttonId == INFO_ID) {
            if (newVersion != null) {
                UIUtils.launchProgram(newVersion.getBaseURL());
            } else if (currentVersion != null) {
                UIUtils.launchProgram(currentVersion.getBaseURL());
            }
        } else if (buttonId == UPGRADE_ID) {
            if (newVersion != null) {
                UIUtils.launchProgram(makeDownloadURL(newVersion.getBaseURL()));
            }
        } else if (buttonId == CHECK_EA_ID) {
            if (!CommonUtils.isEmpty(earlyAccessURL)) {
                UIUtils.launchProgram(earlyAccessURL);
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

    private String makeDownloadURL(String baseURL) {
        while (baseURL.endsWith("/")) baseURL = baseURL.substring(0, baseURL.length() - 1);
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
        return baseURL + "?start" +
            "&os=" + os +
            "&arch=" + arch +
            (dist == null ? "" : "&dist=" + dist);
    }

    public static boolean isSuppressed(VersionDescriptor version) {
        CoreApplicationActivator activator = CoreApplicationActivator.getDefault();
        return activator != null && activator.getPreferenceStore().getBoolean("suppressUpdateCheck." + version.getPlainVersion());
    }

}
