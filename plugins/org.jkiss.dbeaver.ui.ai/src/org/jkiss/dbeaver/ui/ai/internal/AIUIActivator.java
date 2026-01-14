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
package org.jkiss.dbeaver.ui.ai.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ai.preferences.AIPreferencePageMain;
import org.jkiss.dbeaver.ui.preferences.PrefPageConstants;
import org.jkiss.dbeaver.ui.workbench.WorkbenchUtils;
import org.osgi.framework.BundleContext;

public class AIUIActivator extends AbstractUIPlugin {

    private static AIUIActivator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        DBRFeatureRegistry.getInstance().registerFeatures(AIFeatures.class);
        // Trigger pref defaults
        new BundlePreferenceStore(getBundle());

        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            WorkbenchUtils.removePreferencePages(PrefPageConstants.WORKBENCH_PREF_PAGE_ID + "/" + AIPreferencePageMain.PAGE_ID);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static AIUIActivator getDefault() {
        return plugin;
    }

}
