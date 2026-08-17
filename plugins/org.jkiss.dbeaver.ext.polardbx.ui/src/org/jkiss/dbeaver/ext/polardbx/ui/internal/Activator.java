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
package org.jkiss.dbeaver.ext.polardbx.ui.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    private static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.polardbx.ui";

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        super.stop(context);
    }

    @Nullable
    public static ImageDescriptor getImageDescriptor(@NotNull String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
