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
package org.jkiss.dbeaver.ui.controls.autorefresh;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.ui.UIUtils;

public class RefreshSettings {
    private static IDialogSettings viewerSettings;

    private final String settingsId;
    private int refreshInterval = 0;
    private boolean stopOnError = true;

    RefreshSettings(String settingsId) {
        this.settingsId = settingsId;
    }

    RefreshSettings(RefreshSettings src) {
        this.settingsId = src.settingsId;
        this.refreshInterval = src.refreshInterval;
        this.stopOnError = src.stopOnError;
    }

    int getRefreshInterval() {
        return refreshInterval;
    }

    void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    boolean isStopOnError() {
        return stopOnError;
    }

    void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void loadSettings() {
        IDialogSettings viewerSettings = getViewerSettings(settingsId);
        if (viewerSettings.get("interval") != null) refreshInterval = viewerSettings.getInt("interval");
        if (viewerSettings.get("stopOnError") != null) stopOnError = viewerSettings.getBoolean("stopOnError");
    }

    public void saveSettings() {
        IDialogSettings viewerSettings = getViewerSettings(settingsId);
        viewerSettings.put("interval", refreshInterval);
        viewerSettings.put("stopOnError", stopOnError);
    }

    private static IDialogSettings getViewerSettings(String section) {
        if (viewerSettings == null) {
            viewerSettings = UIUtils.getDialogSettings("DBeaver.AutoRefresh");
        }
        return UIUtils.getSettingsSection(viewerSettings, section);
    }

}
