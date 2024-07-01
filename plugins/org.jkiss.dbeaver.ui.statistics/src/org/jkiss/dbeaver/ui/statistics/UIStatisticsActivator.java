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
package org.jkiss.dbeaver.ui.statistics;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.osgi.framework.BundleContext;

import java.io.IOException;

public class UIStatisticsActivator extends AbstractUIPlugin {

    private static final Log log = Log.getLog(UIStatisticsActivator.class);

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ui.statistics";

    public static final String PREF_FEATURE_TRACKING_ENABLED = "feature.tracking.enabled";
    public static final String PREF_SKIP_DATA_SHARE_CONFIRMATION = "feature.tracking.skipConfirmation";

    // The shared instance
    private static UIStatisticsActivator plugin;
    private DBPPreferenceStore preferences;

    public UIStatisticsActivator() {
    }

    public static boolean isTrackingEnabled() {
        return DBWorkbench.getPlatform().getApplication().isStatisticsCollectionRequired()
            || getDefault().getPreferences().getBoolean(PREF_FEATURE_TRACKING_ENABLED);
    }

    public static void setTrackingEnabled(boolean enabled) {
        if (enabled == isTrackingEnabled()) {
            return;
        }
        setPreferenceValue(PREF_FEATURE_TRACKING_ENABLED, enabled);
    }

    public static boolean isSkipDataShareConfirmation() {
        return getDefault().getPreferences().getBoolean(PREF_SKIP_DATA_SHARE_CONFIRMATION);
    }

    public static void setSkipDataShareConfirmation(boolean skip) {
        setPreferenceValue(PREF_SKIP_DATA_SHARE_CONFIRMATION, skip);
    }

    private static void setPreferenceValue(String key, boolean value) {
        DBPPreferenceStore preferenceStore = getDefault().getPreferences();
        preferenceStore.setValue(key, value);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        preferences = new BundlePreferenceStore(getBundle());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static UIStatisticsActivator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }
}

