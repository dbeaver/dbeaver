/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.osgi.framework.BundleContext;

public class AIActivator extends Plugin {
    
    private static AIActivator instance;
    
    private BundlePreferenceStore preferences;

    public AIActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        instance = this;
        preferences = new BundlePreferenceStore(getBundle());
        
        DBRFeatureRegistry.getInstance().registerFeatures(AIBaseFeatures.class);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        
        instance = null;
        preferences = null;
    }

    public static AIActivator getInstance() {
        return instance;
    }

    public BundlePreferenceStore getPreferences() {
        return preferences;
    }
}
