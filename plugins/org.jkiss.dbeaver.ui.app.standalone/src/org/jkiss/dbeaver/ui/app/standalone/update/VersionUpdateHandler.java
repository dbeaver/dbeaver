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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor.PatchSeverity;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationActivator;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.osgi.framework.Version;

import java.util.Map;

public class VersionUpdateHandler extends AbstractHandler implements IElementUpdater {
    public static class ReleaseNotes extends VersionUpdateHandler {
        @Override
        public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
            element.setIcon(DBeaverIcons.getImageDescriptor(UIIcon.FILE));
        }
    }

    private static final int ANIMATION_DELAY = 100;

    public static final String COMMAND_UPDATE = "org.jkiss.dbeaver.ui.versionUpdate";
    private static final String COMMAND_INSTALL = "org.jkiss.dbeaver.ui.versionUpdate.install";
    public static final String COMMAND_RELEASE_NOTES = "org.jkiss.dbeaver.ui.versionUpdate.releaseNotes";
    public static final String PROPERTY_AVAILABLE = "org.jkiss.dbeaver.ui.versionUpdate.available";

    private static Version currentVersion;
    private static int currentPatch;
    private static VersionDescriptor newVersion;
    private static boolean updateStarted;
    private static boolean downloading;
    private static int animationFrame;

    static void showNotification(@NotNull Version current, @NotNull VersionDescriptor available) {
        currentVersion = current;
        currentPatch = DBeaverVersionChecker.getProductPatchNumber();
        newVersion = available;
        ActionUtils.evaluatePropertyState(PROPERTY_AVAILABLE);
        ActionUtils.fireCommandRefresh(COMMAND_UPDATE);
    }

    static boolean isUpdateAvailable() {
        return currentVersion != null && newVersion != null;
    }

    static boolean isMajorUpdate() {
        return newVersion != null && newVersion.isMajorUpdate();
    }

    static boolean isCriticalUpdate() {
        return currentVersion != null && newVersion != null && newVersion.isNewerThan(currentVersion, currentPatch)
            && getUpdateSeverity() == PatchSeverity.CRITICAL;
    }

    @NotNull
    private static PatchSeverity getUpdateSeverity() {
        return currentVersion == null || newVersion == null
            ? PatchSeverity.NONE
            : newVersion.getUpdateSeverity(currentVersion, currentPatch);
    }

    static void startUpdate(@NotNull Version current, @NotNull VersionDescriptor available) {
        UIUtils.runInUIThread(() -> {
            if (!updateStarted) {
                currentVersion = current;
                currentPatch = DBeaverVersionChecker.getProductPatchNumber();
                newVersion = available;
                // Dialog confirmation must install directly, even when the toolbar opens a critical notification.
                ActionUtils.runCommand(COMMAND_INSTALL, PlatformUI.getWorkbench());
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return !updateStarted && super.isEnabled();
    }

    @Override
    @Nullable
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        Version current = currentVersion;
        VersionDescriptor available = newVersion;
        if (current == null || available == null || updateStarted) {
            return null;
        }
        String commandId = event.getCommand().getId();
        if (COMMAND_RELEASE_NOTES.equals(commandId) || (COMMAND_UPDATE.equals(commandId) && isCriticalUpdate())) {
            DBeaverApplication.getInstance().notifyVersionUpgrade(current, available, true);
            if (DBeaverVersionChecker.isSuppressed(available)) {
                newVersion = null;
                currentVersion = null;
                ActionUtils.evaluatePropertyState(PROPERTY_AVAILABLE);
            }
        } else {
            updateStarted = true;
            downloading = true;
            setBaseEnabled(false);
            refreshUpdateButton();
            animateDownload();
            if (!VersionUpdateDialog.performUpdate(available, this::downloadFinished)) {
                downloading = false;
                updateStarted = false;
                setBaseEnabled(true);
                refreshUpdateButton();
            }
        }
        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
        var severity = getUpdateSeverity();
        var icon = downloading
            ? UIIcon.LOADING.get(animationFrame % UIIcon.LOADING.size())
            : VersionUpdatePresentation.getSeverityIcon(severity);
        element.setIcon(DBeaverIcons.getImageDescriptor(icon));
        element.setText(isCriticalUpdate()
            ? CoreApplicationMessages.dialog_version_update_critical_title
            : Platform.getResourceBundle(CoreApplicationActivator.getDefault().getBundle())
                .getString("command." + COMMAND_UPDATE + ".name"));
        String tooltip = isCriticalUpdate()
            ? CoreApplicationMessages.dialog_version_update_critical_title
            : CoreApplicationMessages.dialog_version_update_title;
        if (isUpdateAvailable() && isMajorUpdate()) {
            tooltip += "\n" + CoreApplicationMessages.dialog_version_update_major_version;
        }
        element.setTooltip(tooltip);
    }

    private void downloadFinished(@NotNull IStatus status) {
        UIUtils.asyncExec(() -> {
            downloading = false;
            if (!status.isOK()) {
                updateStarted = false;
                setBaseEnabled(true);
            }
            refreshUpdateButton();
        });
    }

    private static void animateDownload() {
        UIUtils.timerExec(ANIMATION_DELAY, () -> {
            if (downloading) {
                animationFrame++;
                refreshUpdateButton();
                animateDownload();
            }
        });
    }

    private static void refreshUpdateButton() {
        ActionUtils.fireCommandRefresh(COMMAND_UPDATE);
    }
}
