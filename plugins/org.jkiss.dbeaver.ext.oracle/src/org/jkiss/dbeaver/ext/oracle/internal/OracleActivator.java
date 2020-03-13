/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.internal;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class OracleActivator extends Plugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.oracle";

    // The shared instance
    private static OracleActivator plugin;
    private BundlePreferenceStore preferenceStore;

    /**
     * The constructor
     */
    public OracleActivator() {
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(), OracleConstants.PREF_SUPPORT_ROWID, true);
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(), OracleConstants.PREF_DBMS_OUTPUT, true);
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(), OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS, true);
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(), OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING, true);
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static OracleActivator getDefault() {
        return plugin;
    }

    public DBPPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        if (preferenceStore == null) {
            preferenceStore = new BundlePreferenceStore(getBundle());
        }
        return preferenceStore;
    }

}
