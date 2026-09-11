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
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.DBeaverApplication;
import org.osgi.framework.Version;

import java.util.Map;

public class VersionUpdateHandler extends AbstractHandler implements IElementUpdater {
    private static final int ANIMATION_DELAY = 100;

    public static final String COMMAND_UPDATE = "org.jkiss.dbeaver.ui.versionUpdate";
    public static final String COMMAND_RELEASE_NOTES = "org.jkiss.dbeaver.ui.versionUpdate.releaseNotes";
    public static final String PROPERTY_AVAILABLE = "org.jkiss.dbeaver.ui.versionUpdate.available";

    private static Version currentVersion;
    private static VersionDescriptor newVersion;
    private static boolean updateStarted;
    private static boolean downloading;
    private static int animationFrame;

    static void showNotification(@NotNull Version current, @NotNull VersionDescriptor available) {
        currentVersion = current;
        newVersion = available;
        ActionUtils.evaluatePropertyState(PROPERTY_AVAILABLE);
        ActionUtils.fireCommandRefresh(COMMAND_UPDATE);
    }

    static boolean isUpdateAvailable() {
        return newVersion != null;
    }

    @Override
    public boolean isEnabled() {
        return !updateStarted && super.isEnabled();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        VersionDescriptor available = newVersion;
        if (available == null) {
            return null;
        }
        if (COMMAND_RELEASE_NOTES.equals(event.getCommand().getId())) {
            DBeaverApplication.getInstance().notifyVersionUpgrade(currentVersion, available, true);
            if (DBeaverVersionChecker.isSuppressed(available)) {
                newVersion = null;
                currentVersion = null;
                ActionUtils.evaluatePropertyState(PROPERTY_AVAILABLE);
            }
        } else if (!updateStarted) {
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
    public void updateElement(UIElement element, Map parameters) {
        var icon = downloading ? UIIcon.LOADING.get(animationFrame % UIIcon.LOADING.size()) : UIIcon.DOWNLOAD;
        element.setIcon(DBeaverIcons.getImageDescriptor(icon));
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
