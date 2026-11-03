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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.osgi.framework.BundleContext;

public class UINavigatorActivator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ui.navigator";

    public UINavigatorActivator() {
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);
        new BundlePreferenceStore(getBundle());
    }
}
