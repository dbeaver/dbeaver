/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.application.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

class VersionUpdateDialog extends Dialog {

    private VersionDescriptor newVersion;
    private static final int INFO_ID = 1000;
    private Font boldFont;
    private boolean autoCheck;
    private Button dontShowAgainCheck;

    public VersionUpdateDialog(Shell parentShell, VersionDescriptor newVersion, boolean autoCheck)
    {
        super(parentShell);
        this.newVersion = newVersion;
        this.autoCheck = autoCheck;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_version_update_title);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));
        Composite propGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_version_update_title, 2, GridData.FILL_BOTH, 0);

        boldFont = UIUtils.makeBoldFont(composite.getFont());

        final Label titleLabel = new Label(propGroup, SWT.NONE);
        titleLabel.setText(
            NLS.bind(newVersion == null ? CoreMessages.dialog_version_update_no_new_version : CoreMessages.dialog_version_update_available_new_version, GeneralUtils.getProductName()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        titleLabel.setLayoutData(gd);
        titleLabel.setFont(boldFont);

        final String versionStr = GeneralUtils.getProductVersion().toString();

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_current_version);
        new Label(propGroup, SWT.NONE)
            .setText(versionStr);

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_new_version);
        new Label(propGroup, SWT.NONE)
            .setText(newVersion == null ? versionStr : newVersion.getProgramVersion().toString() + "    (" + newVersion.getUpdateTime() + ")"); //$NON-NLS-2$ //$NON-NLS-3$

        if (newVersion != null) {
            final Label notesLabel = UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_notes);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesLabel.setLayoutData(gd);

            final Text notesText = new Text(propGroup, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
            notesText.setText(newVersion.getReleaseNotes());
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            gd.heightHint = notesText.getLineHeight() * 20;
            notesText.setLayoutData(gd);

            final Label hintLabel = new Label(propGroup, SWT.NONE);
            hintLabel.setText(CoreMessages.dialog_version_update_press_more_info_);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            hintLabel.setLayoutData(gd);
            hintLabel.setFont(boldFont);
        }

        return parent;
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
        if (autoCheck && newVersion != null) {
            ((GridLayout) parent.getLayout()).numColumns++;
            dontShowAgainCheck = UIUtils.createCheckbox(parent, "Don't show for the version " + newVersion.getPlainVersion(), false);
        }

        if (newVersion != null) {
/*
            // Disable P2 update. Doesn't work and can't work properly in most cases.
            boolean hasUpdate = Platform.getBundle(CheckForUpdateAction.P2_PLUGIN_ID) != null;
            if (hasUpdate) {
                createButton(
                    parent,
                    IDialogConstants.PROCEED_ID,
                    "Update",
                    true);
            }
*/
            createButton(
                parent,
                INFO_ID,
                CoreMessages.dialog_version_update_button_more_info,
                true);
        }

        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            newVersion == null);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (dontShowAgainCheck != null && dontShowAgainCheck.getSelection()) {
            CoreApplicationActivator.getDefault().getPreferenceStore().setValue("suppressUpdateCheck." + newVersion.getPlainVersion(), true);
        }
        if (buttonId == INFO_ID) {
            if (newVersion != null) {
                UIUtils.launchProgram(newVersion.getBaseURL());
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

    public static boolean isSuppressed(VersionDescriptor version) {
        return CoreApplicationActivator.getDefault().getPreferenceStore().getBoolean("suppressUpdateCheck." + version.getPlainVersion());
    }

}
