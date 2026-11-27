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
package org.jkiss.dbeaver.ext.frostlake.internal;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.osgi.framework.BundleContext;

/**
 * Frostlake plugin activator.
 */
public class FrostlakeActivator extends Plugin {

    private static FrostlakeActivator instance;

    public FrostlakeActivator() {
        // default constructor
    }

    @Nullable
    public static FrostlakeActivator getDefault() {
        return instance;
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        instance = null;
        super.stop(context);
    }
}
