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
package org.jkiss.dbeaver.ui.macos;

import org.eclipse.swt.internal.cocoa.NSString;
import org.eclipse.swt.internal.cocoa.NSUserDefaults;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.utils.PrefUtils;

public class CocoaUIService implements IPluginService {

    public static final String PREF_TOOLTIP_DELAY = "macos.tooltip.delay";
    public static final String PREF_TOOLTIP_DELAY_ENABLED = "macos.tooltip.delay.enabled";

    public static final int DEFAULT_TOOLTIP_DELAY = 300;

    public static void updateTooltipDefaults() {
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        boolean isDelaySetEnabled = store.getBoolean(CocoaUIService.PREF_TOOLTIP_DELAY_ENABLED);

        NSUserDefaults nsUserDefaults = NSUserDefaults.standardUserDefaults();
        if (isDelaySetEnabled) {
            int tooltipDelay = store.getInt(CocoaUIService.PREF_TOOLTIP_DELAY);
            if (tooltipDelay <= 0) {
                tooltipDelay = 0;
            }
            nsUserDefaults.setInteger(tooltipDelay, NSString.stringWith("NSInitialToolTipDelay"));
        } else {
            nsUserDefaults.setValue(null, NSString.stringWith("NSInitialToolTipDelay"));
        }
    }

    @Override
    public void activateService() {
        // Init default preferences
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        PrefUtils.setDefaultPreferenceValue(store, CocoaUIService.PREF_TOOLTIP_DELAY_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, CocoaUIService.PREF_TOOLTIP_DELAY, CocoaUIService.DEFAULT_TOOLTIP_DELAY);

        updateTooltipDefaults();
    }

    @Override
    public void deactivateService() {

    }
}
